package pl.devsite.bigbitbox.client;

import pl.devsite.bigbitbox.server.SocketCommunicator;
import pl.devsite.log.ConfigLog;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import pl.devsite.bitbox.server.BitBoxConfiguration;
import static pl.devsite.bitbox.server.BitBoxConfiguration.*;
import static pl.devsite.bigbitbox.client.MainThread.*;

/**
 *
 * @author Damian Gorlach
 */
public class BigBitBoxClient extends SocketCommunicator {

	private static final Logger logger = Logger.getLogger(BigBitBoxClient.class.getName());
//	private boolean alive = true;
	private ExecutorService threadPool = Executors.newCachedThreadPool();
	//
	private int localhostPort;
	//
	private static Thread thread;
//	private static BitBoxConfiguration config;

	public static void start() {
		thread = new Thread(new MainThread());
		thread.start();
	}
/*
	public static void start() {
		thread = new Thread(new Runnable() implements ConfigurationChangeListener{

			long modified;
			BigBitBoxClient m = null;
			long lastModified = 0;

			void kill() {
				if (m != null) {
					logger.info("stopping old BigBitBoxClient");
					m.alive = false;
					try {
						m.getSocket().close();
					} catch (IOException ex) {
						logger.log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
			}

			@Override
			public void run() {
				ExecutorService executorService = Executors.newCachedThreadPool();
				BitBoxConfiguration config = BitBoxConfiguration.getInstance();
				try {
					while (!Thread.interrupted()) {
						modified = config.getLastModified();
						try {
							if (lastModified != modified || startAnotherBigBitBoxClient) {
								startAnotherBigBitBoxClient = false;
								kill();
								logger.info("starting BigBitBoxClient");
								lastModified = modified;
								try {
									Socket socket = createSocketToBigBitBoxServer();
									if (socket == null) {
										startAnotherBigBitBoxClient = true;
										continue;
									}
									m = new BigBitBoxClient(socket,
											config.getProperty(PROPERTY_BIGBIT_NODE_NAME),
											str2int(config.getProperty(PROPERTY_PORT)));
									executorService.submit(m);
								} catch (IOException ex) {
									startAnotherBigBitBoxClient = true;
									logger.log(Level.SEVERE, ex.toString());
								}
							}
							Thread.sleep(10000);
						} catch (IncorrectParametersException ex) {
							logger.log(Level.SEVERE, ex.toString());
							kill();
							//break;
						} catch (InterruptedException ex) {
							logger.log(Level.SEVERE, ex.toString());
							kill();
							break;
						}
					}
				} finally {
					executorService.shutdownNow();
				}
			}
		});
		thread.start();
	}*/
	public static void stop() {
		thread.interrupt();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) throws Exception {
		// TODO code application logic here
//		if (USEPROXY) {
//			System.setProperty("http.proxyHost", PROXYHOST);
//			System.setProperty("http.proxyPort", "" + PROXYPORT);
//		}
		ConfigLog.apply();
		BitBoxConfiguration config = BitBoxConfiguration.getInstance();
//		if (config.tryToLoadConfig(args)) {
//			start();
//		}
		if (config.getFile() != null) {
			start();
		}

	}
	private final String localhostId;


	public BigBitBoxClient(Socket socket, String localhostId, int localhostPort) throws IncorrectParametersException {
		super(socket);
		this.localhostPort = localhostPort;
		this.localhostId = localhostId;
	}

	public BigBitBoxClient(BufferedInputStream clientIn, BufferedOutputStream clientOut, String localhostId, int localhostPort) throws IncorrectParametersException {
		super(clientIn, clientOut);
		this.localhostPort = localhostPort;
		this.localhostId = localhostId;
	}

	@Override
	public void communicate(InputStream in, OutputStream out) throws IOException {
		boolean connectedAtLeastOnce = false;
		String line;
		try {
			BitBoxConfiguration config = BitBoxConfiguration.getInstance();
			//getSocket().setSoTimeout(SERVERREGISTERTIMEOUT);
			//send("bitbox");
//			send("");
//			send("");
			Integer serverPort = str2int(config.getProperty(PROPERTY_BIGBIT_SERVER_PORT, "80"));
			String serverHost = config.getProperty(PROPERTY_BIGBIT_SERVER_HOST);
			String serverHostPort = serverHost + (serverPort != 80 ? ":" + serverPort : "");
//			send("GET / HTTP/1.1");
//			send("Host: " + serverHostPort);
//			send("Connection: keep-alive");
//			send("");
//			do {
//				line = read();
//				if (line != null) {
//					System.out.println(">eating: " + line);
//				}
//			} while (line != null);
//			System.out.println("i'm full");
			send("POST /bitbox/ HTTP/1.0");
			send("Host: " + serverHostPort);
			send("Connection: keep-alive");
//			send("TE: trailers");
//			send("Transfer-Encoding: chunked");
//			send("Content-Type: application/octet-stream");
//			send("Content-Length: 13");
//			send("Content-Type: multipart/form-data; boundary="+BOUNDARY);
			send("");
//			send("");
//			send("92\r\n"+
//"------------------------------ab5090ac7869\r\n"+
//"Content-Disposition: form-data; name=\"query\"\r\n"+
//"\r\n"+
//"zoooom\r\n"+
//"------------------------------ab5090ac7869--\r\n"+
//"\r\n"+
//"\r\n"+
//"0");
//			send(BOUNDARY);
			send("register " + config.getProperty(PROPERTY_BIGBIT_NODE_NAME));
			send("port " + localhostPort);
//			send(BOUNDARY);
			send("");
			do {
				line = read();
				if (line != null) {
					System.out.println("> " + line);
				}
			} while (line != null && !line.startsWith("register "));
			setDefaultTimeOut();
			if ("register ok".equals(line)) {
				System.out.println("estabilished " + serverHost + ":" + serverPort);
				connectedAtLeastOnce = true;
				do {
					line = read();
					if (line != null) {
						if (line.startsWith("req ")) {
							String firstLine = line;
							StringBuilder httpHeader = new StringBuilder();
							for (;;) {
								line = read();
								if (line == null || line.isEmpty()) {
									break;
								}
								httpHeader.append(line).append("\r\n");
							}
							BitBoxServer bbs = new BitBoxServer(new Socket("127.0.0.1", localhostPort), firstLine, httpHeader.toString());
							threadPool.execute(bbs);
						}
					}
				} while (/*alive && */line != null && !Thread.interrupted());
			} else {
				System.err.println("failed \'" + line + "\'!");
			}
		} finally {
//			if (/*connectedAtLeastOnce && */alive) {
//				System.out.println("sleeping before reconnecting");
//				try {
//					Thread.sleep(5000);
//					startAnotherBigBitBoxClient = true;
//				} catch (Exception e) {
//				}
//			}
		}
	}

	private static byte[] str2bytes(String s) {
		return (s + "\r\n").getBytes();
	}

	class BitBoxServer extends SocketCommunicator {

		private String lineWithReq;
		private String httpRequest;

		public BitBoxServer(Socket socket, String lineWithReq, String httpRequest) {
			super(socket);
			this.lineWithReq = lineWithReq;
			this.httpRequest = httpRequest;
			//line = line.substring(line.indexOf(' ') + 1);
		}

		@Override
		public void communicate(InputStream in, OutputStream out) throws IOException {
			send(httpRequest);
			Socket bitBigBox = null;
			OutputStream bitBigBoxOut = null;
			BitBoxConfiguration config = BitBoxConfiguration.getInstance();
			try {
				String line = read();
				System.out.println("" + line);
				bitBigBox = createSocketToBigBitBoxServer();
//				bitBigBox.setTcpNoDelay(true);
//				bitBigBox.setKeepAlive(false);
//				bitBigBox.setSoLinger(true, 60); // wait max XX sec after socket closed
				//System.out.println("connected to bitBigBox: " + bitBigBox.isConnected());
//BufferedOutputStream bitBigBoxOut=out;
				//bitBigBoxOut = new BufferedOutputStream(bitBigBox.getOutputStream());
				bitBigBoxOut = bitBigBox.getOutputStream();
				final InputStream bitBigBoxIn = bitBigBox.getInputStream();
				System.out.println("trash line: " + SocketCommunicator.read(bitBigBoxIn));
				//System.out.println(SocketCommunicator.read(bitBigBoxIn));
				//bitBigBoxOut.write(("HTTP/1.1 200 OK33" + "\r\n").getBytes());
				bitBigBoxOut.write(str2bytes(line));
				System.out.println(lineWithReq);
				Integer serverPort = str2int(config.getProperty(PROPERTY_BIGBIT_SERVER_PORT, "80"));
				String serverHost = config.getProperty(PROPERTY_BIGBIT_SERVER_HOST);
				bitBigBoxOut.write(str2bytes(lineWithReq)); // must be the second line!!
				bitBigBoxOut.write(str2bytes("Host: " + serverHost + ":" + serverPort));
				bitBigBoxOut.write(str2bytes("Connection: close"));
//				bitBigBoxOut.write(str2bytes("Pragma: no-cache"));
//				bitBigBoxOut.write(str2bytes("Expires: 0"));
//				bitBigBoxOut.write(str2bytes("Cache-Control: no-store, no-cache, must-revalidate"));
//				bitBigBoxOut.write(str2bytes("Cache-Control: post-check=0, pre-check=0"));
//                buf = ("Transfer-Encoding: chunked\r\n").getBytes();
//                globalcount += buf.length;
//                bitBigBoxOut.write(buf);
//				bitBigBoxOut.write("Content-Type: application/octet-stream\r\n".getBytes());
//				bitBigBoxOut.write("Content-Length: 1\r\n".getBytes()); // no good
				bitBigBoxOut.flush();
				byte[] buffer = new byte[1024 * 4];
				int count;
				boolean weHaveHtml = false;
				int contentLength = -1;
				String contentType = null;
				////////////////////////////////////////////////////////////////////////////////////////////
//				try {
//					Thread.sleep(500);
//				} catch (InterruptedException ex) {
//					Logger.getLogger(Client.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
//				}
				for (;;) {
					line = read();
					if (line != null && !line.isEmpty()) {
						System.out.println("# " + line);
						if (line.startsWith("Content-Length: ")) {
							contentLength = Integer.parseInt(line.substring(line.indexOf(' ') + 1));
						} else if (line.startsWith("Content-Type: ")) {
							contentType = line.substring(line.indexOf(' ') + 1);
						} else if (line.startsWith("Connection: ")) {
							// swallow
						} else {
							bitBigBoxOut.write(str2bytes(line));
						}
					} else {
						break;
					}
				}
				weHaveHtml = "text/html".equals(contentType);
				if (weHaveHtml) {
					if (contentLength == -1) {
					}
					System.out.println("calculating content length");
					//ByteArrayInputStream memBuf = new ByteArrayInputStream()
					ByteArrayOutputStream memBuf = new ByteArrayOutputStream();
					contentLength = 0;
					for (;;) {
						line = read();
						if (line == null) {
							break;
						}
						line = line.replaceAll("href=\"/", "href=\"/" + config.getProperty(PROPERTY_BIGBIT_NODE_NAME) + "/");
						byte[] b = str2bytes(line);
						contentLength += b.length;
						memBuf.write(b);
					}
//					for (;;) {
//						count = in.read(buffer);
//						if (count > 0) {
//							memBuf.write(buffer, 0, count);
//							contentLength += count;
//						} else {
//							break;
//						}
//					}
					bitBigBoxOut.write(str2bytes("Content-Length: " + contentLength));
					//contentType =  "application/octet-stream";
					bitBigBoxOut.write(str2bytes("Content-Type: " + contentType));
					bitBigBoxOut.write(str2bytes(""));
					memBuf.writeTo(bitBigBoxOut);
				} else {
					bitBigBoxOut.write(str2bytes("Content-Length: " + contentLength));
					bitBigBoxOut.write(str2bytes("Content-Type: " + contentType));
					bitBigBoxOut.write(str2bytes(""));
					while (true) {
						count = in.read(buffer);
						if (count > 0) {
							bitBigBoxOut.write(buffer, 0, count);
							bitBigBoxOut.flush();
							//System.out.println(new String(buffer, 0, count));
						} else {
							break;
						}
					}
				}
				bitBigBox.shutdownOutput();
			} finally {
//                if (bitBigBoxOut != null) {
//                    bitBigBoxOut.flush();
////					bitBigBoxOut.close();
//                }
				if (bitBigBox != null) {
					try {
//                        if (bitBigBox.isConnected()) {
//                            System.out.println("socket is still connected");
//                            try {
//                                Thread.sleep(2110);
//                            } catch (InterruptedException ex) {
//                                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
//                            }
//                        }
					} finally {
						bitBigBox.close();
					}
				}
			}
		}
	}
}
