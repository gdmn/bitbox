package pl.devsite.bitbox.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import pl.devsite.bitbox.sendables.Sendable;

/**
 *
 * @author dmn
 */
public class Parser {
	private RequestContext context = new RequestContext();
	private boolean parsed = false;

	public Parser(Socket socket, Sendable root) throws IOException {
		context.setSocket(socket);
		context.setSendableRoot(root);
		context.setClientIn(new BufferedInputStream(socket.getInputStream()));
		context.setClientOut(new BufferedOutputStream(socket.getOutputStream()));

		context.setRequestHeader(new HttpHeader(context.getClientIn()));
	}

	public RequestContext getContext() {
		if (!parsed) parseRequestAttributes();
		return context;
	}

	private void parseRequestAttributes() {
		parsed = true;
		String temp;
		temp = context.getRequestHeader().get(HttpTools.RANGE);
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
						context.setRangeStart(Integer.valueOf(sstart));
					}
					if (!sstop.equals("")) {
						context.setRangeStop(Integer.valueOf(sstop)); //range is inclusive
					}
				} catch (NumberFormatException e) {
					context.setRangeStart( null);
					context.setRangeStart( null);
				}
			} else {
				context.setTemporaryResultCode(416);
			}
		}
	}
	
}
