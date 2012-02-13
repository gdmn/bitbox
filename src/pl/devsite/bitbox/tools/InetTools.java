package pl.devsite.bitbox.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.server.BitBoxConfiguration;
import static pl.devsite.bitbox.server.BitBoxConfiguration.*;

public class InetTools {

	private static List<String> internalIpCache;
	private static long internalIpCacheTimestamp = -1;
	private static long internalIpCacheRefreshSec = 60 * 5;
	private static String externalIpCache;
	private static long externalIpCacheTimestamp = -1;
	private static long externalIpCacheRefreshSec = 60 * 30;
	private static final Logger logger = Logger.getLogger(InetTools.class.getName());

	public static Socket getProxifiedSocket() throws IOException {
		BitBoxConfiguration config = BitBoxConfiguration.getInstance();
		String type = config.getProperty(PROPERTY_PROXY_TYPE);
		type = type == null ? null : type.toLowerCase();
		boolean enabled = str2boolean(config.getProperty(PROPERTY_PROXY_ENABLED, "0"));
		Integer port = str2int(config.getProperty(PROPERTY_PROXY_PORT));
		String host = config.getProperty(PROPERTY_PROXY_HOST);
		Proxy proxy = null;
		if (enabled) {
			proxy = new Proxy(("socks".equals(type) ? Proxy.Type.SOCKS : Proxy.Type.HTTP), new InetSocketAddress(host, port));
		}
		Socket socket = enabled ? new Socket(proxy) : new Socket();
		return socket;
	}

	public static List<InetAddress> getNonLocalActiveInetAddresses(boolean filterVirtualAdapters) throws SocketException {
		LinkedList<InetAddress> result = new LinkedList<InetAddress>();
		Enumeration e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
			NetworkInterface ni = (NetworkInterface) e.nextElement();
			Enumeration e2 = ni.getInetAddresses();
			if (ni.isUp()) {
				while (e2.hasMoreElements()) {
					InetAddress ip = (InetAddress) e2.nextElement();
					if (!ip.isLoopbackAddress() && !ip.isLinkLocalAddress()) {
						String idn = ni.getDisplayName();
						if (!filterVirtualAdapters || idn == null || idn.isEmpty() || !idn.toLowerCase().contains("virtual")) {
							result.add(ip);
						}
					}
				}
			}
		}
		return result;
	}

	public static List<String> tryToGuessIp() {
		long t = System.currentTimeMillis();
		if (t - internalIpCacheTimestamp < 1000 * internalIpCacheRefreshSec) {
			return internalIpCache;
		}
		internalIpCacheTimestamp = t;

		Runnable r = new Runnable() {

			@Override
			public void run() {
				List<String> result = new LinkedList<String>();
				try {
					List<InetAddress> lip = getNonLocalActiveInetAddresses(false);
					if (lip.size() > 1) {
						List<InetAddress> lip2 = getNonLocalActiveInetAddresses(true);
						if (lip2.size() > 0) {
							lip = lip2;
						}
					}
					for (InetAddress ip : lip) {
						result.add(ip.getHostAddress());
					}
					internalIpCache = result;
					if (internalIpCache != null && internalIpCache.size() > 0) {
						logger.log(Level.INFO, "internal IP: " + (internalIpCache.size() == 1 ? internalIpCache.get(0) : internalIpCache));
					}
				} catch (SocketException e) {
					logger.log(Level.SEVERE, "getting internal IP failed, " + e.getMessage(), e);
					internalIpCache = null;
				}
			}
		};
		new Thread(r).start();
		return internalIpCache;
	}

	public static String getExternalIp() {
		long t = System.currentTimeMillis();
		if (t - externalIpCacheTimestamp < 1000 * externalIpCacheRefreshSec) {
			return externalIpCache;
		}
		externalIpCacheTimestamp = t;

		Runnable r = new Runnable() {

			@Override
			public void run() {
				Socket s = null;
				BufferedReader br = null;

				try {
					try {
						String host = "static.devsite.pl";
						String file = "/ip.php";
						int port = 80;

						s = getProxifiedSocket();
						s.setSoTimeout(1000 * 10); // 2 sek
						s.connect(new InetSocketAddress(host, port));

						OutputStream out = s.getOutputStream();
						PrintWriter outw = new PrintWriter(out, false);
						outw.print("GET " + file + " HTTP/1.1\r\n");
						outw.print("Accept: text/plain, text/html, text/*\r\n");
						outw.print("Host: " + host + "\r\n");
						outw.print("\r\n");
						outw.print("\r\n");
						outw.flush();

						InputStream in = s.getInputStream();
						InputStreamReader inr = new InputStreamReader(in);
						br = new BufferedReader(inr);
						String line;
						boolean afterHeader = false;
						while ((line = br.readLine()) != null) {
							if (afterHeader) {
								externalIpCache = line.trim();
								if (!externalIpCache.isEmpty()) {
									break;
								}
							}
							if (line.isEmpty()) {
								afterHeader = true;
							}
						}
						logger.log(Level.INFO, "external IP: " + externalIpCache);
					} finally {
						if (br != null) {
							br.close();
						}
						if (s != null) {
							s.close();
						}
					}
				} catch (IOException ex) {
					externalIpCache = null;
					logger.log(Level.SEVERE, "getting external IP failed, " + ex.getMessage(), ex);
				}
			}
		};
		new Thread(r).start();
		return externalIpCache;
	}
}
