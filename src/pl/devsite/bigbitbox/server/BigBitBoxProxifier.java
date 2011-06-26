package pl.devsite.bigbitbox.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.log.ConfigLog;

/**
 *
 * @author dmn
 */
public class BigBitBoxProxifier implements Runnable {

	private static final Logger logger = Logger.getLogger(BigBitBoxProxifier.class.getName());
	private boolean started = true;
	private ExecutorService threadPool = Executors.newCachedThreadPool();
//	private ExecutorService threadPool = Executors.newFixedThreadPool(5);
	private final ConnectedNodes nodes = new ConnectedNodes();
	private final RequestList requests = new RequestList();
	private static AtomicInteger requestCounter = new AtomicInteger();
	private static AtomicInteger threadCounter = new AtomicInteger();
	private static final int SERVERPORT = 9099;
	private static final int MAXCONNECTIONS = 20;

	public static void main(String[] args) throws IOException {
		ConfigLog.apply();
		BigBitBoxProxifier m = new BigBitBoxProxifier();
		System.out.println(m.getClass().getCanonicalName());
//		Thread t = new Thread(m);
//		t.start();
		m.run();
	}

	public static String get503() {
		String result = "HTTP/1.1 503 Overloaded\r\n\r\nOverloaded\r\n";
		return result;
	}

	public static String get404() {
		String result = "HTTP/1.1 404 Not found\r\n\r\nBitBox not found\r\n";
		return result;
	}

	@Override
	public void run() {
		try {
			ServerSocket server = null;
			try {
				server = new ServerSocket(SERVERPORT);
				while (started) {
					Socket client = server.accept();
					System.out.println("got new client");
					Runnable t;
					if (threadCounter.intValue() < MAXCONNECTIONS) {
						t = new RequestProcessor(client);
					} else {
						t = new SocketCommunicator(client) {

							@Override
							public void communicate(InputStream in, OutputStream out) throws IOException {
								send(get503());
							}
						};
					}
					threadPool.execute(t);
					Thread.sleep(50);
				}
			} finally {
				if (server != null) {
					server.close();
				}
			}
		} catch (InterruptedException ex) {
		} catch (SocketException ex) {
			if (started) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}
	/*
	public static void passConnection(String dest, int port, String req, PrintWriter output) {
	Socket socket = null;
	try {
	socket = new Socket(dest, port);
	socket.setSoTimeout(3000);
	BufferedReader in;
	PrintWriter out;
	in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	try {
	out = new PrintWriter(socket.getOutputStream(), true);
	try {
	out.print(req);
	out.flush();
	String line;
	while ((line = in.readLine()) != null) {
	output.println(line);
	}
	} finally {
	if (out != null) {
	out.close();
	}
	}
	} finally {
	if (in != null) {
	in.close();
	}
	}
	} catch (Exception ex) {
	logger.info(ex.toString());
	} finally {
	if (socket != null) {
	try {
	socket.close();
	} catch (IOException ex) {
	}
	}
	}
	}
	 */

	class ConnectedNode {

		String id;
		int port;
		String host;
		RequestProcessor processor;
	}

	class SingleRequest {

		String hash;
		ConnectedNode node;
		RequestProcessor processor;
	}

	class ConnectedNodes extends LinkedList<ConnectedNode> {

		public ConnectedNode findById(String id) {
			synchronized (this) {
				for (ConnectedNode n : this) {
					if (n != null && id.equals(n.id)) {
						return n;
					}
				}
				return null;
			}
		}
	}

	class RequestList extends LinkedList<SingleRequest> {

		public SingleRequest findByHash(String hash) {
			synchronized (this) {
				for (SingleRequest n : this) {
					if (n != null && hash.equals(n.hash)) {
						return n;
					}
				}
				return null;
			}
		}
	}

	class RequestProcessor extends SocketCommunicator {

		private boolean canBeFinalized = false;

		@Override
		public void communicate(InputStream in, OutputStream out) throws IOException {
			System.out.println("thread started, count: " + threadCounter.incrementAndGet());
			try {
				System.out.println("communication started");
				this.setDefaultTimeOut();
				boolean httpProtocol = false;
				boolean bitboxProtocol = false;
				boolean passResponse = false;
				String line = null;
				String id = null;
				String page = null;
				try {
					int c = 0;
					int a = 0;
					while (c++ < 5) {
						a = in.available();
						if (a < 1) {
							Thread.sleep(20);
						} else {
							System.out.println("available bytes: " + a);
							break;
						}
					}
					/**
					 * This empty line is strange, but necessary. Some proxies does not work,
					 * if first communication message is "HTTP 200..." which
					 * should be treated as a response.
					 */
					if (a < 1) {
						System.out.println("sending trash line");
						send("");
					}
				} catch (InterruptedException ex) {
					Logger.getLogger(BigBitBoxProxifier.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
				}
				line = SocketCommunicator.read(in);
				String firstLine = line;
				System.out.println("> firstLine: " + firstLine);
				if (line.startsWith("HTTP/")) {
					httpProtocol = true;
					line = SocketCommunicator.read(in);
					if (line.startsWith("req ")) {
						passResponse = true;
					}
				} else if (line.startsWith("GET ") || line.startsWith("PUT ") || line.startsWith("POST ")) {
					httpProtocol = true;
					int p = line.indexOf(" HTTP/");
					page = line.substring(line.indexOf(' ') + 1, p);
					if (page.startsWith("/")) {
						page = page.substring(1);
					}
					p = page.indexOf('/');
					if (p > 0) {
						id = page.substring(0, p);
						page = page.substring(p);
					}
//                            send("id: " + id + "!");
//                            send("page: " + page + "!");
					if ("bitbox".equals(id)) {
						bitboxProtocol = true;
					}
				} else if ("bitbox".equals(line)) {
					bitboxProtocol = true;
//                send("http proto: " + httpProtocol);
//                send("bitbox proto: " + bitboxProtocol);
				}

				if (passResponse) {
					line = line.substring(line.indexOf(' ') + 1);
					SingleRequest req = requests.findByHash(line);
					if (req != null) {
						System.out.println("synchronizing to " + req.processor.hashCode());
						synchronized (req.processor) {
							try {
								System.out.println("passing response " + line);
								//req.processor.send("HTTP/1.1 200 OKi");
								req.processor.send(firstLine);
								byte[] buffer = new byte[1024 * 64];
								int count;
//                                boolean weHaveHtml = false;
								do {
									line = SocketCommunicator.read(in);
									if (line != null) {
										req.processor.send(line);
//                                        if ("Content-Type: text/html".equals(line)) {
//                                            weHaveHtml = true;
//											// UNCOMMENT THIS
//                                        }
										System.out.println("# " + line);
									}
								} while (line != null && !line.isEmpty());
//                                if (weHaveHtml) {
//                                    for (;;) {
//                                        line = SocketCommunicator.read(in);
//                                        if (line == null) {
//                                            break;
//                                        }
//                                        line = line.replaceAll("href=\"/", "href=\"/" + req.node.id + "/");
//                                        req.processor.send(line);
//                                        if ("Content-Type: text/html".equals(line)) {
//                                            weHaveHtml = true;
//                                        }
//                                    }
//                                } else 
								{
									while (true) {
										count = in.read(buffer);
										if (count > 0) {
											req.processor.write(buffer, 0, count);
//                                        System.out.print("####");
//                                        System.out.write(buffer, 0, count);
										} else {
											break;
										}
									}
								}
							} finally {
								System.out.println("finished passing response");
								req.processor.canBeFinalized = true;
								req.processor.notify();
								synchronized (requests) {
									requests.remove(req);
								}
							}
						}
					} else {
						System.err.println("not found req " + line);
					}
				} else if (httpProtocol && !bitboxProtocol && !passResponse && id != null && page != null && !page.isEmpty()) {
					StringBuilder buffer = new StringBuilder();
					SingleRequest req = new SingleRequest();
					req.hash = "rq" + requestCounter.getAndIncrement();
					req.processor = this;
					synchronized (requests) {
						requests.add(req);
					}
					buffer.append("req ").append(req.hash).append("\r\n");
					buffer.append("GET ").append(page).append(" HTTP/1.1").append("\r\n");
					while (!(line = read()).isEmpty()) {
//                                System.out.println("http in: " + line);
						buffer.append(line).append("\r\n");
					}
					buffer.append("\r\n");
					if (id != null && page != null && !id.isEmpty() && !page.isEmpty()) {
						ConnectedNode node = nodes.findById(id);
						req.node = node;
						if (node != null) {
							node.processor.send(buffer.toString());
							System.out.println("synchronizing to " + req.processor.hashCode());
							synchronized (this) {
								try {
									long time = System.currentTimeMillis();
									do {
										req.processor.wait(1000 * 20);
										if (System.currentTimeMillis() - time > 1000 * 60 * 1) {
											// 503 !!
											break;
										}
									} while (!req.processor.canBeFinalized);
								} catch (InterruptedException ex) {
									Logger.getLogger(BigBitBoxProxifier.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
								}
							}
//                        System.out.println("wait complete...........");
							//passConnection("127.0.0.1", 8080, buffer.toString(), out);
						} else {
							send(get404());
						}
					}
				} else if (bitboxProtocol) {
					if (httpProtocol) {
						while (!(line = read()).isEmpty()) {
							System.out.println("skip: " + line);
						}
						send("HTTP/1.1 200 OK");
						send("Transfer-Encoding: chunked");
						send("");
					}
					ConnectedNode node = new ConnectedNode();
					try {
						node.processor = this;
						System.out.println("communication for big boys now");
						do {
							line = read();
							System.out.println("> got: " + line);
							if (line != null && !line.isEmpty()) {
								if (line.startsWith("port ")) {
									line = line.substring(line.indexOf(' ') + 1);
									Integer portToTest = Integer.parseInt(line);
									node.port = portToTest;
									node.host = getSocket().getInetAddress().getCanonicalHostName();
									boolean connect = SocketCommunicator.testPort(getSocket().getInetAddress(), portToTest);
									send("connect: " + getSocket().getInetAddress() + " " + connect);
								} else if (line.startsWith("register ")) {
									line = line.substring(line.indexOf(' ') + 1);
									if (node.id == null && !line.isEmpty()) {
										node.id = line;
										node.host = getSocket().getInetAddress().getCanonicalHostName();
										synchronized (nodes) {
											nodes.add(node);
										}
										send("register ok");
									} else {
										send("register fail");
									}
								} else if (line.equals("ask")) {
									send("req 1111111 /p");
								} else {
									//send("echo: " + line);
								}
							}
						} while (line != null && !"quit".equals(line) && !"exit".equals(line) && !"bye".equals(line));
						if (!httpProtocol) {
							send("bye");
						}
					} finally {
						if (node.id != null) {
							synchronized (nodes) {
								nodes.remove(node);
							}
						}
					}
				} else {
					send(get404());
				}
			} finally {
				System.out.println("thread stopped, count: " + threadCounter.decrementAndGet());

			}
		}

		public RequestProcessor(Socket socket) {
			super(socket);
		}
	}
}
