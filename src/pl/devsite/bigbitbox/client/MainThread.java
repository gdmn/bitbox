package pl.devsite.bigbitbox.client;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.io.IOException;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.server.BitBoxConfiguration;
import pl.devsite.configuration.ConfigurationChangeListener;
import static pl.devsite.bitbox.server.BitBoxConfiguration.*;

/**
 *
 * @author Damian Gorlach
 */
public class MainThread implements Runnable, ConfigurationChangeListener {

	private static final Logger logger = Logger.getLogger(MainThread.class.getName());
	long modified;
	BigBitBoxClient m = null;
	long lastModified = 0;
	boolean startAnotherBigBitBoxClient = false;

	void kill() {
		if (m != null) {
			logger.info("stopping old BigBitBoxClient");
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
							//executorService.submit(m);
							m.run();
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

	@Override
	public void configurationUpdated(Properties properties) {
		kill();
	}

	static Socket createSocketToBigBitBoxServer() throws IOException {
		BitBoxConfiguration config = BitBoxConfiguration.getInstance();
		String type = config.getProperty(PROPERTY_BIGBIT_PROXY_TYPE);
		type = type == null ? null : type.toLowerCase();
		boolean enabled = str2boolean(config.getProperty(PROPERTY_BIGBIT_PROXY_ENABLED, "0"));
		Integer port = str2int(config.getProperty(PROPERTY_BIGBIT_PROXY_PORT));
		String host = config.getProperty(PROPERTY_BIGBIT_PROXY_HOST);
		Proxy proxy = null;
		if (enabled) {
			proxy = new Proxy(("socks".equals(type) ? Proxy.Type.SOCKS : Proxy.Type.HTTP), new InetSocketAddress(host, port));
		}
		Socket socket = enabled ? new Socket(proxy) : new Socket();
		socket.setTcpNoDelay(true);
		//socket.setKeepAlive(false);
		socket.setSoLinger(true, 60); // wait max XX sec after socket closed
		socket.setSoTimeout(1000 * 60 * 60); // 1h
//		socket.setTcpNoDelay(true);
//		socket.setKeepAlive(true);
		socket.setSendBufferSize(512);
		socket.connect(new InetSocketAddress(config.getProperty(PROPERTY_BIGBIT_SERVER_HOST), str2int(config.getProperty(PROPERTY_BIGBIT_SERVER_PORT))));
		return socket;
	}
}
