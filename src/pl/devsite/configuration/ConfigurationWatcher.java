package pl.devsite.configuration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Damian Gorlach
 */
public class ConfigurationWatcher {

	private File file;
	private static final Logger logger = Logger.getLogger(ConfigurationWatcher.class.getName());
	private Thread thread;
	private long lastModified;

	public ConfigurationWatcher(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public void startMonitoring(ConfigurationListener handler) throws IOException {
		thread = new Thread(new Watcher(file, handler));
		handler.readConfiguration(file);
		thread.start();
	}

	public void stopMonitoring() {
		thread.interrupt();
	}

	public long getLastModified() {
		return lastModified;
	}

	class Watcher implements Runnable {

		private final File file;
		private ConfigurationListener handler;

		public Watcher(File file, ConfigurationListener handler) {
			this.file = file;
			this.handler = handler;
			lastModified = file.lastModified();
		}

		public void run() {
			try {
				while (!Thread.interrupted()) {
					Thread.sleep(5000);
					long modified = file.lastModified();
					if (modified > lastModified) {
						logger.info(String.format("Reading configuration file (%s)...", file.getCanonicalPath()));
						lastModified = modified;
						handler.readConfiguration(file);
					}
				}
			} catch (InterruptedException ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			} catch (IOException ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
	}
}
