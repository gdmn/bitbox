package pl.devsite.bitbox.server.renderers;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.server.RequestContext;

/**
 *
 * @author dmn
 */
public class Renderer {

	private static final Logger logger = Logger.getLogger(Renderer.class.getName());
	protected RequestContext context;

	public Renderer(RequestContext context) {
		this.context = context;
	}

	public void send() throws IOException {
		sendHeader();
		if (context.isGetRequest()) {
			if (context.getResponseStream() != null) {
				sendStream(context.getResponseStream(), context.getRangeStart(), context.getRangeStop());
			}
		}
	}

	protected void sendHeader() throws IOException {
		//logger.log(Level.INFO, (context.isIcyMetadata() ? "streaming" : (context.isHeadRequest() ? "head" : "sending")) + " \"" + context.getStringRequest() + "\" to " + context.getHostAddress() + (context.getAuthenticatedUser() == null ? "" : " - " + context.getAuthenticatedUser()));
		if (context.getResponseHeader().getHttpResponseCode() == 200) {
			String sendingTo = " to " + context.getHostAddress() + (context.getAuthenticatedUser() == null ? "" : " - " + context.getAuthenticatedUser());
			String sendingOp;
			if (context.isHeadRequest()) {
				sendingOp = "head ";
			} else if (context.isIcyMetadata()) {
				sendingOp = "streaming ";
			} else {
				sendingOp = "sending ";
			}
			logger.log(Level.INFO, sendingOp + "\"" + context.getStringRequest() + "\"" + sendingTo);
		}
		byte[] headerBytes = (context.getResponseHeader().toString() + "\r\n").getBytes();
		if (headerBytes != null && headerBytes.length > 0) {
			write(headerBytes, 0, headerBytes.length);
		}
	}

	protected void write(byte[] buf, int off, int len) throws IOException {
		//System.out.println("" + new String(buf, off, len));
		context.getClientOut().write(buf, off, len);

	}

	private void sendStream(InputStream in, Integer rangeStart, Integer rangeStop) throws IOException {
		byte[] buffer = new byte[1024 * 64];
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
					write(buffer, 0, count);
					send += count;
				} else {
					break;
				}
			}
		} else {
			while (true) {
				count = in.read(buffer);
				if (count > 0) {
					write(buffer, 0, count);
				} else {
					break;
				}
			}
		}
	}
}
