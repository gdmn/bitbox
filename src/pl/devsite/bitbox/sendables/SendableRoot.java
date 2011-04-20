package pl.devsite.bitbox.sendables;

import pl.devsite.bitbox.server.*;
import java.io.InputStream;
import java.util.ArrayList;

/**
 *
 * @author gorladam
 */
public class SendableRoot extends SendableAdapter {

    private ArrayList<Sendable> list = new ArrayList<Sendable>();
    private SendableFilter filter;

    public SendableRoot(Sendable parent, String name) {
        super(parent, name);
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

    @Override
    public boolean isRawFile() {
        return false;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public Sendable[] getChildren() {
        Sendable[] result = new Sendable[list.size()];
        int count = 0;
        for (Sendable k : list) {
            result[count++] = k;
        }
        return result;
    }

    @Override
    public Sendable getChild(Object id) {
        if (id == null) {
            return null;
        }
        String id_str = id.toString().toLowerCase();
        for (Sendable s : list) {
            if (id_str.equals(s.toString().toLowerCase())) {
                return s;
            }
        }
        return null;
    }

    @Override
    public SendableFilter getFilter() {
        return filter;
    }

    public void setFilter(SendableFilter filter) {
        this.filter = filter;
    }

    public void addSendable(Sendable s) {
        list.add(s);
    }

    public void removeSendable(Sendable s) {
        list.remove(s);
    }

    public void removeAllSendables() {
        list.clear();
    }

    @Override
    public String toString() {
        StringBuilder children = new StringBuilder();
        children.append('[');
        for (Sendable s : list) {
            if (children.length() > 1) {
                children.append(", ");
            }
            children.append(s.toString());
        }
        children.append(']');
        return super.toString() + ": " + children.toString();
    }
}
