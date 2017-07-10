package de.uni_luebeck.itm.itp2017.gruppe2.PiLib.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.kohsuke.args4j.Option;

public class Configuration {
	@Option(name = "-ssphost", usage = "Host of the SSP (ip or domain)")
	private String SSP_HOST = "141.83.151.196";
	@Option(name = "-sspport", usage = "Port of the SSP (ip or domain)")
	private int SSP_PORT = 5683;
	@Option(name = "-datapath", usage = "Path to data-dir for faces")
	private String DATA_PATH = "/opt/projekt-it/data";
	@Option(name = "-mode", usage = "face or sound")
	private String MODE = "face";
	@Option(name = "-ip", usage = "the ip address of this device")
	private String IP;
	
	
	{
		try {
			IP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}



	public String getSSP_HOST() {
		return SSP_HOST;
	}



	public int getSSP_PORT() {
		return SSP_PORT;
	}



	public String getDATA_PATH() {
		return DATA_PATH;
	}



	public String getMODE() {
		return MODE;
	}



	public String getIP() {
		return IP;
	}

	
	
}
