package pl.devsite.bigbitbox.system;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class SystemProcess {

	private Process processHandle;
	private OutputStream processIn;
	private InputStream processStd;
	private InputStream processErr;

	public SystemProcess(String processPath, String[] processOptions) throws IOException {
		String[] array = new String[processOptions.length + 1];
		array[0] = processPath;
		System.arraycopy(processOptions, 0, array, 1, processOptions.length);
		processHandle = Runtime.getRuntime().exec(array);
		initializeStreams();
	}

	public SystemProcess(String processPath, String processOptions) throws IOException {
		String command = "" + processPath + " " + processOptions;
		processHandle = Runtime.getRuntime().exec(command);
		initializeStreams();
	}

	private void initializeStreams() {
		processStd = processHandle.getInputStream();
		processErr = processHandle.getErrorStream();

		processIn = processHandle.getOutputStream();
	}

	protected SystemProcess() {
	}

	public void kill() {
		processHandle.destroy();
	}

	public SystemProcess flush() throws IOException {
		processIn.flush();
		return this;
	}

	public static void pump(InputStream inputStream, OutputStream outputStream) throws IOException {
		if (inputStream == null) {
			return;
		}
		byte[] buf = new byte[1024];
		int count;
		do {
			count = inputStream.read(buf);
			//System.out.println("read "+count);
			if (count > 0) {
				outputStream.write(buf, 0, count);
			}
		} while (count > 0);
		outputStream.flush();
		inputStream.close();
		outputStream.close();
	}

	public static void pumpBackground(final InputStream inputStream, final OutputStream outputStream) throws IOException {
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					pump(inputStream, outputStream);
				} catch (IOException ex) {
					Logger.getLogger(SystemProcess.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
		t.start();
	}

	public Process getProcessHandle() {
		return processHandle;
	}

	public InputStream getProcessErr() {
		return processErr;
	}

	public OutputStream getProcessIn() {
		return processIn;
	}

	public InputStream getProcessStd() {
		return processStd;
	}

	public SystemProcess pipe(SystemProcess process) throws IOException {
		pump(this.getProcessStd(), process.getProcessIn());
		return process;
	}

	public SystemProcess pipeBackground(SystemProcess process) throws IOException {
		pumpBackground(this.getProcessStd(), process.getProcessIn());
		return process;
	}

	public void pipe(OutputStream outputStream) throws IOException {
		pump(this.getProcessStd(), outputStream);
	}

	public void pipeBackground(final OutputStream outputStream) throws IOException {
		pumpBackground(this.getProcessStd(), outputStream);
	}
}
