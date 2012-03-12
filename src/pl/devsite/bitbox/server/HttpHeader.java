package pl.devsite.bitbox.server;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableFileWithMimeResolver;

/**
 *
 * @author dmn
 */
public class HttpHeader {

	private ArrayList<String> data;
	private HttpRequestType bufHttpRequestType;
	private int length = 0;
	private ReaderWriterType readerWriter;
	private int code = 0;
	private String firstLineOfResponse;

	private static enum ReaderWriterType {
		READER, WRITER
	};

	public HttpHeader(String request) {
		this();
		readerWriter = ReaderWriterType.WRITER;
		addMultiple(request);
	}

	public HttpHeader(BufferedInputStream input) throws IOException {
		this();
		readerWriter = ReaderWriterType.READER;
		int c1 = 0, c2 = 0, c3 = 0, c4 = 0;
		StringBuilder result = new StringBuilder(1024);
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
		readerWriter = ReaderWriterType.WRITER;
	}

	public HttpHeader(int httpResponseCode) {
		this();
		setHttpResponseCode(code);
	}

	public static enum HttpRequestType {

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
		remove(key);
		data.add(key + ": " + value);
	}

	public String remove(String key) {
		String keyLower = key.toLowerCase();
		Iterator<String> iterator = data.iterator();
		while (iterator.hasNext()) {
			String line = iterator.next();
			if (line.toLowerCase().startsWith(keyLower + HttpTools.COLON)) {
				iterator.remove();
				return line.substring(key.length() + 1).trim();
			}
		}
		return null;
	}

	private void add(String keyValue) {
		data.add(keyValue);
	}

	public void addHeaderLine(String keyValue) {
		add(keyValue);
	}

	public String getRequestedString() {
		String result = null;
		if (HttpRequestType.GET.equals(getType()) || HttpRequestType.HEAD.equals(getType())
				|| HttpRequestType.POST.equals(getType())) {
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
		if ((data == null || data.isEmpty()) && (firstLineOfResponse == null || firstLineOfResponse.isEmpty())) {
			return null;
		}
		StringBuilder result = new StringBuilder();
		if (readerWriter == ReaderWriterType.WRITER) {
			if (code > 0 && firstLineOfResponse == null) {
				firstLineOfResponse = "HTTP/1.0 " + code + " " + HttpTools.getHttpCodes().get(code);
			}
			if (firstLineOfResponse != null && !firstLineOfResponse.isEmpty()) {
				result.append(firstLineOfResponse).append("\r\n");
			}
		}
		for (String line : data) {
			result.append(line).append("\r\n");
		}
		return result.toString();
	}

	public void setHttpResponseCode(int code) {
		if (readerWriter != ReaderWriterType.WRITER) {
			throw new IllegalArgumentException("HttpHader created as reader");
		}
		this.code = code;
		String message = HttpTools.getHttpCodes().get(code);
		if (message == null) {
			message = "No information";
		}
		String proto = "HTTP/1.0";
		if (code == 206) {
			proto = "HTTP/1.1";
		}
		firstLineOfResponse = proto + " " + code + " " + message;
	}

	public int getHttpResponseCode() {
		return code;
	}

	public void setFirstLineOfResponse(String line) {
		if (readerWriter != ReaderWriterType.WRITER) {
			throw new IllegalArgumentException("HttpHader created as reader");
		}
		firstLineOfResponse = line;
	}

	public HttpHeader append(String key, String value) {
		add(key, value);
		return this;
	}

	public HttpHeader appendRange(long rangeStart, long rangeStop, long contentLength) {
		if (rangeStart < 0) {
			rangeStart = 0;
		}
		if (rangeStop < 0 || rangeStop > contentLength - 1) {
			rangeStop = contentLength - 1;
		}
		return append(HttpTools.CONTENTRANGE, "bytes " + (rangeStart < 0 ? "" : rangeStart) + "-" + (rangeStop < 0 ? "" : rangeStop) + "/" + contentLength).
				append(HttpTools.CONTENTLENGTH, "" + (rangeStop - rangeStart + 1));
	}

	public HttpHeader appendConnectionClose() {
		return append(HttpTools.CONNECTION, "close");
	}

	public HttpHeader appendContentType(String contentType) {
		return append(HttpTools.CONTENTTYPE, contentType);
	}

	public HttpHeader appendContentLength(long contentLength) {
		if (contentLength < 0) {
			return this;
		}
		return append(HttpTools.CONTENTLENGTH, "" + contentLength);
	}

	public HttpHeader appendServer() {
		String server = BitBoxConfiguration.getInstance().getProperty(BitBoxConfiguration.PROPERTY_NAME);
		if (server == null || server.isEmpty()) {
			return this;
		}
		return append(HttpTools.SERVER, server);
	}

	public HttpHeader appendAcceptRanges() {
		return append(HttpTools.ACCEPTRANGES, "bytes");
	}

	public HttpHeader appendFromSendable(Sendable sendable) {
		String mime = sendable.getMimeType();
		if (mime != null && !mime.isEmpty()) {
			appendContentType(mime);
		}
		appendContentLength(sendable.getContentLength());
		String metadata;
		if (sendable instanceof SendableFileWithMimeResolver) {
			SendableFileWithMimeResolver sf = (SendableFileWithMimeResolver) sendable;
			metadata = sf.getMetadata();
			if (metadata != null) {
				metadata.replace("\r\n", "\n");
				for (String m : metadata.split("\n")) {
					add(m);
				}
			}
		}
		return this;
	}

	public HttpHeader appendNoCache() {
		add("Cache-Control", "no-cache");
		return this;
	}

	public HttpHeader appendIcyNotice(String line1, String line2) {
		add("icy-notice1", line1);
		add("icy-notice2", line2);
		return this;
	}

	public HttpHeader appendIcyName(String value) {
		add("icy-name", value);
		return this;
	}

	public HttpHeader appendIcyGenre(String value) {
		add("icy-genre", value);
		return this;
	}

	public HttpHeader appendIcyPub(String value) {
		add("icy-pub", value);
		return this;
	}

	public HttpHeader appendIcyUrl(String value) {
		add("icy-url", value);
		return this;
	}

	public HttpHeader appendIcyMetaint(String value) {
		add("icy-metaint", value);
		return this;
	}

}
