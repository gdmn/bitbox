package pl.devsite.bitbox.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import pl.devsite.bitbox.authenticator.HttpAuthenticator;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableFileWithMimeResolver;

/**
 *
 * @author dmn
 */
public class HttpTools {

	public static final String RN = "\r\n";
	public static final String CONNECTION = "Connection";
	public static final String CONTENTLENGTH = "Content-Length";
	public static final String CONTENTTYPE = "Content-Type";
	public static final String CONTENTDISPOSITION = "Content-Disposition";
	public static final String SERVER = "Server";
	public static final String LOCATION = "Location";
	public static final String RETRYAFTER = "Retry-After";
	public static final String WWWAUTHENTICATE = "WWW-Authenticate";
	public static final String ACCEPTRANGES = "Accept-Ranges";
	public static final String CONTENTRANGE = "Content-Range";
	public static final String AUTHORIZATION = "Authorization";
	public static final String RANGE = "Range";
	public static final String COLON = ":";
	public static final String COLONSPACE = ": ";
	public static final String REFERER = "Referer";
	public static final String HOST = "Host";
	public static final String ICYMETADATA = "Icy-MetaData";
	public static final String XHTMLHEADER = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" + RN
			+ "<html xmlns=\"http://www.w3.org/1999/xhtml\">";
	public static final String CONTENTTYPE_TEXT_HTML = "text/html";
	public static final HttpAuthenticator NULLAUTHENTICATOR = new HttpAuthenticator() {

		@Override
		public void addUser(String user, String password) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public String allowed(String authorization) {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public String getAuthenticate() {
			throw new UnsupportedOperationException("Not supported yet.");
		}
	};

	public static String createHttpResponse(int code, String server, long contentLength, String contentType, String body) {
		StringBuilder result = new StringBuilder(createHttpResponse(code, server, contentLength, contentType));
		if (body != null) {
			result.append(RN + body);
		}
		return result.toString();
	}

	public static String createHttpResponse(int code, String server, long rangeStart, long rangeStop, long contentLength, String contentType) {
		StringBuilder result = new StringBuilder();
		result.append("HTTP/1.0");
		result.append(" " + code + " ");
		String message = getHttpCodes().get(code);
		if (message == null) {
			message = "No information";
		}
		result.append(message + RN);
		result.append(CONNECTION + COLONSPACE + "close" + RN);
		if (server != null) {
			result.append(SERVER + COLONSPACE + server + RN);
		}
		if (contentType != null) {
			result.append(CONTENTTYPE + COLONSPACE + contentType + RN);
		}
		if (rangeStart < 0) {
			rangeStart = 0;
		}
		if (rangeStop < 0 || rangeStop > contentLength - 1) {
			rangeStop = contentLength - 1;
		}
		result.append(CONTENTRANGE + COLONSPACE + "bytes " + (rangeStart < 0 ? "" : rangeStart) + "-" + (rangeStop < 0 ? "" : rangeStop) + "/" + contentLength + RN);
		result.append(CONTENTLENGTH + COLONSPACE + (rangeStop - rangeStart + 1) + RN);
		return result.toString();
	}

	public static String createHttpResponse(String server, Sendable sendable) {
		int code = 200;
		StringBuilder result = new StringBuilder();
		String mime = sendable.getMimeType();
		result.append(createHttpResponse(code, server, sendable.getContentLength(), mime));
		String metadata;
		if (sendable instanceof SendableFileWithMimeResolver) {
			SendableFileWithMimeResolver sf = (SendableFileWithMimeResolver) sendable;
			metadata = sf.getMetadata();
			if (metadata != null) {
				result.append(metadata).append(RN);
			}
		}
		return result.toString();
	}

	public static String createHttpResponse(int code, String server, long contentLength, String contentType) {
		StringBuilder result = new StringBuilder();
		result.append("HTTP/1.0");
		result.append(" " + code + " ");
		String message = getHttpCodes().get(code);
		if (message == null) {
			message = "No information";
		}
		result.append(message + RN);
		result.append(CONNECTION + COLONSPACE + "close" + RN);
		if (server != null) {
			result.append(SERVER + COLONSPACE + server + RN);
		}
		if (contentLength > -1) {
			result.append(CONTENTLENGTH + COLONSPACE + contentLength + RN);
		}
		if (contentType != null) {
			result.append(CONTENTTYPE + COLONSPACE + contentType + RN);
		}
		result.append(ACCEPTRANGES + COLONSPACE + "bytes" + RN);
		return result.toString();
	}

	public static String createHttpResponse(int code, String[][] headerValues) {
		StringBuilder result = new StringBuilder();
		result.append("HTTP/1.0");
		result.append(" " + code + " ");
		String message = getHttpCodes().get(code);
		if (message == null) {
			message = "No information";
		}
		result.append(message + RN);
		if (headerValues != null) {
			for (String[] value : headerValues) {
				result.append(value[0] + COLONSPACE + value[1] + RN);
			}
		}
		return result.toString();
	}

	public static String createHttpResponse(int code, String... headerValues) {
		StringBuilder result = new StringBuilder();
		result.append("HTTP/1.0");
		result.append(" " + code + " ");
		String message = getHttpCodes().get(code);
		if (message == null) {
			message = "No information";
		}
		result.append(message + RN);
		if (headerValues != null) {
			boolean start = true;
			for (String value : headerValues) {
				if (start) {
					result.append(value + COLONSPACE);
				} else {
					result.append(value + RN);
				}
				start = !start;
			}
		}
		return result.toString();
	}

	public static String createHttpResponse(int code, String server, boolean withBody) {
		if (withBody) {
			return createHttpResponse(code, server, -1, CONTENTTYPE_TEXT_HTML, "<h1>" + code + " " + getHttpCodes().get(code) + " </h1><hr><i>" + server + "</i>");
		} else {
			return createHttpResponse(code, server, -1, null, "<h1>" + code + " " + getHttpCodes().get(code) + " </h1><hr><i>" + server + "</i>");
		}
	}

	public static String createIcecastResponse(String server, int chunkSize, Sendable sendable) {
		StringBuilder result = new StringBuilder();
		String[] greetingMpeg = new String[]{
			"ICY 200 OK",
			"Cache-Control: no-cache",
			"Content-Type: audio/mpeg",
			"icy-notice1: <BR>This stream requires <a href=\"http://www.icecast.org/3rdparty.php\">a media player that support Icecast</a><BR>",
			"icy-notice2: " + server + "<BR>",
			"icy-name: " + server,
			"icy-genre: various",
			//"icy-url: http://gdamian.ovh.org",
			"icy-metaint: " + chunkSize,
			"icy-pub: 1",};
		String[] greetingOgg = new String[]{
			"HTTP/1.0 200 OK",
			"Cache-Control: no-cache",
			"Content-Type: application/ogg",
			"icy-notice1: <BR>This stream requires <a href=\"http://www.icecast.org/3rdparty.php\">a media player that support Icecast</a><BR>",
			"icy-notice2: " + server + "<BR>",
			"icy-name: " + server,
			"icy-genre: various",
			//"icy-url: http://gdamian.ovh.org",
			//"icy-metaint: " + chunkSize,
			"icy-pub: 1",};
		BitBoxConfiguration config = BitBoxConfiguration.getInstance();
		String encoder = config.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_ENCODER);
		String[] greetingICY = greetingMpeg;
		if ("oggenc".equals(encoder)) {
			greetingICY = greetingOgg;
		}
		for (String s : greetingICY) {
			result.append(s + RN);
		}
		/*
		 *
		String metadata;
		if (sendable instanceof SendableFileWithMimeResolver) {
			SendableFileWithMimeResolver sf = (SendableFileWithMimeResolver) sendable;
			metadata = sf.getMetadata();
			if (metadata != null) {
				result.append(metadata).append(BR);
			}
		}
		 */
		return result.toString();
	}

	public static String convertToMultiline(String... text) {
//        if (text.length == 1) {
//            return text[0];
//        }
		StringBuilder builder = new StringBuilder();
		for (String s : text) {
			builder.append(s + RN);
		}
		return builder.toString();
	}
	private static String defaultCSS = null;
	private static HashMap<Integer, String> httpCodes = null;

	public static HashMap<Integer, String> getHttpCodes() {
		if (httpCodes == null) {
			httpCodes = new HashMap<Integer, String>();
			httpCodes.put(200, "OK");
			httpCodes.put(403, "Forbidden");
			httpCodes.put(400, "Bad Request");
			httpCodes.put(401, "Authorization Required");
			httpCodes.put(301, "Moved Permanently");
			httpCodes.put(503, "Server too busy");
			httpCodes.put(404, "Not found");
			httpCodes.put(408, "Request Timeout");
			httpCodes.put(206, "Partial content");
			httpCodes.put(416, "Requested range not satisfiable");
		}
		return httpCodes;
	}

	public static String base64encode(String string) {
		byte[] stringArray;
		try {
			stringArray = string.getBytes("UTF-8");  // use appropriate encoding string!
		} catch (Exception ignored) {
			stringArray = string.getBytes();  // use locale default rather than croak
		}
		return new sun.misc.BASE64Encoder().encode(stringArray);
		/*
		 * // base64 encode from http://www.wikihow.com/Encode-a-String-to-Base64-With-Java String base64code =
		 * "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz" + "0123456789" + "+/"; String encoded = ""; //
		 * determine how many padding bytes to add to the output int paddingCount = (3 - (stringArray.length % 3)) % 3;
		 * { // add any necessary padding to the input byte[] padded = new byte[stringArray.length + paddingCount]; //
		 * initialized to zero by JVM System.arraycopy(stringArray, 0, padded, 0, stringArray.length); stringArray =
		 * padded; } // process 3 bytes at a time, churning out 4 output bytes // worry about CRLF insertions later for
		 * (int i = 0; i < stringArray.length; i += 3) { int j = (stringArray[i] << 16) + (stringArray[i + 1] << 8) +
		 * stringArray[i + 2]; encoded = encoded + base64code.charAt((j >> 18) & 0x3f) + base64code.charAt((j >> 12) &
		 * 0x3f) + base64code.charAt((j >> 6) & 0x3f) + base64code.charAt(j & 0x3f); } // replace encoded padding nulls
		 * with "=" return encoded.substring(0, encoded.length() - paddingCount) + "==".substring(0, paddingCount);
		 */
	}

	public static String md5sum(String string) {
		try {
			byte[] stringArray;
			try {
				stringArray = string.getBytes("UTF-8"); // use locale default rather than croak
			} catch (Exception ignored) {
				stringArray = string.getBytes(); // use locale default rather than croak
			}
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] raw = md.digest(stringArray);
			StringBuffer result = new StringBuffer(32);
			for (byte b : raw) {
				int i = (b & 0xFF);
				if (i < 16) {
					result.append("0");
				}
				result.append(Integer.toHexString(i));
			}
			return result.toString();
		} catch (NoSuchAlgorithmException ex) {
			return null;
		}
	}

	public static String randomNonce() {
		StringBuffer result = new StringBuffer(32);
		char[] availableChars = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
		Random r = new Random();
		for (int i = 0; i < 32; i++) {
			result.append(availableChars[r.nextInt(16)]);
		}
		return result.toString();
	}

	public static HashMap<String, String> headersToMap(String headers) {
		HashMap<String, String> result = new HashMap<String, String>();
		String[] lines = headers.split(HttpTools.RN);
		for (String line : lines) {
			int p = line.indexOf('=');
			if (p >= 0 && p < line.length() - 1) {
				String key_ = line.substring(0, p).trim().toLowerCase();
				String value_ = line.substring(p + 1, line.length()).trim();
				result.put(key_, value_);
			}
		}
		return result;
	}

	public static String getDefaultCSS() {
		if (defaultCSS == null) {
			String[] css = {
				/*
				 * font-family: Helvetica, Calibri, Arial, "Nimbus Sans L", sans-serif font-family: Corbel, Verdana,
				 * "Bitstream Vera Sans", "DejaVu Sans", sans-serif font-family: Candara, "Trebuchet MS", Trebuchet,
				 * sans-serif *no good Trebuchet-ish Linux font* font-family: Cambria, "Times New Roman", Times,
				 * FreeSerif, serif font-family: Constantia, "Palatino Linotype", Palatino, Georgia, "Century Schoolbook
				 * L", serif font-family: Consolas, "Bitstream Vera Sans Mono", "Courier New", Courier, "Nimbus Mono L",
				 * monospace
				 */
				"<style type=\"text/css\">",
				"/* <![CDATA[ */",
				"body {",
				"font-family: \"DejaVu Sans\", \"Lucida Sans\", Verdana, Tahoma, Arial, sans-serif;",
				"outline: 0px;",
				"color: #000;",
				"font-size: 12px;",
				"}",
				"/*************/",
				"a.dir {",
				"color: #000;",
				"font-weight: bold;",
				"}",
				"a.file {",
				"color: #000;",
				"text-decoration: bold;",
				"}",
				"/*************/",
				"a:link {",
				"color: #000;",
				"text-decoration: none;",
				"}",
				"a:visited {",
				"color: #000;",
				"text-decoration: none;",
				"}",
				"a:hover, a:active {",
				"text-decoration: none;",
				"color: #000;",
				"}",
				"/*************/",
				"ul.ls {",
				"font-family: \"DejaVu Sans Mono\", \"Bitstream Vera Sans Mono\", Monaco, \"Liberation Mono\", \"Lucida Console\", monospace;",
				"}",
				"ul.ls, ul.ls li {",
				"display: block;",
				"list-style: none;",
				"margin: 0;",
				"padding: 0;",
				"}",
				"ul.ls a:link, ul.ls a:visited {",
				"display: block;",
				"text-decoration: none;",
				"padding: 1px;",
				"}",
				"ul.ls a:hover {",
				"display: block;",
				"padding: 0;",
				"background-color: #eee;",
				"border: 1px solid #bbb;",
				"}",
				"/*************/",
				"ul.ls li a.file span.size {",
				"color: #666 !important;",
				"font-size: 10px;",
				"}",
				"/*************/",
				"#CONTENT {",
				"margin-left: 150px;",
				"margin-right: 150px;",
				"background-color: #fff;",
				"}",
				"#HEADER, #HEADER a {",
				"background-color: #888;",
				"color: #FFF;",
				"padding: 5px;",
				"}",
				"#FOOTER, #FOOTER a {",
				//"clear: both;",
				"background-color: #888;",
				"color: #FFF;",
				"padding: 5px;",
				"font-size: 10px;",
				//"white-space:nowrap;",
				"}",
				".right {",
				"text-align: right;",
				"float:right;",
				"display:inline",
				"}",
				".left {",
				//"float: left;",
				"text-align: left;",
				"display:inline",
				"}",
				"#HEADER a, #FOOTER a {",
				"color: #FFF;",
				"padding: 0px;",
				"}",
				"#HEADER a:hover, #FOOTER a:hover {",
				"font-weight: bold;",
				"}",
				"#MENU {",
				"width: 140px;",
				"float: left;",
				"overflow: hidden;",
				"position: relative;",
				"background-color: #fff;",
				"border: solid;",
				"border-width: 1px;",
				"}",
				"#MENU ul, #MENU ul li {",
				"display: block;",
				"list-style: none;",
				"margin: 0;",
				"padding: 0;",
				"}",
				"#MENU ul a:link, #MENU ul a:visited {",
				"display: block;",
				"text-decoration: none;",
				"padding: 5px;",
				"}",
				"#MENU ul a:hover {",
				"border: 1px solid #bbb;",
				"padding: 4px;",
				"background-color: #eee;",
				"}",
				"#INFORMATIONS {",
				"width: 150px;",
				"float: right;",
				"overflow: hidden;",
				"position: relative;",
				"background-color: #ccc;",
				"}",
				"/* ]]> */",
				"</style>",};
			defaultCSS = convertToMultiline(css);
		}
		return defaultCSS;
	}
}
