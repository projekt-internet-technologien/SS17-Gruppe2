package de.uni_luebeck.itm.itp2017.gruppe2.PiLib;

import org.kohsuke.args4j.CmdLineParser;

import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.tasks.FaceTask;
import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.tasks.SoundTask;
import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.util.Configuration;

/**
 * Start-Point
 */
public class App {
	
	public static void main(String[] args) throws Throwable {
		Configuration config = new Configuration();
		CmdLineParser cmdLineParser = new CmdLineParser(config);
		System.out.println("Possible Arguments:");
		cmdLineParser.printUsage(System.out);
		cmdLineParser.parseArgument(args);
		
		System.out.println("starting...");
		// decide whether to start the face or sound task.
		if ("face".equals(config.getMODE())) {
			new FaceTask().run(config);
		} else {
			new SoundTask().run(config);
		}
	}

}
