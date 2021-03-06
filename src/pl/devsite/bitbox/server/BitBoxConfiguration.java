package pl.devsite.bitbox.server;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.sendables.SendableFilter;
import pl.devsite.bitbox.sendables.SendableRoot;
import pl.devsite.configuration.Configuration;

/**
 *
 * @author Damian Gorlach
 */
public class BitBoxConfiguration extends Configuration {

	private static final Logger logger = Logger.getLogger(BitBoxConfiguration.class.getName());
	private final SendableRoot bitBoxRoot = new SendableRoot(null, "(root)");
	private String menuBuffer = null;
	//--
	public static final String AUTHENTICATOR_BASIC = "basic";
	public static final String AUTHENTICATOR_DIGEST = "digest";
	//--
	public static final String PROPERTY_ADDR = "server.bind";
	public static final String PROPERTY_PORT = "server.port";
	public static final String PROPERTY_NAME = "server.name";
	public static final String PROPERTY_WWW = "server.www";
	public static final String PROPERTY_POOLSIZE = "server.poolsize";
	public static final String PROPERTY_OUTPUTDIRECTORY = "server.output";
	public static final String PROPERTY_USER = "user";
	public static final String PROPERTY_SHARE = "share";
	public static final String PROPERTY_MENU = "menu";
	public static final String PROPERTY_AUTHENTICATOR = "authenticator";
	public static final String PROPERTY_SHOW_INTERNAL_IP_IN_FOOTER = "server.footer.ip.internal";
	public static final String PROPERTY_SHOW_EXTERNAL_IP_IN_FOOTER = "server.footer.ip.external";
	//--
	public final static String PROPERTY_PROXY_PORT = "proxy.port";
	public final static String PROPERTY_PROXY_HOST = "proxy.host";
	public final static String PROPERTY_PROXY_TYPE = "proxy.type";
	public final static String PROPERTY_PROXY_ENABLED = "proxy.enabled";

	public final static String PROPERTY_BIGBIT_NODE_NAME = "bigbit.node.name";
	public final static String PROPERTY_BIGBIT_SERVER_HOST = "bigbit.server.host";
	public final static String PROPERTY_BIGBIT_SERVER_PORT = "bigbit.server.port";
	public final static String PROPERTY_BIGBIT_ENABLED = "bigbit.enabled";
	//--
	/** lame, oggenc, none */
	public static final String PROPERTY_TOOLS_ENCODER = "server.tools.encoder";
	public static final String PROPERTY_TOOLS_SOXI = "server.tools.soxi";
	public static final String PROPERTY_TOOLS_FFMPEG = "server.tools.ffmpeg";
	public static final String PROPERTY_TOOLS_LAME = "server.tools.lame";
	public static final String PROPERTY_TOOLS_OGGENC = "server.tools.oggenc";
	public static final String PROPERTY_TOOLS_AAC = "server.tools.aac";
	public static final String PROPERTY_TOOLS_LAME_OPTIONS = "server.tools.lame.options";
	public static final String PROPERTY_TOOLS_OGGENC_OPTIONS = "server.tools.oggenc.options";
	public static final String PROPERTY_TOOLS_AAC_OPTIONS = "server.tools.aac.options";
	//--
	private static BitBoxConfiguration instance;

	static {
		instance = new BitBoxConfiguration();
	}

	private BitBoxConfiguration() {
		super();
		tryToLoadConfig(null);
		bitBoxRoot.setFilter(new SendableFilter() {

			@Override
			public boolean isAllowed(Sendable sendable, Object hostAddress, Object authenticatedUser) {
				//System.out.println(">> "+hostAddress);
				return true;
			}
		});
	}

	public static BitBoxConfiguration getInstance() {
		return instance;
	}

	void resetBuffers() {
		menuBuffer = null;
	}

//	public abstract Properties getDefaultProperties();
//	@Override
//	public void readConfiguration(File file) throws IOException {
//		super.readConfiguration(file);
//		properties = new Properties(getDefaultProperties());
//		menuBuffer = null;
//		try {
//			properties.load(new FileInputStream(file));
//		} finally {
//			lastModified = conf.getLastModified();
//			parseProperties();
//		}
//	}
	public String getHeadBodyHTTP() {
		String headBodyHTTP = HttpTools.convertToMultiline(new String[]{
					HttpTools.XHTMLHEADER,
					"<head>",
					"<title>" + getProperty(PROPERTY_NAME) + "</title>",
					"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />",
					HttpTools.getDefaultCSS(),
					"</head>",
					"<body>",});
		return headBodyHTTP;
	}

//    public void setHeadBodyHTTP(String headBodyHTTP) {
//        this.headBodyHTTP = headBodyHTTP;
//    }
	public String getMenuHtml() {
		if (menuBuffer == null) {
			StringBuilder buffer = new StringBuilder();
			Collection<String> menus = Configuration.grep(stringPropertyNames(), PROPERTY_MENU);
			if (menus == null || menus.isEmpty()) {
				menuBuffer = "";
				return "";
			}
			buffer.append("<div id=\"MENU\"><ul>" + "\r\n");
			for (String menu : menus) {
				String name = getProperty(menu + "name");
				String link = getProperty(menu + "link");
				buffer.append("  <li>");
				buffer.append(String.format("<a href=\"%s\">%s</a>", link, name));
				buffer.append("</li>" + "\r\n");
			}
			buffer.append("</ul></div>");
			menuBuffer = buffer.toString();
		}
		return menuBuffer;
	}

	private boolean readConfigIfExists(String file) {
		File f = new File(file);
		try {
			logger.log(Level.INFO, "trying to find config file - {0}", f.getCanonicalPath());
		} catch (IOException ex) {
		}
		if (f.isFile() && f.canRead()) {
			try {
				setFile(f);
			} catch (IOException ex) {
				return false;
			}
			return true;
		}
		return false;
	}

	public final boolean tryToLoadConfig(String args[]) {
		boolean configRead = false;
		final String logMsgFound = "Config file found: %s";
		final String logMsgNotFound = "Config file not found";
		if (args != null && args.length == 2 && args[0].equals("-c") && readConfigIfExists(args[1])) {
			configRead = true;
			logger.info(String.format(logMsgFound, args[1]));
		} else {
			for (String name : Configuration.getDefaultConfigLocations("bitbox")) {
				if (readConfigIfExists(name)) {
					configRead = true;
					break;
				}
			}
		}
		if (configRead) {
			try {
				logger.info(String.format(logMsgFound, getFile().getCanonicalPath()));
			} catch (IOException ex) {
			}
		} else {
			logger.warning(logMsgNotFound);
		}
		return configRead;
	}

	public SendableRoot getSendableRoot() {
		return bitBoxRoot;
	}

	@Override
	public Properties getDefaultProperties() {
		Properties defaultProperties = new Properties();
		defaultProperties.setProperty(PROPERTY_WWW, "/");
		defaultProperties.setProperty(PROPERTY_NAME, "BitBox Server");
		defaultProperties.setProperty(PROPERTY_PORT, "8080");
		defaultProperties.setProperty(PROPERTY_POOLSIZE, "0");
		defaultProperties.setProperty(PROPERTY_AUTHENTICATOR, "basic");
		return defaultProperties;
	}
}
