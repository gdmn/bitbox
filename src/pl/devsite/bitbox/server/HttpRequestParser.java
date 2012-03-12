package pl.devsite.bitbox.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableAdapter;

/**
 *
 * @author dmn
 */
public class HttpRequestParser implements Processor<String> {
	private RequestContext context;

	@Override
	public void initialize(RequestContext context) {

		this.context = context;
		try {
			context.setClientIn(new BufferedInputStream(context.getSocket().getInputStream()));
			context.setClientOut(new BufferedOutputStream(context.getSocket().getOutputStream()));

			context.setRequestHeader(new HttpHeader(context.getClientIn()));
		} catch (IOException ex) {
			this.context.getExceptionListener().exceptionThrown(ex);
		}
	}

	@Override
	public String execute() throws Exception {
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
				context.setResponseHeader(new HttpHeader(416));
			}
		}
//		Sendable sendable = SendableAdapter.tryToFindSendable(context.getSendableRoot(), context.getStringRequest());
//		context.setSendableResponse(sendable);
		return context.getStringRequest();
	}

}
