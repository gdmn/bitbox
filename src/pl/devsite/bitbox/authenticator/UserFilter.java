/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.devsite.bitbox.authenticator;

import java.util.Arrays;

/**
 *
 * @author dmn
 */
public class UserFilter implements HttpAuthenticator {

    private HttpAuthenticator baseAuthenticator;
    private String[] users;

    public UserFilter(HttpAuthenticator baseAuthenticator, String... users) {
        this.baseAuthenticator = baseAuthenticator;
        this.users = users;
    }

    @Override
    public void addUser(String user, String password) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String allowed(String authorization) {
        String baseResult = baseAuthenticator.allowed(authorization);
        if (baseResult == null) {
            return null;
        }
        for (String user : users) {
            if (user.equals(baseResult)) {
                return user;
            }
        }
        return null;
    }

    @Override
    public String getAuthenticate() {
        return baseAuthenticator.getAuthenticate();
    }

    @Override
    public String toString() {
        return UserFilter.class.getSimpleName() +
                (baseAuthenticator == null ? "" : " (over " + baseAuthenticator.toString() + ")") +
                " " + Arrays.asList(users);
    }


}
