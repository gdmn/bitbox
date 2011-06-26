package pl.devsite.bitbox.authenticator;

import pl.devsite.bitbox.server.*;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Logger;

/**
 *
 * @author Damian Gorlach
 */
public class HttpBasicAuthenticator implements HttpAuthenticator {

    private HashMap<String, String> users;
    private static final Logger logger = Logger.getLogger(HttpBasicAuthenticator.class.getName());

    public HttpBasicAuthenticator() {
        users = new HashMap<String, String>();
    }

    @Override
    public void addUser(String user, String password) {
        String user_pass = HttpTools.base64encode(user + ":" + password);
        users.put(user, user_pass);
    }

    @Override
    public String getAuthenticate() {
        return "Basic realm=\"" + HttpAuthenticator.defaultMessage + "\"";
    }

    @Override
    public String allowed(String authorization) {
        String result = null;
        String authorizationType = authorization.substring(0, authorization.indexOf(' '));
        if (authorizationType.toLowerCase().equals("basic")) {
            String authorizationData = authorization.substring(authorization.indexOf(' ') + 1);
            logger.finest("http basic authorization data: " + authorizationData);
            for (Entry<String, String> entry : users.entrySet()) {
                if (authorizationData.equals(entry.getValue())) {
                    return entry.getKey();
                }
            }
            return null;
        }
        return result;
    }

    @Override
    public String toString() {
        return HttpBasicAuthenticator.class.getSimpleName()
                + (users.keySet().size() < 1 ? "" : " " + users.keySet());
    }
}
