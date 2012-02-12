package pl.devsite.bitbox.server.renderers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.server.BitBoxConfiguration;
import pl.devsite.bitbox.server.EncodingTools;
import pl.devsite.bitbox.server.HttpHeader;
import pl.devsite.bitbox.server.HttpTools;
import static pl.devsite.bitbox.server.BitBoxConfiguration.*;
/**
 *
 * @author dmn
 */
public class FileReceiverRenderer {

	private static final Logger logger = Logger.getLogger(FileReceiverRenderer.class.getName());
	private BitBoxConfiguration bitBoxConfiguration = BitBoxConfiguration.getInstance();
/*
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
	* */
}
