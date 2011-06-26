package pl.devsite.bitbox.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class HttpHeader {

	private ArrayList<String> data;
	private HttpRequestType bufHttpRequestType;
	private int length = 0;

	public HttpHeader(String request) {
		this();
		addMultiple(request);
	}

	public HttpHeader(BufferedInputStream input) {
		this();
		int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
		StringBuilder result = new StringBuilder(1024);
		try {
			do {
				c1 = c2;
				c2 = c3;
				c3 = c4;
				c4 = input.read();
				if (c4 > -1) {
					result.append((char) c4);
					length++;
				}
			} while (c4 > -1 && !((c1 == '\r' && c2 == '\n' && c3 == '\r' && c4 == '\n')
					|| (c3 == '\n' && c4 == '\n'))); // for linux nc compatibility
		} catch (IOException ex) {
			Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
		}
		addMultiple(result.toString());
	}

	private void addMultiple(String request) {
		if (request.length() > 0) {
			for (String line : request.split("\r\n")) {
				if (!line.isEmpty()) {
					data.add(line);
				}
			}
		}
	}

	public HttpHeader() {
		data = new ArrayList<String>();
	}

	public enum HttpRequestType {

		GET, HEAD, POST
	};

	public HttpRequestType getType() {
		if (bufHttpRequestType != null) {
			return bufHttpRequestType;
		}
		if (data.size() < 1) {
			return null;
		}
		String firstLine = data.get(0);
		if (firstLine.startsWith("GET ")) {
			bufHttpRequestType = HttpRequestType.GET;
		} else if (firstLine.startsWith("HEAD ")) {
			bufHttpRequestType = HttpRequestType.HEAD;
		} else if (firstLine.startsWith("POST ")) {
			bufHttpRequestType = HttpRequestType.POST;
		} else {
			bufHttpRequestType = null;
		}
		return bufHttpRequestType;
	}

	public ArrayList<String> getData() {
		return data;
	}

	public String get(String key) {
		String keyLower = key.toLowerCase();
		for (String line : data) {
			if (line.toLowerCase().startsWith(keyLower + HttpTools.COLON)) {
				return line.substring(key.length() + 1).trim();
			}
		}
		return null;
	}

	public String get(int id) {
		return id < data.size() ? data.get(id) : null;
	}

	public void add(String key, String value) {
		data.add(key + ": " + value);
	}

	private void add(String keyValue) {
		data.add(keyValue);
	}

	public String getRequestedString() {
		String result = null;
		if (HttpRequestType.GET.equals(bufHttpRequestType) || HttpRequestType.HEAD.equals(bufHttpRequestType)
				|| HttpRequestType.POST.equals(bufHttpRequestType)) {
			String line = data.get(0);
			result = line.substring(line.indexOf(' '), line.lastIndexOf(' ')).trim();
			result = EncodingTools.urlDecodeUTF(result);
		}
		return result;
	}

	public String getAuthorization() {
		String result = get(HttpTools.AUTHORIZATION);
		return result;
	}

	public int getLength() {
		return length;
	}

	@Override
	public String toString() {
		if (data == null || data.isEmpty()) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		for (String line : data) {
			result.append(line).append("\r\n");
		}
		return result.toString();
	}
}
