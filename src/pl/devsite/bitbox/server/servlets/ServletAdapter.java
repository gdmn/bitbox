package pl.devsite.bitbox.server.servlets;

import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableAdapter;
import pl.devsite.bitbox.server.HttpHeader;
import pl.devsite.bitbox.server.HttpTools;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author dmn
 */
public abstract class ServletAdapter  {
/*
    protected InputStream requestContentStream;
    //protected String requestHeader;
    protected String requestContent;
    protected HttpHeader requestHeader;
    protected HttpHeader responseHeader;

    public ServletAdapter(Sendable parent, String name) {
        super(parent, name);
    }

    @Override
    public boolean isRawFile() {
        return true;
    }

    @Override
    public void setRequestStream(InputStream requestContentStream) {
        this.requestContentStream = requestContentStream;
        this.requestContent = null;
    }

    @Override
    public void setRequestHeader(HttpHeader requestHeader) {
        this.requestHeader = requestHeader;
    }

    public InputStream getRequestContentStream() {
        return requestContentStream;
    }

    public HttpHeader getRequestHeader() {
        return requestHeader;
    }

    @Override
    public String getMimeType() {
        return HttpTools.CONTENTTYPE_TEXT_HTML;
    }

    public String getRequestContent() throws IOException {
        if (requestContentStream == null) {
            return null;
        }
        if (requestContent == null) {
            BufferedInputStream bis = new BufferedInputStream(requestContentStream);
            byte[] buffer = new byte[1024 * 8];
            StringBuffer buf = new StringBuffer();
            int count;
            while (bis.available() > 0) {
                count = bis.read(buffer);
                String part = new String(buffer, 0, count);
                buf.append(part);
            }
            requestContent = buf.toString();
        }
        return requestContent;
    }
	*/
}
