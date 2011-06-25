package pl.devsite.bitbox.server;

import pl.devsite.bitbox.sendables.SendableRoot;
import java.io.File;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.authenticator.HttpAuthenticator;
import pl.devsite.bitbox.authenticator.HttpBasicAuthenticator;
import pl.devsite.bitbox.authenticator.HttpDigestAuthenticator;
import pl.devsite.bitbox.authenticator.UserFilter;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableAdapter;
import pl.devsite.bitbox.sendables.SendableFileWithMimeResolver;
import pl.devsite.configuration.ConfigurationChangeListener;
import static pl.devsite.bitbox.server.BitBoxConfiguration.*;
import static pl.devsite.configuration.Configuration.*;

/**
 *
 * @author Damian Gorlach
 */
public class BitBoxConfigurationListener implements ConfigurationChangeListener {

	private BitBoxConfiguration configurationAdapter;
	private static final Logger logger = Logger.getLogger(BitBoxConfigurationListener.class.getName());

	public BitBoxConfigurationListener(BitBoxConfiguration configurationAdapter) {
		this.configurationAdapter = configurationAdapter;
	}

	@Override
	public void configurationUpdated(Properties properties) {
		try {
			// process shares
			SendableRoot sendableRoot = configurationAdapter.getSendableRoot();
			sendableRoot.removeAllSendables();
			if (properties != null) {
				for (String share : grep(properties.stringPropertyNames(), PROPERTY_SHARE)) {
					String name = properties.getProperty(share + "name");
					String sysPath = properties.getProperty(share + "path");
					String type = properties.getProperty(share + "type");
					// System.out.println("#share " + name + " " + sysPath + " " + type);
					SendableAdapter sh = new SendableFileWithMimeResolver(sendableRoot, new File(sysPath));
					if (name != null && !name.isEmpty()) {
						sh.setName(name);
					}
					properties.setProperty(share + "name", sh.getName());
					sendableRoot.addSendable(sh);
				}

				// process menu
				{
					Collection<String> menus = grep(properties.stringPropertyNames(), PROPERTY_MENU);
					for (String menu : menus) {
						String name = properties.getProperty(menu + "name");
						String link = properties.getProperty(menu + "link");
						System.out.println("#menu " + name + " " + link);
					}
				}

				// process user list
				{
					Collection<String> users = grep(properties.stringPropertyNames(), PROPERTY_USER);
					if (users != null && !users.isEmpty()) {
						HttpAuthenticator httpAuthenticator = AUTHENTICATOR_BASIC.equals(properties.getProperty(PROPERTY_AUTHENTICATOR))
								? new HttpBasicAuthenticator() : new HttpDigestAuthenticator();
						for (String user : users) {
							String name = properties.getProperty(user + "name");
							String password = properties.getProperty(user + "password");
							System.out.println("#user " + name + " " + password);
							httpAuthenticator.addUser(name, password);
						}
						sendableRoot.setAuthenticator(httpAuthenticator);
					} else {
						sendableRoot.setAuthenticator(HttpTools.NULLAUTHENTICATOR);
					}
				}

				// process allowed users
				for (String share : grep(properties.stringPropertyNames(), PROPERTY_SHARE)) {
					String allowedUsers = properties.getProperty(share + "user.list");
					if (allowedUsers != null) {
						String name = properties.getProperty(share + "name");
						Sendable serverPath = SendableAdapter.tryToFindSendable(sendableRoot, name);
						System.out.println("#allowed " + name + " " + allowedUsers);
						String[] userArray = allowedUsers.split(",");
						if (serverPath instanceof SendableAdapter) {
							SendableAdapter sendableServerPath = (SendableAdapter) serverPath;
							if (allowedUsers.equals("*")) {
								sendableServerPath.setAuthenticator(HttpTools.NULLAUTHENTICATOR);
							} else {
								UserFilter userFilter = new UserFilter(sendableServerPath.getAuthenticator(), userArray);
								sendableServerPath.setAuthenticator(userFilter);
							}
						}
					}
				}
			} else {
				sendableRoot.setAuthenticator(HttpTools.NULLAUTHENTICATOR);
			}
		} catch (NumberFormatException e) {
			logger.log(Level.SEVERE, null, e);
		} finally {
		}
	}
}
