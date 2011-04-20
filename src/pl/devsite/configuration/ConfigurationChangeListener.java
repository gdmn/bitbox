package pl.devsite.configuration;

import java.util.Properties;

/**
 *
 * @author Damian Gorlach
 */
public interface ConfigurationChangeListener {
	void configurationUpdated(Properties properties);
}
