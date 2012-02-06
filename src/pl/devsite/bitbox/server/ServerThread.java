package pl.devsite.bitbox.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bigbitbox.system.MusicEncoder;
import pl.devsite.bitbox.authenticator.HttpAuthenticator;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableAdapter;
import pl.devsite.bitbox.sendables.SendableFile;
import pl.devsite.bitbox.sendables.SendableFileWithMimeResolver;
import static pl.devsite.bitbox.server.BitBoxConfiguration.*;
import pl.devsite.bitbox.server.HttpHeader.HttpRequestType;
import pl.devsite.bitbox.server.servlets.InputProcessor;
import pl.devsite.bitbox.tools.InetTools;
import static pl.devsite.configuration.Configuration.str2boolean;

/**
 *
 * @author dmn
 */
public class ServerThread implements Runnable {

	private static final Logger logger = Logger.getLogger(ServerThread.class.getName());
	private String authenticatedUser;
	private long contentLength = -1;
	private String stringRequest;
	private String referer;
	private BufferedInputStream clientIn;
	private BufferedOutputStream clientOut;
	private InputStream inputStream;
	private Socket socket;
	private byte[] buffer = new byte[1024 * 64];
	private Sendable root;
	private BitBoxConfiguration bitBoxConfiguration = BitBoxConfiguration.getInstance();

	public ServerThread(final Socket socket_, final Sendable root) {
		this.socket = socket_;
		this.root = root;
	}

	private static String divideStringRequest(String path) {
		StringBuilder result = new StringBuilder();
		StringBuilder prev = new StringBuilder();
		prev.append("/");
		result.append("<a href=\"/\">[root]</a>");
		for (String s : path.split("/")) {
			if (!"".equals(s)) {
				prev.append(EncodingTools.urlEncodeUTF(s));
				prev.append("/");
				result.append(" / <a href=\"" + prev.toString() + "\">" + s + "</a>");
			}
		}
		return result.toString();
	}

	private void sendUTF8(String text) throws IOException {
		String tmp = text + "\r\n";
		clientOut.write(tmp.getBytes("UTF-8"));
		//System.out.println(tmp.replace("\n", "\n<<< "));
	}

	private void sendHeader(String stringRequest) throws IOException {
		if (!"".equals(stringRequest)) {
			String title;
			title = bitBoxConfiguration.getProperty(PROPERTY_NAME) + " - " + stringRequest;
			sendUTF8(bitBoxConfiguration.getHeadBodyHTTP().replace(bitBoxConfiguration.getProperty(PROPERTY_NAME), title));
		} else {
			sendUTF8(bitBoxConfiguration.getHeadBodyHTTP());
		}
		String menu = bitBoxConfiguration.getMenuHtml();
		if (menu != null && !menu.isEmpty()) {
			sendUTF8(menu);
		}
		sendUTF8("<div id=\"CONTENT\">");
		clientOut.write(("<div id=\"HEADER\">" + divideStringRequest(stringRequest) + "</div>\r\n").getBytes("UTF-8"));
	}

	private void sendFooter(BufferedOutputStream clientOut) throws IOException {
		StringBuilder b = new StringBuilder();
		b.append("\r\n<div id=\"FOOTER\">");
		b.append("<div class=\"left\">");
		b.append("Generated by: <a href=\"").append(bitBoxConfiguration.getProperty(PROPERTY_WWW)).append("\">").append(bitBoxConfiguration.getProperty(PROPERTY_NAME)).append("</a>");
		b.append(" on ").append(SimpleDateFormat.getDateTimeInstance().format(new Date()));
		b.append("</div>");

		if (str2boolean(bitBoxConfiguration.getProperty(PROPERTY_SHOW_INTERNAL_IP_IN_FOOTER))) {
			List<String> internalIp = InetTools.tryToGuessIp();
			if (internalIp != null && !internalIp.isEmpty()) {
				b.append("\r\n<div class=\"right\">");
				for (String ip : internalIp) {
					b.append("&nbsp;[").append("<a href=\"http://").append(ip).append(":").append(bitBoxConfiguration.getProperty(PROPERTY_PORT)).append("/\">").append(ip).append("</a>]");
				}
				b.append("</div>");
			}
		}

		if (str2boolean(bitBoxConfiguration.getProperty(PROPERTY_SHOW_EXTERNAL_IP_IN_FOOTER))) {
			String ip = InetTools.getExternalIp();
			if (ip != null && !ip.isEmpty()) {
				b.append("\r\n<div class=\"right\">");
				b.append("&nbsp;[").append("<a href=\"http://").append(ip).append(":").append(bitBoxConfiguration.getProperty(PROPERTY_PORT)).append("/\">").append(ip).append("</a>]");
				b.append("</div>");
			}
		};

		b.append("</div>").append("\r\n");
		b.append("</div></body></html>");
		sendUTF8(b.toString());
	}

	private void sendStream(InputStream in, Integer rangeStart, Integer rangeStop) throws IOException {
		int count;
		if (rangeStop != null || rangeStart != null) {
			long send = 0;
			if (rangeStart != null) {
				in.skip(rangeStart);
				send = rangeStart;
			}
			if (rangeStop != null) {
				rangeStop++; // inclusive
			}
			while (true) {
				if (rangeStop != null && buffer.length + send > rangeStop) {
					count = in.read(buffer, 0, (int) (-send + rangeStop));
				} else {
					count = in.read(buffer);
				}
				if (count > 0) {
					clientOut.write(buffer, 0, count);
					send += count;
				} else {
					break;
				}
			}
		} else {
			while (true) {
				count = in.read(buffer);
				if (count > 0) {
					clientOut.write(buffer, 0, count);
				} else {
					break;
				}
			}
		}
	}

	private void sendIcyStreamTitle(String title, int chunkSize) throws IOException {
		byte[] fileBuffer = new byte[chunkSize];
		Arrays.fill(fileBuffer, (byte) 0);
		String s = "StreamTitle='" + title + "';";
		int dlugosc = 1 + s.length() / 16;
		fileBuffer[0] = (byte) dlugosc;
		for (int i = 0; i < s.length(); i++) {
			fileBuffer[i + 1] = (byte) s.charAt(i);
		}
		clientOut.write(fileBuffer, 0, dlugosc * 16 + 1);
	}

	private void sendIcyStream(InputStream in, Sendable sendable, int chunkSize) throws IOException {
		int n;
		boolean sendHeader = false;
		String name = sendable.toString();
		if (sendable instanceof SendableFile) {
			InputStream encodedStream;
			SendableFile sf = (SendableFile) sendable;
			try {
				MusicEncoder encoder = new MusicEncoder();
				encodedStream = encoder.encode(sf.getFile().getCanonicalPath());
				if (encodedStream != null) {
					in.close();
					in = encodedStream;
				}
			} catch (IOException e) {
				logger.severe("Could not initialize MusicEncoder " + e.getMessage());
			}
		}
		if (sendable instanceof SendableFileWithMimeResolver) {
			SendableFileWithMimeResolver sf = (SendableFileWithMimeResolver) sendable;
			StringBuilder nameBuilder = new StringBuilder();
			String album = sf.getMetadataValue(SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX + "Album");
			String artist = sf.getMetadataValue(SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX + "Artist");
			String title = sf.getMetadataValue(SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX + "Title");
			String length = sf.getMetadataValue(SendableFileWithMimeResolver.AUDIO_HTTP_HEADER_PREFIX + "Length");
			if (artist != null) {
				nameBuilder.append(artist).append(' ');
			}
			if (album != null) {
				nameBuilder.append("[").append(album).append("] ");
			}
			if (title != null) {
				if (nameBuilder.length() > 0) {
					nameBuilder.append("- ");
				}
				nameBuilder.append(title).append(' ');
			}
			if (nameBuilder.length() > 0) {
				name = nameBuilder.substring(0, nameBuilder.length() - 1);
			}
		}

		byte[] fileBuffer = new byte[chunkSize];
		try {
			int lastWrote = 0;
			while ((n = in.read(fileBuffer, 0, lastWrote < chunkSize ? chunkSize - lastWrote : chunkSize)) > 0) {
				clientOut.write(fileBuffer, 0, n);
				lastWrote += n;
				while (lastWrote >= chunkSize) {
					lastWrote -= chunkSize;
				}
				if (lastWrote == 0) {
					if (sendHeader) {
						clientOut.write(0);
					} else {
						sendHeader = true;
						sendIcyStreamTitle(name, chunkSize);
					}
				} else {
				}
			}
			Arrays.fill(fileBuffer, (byte) 0);
			clientOut.write(fileBuffer, 0, chunkSize - lastWrote);
			clientOut.write(0);
		} finally {
		}
	}
	private Sendable response = null;
	private HttpAuthenticator authenticator = null;
	private String authorization = null;
	private Integer rangeStart = null;
	private Integer rangeStop = null;
	private int httpResultCode = 0;
	private boolean headRequest = false;
	private boolean getRequest = false;
	private boolean postRequest = false;
	private HttpHeader requestHeader = null;
	private String contentType = null;
	private String host;
	private boolean icyMetaData = false;

	private void parseRequestAttributes() {
		String temp;
		authorization = requestHeader.get(HttpTools.AUTHORIZATION);
		temp = requestHeader.get(HttpTools.RANGE);
		if (temp != null) {
			String range = temp;
			int pos1 = range.indexOf('=');
			int pos2 = range.indexOf('-');
			int posmulti = range.indexOf(','); // multi ranges are not supported
			if (range.startsWith("bytes") && pos1 > 0 && pos2 > pos1 && posmulti == -1) {
				String sstart = range.substring(pos1 + 1, pos2).trim();
				String sstop = range.substring(pos2 + 1).trim();
				try {
					if (!sstart.equals("")) {
						rangeStart = Integer.valueOf(sstart);
					}
					if (!sstop.equals("")) {
						rangeStop = Integer.valueOf(sstop); //range is inclusive
					}
				} catch (NumberFormatException e) {
					rangeStart = null;
					rangeStop = null;
				}
			} else {
				httpResultCode = 416;
			}
		}
		temp = requestHeader.get(HttpTools.CONTENTLENGTH);
		if (temp != null) {
			contentLength = Long.valueOf(temp);
		}
		contentType = requestHeader.get(HttpTools.CONTENTTYPE);
		referer = requestHeader.get(HttpTools.REFERER);
		host = requestHeader.get(HttpTools.HOST);
		{
			String icyMetaDataStr = requestHeader.get(HttpTools.ICYMETADATA);
			icyMetaData = "1".equals(icyMetaDataStr);
		}
	}

	private boolean isOperationAllowed() throws IOException {
		boolean result = false;
		if (httpResultCode > 0) {
			sendUTF8(HttpTools.createHttpResponse(httpResultCode, bitBoxConfiguration.getProperty(PROPERTY_NAME), true));
		} else if (stringRequest == null || (postRequest && (contentLength < 1 || contentType == null))) {
			logger.log(Level.WARNING, "bad request, invader: {0}", socket.getInetAddress().getHostAddress());
			sendUTF8(HttpTools.createHttpResponse(400, bitBoxConfiguration.getProperty(PROPERTY_NAME), true));
		} else if (!icyMetaData && (getRequest || headRequest) && authenticator != null && authenticatedUser == null) {
			logger.log(Level.WARNING, "not authorized, invader: {0}", socket.getInetAddress().getHostAddress());
			sendUTF8(HttpTools.createHttpResponse(401, HttpTools.WWWAUTHENTICATE, authenticator.getAuthenticate()));
		} else if (response == null && (getRequest || headRequest)) {
			logger.log(Level.WARNING, "not found: {0}, invader: {1}", new Object[]{stringRequest, socket.getInetAddress().getHostAddress()});
			sendUTF8(HttpTools.createHttpResponse(404, bitBoxConfiguration.getProperty(PROPERTY_NAME), true));
		} else if (icyMetaData && !("audio/mpeg".equals(response.getMimeType()) 
				 //|| "audio/ogg".equals(response.getMimeType())
				|| (response.getMimeType() != null && response.getMimeType().startsWith("audio/"))
				 )) {
			logger.log(Level.WARNING, "streaming forbidden, invader: {0}", socket.getInetAddress().getHostAddress());
			sendUTF8(HttpTools.createHttpResponse(403, bitBoxConfiguration.getProperty(PROPERTY_NAME), true));
		} else if ((getRequest || headRequest) && response.hasChildren() && !stringRequest.isEmpty() && !stringRequest.endsWith("/")) {
			logger.log(Level.INFO, "redirecting to /{0}/", stringRequest);
			sendUTF8(HttpTools.createHttpResponse(301, HttpTools.LOCATION, "/" + stringRequest + "/"));
		} else if ((getRequest || headRequest) && response.getFilter() != null && !response.getFilter().isAllowed(response, socket.getInetAddress().getHostAddress(), authenticatedUser)) {
			logger.log(Level.WARNING, "not allowed, invader: {0}", socket.getInetAddress().getHostAddress());
			sendUTF8(HttpTools.createHttpResponse(403, bitBoxConfiguration.getProperty(PROPERTY_NAME), true));
		} else {
			result = true;
		}
		return result;
	}

	private void processAuthorization() {
		authenticator = response == null ? null : response.getAuthenticator();
		if (authenticator == HttpTools.NULLAUTHENTICATOR) {
			authenticator = null;
		}
		authenticatedUser = (authorization == null || authenticator == null) ? null : authenticator.allowed(authorization);
	}

	private void processRequestAndSendResult() throws IOException {
		if (response instanceof InputProcessor) {
			((InputProcessor) response).setRequestHeader(requestHeader);
			((InputProcessor) response).setRequestStream(clientIn);
		}
		inputStream = (getRequest || headRequest) ? response.getResponseStream() : null;
		if ((getRequest || headRequest) && (inputStream == null)) {
			logger.log(Level.WARNING, "not found: " + response + ", invader: " + socket.getInetAddress().getHostAddress());
			sendUTF8(HttpTools.createHttpResponse(404, bitBoxConfiguration.getProperty(PROPERTY_NAME), true));
		} else if (postRequest) {
			receiveFile();
		} else {
			logger.log(Level.INFO, (icyMetaData ? "streaming" : "sending") + " \"" + stringRequest + "\" to " + socket.getInetAddress().getHostAddress() + (authenticatedUser == null ? "" : " - " + authenticatedUser));
//			}
			BufferedInputStream bis = new BufferedInputStream(inputStream);
//			if (getRequest || headRequest) {
			boolean raw = response.isRawFile();
			if (icyMetaData && raw) {
				sendUTF8(HttpTools.createIcecastResponse(bitBoxConfiguration.getProperty(PROPERTY_NAME), 1024 * 16));
			} else if (raw && response instanceof HasHtmlHeaders) {
				sendUTF8(((HasHtmlHeaders) response).getHtmlHeader());
			} else if (rangeStart != null || rangeStop != null) {
				sendUTF8(HttpTools.createHttpResponse(206, bitBoxConfiguration.getProperty(PROPERTY_NAME),
						(rangeStart == null ? -1 : rangeStart),
						(rangeStop == null ? -1 : rangeStop),
						response.getContentLength(),
						response.getMimeType()));
			} else {
				sendUTF8(HttpTools.createHttpResponse(bitBoxConfiguration.getProperty(PROPERTY_NAME), response));
			}
			if (getRequest) {
				if (!raw) {
					sendHeader(stringRequest);
				}
				if (icyMetaData) {
					sendIcyStream(bis, response, 1024 * 16);
				} else {
					if (response.getContentLength() > 0 && rangeStop == null) {
						rangeStop = new Integer((int) response.getContentLength() - 1);
					}
					sendStream(bis, rangeStart, rangeStop);
				}
				if (!raw) {
					sendFooter(clientOut);
				}
			}
		}
	}

	private void processResponse() {
		if (getRequest || headRequest) {
			response = SendableAdapter.tryToFindSendable(root, stringRequest);
		}
	}

	private boolean receiveFile() throws FileNotFoundException, IOException {
		long done = 0;
		long written = 0;
		//requestHeader
		File file = null;
		FileOutputStream fileOutputStream = null;
		HttpHeader partHeader = new HttpHeader(clientIn);
		String contentDisposition = partHeader.get(HttpTools.CONTENTDISPOSITION);
		{
			int pos1 = contentDisposition.indexOf("filename=\"") + 10;
			int pos2 = contentDisposition.indexOf("\";", pos1);
			if (pos2 < 0) {
				pos2 = contentDisposition.length() - 1;
			}
			String fileName = contentDisposition.substring(pos1, pos2);
			for (String k : new String[]{"/", "\\", System.getProperty("file.separator")}) {
				pos1 = fileName.lastIndexOf(k);
				if (pos1 > 0) {
					fileName = fileName.substring(pos1 + 1);
				}
			}
			String decodedFileName = EncodingTools.urlDecodeUTF(fileName);
			file = new File(bitBoxConfiguration.getProperty(PROPERTY_OUTPUTDIRECTORY) + decodedFileName);
			if (file.exists()) {
				int i = 1;
				int p = decodedFileName.lastIndexOf(".");
				String k = null;
				do {
					k = (p < 1) ? decodedFileName + "." + i : decodedFileName.substring(0, p) + i + decodedFileName.substring(p);
					file = new File(bitBoxConfiguration.getProperty(PROPERTY_OUTPUTDIRECTORY) + k);
					i++;
				} while (file.exists());
			}
			fileOutputStream = new FileOutputStream(file);
			if (fileOutputStream == null) {
				logger.log(Level.WARNING, "bad post request, invader: {0}", socket.getInetAddress().getHostAddress());
				sendUTF8(HttpTools.createHttpResponse(400, bitBoxConfiguration.getProperty(PROPERTY_NAME), true));
				return false;
			}
		}

		socket.setReceiveBufferSize(buffer.length);
		socket.setSoTimeout(5000);

		String splitter = "\r\n" + partHeader.get(0) + "--\r\n";
		int splitterLength = splitter.length();
		boolean ok = false;
		int partHeaderLength = partHeader.getLength();
		long fileSize = contentLength - partHeaderLength - splitterLength;
		logger.log(Level.INFO, "receiving file \"{0}\" ({3} bytes) from {1}{2}", new Object[]{file.getCanonicalPath(), socket.getInetAddress().getHostAddress(), authenticatedUser == null ? "" : " - " + authenticatedUser, fileSize});
		try {
			done = partHeaderLength;
			int count = 0;
			while (count != -1 && done < contentLength) {
				int retrycount = 5;
				while ((count = clientIn.read(buffer)) < 1) {
					if (--retrycount < 0) {
						break;
					}
					logger.log(Level.WARNING, "waiting for data, {0}", file.getCanonicalPath());
					try {
						Thread.sleep(5000);
					} catch (InterruptedException ex) {
					}
				}
				if (count > 0) {
					//System.out.println("" + new String(buffer, 0, count) + "!");
					if (written < fileSize) {
						if (written + count > fileSize) {
							fileOutputStream.write(buffer, 0, (int) (fileSize - written));
							written += fileSize - written;
						} else {
							fileOutputStream.write(buffer, 0, count);
							written += count;
						}
					}
					done += count;
				} else {
					break;
				}
			}
			fileOutputStream.close();
			fileOutputStream = null;
			long realFileSize = file.length();
			ok = (done == contentLength) && (fileSize == realFileSize) && (written == realFileSize);
		} finally {
			String back = referer != null ? referer : stringRequest != null ? stringRequest : "/";
			if (ok) {
				String message = "succesfully received file \"" + file.getName() + "\" (" + fileSize + " bytes) from " + socket.getInetAddress().getHostAddress() + (authenticatedUser == null ? "" : " - " + authenticatedUser);
				logger.log(Level.INFO, message);
				sendUTF8(HttpTools.createHttpResponse(200, bitBoxConfiguration.getProperty(PROPERTY_NAME), -1, HttpTools.CONTENTTYPE_TEXT_HTML));
				sendHeader(stringRequest);
				sendUTF8("<h3>OK</h3><p>" + message + "</p><p><a href=\"" + back + "\">send more...</a></p>");
			} else {
				String message = "failed receiving file \"" + file.getName() + "\" from " + socket.getInetAddress().getHostAddress() + (authenticatedUser == null ? "" : " - " + authenticatedUser);
				logger.log(Level.SEVERE, message);
				sendUTF8(HttpTools.createHttpResponse(400, bitBoxConfiguration.getProperty(PROPERTY_NAME), -1, HttpTools.CONTENTTYPE_TEXT_HTML));
				sendHeader(stringRequest);
				sendUTF8("<h3>ERROR</h3><p>" + message + "</p><p><a href=\"" + back + "\">send more...</a></p>");
			}
			sendFooter(clientOut);
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
			return ok;
		}
	}

	@Override
	public void run() {
		try {
			clientIn = new BufferedInputStream(socket.getInputStream());

			requestHeader = new HttpHeader(clientIn);
			getRequest = HttpRequestType.GET.equals(requestHeader.getType());
			headRequest = HttpRequestType.HEAD.equals(requestHeader.getType());
			postRequest = HttpRequestType.POST.equals(requestHeader.getType());
			stringRequest = requestHeader.getRequestedString();
			if (stringRequest == null) {
				httpResultCode = 400;
			} else {
				parseRequestAttributes();
			}
			processResponse();
			processAuthorization();

			clientOut = new BufferedOutputStream(socket.getOutputStream());
			if (isOperationAllowed()) {
				processRequestAndSendResult();
			}
			clientOut.flush();
		} catch (SocketException ex) {
			logger.log(Level.SEVERE, ex.getMessage());
		} catch (IOException ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException ex) {
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			}
			if (clientOut != null) {
				try {
					clientOut.close();
				} catch (IOException ex) {
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			}
			try {
				socket.close();
			} catch (IOException ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}

	}
}
