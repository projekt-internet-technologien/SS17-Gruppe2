/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uni_luebeck.itm.itp2017.gruppe2.PiLib.tasks;

import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.*;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.SettableFuture;

import de.uzl.itm.ncoap.application.linkformat.LinkParam;
import de.uzl.itm.ncoap.application.server.resource.ObservableWebresource;
import de.uzl.itm.ncoap.application.server.resource.WrappedResourceStatus;
import de.uzl.itm.ncoap.message.CoapMessage;
import de.uzl.itm.ncoap.message.CoapRequest;
import de.uzl.itm.ncoap.message.CoapResponse;
import de.uzl.itm.ncoap.message.MessageCode;
import de.uzl.itm.ncoap.message.MessageType;
import de.uzl.itm.ncoap.message.options.ContentFormat;

/**
 * This {@link de.uzl.itm.ncoap.application.server.resource.Webresource} updates on a regular basis and provides
 * the current ldr value.
 *
 */
public class SimpleObservableLightService extends ObservableWebresource<String> {

    public static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

    private static Logger LOG = Logger.getLogger(SimpleObservableLightService.class.getName());

    private static HashMap<Long, String> payloadTemplates = new HashMap<>();
    static{
        //Add template for plaintext UTF-8 payload
        payloadTemplates.put(
                ContentFormat.TEXT_PLAIN_UTF8,
                "LDR: %s"
        );

        //Add template for XML payload
        payloadTemplates.put(
                ContentFormat.APP_XML,
                "<ldr>%s</ldr>\n"
        );
        
        
        
        payloadTemplates.put(
	        ContentFormat.APP_TURTLE,
	        "@prefix gruppe2: <http://gruppe02.pit.itm.uni-luebeck.de/>\n" +
	        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
	        "@prefix itm: <https://pit.itm.uni-luebeck.de/>\n"+
	        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"+
	        "gruppe2:ldr rdf:type itm:Component.\n"+
	        "gruppe2:ldr itm:hasStatus gruppe2:ldrStatus.\n"+
	        "gruppe2:ldrStatus itm:hasValue \"%s\"^^xsd:string.\n"+
	        "gruppe2:ldrStatus itm:hasScaleUnit \"Lux\"^^xsd:string.\n"+
	        "gruppe2:ldr itm:isType \"LDR\"^^xsd:string.\n"+
	        "gruppe2:myPi rdf:type itm:Device.\n"+
	        "gruppe2:myPi itm:hasIP \"141.83.175.235\"^^xsd:string.\n"+
	        "gruppe2:myPi itm:hasGroup \"PIT_02-SS17\"^^xsd:string.\n"+
	        "gruppe2:myPi itm:hasLabel \"PIET\"^^xsd:string.\n"+
	        "gruppe2:ldr itm:hasURL \"coap://141.83.175.235:5683/ldr\"^^xsd:anyURI"

        );        
    }

    private ScheduledFuture periodicUpdateFuture;
    private int updateInterval;

    // This is to handle whether update requests are confirmable or not (remoteSocket -> MessageType)
    private HashMap<InetSocketAddress, Integer> observations = new HashMap<>();
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Task1_3 task;
    /**
     * Creates a new instance of {@link SimpleObservableLightService}.
     *
     * @param path the path of this {@link SimpleObservableLightService} (e.g. /utc-time)
     * @param updateInterval the interval (in seconds) for resource status updates (e.g. 5 for every 5 seconds).
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     * @throws SecurityException 
     * @throws NoSuchFieldException 
     */
    public SimpleObservableLightService(String path, int updateInterval, ScheduledExecutorService executor, Task1_3 task) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        super(path, "0", executor);

    	this.task = task;
    	
        //Set the update interval, i.e. the frequency of resource updates
        this.updateInterval = updateInterval;
        schedulePeriodicResourceUpdate();

        Set<Long> keys = payloadTemplates.keySet();
        Long[] array = keys.toArray(new Long[keys.size()]);
        
        // Convert to "1 3 45"
        String[] values = new String[keys.size()];
        for (int i = 0; i < array.length; i++) {
        	values[i] = array[i].toString();
        }
        
        //Sets the link attributes for supported content types ('ct')
        String ctValue = "\"" + String.join(" ", values) + "\"";
        this.setLinkParam(LinkParam.createLinkParam(CT, ctValue));

        //Sets the link attribute to give the resource a title
        String title = "\"UTC time (updated every " + updateInterval + " seconds)\"";
        this.setLinkParam(LinkParam.createLinkParam(TITLE, title));

        //Sets the link attribute for the resource type ('rt')
        String rtValue = "\"time\"";
        this.setLinkParam(LinkParam.createLinkParam(RT, rtValue));

        //Sets the link attribute for max-size estimation ('sz')
        this.setLinkParam(LinkParam.createLinkParam(SZ, "" + 100L));

        //Sets the link attribute for interface description ('if')
        String ifValue = "\"GET only\"";
        this.setLinkParam(LinkParam.createLinkParam(IF, ifValue));
    }


    @Override
    public boolean isUpdateNotificationConfirmable(InetSocketAddress remoteAddress) {
        try {
            this.lock.readLock().lock();
            if (!this.observations.containsKey(remoteAddress)) {
                LOG.error("This should never happen (no observation found for \"" + remoteAddress + "\")!");
                return false;
            } else {
                return this.observations.get(remoteAddress) == MessageType.CON;
            }
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public void removeObserver(InetSocketAddress remoteAddress) {
    	System.out.println("losing Observer...");
        try {
            this.lock.writeLock().lock();
            if (this.observations.remove(remoteAddress) != null) {
                LOG.info("Observation canceled for remote socket \"" + remoteAddress + "\".");
            } else {
                LOG.warn("No observation found to be canceled for remote socket \"remoteAddress\".");
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }


    @Override
    public byte[] getEtag(long contentFormat) {
    	return task.getCurrentValue().getBytes();
    }



    private void schedulePeriodicResourceUpdate() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

        this.periodicUpdateFuture = this.getExecutor().scheduleAtFixedRate(() -> {
		    try{
		        String currentValue = task.getCurrentValue();
		        System.out.println("sending value "+ currentValue);
		        System.out.println("observations: "+this.observations.size());
				setResourceStatus(currentValue, updateInterval);
		        LOG.info("New status of resource " + getUriPath() + ": " + getResourceStatus());
		    } catch(Exception ex) {
		        LOG.error("Exception while updating actual status...", ex);
		    }
		}, updateInterval, updateInterval, TimeUnit.SECONDS);
    }


    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteAddress) {
        try{
            if (coapRequest.getMessageCode() == MessageCode.GET) {
                processGet(responseFuture, coapRequest, remoteAddress);
            } else {
                CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(),
                        MessageCode.METHOD_NOT_ALLOWED_405);
                String message = "Service does not allow " + coapRequest.getMessageCodeName() + " requests.";
                coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
                responseFuture.set(coapResponse);
            }
        }
        catch(Exception ex) {
            responseFuture.setException(ex);
        }
    }


    private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                            InetSocketAddress remoteAddress) throws Exception {

        //create resource status
        WrappedResourceStatus resourceStatus;
        if (coapRequest.getAcceptedContentFormats().isEmpty()) {
            resourceStatus = getWrappedResourceStatus(DEFAULT_CONTENT_FORMAT);
        } else {
            resourceStatus = getWrappedResourceStatus(coapRequest.getAcceptedContentFormats());
        }

        CoapResponse coapResponse;

        if (resourceStatus != null) {
            //if the payload could be generated, i.e. at least one of the accepted content formats (according to the
            //requests accept option(s)) is offered by the Webservice then set payload and content format option
            //accordingly
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.CONTENT_205);
            coapResponse.setContent(resourceStatus.getContent(), resourceStatus.getContentFormat());

            coapResponse.setEtag(resourceStatus.getEtag());
            coapResponse.setMaxAge(resourceStatus.getMaxAge());

            // this is to accept the client as an observer
            if (coapRequest.getObserve() == 0) {
                coapResponse.setObserve();
                try {
                    this.lock.writeLock().lock();
                    this.observations.put(remoteAddress, coapRequest.getMessageType());
                } catch(Exception ex) {
                    LOG.error("This should never happen!");
                } finally {
                    this.lock.writeLock().unlock();
                }
            }
        } else {
            //if no payload could be generated, i.e. none of the accepted content formats (according to the
            //requests accept option(s)) is offered by the Webservice then set the code of the response to
            //400 BAD REQUEST and set a payload with a proper explanation
            coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.NOT_ACCEPTABLE_406);

            StringBuilder payload = new StringBuilder();
            payload.append("Requested content format(s) (from requests ACCEPT option) not available: ");
            for(long acceptedContentFormat : coapRequest.getAcceptedContentFormats())
                payload.append("[").append(acceptedContentFormat).append("]");

            coapResponse.setContent(payload.toString().getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
        }

        //Set the response future with the previously generated CoAP response
        responseFuture.set(coapResponse);
    }


    @Override
    public void shutdown() {
        // cancel the periodic update task
        LOG.info("Shutdown service " + getUriPath() + ".");
        boolean futureCanceled = this.periodicUpdateFuture.cancel(true);
        LOG.info("Future canceled: " + futureCanceled);
    }


    @Override
    public byte[] getSerializedResourceStatus(long contentFormat) {
        LOG.debug("Try to create payload (content format: " + contentFormat + ")");

        String template = payloadTemplates.get(contentFormat);
        if (template == null) {
            return null;
        } else {
            return String.format(template, getResourceStatus()).getBytes(CoapMessage.CHARSET);
        }
    }


	@Override
	public void updateEtag(String resourceStatus) {
		
	}
}
