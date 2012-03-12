package pl.devsite.bitbox.server;

import java.beans.ExceptionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.logging.Logger;
import pl.devsite.bitbox.authenticator.HttpAuthenticator;
import pl.devsite.bitbox.sendables.Sendable;

/**
 *
 * @author dmn
 */
public class RequestContext {

	private BufferedInputStream clientIn;
	private BufferedOutputStream clientOut;
	private Integer rangeStart = null;
	private Integer rangeStop = null;
	private HttpHeader requestHeader, responseHeader;
	private Socket socket;
	private Sendable sendableRoot, sendableResponse;
	private String authenticatedUser;
	private HttpAuthenticator authenticator;
	public static final Logger logger = Logger.getLogger(RequestContext.class.getName());
	//private Operation operation;

	public BufferedInputStream getClientIn() {
		return clientIn;
	}

	public void setClientIn(BufferedInputStream clientIn) {
		this.clientIn = clientIn;
	}

	public BufferedOutputStream getClientOut() {
		return clientOut;
	}

	public void setClientOut(BufferedOutputStream clientOut) {
		this.clientOut = clientOut;
	}

	public Integer getRangeStart() {
		return rangeStart;
	}

	public void setRangeStart(Integer rangeStart) {
		this.rangeStart = rangeStart;
	}

	public Integer getRangeStop() {
		return rangeStop;
	}

	public void setRangeStop(Integer rangeStop) {
		this.rangeStop = rangeStop;
	}

	public HttpHeader getRequestHeader() {
		return requestHeader;
	}

	public void setRequestHeader(HttpHeader requestHeader) {
		this.requestHeader = requestHeader;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	public int getHttpResponseCode() {
		return responseHeader == null ? 0 : responseHeader.getHttpResponseCode();
	}

	public String getAuthorization() {
		return requestHeader.get(HttpTools.AUTHORIZATION);
	}

	public boolean isIcyMetadata() {
		String icyMetaDataStr = requestHeader.get(HttpTools.ICYMETADATA);
		return "1".equals(icyMetaDataStr);
	}

	public long getContentLength() {
		String temp = requestHeader.get(HttpTools.CONTENTLENGTH);
		try {
			return Long.valueOf(temp);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public String getContentType() {
		return requestHeader.get(HttpTools.CONTENTTYPE);
	}

	public String getReferer() {
		return requestHeader.get(HttpTools.REFERER);
	}

	public String getHost() {
		return requestHeader.get(HttpTools.HOST);
	}

	public HttpHeader.HttpRequestType getRequestType() {
		return requestHeader.getType();
	}

	public boolean isGetRequest() {
		return HttpHeader.HttpRequestType.GET.equals(requestHeader.getType());
	}

	public boolean isHeadRequest() {
		return HttpHeader.HttpRequestType.HEAD.equals(requestHeader.getType());
	}

	public boolean isPostRequest() {
		return HttpHeader.HttpRequestType.POST.equals(requestHeader.getType());
	}

	public String getStringRequest() {
		return requestHeader.getRequestedString();
	}

	public String getHostAddress() {
		return socket.getInetAddress().getHostAddress();
	}

	public Sendable getSendableRoot() {
		return sendableRoot;
	}

	public void setSendableRoot(Sendable sendableRoot) {
		this.sendableRoot = sendableRoot;
	}

	public String getAuthenticatedUser() {
		return authenticatedUser;
	}

	public void setAuthenticatedUser(String authenticatedUser) {
		this.authenticatedUser = authenticatedUser;
	}

	public HttpAuthenticator getAuthenticator() {
		return authenticator;
	}

	public void setAuthenticator(HttpAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	public HttpHeader getResponseHeader() {
		return responseHeader;
	}

	public void setResponseHeader(HttpHeader responseHeader) {
		this.responseHeader = responseHeader;
	}

	public InputStream getResponseStream() {
		if (sendableResponse != null) {
			return sendableResponse.getResponseStream();
		} else {
			return null;
		}
	}

	public Sendable getSendableResponse() {
		return sendableResponse;
	}

	public void setSendableResponse(Sendable sendableResponse) {
		this.sendableResponse = sendableResponse;
	}

	private ExceptionListener exceptionListener;

	public ExceptionListener getExceptionListener() {
		return exceptionListener;
	}

	public void setExceptionListener(ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}
/*
	private Processor renderer;

	public void setRenderer(Processor renderer) {
		this.renderer = renderer;
	}

	public Processor getRenderer() {
		return renderer;
	}*/
/*
	public Operation getOperation() {
		return operation;
	}

	public void setOperation(Operation operation) {
		this.operation = operation;
	}
*/

}
