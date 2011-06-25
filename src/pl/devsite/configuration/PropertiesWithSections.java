package pl.devsite.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

/**
 * Handles sections in properties java files.
 * @author dmn
 */
public class PropertiesWithSections extends Properties {

	private String currentSection = null;

	public PropertiesWithSections(Properties defaults) {
		super(defaults);
	}

	public PropertiesWithSections() {
	}
	
	public void resetCurrentSection() {
		currentSection = null;
	}

	@Override
	public synchronized void load(Reader reader) throws IOException {
		super.load(reader);
		resetCurrentSection();
	}

	@Override
	public synchronized void load(InputStream inStream) throws IOException {
		super.load(inStream);
		resetCurrentSection();
	}

	@Override
	public synchronized Object put(Object key, Object value) {
		String sKey = key.toString();
		if (sKey.startsWith("[") && sKey.endsWith("]")) {
			sKey = sKey.substring(1, sKey.length() - 1);
			if ("ROOT".equals(sKey.toUpperCase())) {
				currentSection = null;
			} else {
				currentSection = sKey;
			}
			return null;
		}
		return super.put((currentSection == null ? "" : currentSection + ".") + key, value);
	}
}