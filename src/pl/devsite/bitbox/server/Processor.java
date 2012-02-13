package pl.devsite.bitbox.server;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.authenticator.HttpAuthenticator;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableAdapter;
import pl.devsite.bitbox.sendables.SendableTemplates;
import static pl.devsite.bitbox.server.BitBoxConfiguration.*;
import pl.devsite.bitbox.server.servlets.InputProcessor;

/**
 *
 * @author dmn
 */
public class Processor {

	private static final Logger logger = Logger.getLogger(Parser.class.getName());
	private BitBoxConfiguration config = BitBoxConfiguration.getInstance();
	private final RequestContext context;
	private Sendable response = null, potentialResponse = null;

	public Processor(RequestContext context) {
		this.context = context;
	}

	public void process() throws IOException {
		context.setResponseHeader(new HttpHeader());
		if (context.getTemporaryResultCode() == 0) {
			processResponse();
			processAuthorization();
		}
		if (isOperationAllowed() && response == null) {
			// true -> render
			response = potentialResponse;
		}
		context.setSendableResponse(response);
	}

	private void processAuthorization() {
		context.setAuthenticator(potentialResponse == null ? null : potentialResponse.getAuthenticator());
		if (context.getAuthenticator() == HttpTools.NULLAUTHENTICATOR) {
			context.setAuthenticator( null);
		}
		context.setAuthenticatedUser((context.getAuthorization() == null || context.getAuthenticator() == null) ? null : context.getAuthenticator().allowed(context.getAuthorization()));
	}

	private void processResponse() {
		if (context.isGetRequest() || context.isHeadRequest()) {
			potentialResponse = SendableAdapter.tryToFindSendable(context.getSendableRoot(), context.getStringRequest());
		}
	}

	private boolean isOperationAllowed() throws IOException {
		boolean result = false;
		HttpHeader responseHeader = context.getResponseHeader();
		if (context.getTemporaryResultCode() > 0) {
			responseHeader.setHttpResponseCode(context.getTemporaryResultCode());
			response = SendableTemplates.SIMPLE.create(responseHeader);
		} else if (context.getStringRequest() == null || (context.isPostRequest() && (context.getContentLength() < 1 || context.getContentType() == null))) {
			logger.log(Level.WARNING, "bad request, invader: {0}", context.getHostAddress());
			responseHeader.setHttpResponseCode(400);
			response = SendableTemplates.SIMPLE.create(responseHeader);
		} else if (!context.isIcyMetadata() && (context.isGetRequest() || context.isHeadRequest()) && context.getAuthenticator() != null && context.getAuthenticatedUser() == null) {
			logger.log(Level.WARNING, "not authorized, invader: {0}", context.getHostAddress());
			responseHeader.setHttpResponseCode(401);
			responseHeader.add(HttpTools.WWWAUTHENTICATE, context.getAuthenticator().getAuthenticate());
		} else if (potentialResponse == null && (context.isGetRequest() || context.isHeadRequest())) {
			logger.log(Level.WARNING, "not found: {0}, invader: {1}", new Object[]{context.getStringRequest(), context.getHostAddress()});
			responseHeader.setHttpResponseCode(404);
			response = SendableTemplates.SIMPLE.create(responseHeader);
		} else if (context.isIcyMetadata() && !("audio/mpeg".equals(potentialResponse.getMimeType())
				//|| "audio/ogg".equals(response.getMimeType())
				//|| "application/x-flac".equals(response.getMimeType())
				|| (potentialResponse.getMimeType() != null && potentialResponse.getMimeType().startsWith("audio/")))) {
			logger.log(Level.WARNING, "streaming forbidden, invader: {0}", context.getHostAddress());
			responseHeader.setHttpResponseCode(403);
			response = SendableTemplates.SIMPLE.create(responseHeader);
		} else if ((context.isGetRequest() || context.isHeadRequest()) && potentialResponse.hasChildren() && !context.getStringRequest().isEmpty() && !context.getStringRequest().endsWith("/")) {
			logger.log(Level.INFO, "redirecting to {0}/", context.getStringRequest());
			responseHeader.setHttpResponseCode(301);
			responseHeader.add(HttpTools.LOCATION, "" + context.getStringRequest() + "/");
		} else if ((context.isGetRequest() || context.isHeadRequest()) && potentialResponse.getFilter() != null && !potentialResponse.getFilter().isAllowed(potentialResponse, context.getHostAddress(), context.getAuthenticatedUser())) {
			logger.log(Level.WARNING, "not allowed, invader: {0}", context.getHostAddress());
			responseHeader.setHttpResponseCode(403);
			response = SendableTemplates.SIMPLE.create(responseHeader);
		} else {
			//context.setResponseStream((context.isGetRequest() || context.isHeadRequest()) ? potentialResponse.getResponseStream() : null);
			if ((context.isGetRequest() || context.isHeadRequest()) && (potentialResponse.getResponseStream() == null)) {
				logger.log(Level.WARNING, "not found: " + response + ", invader: " + context.getHostAddress());
				responseHeader.setHttpResponseCode(404);
				response = SendableTemplates.SIMPLE.create(responseHeader);
			} else {
				result = true;
			}
		}
		return result;
	}
}
