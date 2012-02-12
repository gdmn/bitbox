package pl.devsite.bitbox.server.renderers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableFileWithMimeResolver;
import pl.devsite.bitbox.server.BitBoxConfiguration;

import static pl.devsite.bitbox.server.BitBoxConfiguration.*;
import pl.devsite.bitbox.server.HttpHeader;
import pl.devsite.bitbox.server.HttpTools;
import pl.devsite.bitbox.server.RequestContext;

/**
 *
 * @author dmn
 */
public class Router {

	private static final Logger logger = Logger.getLogger(Router.class.getName());
	private BitBoxConfiguration config = BitBoxConfiguration.getInstance();
	private RequestContext context;
	private Renderer renderer;

	public Router(RequestContext context) {
		this.context = context;
	}

	public Renderer getRenderer() throws IOException {
		if (renderer == null) {
			processRequestAndSendResult();
		}
		return renderer;
	}

//
//	private void sendUTF8(String text) throws IOException {
//		String tmp = text + "\r\n";
//		context.getClientOut().write(tmp.getBytes("UTF-8"));
//		//System.out.println(tmp.replace("\n", "\n<<< "));
//	}
	private void processRequestAndSendResult() throws IOException {
//		if (response instanceof InputProcessor) {
//			((InputProcessor) response).setRequestHeader(requestHeader);
//			((InputProcessor) response).setRequestStream(clientIn);
//		}
		// wywalic inputStream = (parser.isGetRequest() || parser.isHeadRequest()) ? response.getResponseStream() : null;
		if (context.isPostRequest()) {
			//receiveFile();
		} else {
			logger.log(Level.INFO, (context.isIcyMetadata() ? "streaming" : (context.isHeadRequest() ? "head" : "sending")) + " \"" + context.getStringRequest() + "\" to " + context.getHostAddress() + (context.getAuthenticatedUser() == null ? "" : " - " + context.getAuthenticatedUser()));
//			}
			BufferedInputStream bis = new BufferedInputStream(context.getResponseStream());
			Sendable response = context.getSendableResponse();
			HttpHeader header = context.getResponseHeader();
			if (context.isIcyMetadata()) {
				String encoder = config.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_ENCODER);
				if ("lame".equals(encoder)) {
					header.appendContentType("audio/mpeg").appendIcyMetaint("" + IcyRenderer.CHUNK_SIZE).setFirstLineOfResponse("ICY 200 OK");
				} else {
					header.appendContentType("application/ogg").setHttpResponseCode(200);
				}
				header.appendNoCache().appendIcyNotice(
						"<BR>This stream requires <a href=\"http://www.icecast.org/3rdparty.php\">a media player that support Icecast</a><BR>",
						config.getProperty(BitBoxConfiguration.PROPERTY_NAME) + "<BR>").appendIcyName(config.getProperty(BitBoxConfiguration.PROPERTY_NAME)).appendIcyGenre("various").appendIcyPub("1");
			} else if (context.getRangeStart() != null || context.getRangeStop() != null) {
				header.appendConnectionClose();
				header.appendServer();
				header.appendContentType(response.getMimeType());
				header.appendRange((context.getRangeStart() == null ? -1 : context.getRangeStart()),
						(context.getRangeStop() == null ? -1 : context.getRangeStop()),
						response.getContentLength());
				header.setHttpResponseCode(206);
			} else {
				header.appendConnectionClose();
				header.appendServer();
				header.appendContentType(response.getMimeType());
				if (header.getHttpResponseCode() < 1) {
					header.setHttpResponseCode(200);
				}
				String metadata;
				if (response instanceof SendableFileWithMimeResolver) {
					SendableFileWithMimeResolver sf = (SendableFileWithMimeResolver) response;
					metadata = sf.getMetadata();
					if (metadata != null) {
						for (String s : metadata.split("\r\n")) {
							header.addHeaderLine(s);
						}
					}
				}
			}
			//sendUTF8(header.toString());

			if (context.isGetRequest()) {
				if (context.isIcyMetadata()) {
					renderer = new IcyRenderer(context);
					//sendIcyStream(bis, response, 1024 * 16);
				} else {
					if (response.getContentLength() > 0 && context.getRangeStop() == null) {
						context.setRangeStop(new Integer((int) response.getContentLength() - 1));
					}
					renderer = new Renderer(context);
					//sendStream(bis, context.getRangeStart(), context.getRangeStop());
				}
			}

			if (renderer == null) {
				renderer = new Renderer(context);
			}
		}
	}
}
