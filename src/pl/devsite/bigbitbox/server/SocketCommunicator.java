package pl.devsite.bigbitbox.server;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Damian Gorlach
 */
public abstract class SocketCommunicator implements Runnable {

	private static final Logger logger = Logger.getLogger(SocketCommunicator.class.getName());
	private OutputStream clientOut;
	private InputStream clientIn;
	private Socket socket;

	public SocketCommunicator(Socket socket) {
		this.socket = socket;
		setDefaultTimeOut();
	}

	public SocketCommunicator(InputStream clientIn, OutputStream clientOut) {
		this.clientIn = clientIn;
		this.clientOut = clientOut;
	}

	protected void send(String text) throws IOException {
		send(clientOut, text);
//		System.out.println("<<< " + text);
	}

	public static void send(OutputStream out, String text) throws IOException {
		String tmp = text + "\r\n";
		out.write(tmp.getBytes("UTF-8"));
		out.flush();
	}

	protected String read() throws IOException {
		String result = read(clientIn);
		//System.out.println("> " + result);
		return result;
	}

	public static String read(InputStream in) throws IOException {
		int c1 = 0, c2 = 0;
		StringBuilder result = new StringBuilder(1024);
		do {
			c1 = c2;
			c2 = in.read();
			if (c2 > -1 && c2 != '\r' && c2 != '\n') {
				result.append((char) c2);
			}
		} while (c2 > -1 && !((c1 == '\r' && c2 == '\n')
				|| (c2 == '\n'))); // for linux nc compatibility
		if (c2 == -1 && result.length() == 0) {
			return null;
		}
		return result.toString();
	}

	public Socket getSocket() {
		return socket;
	}

	public final void setDefaultTimeOut() {
		if (socket != null) {
			try {
				socket.setSoTimeout(1000 * 60 * 60); // 1h				
				socket.setTcpNoDelay(true);
				socket.setSoLinger(true, 10); // wait max XX sec after socket closed
			} catch (SocketException ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
	}

	private InputStream getClientIn() throws IOException {
		if (clientIn != null) {
			return clientIn;
		} else if (socket != null) {
			clientIn = new BufferedInputStream(socket.getInputStream());
			return clientIn;
		} else {
			return null;
		}
	}

	private OutputStream getclientOut() throws IOException {
		if (clientOut != null) {
			return clientOut;
		} else if (socket != null) {
			clientOut = new BufferedOutputStream(socket.getOutputStream());
			return clientOut;
		} else {
			return null;
		}
	}

	public abstract void communicate(InputStream in, OutputStream out) throws IOException;

	@Override
	public final void run() {
		OutputStream cOut;
		InputStream cIn;
		try {
			cIn = getClientIn();
			try {
				cOut = getclientOut();
				try {
					communicate(cIn, cOut);
				} finally {
					if (cOut != null) {
						cOut.close();
					}
				}
			} finally {
				if (cIn != null) {
					cIn.close();
				}
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, ex.toString());
		} finally {
			if (socket != null) {
				try {
//					if (!socket.isInputShutdown()) {
//						socket.shutdownInput();
//					}
//					if (!socket.isOutputShutdown()) {
//						socket.shutdownOutput();
//					}
					socket.close();
				} catch (IOException ex) {
					logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			}

		}
	}

	public static boolean testPort(InetAddress host, int port) {
		Socket s = null;
		try {
			s = new Socket(host, port);
			s.setSoTimeout(3000);
			s.getInputStream().close();
			return true;
		} catch (Exception ex) {
			logger.info(ex.toString());
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException ex) {
				}
			}
		}
		return false;
	}

	public synchronized void write(byte[] b, int off, int len) throws IOException {
		clientOut.write(b, off, len);
	}

	public synchronized int read(byte[] b, int off, int len) throws IOException {
		return clientIn.read(b, off, len);
	}
}
