package pl.devsite.log;


import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

/**
 *
 * @author Damian Gorlach
 */
public class ConfigLog {

	public ConfigLog() {
	}

	public static void apply() throws IOException {
		Object a = new ConfigLog();
		InputStream is = a.getClass().getResourceAsStream("log.properties");
		LogManager.getLogManager().readConfiguration(is);		
	}
}
