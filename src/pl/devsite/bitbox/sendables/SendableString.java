package pl.devsite.bitbox.sendables;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class SendableString extends SendableStream {

    private byte[] byteArrayString = null;

    public SendableString(Sendable parent, String name, String mimeType, String string) {
        super(parent, name, mimeType, null, -1);
        try {
            byteArrayString = string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SendableString.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public SendableString(Sendable parent, String name, String string) {
        this(parent, name, "text/plain", string);
    }

    @Override
    public long getContentLength() {
        return byteArrayString.length;
    }

    @Override
    public InputStream getResponseStream() {
        return new ByteArrayInputStream(byteArrayString);
    }
}
