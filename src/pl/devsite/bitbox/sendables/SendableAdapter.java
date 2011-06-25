/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.devsite.bitbox.sendables;

import pl.devsite.bitbox.server.*;
import pl.devsite.bitbox.authenticator.HttpAuthenticator;
import java.util.logging.Logger;

/**
 *
 * @author gorladam
 */
public abstract class SendableAdapter implements Sendable {

    private Sendable parent;
    protected String name;
    private HttpAuthenticator authenticator;
    private Logger logger = null;

    public SendableAdapter(Sendable parent, String name) {
        this.parent = parent;
        this.name = name;
    }

    @Override
    public String getAddress() {
        if (name != null) {
            String result;
            if (getParent() != null) {
                result = getParent().getAddress() + "/";
            } else {
                result = "";
            }
            //result = result + EncodingTools.urlEncodeUTF(name);
            result = result + name;
            return result;
        }
        return null;
    }

    @Override
    public Sendable getParent() {
        return parent;
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

    public boolean isAllowed(Object ip, Object user) {
        return getFilter() == null ? true : getFilter().isAllowed(this, ip, user);
    }

    @Override
    public SendableFilter getFilter() {
        return parent == null ? null : parent.getFilter();
    }

    @Override
    public int getAttributes() {
        return Sendable.ATTR_NOTHING;
    }

    @Override
    public HttpAuthenticator getAuthenticator() {
        if (authenticator != null) {
            return authenticator;
        }
        return parent == null ? null : parent.getAuthenticator();
    }

    public void setAuthenticator(HttpAuthenticator authenticator) {
        this.authenticator = authenticator;
        if (authenticator == null) {
            getLogger().info("Set authenticator to parent for path " + getAddress() + "");
        } else if (authenticator == HttpTools.NULLAUTHENTICATOR) {
            getLogger().info("Set authenticator to NULL for path " + getAddress() + "");
        } else {
            getLogger().info("Set authenticator to " + authenticator.toString() + " for path " + getAddress() + "");
        }
    }

    private Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(SendableAdapter.class.getName());
        }
        return logger;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Sendable tryToFindSendable(Sendable root, String path) {
		if (path == null) return null;
        String[] splitted = path.split("/");
        Sendable now = root;
        for (String spl : splitted) {
            if (!spl.equals("")) {
                now = now.getChild(spl);
            }
            if (now == null) {
                return null;
            }
        }
        return now;
    }
}
