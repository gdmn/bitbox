package pl.devsite.bigbitbox.system;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class SystemProcessWrapper extends SystemProcess {

	public static final String ERR_OUT = "err";
	public static final String STD_OUT = "std";
	private final Object listenerLock = new Object();
	private PrintStream processPrintIn;

	public SystemProcessWrapper(String processPath, String processOptions) throws IOException {
		super(processPath, processOptions);
		initializeStreams();
	}

	public SystemProcessWrapper(String processPath, String[] processOptions) throws IOException {
		super(processPath, processOptions);
		initializeStreams();
	}
	
	private void initializeStreams() {		
		new LineCapture(getProcessStd(), STD_OUT).start();
		new LineCapture(getProcessErr(), ERR_OUT).start();		
		processPrintIn = new PrintStream(getProcessIn());		
	}

	private String printAndWait(String property, String input, String expected) throws InterruptedException {
		synchronized (listenerLock) {
			OneLineListener listener = new OneLineListener(input, expected, property, propertyChangeSupport);
			try {
				propertyChangeSupport.addPropertyChangeListener(listener);
				String result = listener.getResult();
				return result;
			} finally {
				propertyChangeSupport.removePropertyChangeListener(listener);
			}
		}
	}

	public String printAndWait(String input, String expected) throws InterruptedException {
		return printAndWait(null, input, expected);
	}
	
	public SystemProcessWrapper print(String value) {
		processPrintIn.print(value);
		return this;
	}
	private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

	/**
	 * Add PropertyChangeListener.
	 *
	 * @param listener
	 */
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if (propertyName == null) {
			propertyChangeSupport.addPropertyChangeListener(listener);
		}
		propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
	}

	/**
	 * Remove PropertyChangeListener.
	 *
	 * @param listener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(listener);
	}

	private class OneLineListener implements PropertyChangeListener {

		private String waitFor, property, result, input;
		private PropertyChangeSupport propertyChangeSupport;

		public OneLineListener(String input, String waitFor, String property, PropertyChangeSupport propertyChangeSupport) {
			this.waitFor = waitFor;
			this.propertyChangeSupport = propertyChangeSupport;
			this.input = input;
			this.property = property;
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			synchronized (this) {
				String value = evt.getNewValue().toString();
				//System.out.println("?? "+value);
				if (waitFor == null || value.startsWith(waitFor)) {
					result = evt.getNewValue().toString();
					this.notify();
				}
			}
		}

		public String getResult() throws InterruptedException {
			if (input != null) {
				try {
					flush();
				} catch (IOException ex) {
				}
			}
			int counter = 0;
			synchronized (this) {
				while (result == null && ++counter < 10) {
					this.wait(1000);
					//Thread.sleep(1000);
				}
			}
			return result;
		}
	}

	private class LineCapture extends Thread {

		/**
		 * The input stream to read from.
		 */
		private InputStream in;
		/**
		 * The output stream to write to.
		 */
		//private OutputStream out;
		/**
		 * The prefix used to prefix the lines when outputting to the logger.
		 */
		private String propertyName;

		LineCapture(InputStream in, String prefix) {
			this.in = in;
			//this.out = out;
			this.propertyName = prefix;
		}

		public void run() {
			try {
				// creates the decorating reader and writer
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				//PrintStream printStream = new PrintStream(out);
				String line;
				// read line by line
				do {
					line = reader.readLine();
					//System.out.println(": " + line);
					//printStream.println(line);
					//if (line !=null)
					propertyChangeSupport.firePropertyChange(propertyName, null, line);
				} while (line != null);
			} catch (IOException ex) {
				Logger.getLogger(SystemProcessWrapper.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}
}
