package de.uni_luebeck.itm.itp2017.gruppe2.PiLib.tasks;

import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.CT;
import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.IF;
import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.RT;
import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.SZ;
import static de.uzl.itm.ncoap.application.linkformat.LinkParam.Key.TITLE;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

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
 * Service to provide the current users face for the SSP.
 * 
 * @author drickert
 *
 */
public class ObservableFaceService extends ObservableWebresource<String> {
	public static long DEFAULT_CONTENT_FORMAT = ContentFormat.TEXT_PLAIN_UTF8;

	private static Logger LOG = Logger.getLogger(SimpleObservableTimeService.class.getName());
	private String face = FaceTask.UNKNOWN;
	private String userName = FaceTask.UNKNOWN;
	// templates
	private static HashMap<Long, String> payloadTemplates = new HashMap<>();
	static {
		// Add template for plaintext UTF-8 payload
		payloadTemplates.put(ContentFormat.TEXT_PLAIN_UTF8, "The current face is: %s");

		// Add template for XML payload
		payloadTemplates.put(ContentFormat.APP_XML, "<face>\n%s\n</face>");

		payloadTemplates.put(ContentFormat.APP_TURTLE,
				"@prefix gruppe2: <http://gruppe02.pit.itm.uni-luebeck.de/>\n"
						+ "@prefix xsd: <http://www.w3.org/2001/XMLSchema#>\n"
						+ "@prefix itm: <https://pit.itm.uni-luebeck.de/>\n"
						+ "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "gruppe2:cam rdf:type itm:Component.\n" + "gruppe2:cam itm:hasStatus gruppe2:camStatus.\n"
						+ "gruppe2:camStatus itm:hasValue \"%s\"^^xsd:string.\n"
						+ "gruppe2:camStatus itm:hasScaleUnit \"UID\"^^xsd:string.\n"

						+ "gruppe2:cam itm:hasStatus gruppe2:camStatusName.\n"
						+ "gruppe2:camStatusName itm:hasValue \"%s\"^^xsd:string.\n"
						+ "gruppe2:camStatusName itm:hasScaleUnit \"Name\"^^xsd:string.\n"

						+ "gruppe2:cam itm:isType \"Camera\"^^xsd:string.\n" + "gruppe2:myPi rdf:type itm:Device.\n"
						+ "gruppe2:myPi itm:hasIP \"141.83.175.235\"^^xsd:string.\n"
						+ "gruppe2:myPi itm:hasGroup \"PIT_02-SS17\"^^xsd:string.\n"
						+ "gruppe2:myPi itm:hasLabel \"Face\"^^xsd:string.\n"
						+ "gruppe2:myPi itm:hasComponent \"Face\"^^xsd:string.\n"
						+ "gruppe2:cam itm:hasURL \"coap://141.83.175.235:5683/face\"^^xsd:anyURI");
	}

	private ScheduledFuture periodicUpdateFuture;
	private int updateInterval;

	// This is to handle whether update requests are confirmable or not
	// (remoteSocket -> MessageType)
	private HashMap<InetSocketAddress, Integer> observations = new HashMap<>();
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	public ObservableFaceService(String path, int updateInterval, ScheduledExecutorService executor) {
		super(path, "", executor);
		// Set the update interval, i.e. the frequency of resource updates
		this.updateInterval = updateInterval;
		schedulePeriodicResourceUpdate();

		Set<Long> keys = payloadTemplates.keySet();
		Long[] array = keys.toArray(new Long[keys.size()]);

		// Convert to "1 3 45"
		String[] values = new String[keys.size()];
		for (int i = 0; i < array.length; i++) {
			values[i] = array[i].toString();
		}

		// Sets the link attributes for supported content types ('ct')
		String ctValue = "\"" + String.join(" ", values) + "\"";
		this.setLinkParam(LinkParam.createLinkParam(CT, ctValue));

		// Sets the link attribute to give the resource a title
		String title = "\"UTC time (updated every " + updateInterval + " seconds)\"";
		this.setLinkParam(LinkParam.createLinkParam(TITLE, title));

		// Sets the link attribute for the resource type ('rt')
		String rtValue = "\"time\"";
		this.setLinkParam(LinkParam.createLinkParam(RT, rtValue));

		// Sets the link attribute for max-size estimation ('sz')
		this.setLinkParam(LinkParam.createLinkParam(SZ, "" + 100L));

		// Sets the link attribute for interface description ('if')
		String ifValue = "\"GET only\"";
		this.setLinkParam(LinkParam.createLinkParam(IF, ifValue));
	}

	public void setFace(String face) {
		this.face = face;
	}

	public void setUserName(String userName) {
		this.userName = userName;
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
		return face.substring(0, Math.min(7, face.length())).getBytes();
		// return face.getBytes();
	}

	@Override
	public void updateEtag(String resourceStatus) {
		// nothing to do here as the ETAG is constructed on demand in the
		// getEtag(long contentFormat) method
	}

	private void schedulePeriodicResourceUpdate() {
		this.periodicUpdateFuture = this.getExecutor().scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				try {
					setResourceStatus(face, updateInterval);
					LOG.info("New status of resource " + getUriPath() + ": " + getResourceStatus());
				} catch (Exception ex) {
					LOG.error("Exception while updating actual time...", ex);
				}
			}
		}, updateInterval, updateInterval, TimeUnit.SECONDS);
	}

	@Override
	public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
			InetSocketAddress remoteAddress) {
		try {
			if (coapRequest.getMessageCode() == MessageCode.GET) {
				processGet(responseFuture, coapRequest, remoteAddress);
			} else {
				CoapResponse coapResponse = new CoapResponse(coapRequest.getMessageType(),
						MessageCode.METHOD_NOT_ALLOWED_405);
				String message = "Service does not allow " + coapRequest.getMessageCodeName() + " requests.";
				coapResponse.setContent(message.getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
				responseFuture.set(coapResponse);
			}
		} catch (Exception ex) {
			responseFuture.setException(ex);
		}
	}

	private void processGet(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
			InetSocketAddress remoteAddress) throws Exception {

		// create resource status
		WrappedResourceStatus resourceStatus;
		if (coapRequest.getAcceptedContentFormats().isEmpty()) {
			resourceStatus = getWrappedResourceStatus(DEFAULT_CONTENT_FORMAT);
		} else {
			resourceStatus = getWrappedResourceStatus(coapRequest.getAcceptedContentFormats());
		}

		CoapResponse coapResponse;

		if (resourceStatus != null) {
			// if the payload could be generated, i.e. at least one of the
			// accepted content formats (according to the
			// requests accept option(s)) is offered by the Webservice then set
			// payload and content format option
			// accordingly
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
				} catch (Exception ex) {
					LOG.error("This should never happen!");
				} finally {
					this.lock.writeLock().unlock();
				}
			}
		} else {
			// if no payload could be generated, i.e. none of the accepted
			// content formats (according to the
			// requests accept option(s)) is offered by the Webservice then set
			// the code of the response to
			// 400 BAD REQUEST and set a payload with a proper explanation
			coapResponse = new CoapResponse(coapRequest.getMessageType(), MessageCode.NOT_ACCEPTABLE_406);

			StringBuilder payload = new StringBuilder();
			payload.append("Requested content format(s) (from requests ACCEPT option) not available: ");
			for (long acceptedContentFormat : coapRequest.getAcceptedContentFormats())
				payload.append("[").append(acceptedContentFormat).append("]");

			coapResponse.setContent(payload.toString().getBytes(CoapMessage.CHARSET), ContentFormat.TEXT_PLAIN_UTF8);
		}

		// Set the response future with the previously generated CoAP response
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
			return String.format(template, face, userName).getBytes();
		}
	}
}
