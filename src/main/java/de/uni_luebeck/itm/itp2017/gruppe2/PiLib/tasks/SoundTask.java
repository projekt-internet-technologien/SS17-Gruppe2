package de.uni_luebeck.itm.itp2017.gruppe2.PiLib.tasks;

import java.io.FileInputStream;
import java.util.LinkedList;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.pojo.InnerResults;
import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.pojo.SSPRestResult;
import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.service.SparqlParser;
import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.util.Configuration;
import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.util.RestClient;
import javazoom.jl.player.advanced.AdvancedPlayer;

/**
 * This Task asks the SSP for the current user and temperature to play the right
 * music.
 * 
 * @author drickert
 *
 */
public class SoundTask implements ITask {

	private static final double BORDER_TEMPERATURE = 10.0;
	private Temperature currentTemperature = Temperature.Cold;
	private String SSP_HOST;
	private Thread soundThread;
	private String lastUser = "";

	@Override
	public void run(Configuration config) throws Throwable {
		// get the ssp-host from configuration
		SSP_HOST = config.getSSP_HOST();

		findFaces();
		findTemprature();
	}

	/**
	 * Possible Temperatures
	 * 
	 * @author drickert
	 *
	 */
	private enum Temperature {
		Warm("warm"), Cold("cold");
		private String tempName;

		Temperature(String tempName) {
			this.tempName = tempName;
		}

		String getTempName() {
			return tempName;
		}
	};

	/**
	 * Starts a Thread that fetches periodic the current Temperature
	 */
	private void findTemprature() {
		String sparql = "PREFIX gruppe2: <http://gruppe02.pit.itm.uni-luebeck.de/>"
				+ "PREFIX itm: <https://pit.itm.uni-luebeck.de/>" + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>"
				+ "PREFIX gruppe1: <http://gruppe01.pit.itm.uni-luebeck.de/>" + "SELECT ?temp WHERE {"
				+ "gruppe1:tempSensor itm:hasStatus ?status." + "?status itm:hasValue ?temp."
				+ "?status itm:hasScaleUnit \"Celcius\"^^xsd:string." + "}";

		String port = "8080";

		RestClient rc = new RestClient(SSP_HOST, port, sparql, "application/sparql-results+json");

		new Thread(() -> {
			while (true) {
				try {
					LinkedList<String> ll;
					// get result lines from query
					ll = rc.getResult();
					// for each result
					for (String s : ll) {
						InnerResults inner = SparqlParser.parseJson(s);
						if (inner.getResults().getBindings().isEmpty()) {
							System.out.println("No results found for temperature!");
							continue;
						}
						String value = inner.getResults().getBindings().get(0).get("temp").getValue();
						if (Double.parseDouble(value) < BORDER_TEMPERATURE) {
							currentTemperature = Temperature.Cold;
						} else {
							currentTemperature = Temperature.Warm;
						}
					}
					// sleep a second
					Thread.sleep(10000);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}).start();
	}

	/**
	 * Plays music for the passed user
	 * 
	 * @param name
	 */
	public synchronized void playMusicFor(String name) {

		// path to the mp3-file
		String mp3 = "/opt/projekt-it/data/" + name + "/" + currentTemperature.getTempName();
		if (soundThread != null) {
			soundThread.stop();
		}
		soundThread = new Thread(new Mp3Player(mp3));
		soundThread.start();

	}

	/**
	 * Player for MP3-Files
	 * 
	 * @author drickert
	 *
	 */
	class Mp3Player implements Runnable {

		private String filename;
		private AdvancedPlayer player;

		/**
		 * Constructor
		 * @param filename
		 */
		Mp3Player(String filename) {
			this.filename = filename;
		}

		/**
		 * Play the file
		 */
		public void play() {
			try {
				FileInputStream buffer = new FileInputStream(filename);
				player = new AdvancedPlayer(buffer);
				player.play();
			} catch (Exception e) {
				System.out.println(e);
			}

		}

		@Override
		public void run() {
			// always repeat the mp3
			while (true) {
				play();
			}
		}

	}

	/**
	 * Starts a Thread that fetches the current user periodically
	 */
	protected void findFaces() {
		new Thread(() -> {
			String sparql = "" + "PREFIX gruppe2: <http://gruppe02.pit.itm.uni-luebeck.de/>"
					+ "PREFIX itm: <https://pit.itm.uni-luebeck.de/>"
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +

					"SELECT ?name WHERE {" + "?cam itm:isType \"Camera\"^^xsd:string." + "?cam itm:hasStatus ?status."
					+ "?status itm:hasValue ?name." + "?status itm:hasScaleUnit \"UID\"^^xsd:string." + "}";

			String port = "8080";

			RestClient rc = new RestClient(SSP_HOST, port, sparql, "application/sparql-results+json");

			while (true) {
				System.out.println("new iteration+");
				try {
					LinkedList<String> ll;
					// get result lines from query
					ll = rc.getResult();
					// for each result
					for (String s : ll) {
						InnerResults inner = SparqlParser.parseJson(s);
						if (inner.getResults().getBindings().isEmpty()) {
							System.out.println("No results found!");
							continue;
						}
						String value = inner.getResults().getBindings().get(0).get("name").getValue();
						if (!FaceTask.UNKNOWN.equals(value)) {
							System.out.println(
									"Playing music for: " + value + " (" + currentTemperature.getTempName() + ")");
							if (!lastUser.equals(value)) {
								playMusicFor(value);
								lastUser = value;
							}
						} else {
							System.out.println("Dont play music: unknown");
						}
					}
					// sleep a second
					Thread.sleep(1000);
				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
				}
			}
		}).start();

	}

}
