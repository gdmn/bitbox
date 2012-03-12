package pl.devsite.bitbox.server;

import java.io.IOException;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableAdapter;
import pl.devsite.bitbox.sendables.SendableFileWithMimeResolver;
import pl.devsite.bitbox.sendables.SendableString;
import pl.devsite.bitbox.server.*;
import pl.devsite.bitbox.server.renderers.IcyRenderer;
import pl.devsite.bitbox.server.renderers.Renderer;
import pl.devsite.bitbox.server.renderers.RendererFactory;

/**
 *
 * @author dmn
 */
public class Router implements Processor<Object> {

	private BitBoxConfiguration config = BitBoxConfiguration.getInstance();
	private RequestContext context;

	@Override
	public void initialize(RequestContext context) {
		this.context = context;
	}

	@Override
	public Object execute() throws Exception {
		processRequestAndSendResult();
		return null;
	}

	public Object execute(Object command) throws Exception {
		if (command instanceof Processor) {
			return ((Processor) command).execute();
		}

		if (command instanceof Sendable) {
			return command;
		}

		Object result = null;

		Sendable sendable = null;
		Operation op = null;

		if (command instanceof Operation) {
			op = (Operation) command;
		} else if (command instanceof String) {
			op = new Operation((String) command);
		}

		Operation.Type type = op == null ? null : op.getType();

		if (Operation.Type.UNKNOWN.equals(type)) {
			sendable = SendableAdapter.tryToFindSendable(context.getSendableRoot(), op.getArgument());
			if (sendable != null) {
				result = sendable;
			} else {
				result = "error:404";
			}
		} else if (Operation.Type.ERROR.equals(type)) {
			HttpHeader resultHeader = new HttpHeader();
			int code = 400;
			int colonIndex = op.getArgument().indexOf(':');
			String codeArgument = op.getArgument(), messageArgument = null;
			if (colonIndex > 0) {
				codeArgument = op.getArgument().substring(0, colonIndex);
				messageArgument = op.getArgument().substring(colonIndex + 1);
			}
			try {
				code = Integer.parseInt(codeArgument);
			} catch (Exception e) {
				messageArgument = codeArgument;
			}
			result = createError(resultHeader, code, messageArgument);
			context.setResponseHeader(resultHeader);
		}
		return result;
	}

	private static Sendable createError(HttpHeader header, int code, String message) {
		if (message == null) {
			message = HttpTools.getHttpCodes().get(code);
		}
		Sendable sendable = new SendableString(null, "error", message);
		header.setHttpResponseCode(code);
		return sendable;
	}

	private static Sendable createError(HttpHeader header, int code) {
		return createError(header, code, HttpTools.getHttpCodes().get(code));
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
			//logger.log(Level.INFO, (context.isIcyMetadata() ? "streaming" : (context.isHeadRequest() ? "head" : "sending")) + " \"" + context.getStringRequest() + "\" to " + context.getHostAddress() + (context.getAuthenticatedUser() == null ? "" : " - " + context.getAuthenticatedUser()));
//			}
			Sendable response = context.getSendableResponse();
			HttpHeader header = context.getResponseHeader();
			if (context.isIcyMetadata() && (header.getHttpResponseCode() == 200 || header.getHttpResponseCode() == 0)) {
				String encoder = config.getProperty(BitBoxConfiguration.PROPERTY_TOOLS_ENCODER);
				header.setHttpResponseCode(200);
				if ("lame".equals(encoder)) {
					header.appendContentType("audio/mpeg").appendIcyMetaint("" + IcyRenderer.CHUNK_SIZE).setFirstLineOfResponse("ICY 200 OK");
				} else {
					header.appendContentType("application/ogg");
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
				if (response != null) {
					header.appendContentType(response.getMimeType());
				}
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

			//Processor renderer = RendererFactory.getRenderer(context);
		}
	}
}
