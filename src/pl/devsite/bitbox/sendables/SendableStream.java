package pl.devsite.bitbox.sendables;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class SendableStream extends SendableAdapter {

    private String mimeType;
    private BufferedInputStream stream;
    private long lengthOfStream;

    public SendableStream(Sendable parent, String name, String mimeType, InputStream stream, long lengthOfStream) {
        super(parent, name);
        this.mimeType = mimeType;
        this.stream = new BufferedInputStream(stream);
        this.lengthOfStream = lengthOfStream;
        this.stream.mark(Integer.MAX_VALUE);
    }

    public SendableStream(Sendable parent, String name, String mimeType, InputStream stream) {
        this(parent, name, mimeType, stream, -1);
    }

    @Override
    public InputStream getResponseStream() {
        try {
            stream.reset();
        } catch (IOException ex) {
            Logger.getLogger(SendableStream.class.getName()).log(Level.SEVERE, null, ex);
        }
        return stream;
    }

//    protected void setInputStream(InputStream stream) {
//        this.stream = stream;
//    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public long getContentLength() {
        return lengthOfStream;
    }

//    protected void setContentLength(long lengthOfStream) {
//        this.lengthOfStream = lengthOfStream;
//    }

    @Override
    public boolean isRawFile() {
        return true;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public Sendable[] getChildren() {
        return null;
    }

    @Override
    public Sendable getChild(Object id) {
        return null;
    }
}
