package pl.devsite.bitbox.sendables;

import pl.devsite.bitbox.server.*;

/**
 *
 * @author dmn
 */
public class SendableError extends SendableString implements HasHtmlHeaders {

    private final String header;

    public SendableError(Sendable parent, String name, int code, String server) {
        super(parent, name, null, "<h1>" + code + " " + HttpTools.getHttpCodes().get(code) + "</h1><hr><i>" + server + "</i>");
        header = HttpTools.createHttpResponse(code, server, -1, HttpTools.CONTENTTYPE_TEXT_HTML);
    }

    public SendableError(Sendable parent, String name, int code, String[][] headers) {
        super(parent, name, null, 
                findIsTextHtml(headers) ?
                "<h1>" + code + " " + HttpTools.getHttpCodes().get(code) + "</h1><hr><i>" + findServer(headers) + "</i>" :
                    code + " " + HttpTools.getHttpCodes().get(code) + ", " + findServer(headers)
                    );
        header = HttpTools.createHttpResponse(code, headers);
    }

    private static String findServer(String[][] headers) {
        String server = null;
        for (String[] k : headers) {
            if (HttpTools.SERVER.equals(k[0])) {
                server = k[1];
                break;
            }
        }
        return server;
    }

    private static boolean findIsTextHtml(String[][] headers) {
        for (String[] k : headers) {
            if (HttpTools.CONTENTTYPE.equals(k[0]) && HttpTools.CONTENTTYPE_TEXT_HTML.equals(k[1])) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getHtmlHeader() {
        return header;
    }

    @Override
    public Sendable getChild(Object id) {
        return this;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

}
