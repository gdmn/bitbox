package pl.devsite.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Damian Gorlach
 */
public abstract class Configuration implements ConfigurationListener {

	protected Properties properties;
	private static final Logger logger = Logger.getLogger(Configuration.class.getName());
	private ConfigurationWatcher configurationWatcher;
	private LinkedList<ConfigurationChangeListener> listeners = new LinkedList<ConfigurationChangeListener>();

	public abstract Properties getDefaultProperties();

	public void notifyListeners() {
		for (ConfigurationChangeListener l : listeners) {
			l.configurationUpdated(properties);
		}
	}

	public void addConfigurationChangeListener(ConfigurationChangeListener l) {
		listeners.add(l);
		l.configurationUpdated(properties);
	}

	public void removeConfigurationChangeListener(ConfigurationChangeListener l) {
		listeners.remove(l);
	}

	@Override
	public void readConfiguration(File file) throws IOException {
		properties = new Properties(getDefaultProperties());
		try {
			properties.load(new FileInputStream(file));
		} finally {
			notifyListeners();
		}
	}

	public long getLastModified() {
		return configurationWatcher.getLastModified();
	}

	@Override
	public void readConfiguration(List<String> args) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setFile(File configurationFile) throws IOException {
		if (!(configurationFile.isFile() && configurationFile.canRead())) {
			throw new IOException("File neither found nor readable");
		}
		if (configurationWatcher != null) {
			configurationWatcher.stopMonitoring();
			configurationWatcher = null;
		}
		configurationWatcher = new ConfigurationWatcher(configurationFile);
		configurationWatcher.startMonitoring(this);
	}

	public File getFile() {
		return configurationWatcher.getFile();
	}

	public String getProperty(String key) {
		return getProperty(key, null);
	}

	public String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public void setProperty(String key, String value) {
		logger.log(Level.FINER, "Set {0} = {1}", new Object[]{key, value});
		properties.setProperty(key, value);
	}

	public Set<String> stringPropertyNames() {
		return properties.stringPropertyNames();
	}

	public Enumeration<?> propertyNames() {
		return properties.propertyNames();
	}

	public static Collection<String> grep(Collection coll, String mainKey) {
		TreeSet<String> result = new TreeSet<String>();
		for (Object o : coll) {
			String s = o.toString();
			if (s.indexOf(mainKey + ".") == 0) {
				int p = s.indexOf('.', mainKey.length() + 1);
				if (p > 0) {
					result.add(s.substring(0, p + 1));
				}
			}
		}
		return result;
	}

	public static String[] getDefaultConfigLocations(final String appName) {
		final String USERHOME = System.getProperty("user.home");
		final String CONFIGFILENAME = appName + ".conf";
		final String CONFIGDIR = appName;
		final String CONFIGCONFIG = ".config";
		String[] configFileLocations = new String[]{
			USERHOME + File.separator + CONFIGCONFIG + File.separator + CONFIGDIR + File.separator + CONFIGFILENAME,
			USERHOME + File.separator + CONFIGCONFIG + File.separator + CONFIGFILENAME,
			USERHOME + File.separator + "." + CONFIGDIR + File.separator + CONFIGFILENAME,
			USERHOME + File.separator + CONFIGDIR + File.separator + CONFIGFILENAME,
			USERHOME + File.separator + "." + CONFIGFILENAME,
			USERHOME + File.separator + CONFIGFILENAME,};
		return configFileLocations;
	}

	public static boolean str2boolean(String str) {
		String s = str != null ? str.toLowerCase() : null;
		return "true".equals(s) || "1".equals(s);
	}

	public static Integer str2int(String str) {
		return str != null ? Integer.parseInt(str) : null;
	}
}
