package pl.devsite.bitbox.server.renderers;

import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.server.Processor;
import pl.devsite.bitbox.server.RequestContext;

/**
 *
 * @author dmn
 */
public class RendererFactory {

	public static Processor getRenderer(RequestContext context) {
		Processor renderer = null;
		if (context.isGetRequest()) {
			if (context.isIcyMetadata() && context.getResponseHeader().getHttpResponseCode() == 200) {
				renderer = new IcyRenderer();
				//sendIcyStream(bis, response, 1024 * 16);
			} else {
				Sendable response = context.getSendableResponse();
				if (response != null) {
					if (response.getContentLength() > 0 && context.getRangeStop() == null) {
						context.setRangeStop(new Integer((int) response.getContentLength() - 1));
					}
				}
				renderer = new Renderer();
				//sendStream(bis, context.getRangeStart(), context.getRangeStop());
			}
		}

		if (renderer == null) {
			renderer = new Renderer();
		}

		renderer.initialize(context);

		return renderer;
	}
}
