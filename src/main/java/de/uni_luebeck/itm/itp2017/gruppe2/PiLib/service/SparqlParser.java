package de.uni_luebeck.itm.itp2017.gruppe2.PiLib.service;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.pojo.InnerResults;
import de.uni_luebeck.itm.itp2017.gruppe2.PiLib.pojo.SSPRestResult;

/**
 * Parser to results from the SSP.
 * 
 * @author drickert
 *
 */
public class SparqlParser {

	/**
	 * Takes the JSON-Response of a SPARQL-Query and maps it into an
	 * {@link InnerResults}-Instance.
	 * 
	 * @param json
	 *            The json-response
	 * @return The Pojo containing the results
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static InnerResults parseJson(String json) throws JsonParseException, JsonMappingException, IOException {
		final ObjectMapper objectMapper = new ObjectMapper();
		// map resulting json object to java object using
		// objectMapper
		SSPRestResult r = objectMapper.readValue(json, SSPRestResult.class);
		// map the result inside the SSPRestResult to
		// Java-Object
		return objectMapper.readValue(r.getResults(), InnerResults.class);
	}
}
