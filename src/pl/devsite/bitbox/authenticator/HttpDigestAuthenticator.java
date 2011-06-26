package pl.devsite.bitbox.authenticator;

import pl.devsite.bitbox.server.*;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class HttpDigestAuthenticator implements HttpAuthenticator {

    private HashMap<String, String> users;
    private String realm = HttpAuthenticator.defaultMessage;
    private String opaque = HttpTools.randomNonce();
    private Logger logger = Logger.getLogger(HttpDigestAuthenticator.class.getName());

    public HttpDigestAuthenticator() {
        users = new HashMap<String, String>();
    }

    @Override
    public void addUser(String user, String password) {
        String user_realm_pass = HttpTools.md5sum(user + ":" + realm + ":" + password);
        users.put(user, user_realm_pass);
    }

    @Override
    public String getAuthenticate() {
        String nonce = HttpTools.randomNonce();
//        realm = "testrealm@host.com";
//        nonce = "dcd98b7102dd2f0e8b11d0f600bfb0c093";
        return "Digest realm=\"" + realm + "\", qop=\"auth\", nonce=\"" + nonce + "\", opaque=\"" + opaque + "\"";
    }

    /**
     * findValue("username", "username="Mufasa", realm="testrealm@host.com", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093", uri="/"........) --> "Mufasa"
     * @param key
     * @param data
     */
    private static String findValue(String key, String data) {
        int prevpos = -1;
        boolean good = false;
        int pos;
        do {
            boolean found = false;
            boolean inside = false;
            char prevchar = 0;
            pos = 0;
            for (int i = prevpos + 1; i < data.length(); i++) {
                char ch = data.charAt(i);
                if (ch == '\"' && prevchar != '\\') {
                    inside = !inside;
                }
                if (!inside && ch == ',') {
                    found = true;
                    pos = i;
                    break;
                }
                prevchar = ch;
            }
            if (!found) {
                pos = data.length();
            }
            String pair = data.substring(prevpos + 1, pos).trim();
            int splitPos = pair.indexOf('=');
            String aKey = pair.substring(0, splitPos).trim();
            if (!aKey.equals(key)) {
                prevpos = pos;
                continue;
            } else {
                String aVal = pair.substring(splitPos + 1).trim();
                if (aVal.startsWith("\"") && aVal.endsWith("\"")) {
                    aVal = aVal.substring(1, aVal.length() - 1);
                }
                return aVal;
            }
        } while (pos < data.length());
        return null;
    }

    @Override
    public String allowed(String authorization) {
        String result = null;
        String authorizationType = authorization.substring(0, authorization.indexOf(' '));
        if (authorizationType.toLowerCase().equals("digest")) {
            String authorizationData = authorization.substring(authorization.indexOf(' ') + 1);
            logger.finest("http digest authorization data: " + authorizationData);
            String username = findValue("username", authorizationData);
            String ha1 = users.get(username);
            String uri = findValue("uri", authorizationData);
            String nonce = findValue("nonce", authorizationData);
            String nc = findValue("nc", authorizationData);
            String cnonce = findValue("cnonce", authorizationData);
            String response = findValue("response", authorizationData);
            if (username == null || ha1 == null || uri == null || nonce == null || nc == null || 
                    cnonce == null || response == null || !opaque.equals(findValue("opaque", authorizationData)) ) {
                return null;
            }
            String ha2 = HttpTools.md5sum("GET:"+uri);
            String proper = HttpTools.md5sum(ha1+":"+nonce+":"+nc+":"+cnonce+":auth:"+ha2);
            if (proper.equals(response)) {
                return username;
            }
            return null;
        }
        return result;
    }

    @Override
    public String toString() {
        return HttpDigestAuthenticator.class.getSimpleName() +
                (users.keySet().size() < 1 ? "" : " " + users.keySet());
    }


}
