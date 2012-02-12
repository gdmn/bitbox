package pl.devsite.bitbox.sendables;

import pl.devsite.bitbox.server.renderers.HtmlLister;
import pl.devsite.bitbox.server.*;
import java.io.InputStream;

/**
 *
 * @author dmn
 */
public class SendableTouple extends SendableAdapter {
    private Sendable master, slave;

    public SendableTouple(Sendable master, Sendable slave) {
        super(master.getParent(), master.toString());
        this.master = master;
        this.slave = slave;
    }

    @Override
    public InputStream getResponseStream() {
        return new HtmlLister(this);
    }

    @Override
    public String getMimeType() {
        return "text/html";
    }

    @Override
    public long getContentLength() {
        return -1;
    }

//    @Override
//    public boolean isRawFile() {
//        return false;
//    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public Sendable[] getChildren() {
        Sendable[] m = master.getChildren();
        Sendable[] s = slave.getChildren();
        Sendable[] result = new Sendable[m.length + s.length];
        int count = 0;
        for (Sendable k : m) {
            result[count++] = k;
        }
        for (Sendable k : s) {
            result[count++] = k;
        }
        return result;
    }

    @Override
    public Sendable getChild(Object id) {
        if (id == null) {
            return null;
        }
        Sendable m = master.getChild(id);
        if (m != null) {
            return m;
        }
        Sendable s = slave.getChild(id);
        return s;
    }

    @Override
    public SendableFilter getFilter() {
        return master.getFilter();
    }

}
