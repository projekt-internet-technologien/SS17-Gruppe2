package de.uni_luebeck.itm.itp2017.gruppe2.PiLib.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

public class RestClient {

	private String accept;
	private String sparql;
	private String ip;
	private String port;
	
	public RestClient(String ip, String port, String sparql, String accept) {
		this.ip = ip;
		this.port = port;
		this.sparql = sparql;
		this.accept = accept;
	}
	
	public LinkedList<String> getResult() throws Exception {
	
		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost("http://" + this.ip + ":" + this.port + "/services/sparql-endpoint");
		
		post.addHeader("Accept", this.accept);
		post.addHeader("Content-Type", "multipart/form-data; boundary=DATA");

		String data = "--DATA\n" +
				"Content-Disposition: form-data; name=\"query\"\n\n" +
				this.sparql + "\n" +
				"--DATA--";
		post.setEntity(new StringEntity(data));
		
		HttpResponse response = client.execute(post);
		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
		LinkedList<String> lines = new LinkedList<String>();
		String line = "";
		while ((line = rd.readLine()) != null) {
			lines.add(line.trim());
		}
		return lines;

	}	
	
	public static void main(String[] args) throws Exception {
		
		String sparql = "SELECT * WHERE { ?s ?p ?o }";
		String ip = "141.83.151.196";
		String port = "8080";
		
		RestClient rc = new RestClient(ip, port, sparql, "text/csv");
		
		while(true) {
			LinkedList<String> ll = rc.getResult();
			for (String s : ll) {
				System.out.println(s);
			}
			Thread.sleep(1000);
		}

	}
	
}
