package pl.devsite.bitbox.sendables;

import pl.devsite.bitbox.authenticator.HttpAuthenticator;
import java.io.InputStream;

public interface Sendable {
    int ATTR_HIDDEN = 1;
    int ATTR_NOTHING = 0;

    String getAddress();
    InputStream getResponseStream();
    /** http://www.iana.org/assignments/media-types/
     * http://www.webmaster-toolkit.com/mime-types.shtml
     */
    String getMimeType();
    long getContentLength();
    
    int getAttributes();
    HttpAuthenticator getAuthenticator();
    boolean hasChildren();
    Sendable[] getChildren();
    Sendable getChild(Object id);
    Sendable getParent();
    SendableFilter getFilter();
}
