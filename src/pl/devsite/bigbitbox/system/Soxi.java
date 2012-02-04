package pl.devsite.bigbitbox.system;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.server.BitBoxConfiguration;

/**
 *
 * @author dmn
 */
public class Soxi implements SystemProcessInterface<String>, SystemProcessCallback<String> {

	private String command;
	private SystemProcessWrapper proc;
	private boolean collected = false;
	private String resultValue;
	private StringBuilder buffer = new StringBuilder();
	private String additionalOptions;

	public static String query(String fileName) {
		try {
			File f = new File(fileName);
			if (!f.canRead()) {
				return null;
			}
			Soxi soxi = Soxi.getInstance();
			soxi.setAdditionalOptions("-D");//seconds
			String seconds = soxi.query(fileName, soxi);
			if (seconds == null) {
				return null;
			}
			soxi = Soxi.getInstance();
			soxi.setAdditionalOptions("-a");//tags
			return "Length="+seconds + '\n' + soxi.query(fileName, soxi);
		} catch (InstantiationException ex) {
			return null;
		}
	}

	@Override
	public synchronized void results(String value) {
		resultValue = value;
		collected = true;
		notify();
	}

	public static void main(String[] args) {
		System.out.println("result = " + query("/home/dmn/mp3/song.mp3"));
		System.out.println("result = " + query("/home/dmn/Muzyka/GTA.mp3"));
	}

	private Soxi() throws InstantiationException {
		command = BitBoxConfiguration.getInstance().getProperty(BitBoxConfiguration.PROPERTY_SOXI);
		if (command == null || command.isEmpty()) {
			throw new InstantiationException("External command not found in properties");
		}
	}

	private static Soxi getInstance() throws InstantiationException {
		Soxi result = new Soxi();
		return result;
	}

	private String getAdditionalOptions() {
		return additionalOptions;
	}

	private void setAdditionalOptions(String additionalOptions) {
		this.additionalOptions = additionalOptions;
	}

	@Override
	public void execute(String options, final SystemProcessCallback<String> callback) {
		try {
			proc = new SystemProcessWrapper(command, new String[]{additionalOptions, options});
			proc.addPropertyChangeListener(null, new PropertyChangeListener() {

				boolean done = false;

				@Override
				public void propertyChange(PropertyChangeEvent pce) {
					if (!SystemProcessWrapper.STD_OUT.equals(pce.getPropertyName())) {
						return;
					}
					Object value = pce.getNewValue();
					String s = value == null ? null : value.toString();
					if (s == null) {
						done = true;
						String result = buffer.length() > 0 ? buffer.substring(0, buffer.length() - 1) : buffer.toString();
						callback.results(result);
					} else if (s.isEmpty()) {
						return ;
					} else {
						buffer.append(s).append('\n');
					}
				}
			});
			synchronized (this) {
				wait(5000);
			}
			if (!collected) {
				proc.kill();
			}
		} catch (IOException ex) {
			Logger.getLogger(Soxi.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			Logger.getLogger(Soxi.class.getName()).log(Level.SEVERE, null, ex);

		}
	}

	public String query(String options, final SystemProcessCallback<String> callback) {
		execute(options, callback);
		if (!collected) {
			return null;
		} else {
			return resultValue;
		}
	}
}
