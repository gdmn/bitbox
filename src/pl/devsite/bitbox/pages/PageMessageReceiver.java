package pl.devsite.bitbox.pages;

import pl.devsite.bitbox.authenticator.HttpAuthenticator;
import pl.devsite.bitbox.sendables.SendableRoot;
import pl.devsite.bitbox.server.HttpTools;
import pl.devsite.bitbox.server.servlets.ServletAdapter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class PageMessageReceiver extends ServletAdapter {

    public PageMessageReceiver(SendableRoot sl, String string) {
        super(sl, string);
    }

    @Override
    public InputStream getResponseStream() {
        try {
            return new ByteArrayInputStream((requestHeader.toString() + requestContent).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
        }
        return null;
    }

    @Override
    public HttpAuthenticator getAuthenticator() {
        return null;
    }

    @Override
    public long getContentLength() {
        return requestHeader.toString().length() + requestContent.length();
    }

    @Override
    public String getHtmlHeader() {
        return HttpTools.createHttpResponse(200);
    }

    @Override
    public void setRequestStream(InputStream requestContentStream) {
        super.setRequestStream(requestContentStream);
        try {
            getRequestContent();
        } catch (IOException ex) {
            Logger.getLogger(PageMessageReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
