
package pl.devsite.bitbox.authenticator;

/**
 *
 * @author Damian Gorlach
 */
public interface HttpAuthenticator {
    final String defaultMessage = "Secured Area";

    void addUser(String user, String password);

    String allowed(String authorization);

    String getAuthenticate();
}
