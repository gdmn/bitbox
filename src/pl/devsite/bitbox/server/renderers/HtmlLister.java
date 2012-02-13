package pl.devsite.bitbox.server.renderers;

import pl.devsite.bitbox.sendables.Sendable;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.SendableRoot;
import pl.devsite.bitbox.server.BitBoxConfiguration;
import pl.devsite.bitbox.server.HttpTools;
import pl.devsite.bitbox.tools.InetTools;
import pl.devsite.configuration.Configuration;

public class HtmlLister extends InputStream {

	private static final Logger logger = Logger.getLogger(HtmlLister.class.getName());
	private BitBoxConfiguration bitBoxConfiguration = BitBoxConfiguration.getInstance();
    private Sendable sendable;
    private Comparator<Sendable> comparator;

    public HtmlLister(Sendable sendable) {
        super();
        this.sendable = sendable;
        comparator = new Comparator<Sendable>() {

            @Override
            public int compare(Sendable f1, Sendable f2) {
                boolean children1 = f1.hasChildren();
                boolean children2 = f2.hasChildren();
                return (children1 ^ children2) ? (children1 ? -1 : 1) : f1.toString().toLowerCase().compareTo(f2.toString().toLowerCase());
            }
        };
    }

    public String getHtmlList(Sendable... elements) {
        if (elements == null) {
            return null;
        }
        Arrays.sort(elements, comparator);
        StringBuilder listBuffer = new StringBuilder();
        listBuffer.append("<ul class=\"ls\">");
        for (Sendable f : elements) {
            if ((f.getAttributes() & Sendable.ATTR_HIDDEN) == 0) {
                listBuffer.append("<li>");
                boolean dir = f.hasChildren();
                //String href = EncodingTools.urlEncodeUTF(f.toString()) + (dir ? "/" : "");
                String href = f.toString() + (dir ? "/" : "");
                String s = dir ? "[" + f.toString() + "]" : f.toString();
                listBuffer.append(String.format("<a class=\"" + (dir ? "dir" : "file") + "\" href=\"%1$s\">%2$s", href, s));
                long len = f.getContentLength();
                if (len > 0) {
                    listBuffer.append(" <span class=\"size\">(" + String.format("%.02f MB", (double) len / (1024 * 1024)) + ")</span>");
                }
                listBuffer.append("</a></li>");
                listBuffer.append("\n");
            }
        }
        listBuffer.append("</ul>");
        return listBuffer.toString();
    }

    private String getHtmlList() {
        Sendable[] elements = sendable.getChildren();
        return getHtmlList(elements);
    }
    byte[] htmlListBuffer = null;
    int htmlListBufferPos = 0;

    @Override
    public int read() throws IOException {
        if (htmlListBuffer == null) {
            htmlListBuffer = (createHtmlHeader(null) + getHtmlList() +createHtmlFooter()).getBytes("UTF-8");
        }
        if (htmlListBufferPos >= htmlListBuffer.length) {
            return -1;
        } else {
            return htmlListBuffer[htmlListBufferPos++];
        }
    }


	/*private static String divideStringRequest(String path) {
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
	}*/
	private String divideStringRequest(Sendable s) {
		StringBuilder result = new StringBuilder();
		if (s.getParent() != null && !(s.getParent() instanceof SendableRoot)) {
			result.append(divideStringRequest(s.getParent()));
		} else {
			result.append("<a href=\"/\">[root]</a>");
		}
		if (!(s instanceof SendableRoot)) {
			result.append(" / <a href=\"" + s.getAddress() + "\">" + s + "</a>");
		}
		return result.toString();
	}

	private String createHtmlHeader(String stringRequest) throws IOException {
		StringBuilder result = new StringBuilder();
		if (!"".equals(stringRequest)) {
			String title;
			title = bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_NAME) + " - " + stringRequest;
			result.append(bitBoxConfiguration.getHeadBodyHTTP().replace(bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_NAME), title)).append(HttpTools.RN);
		} else {
			result.append(bitBoxConfiguration.getHeadBodyHTTP()).append(HttpTools.RN);
		}
		String menu = bitBoxConfiguration.getMenuHtml();
		if (menu != null && !menu.isEmpty()) {
			result.append(menu).append(HttpTools.RN);
		}
		result.append("<div id=\"CONTENT\">").append(HttpTools.RN);
		result.append("<div id=\"HEADER\">" + divideStringRequest(sendable) + "</div>").append(HttpTools.RN);
		return result.toString();
	}

	private String createHtmlFooter() throws IOException {
		StringBuilder b = new StringBuilder();
		b.append("\r\n<div id=\"FOOTER\">");
		b.append("<div class=\"left\">");
		b.append("Generated by: <a href=\"").append(bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_WWW)).append("\">").append(bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_NAME)).append("</a>");
		b.append(" on ").append(SimpleDateFormat.getDateTimeInstance().format(new Date()));
		b.append("</div>");

		if (Configuration.str2boolean(bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_SHOW_INTERNAL_IP_IN_FOOTER))) {
			List<String> internalIp = InetTools.tryToGuessIp();
			if (internalIp != null && !internalIp.isEmpty()) {
				b.append("\r\n<div class=\"right\">");
				for (String ip : internalIp) {
					b.append("&nbsp;[").append("<a href=\"http://").append(ip).append(":").append(bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_PORT)).append("/\">").append(ip).append("</a>]");
				}
				b.append("</div>");
			}
		}

		if (Configuration.str2boolean(bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_SHOW_EXTERNAL_IP_IN_FOOTER))) {
			String ip = InetTools.getExternalIp();
			if (ip != null && !ip.isEmpty()) {
				b.append("\r\n<div class=\"right\">");
				b.append("&nbsp;[").append("<a href=\"http://").append(ip).append(":").append(bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_PORT)).append("/\">").append(ip).append("</a>]");
				b.append("</div>");
			}
		};

		b.append("</div>").append("\r\n");
		b.append("</div></body></html>");
		return b.toString();
	}

}
