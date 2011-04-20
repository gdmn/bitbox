package pl.devsite.bitbox.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Logger;
import pl.devsite.bigbitbox.client.BigBitBoxClient;
import pl.devsite.configuration.Configuration;
import pl.devsite.log.ConfigLog;
import static pl.devsite.bitbox.server.BitBoxConfiguration.*;

/**
 *
 * @author Damian Gorlach
 */
public class BitBox {

	private static final Logger logger = Logger.getLogger(BitBox.class.getName());

	public static void main(String args[]) throws FileNotFoundException, IOException {
		ConfigLog.apply();
		Runtime.getRuntime().addShutdownHook(new Hook());
		BitBoxConfiguration config = BitBoxConfiguration.getInstance();
		BitBoxConfigurationListener listener = new BitBoxConfigurationListener(config);
		config.addConfigurationChangeListener(listener);
		boolean configRead = false;
		//configRead = config.tryToLoadConfig(args);
		configRead = config.getFile() != null;
		Server bitBoxServer = new Server(Integer.parseInt(config.getProperty(PROPERTY_PORT)),
				Integer.parseInt(config.getProperty(PROPERTY_POOLSIZE)));

		if (!configRead && (args.length > 0)) {
			int i = 0;
			for (String path : args) {
				File f = new File(path);
				config.setProperty("share." + (i++) + ".path", f.getCanonicalPath());
			}
			config.notifyListeners();
		}
		bitBoxServer.startListening();
		if (Configuration.str2boolean(config.getProperty(BitBoxConfiguration.PROPERTY_BIGBIT_ENABLED, "0"))) {
			BigBitBoxClient.start();
		}
	}

	private static class Hook extends Thread {

		public void run() {
			System.out.println("Bye...");
		}
	}
	/*
	private final ArrayList<String> allowedIP = new ArrayList<String>();
	private final ArrayList<String> allowedUsers = new ArrayList<String>();
	private final ArrayList<String> bannedIP = new ArrayList<String>();
	private final ArrayList<String> links = new ArrayList<String>();
	private final ArrayList<String> readable = new ArrayList<String>();
	private ArrayList<String> sharingTable = new ArrayList<String>();
	private Server bitBoxServer;
	private int port = 8080;
	private int threads = 10;
	private String homepage = null;
	private String servername = null;
	private boolean helpDisplayed = false;
	private boolean interactive = false;
	private boolean mimeTypes = true;
	private boolean directoryListing = true;
	private boolean basicAuthenticator = false;
	private String outputDirectory = null;
	
	protected void parseArguments(List<String> args) {
	String prev_arg = null;
	for (String arg : args) {
	if (arg.charAt(0) == '-') {
	if ("-h".equals(arg) || "--help".equals(arg)) {
	showHelp();
	helpDisplayed = true;
	} else if ("-i".equals(arg) || "--interactive".equals(arg)) {
	interactive = true;
	} else if ("-m".equals(arg) || "--nomime".equals(arg)) {
	mimeTypes = false;
	} else if ("-d".equals(arg) || "--directory".equals(arg)) {
	directoryListing = false;
	} else if ("-b".equals(arg) || "--basic".equals(arg)) {
	basicAuthenticator = true;
	} else {
	prev_arg = arg.toLowerCase();
	}
	} else if (prev_arg != null) {
	if ("-p".equals(prev_arg) || "--port".equals(prev_arg)) {
	port = Integer.valueOf(arg);
	} else if ("-t".equals(prev_arg) || "--threads".equals(prev_arg)) {
	threads = Integer.valueOf(arg);
	} else if ("-n".equals(prev_arg) || "--name".equals(prev_arg) || "--servername".equals(prev_arg)) {
	servername = arg;
	} else if ("-w".equals(prev_arg) || "--homepage".equals(prev_arg)) {
	homepage = arg;
	} else if ("-a".equals(prev_arg) || "--allow".equals(prev_arg)) {
	allowedIP.add(arg);
	} else if ("-u".equals(prev_arg) || "--user".equals(prev_arg)) {
	allowedUsers.add(arg);
	} else if ("-r".equals(prev_arg) || "--readable".equals(prev_arg)) {
	readable.add(arg);
	} else if ("-o".equals(prev_arg) || "--output".equals(prev_arg)) {
	outputDirectory = arg;
	} else if ("-l".equals(prev_arg) || "--link".equals(prev_arg)) {
	int p = arg.indexOf('|');
	String name, url;
	name = p > 0 ? arg.substring(0, p) : "unknown name";
	url = p > 0 ? arg.substring(p + 1) : arg;
	links.add("<a href=\"" + url + "\">" + name + "</a>");
	} else {
	System.err.println("Unknown parameter: " + prev_arg);
	System.exit(-2);
	}
	prev_arg = null;
	} else {
	sharingTable.add(arg);
	}
	}
	if (prev_arg != null) {
	System.err.println("Unknown parameter or inproper usage: " + prev_arg);
	System.exit(-2);
	}
	if (sharingTable.size() == 0) {
	if (helpDisplayed) {
	System.exit(0);
	} else {
	showHelp();
	System.exit(-1);
	}
	}
	SendableFilter filter = null;
	if (allowedIP.size() == 0 && !interactive && bannedIP.size() == 0) {
	if (!directoryListing) {
	filter = new SendableFilter() {
	
	@Override
	public boolean isAllowed(Sendable sendable, Object hostAddress, Object authenticatedUser) {
	return !sendable.hasChildren();
	}
	};
	}
	} else {
	filter = new IPFilter(interactive, directoryListing, allowedIP, bannedIP);
	}
	
	bitBoxServer = new Server(port, threads);
	BitBoxConfiguration.getInstance().setProperty(BitBoxConfiguration.PROPERTY_NAME, servername);
	BitBoxConfiguration.getInstance().setProperty(BitBoxConfiguration.PROPERTY_WWW, homepage);
	BitBoxConfiguration.getInstance().setProperty(BitBoxConfiguration.PROPERTY_OUTPUTDIRECTORY, outputDirectory);
	
	SendableRoot bitBoxRoot = new SendableRoot(null, "(root)");
	bitBoxRoot.setFilter(filter);
	links.add(0, "<a href=\"/\">home</a>");
	if (outputDirectory != null) {
	SendableAdapter pu = new PageUpload(bitBoxRoot, "upload");
	bitBoxRoot.addSendable(pu);
	links.add(1, "<a href=\"/upload\">upload</a>");
	}
	{
	//            SendableAdapter pu = new PageMessageReceiver(sl, "msg");
	//            sl.addSendable(pu);
	//            links.add(1, "<a href=\"/msg\">msg</a>");
	}
	//        if (links.size() > 1) {
	//            BitBoxConfiguration.getInstance().setMenu(links.toArray(new String[links.size()]));
	//        }
	for (String share : sharingTable) {
	if (mimeTypes) {
	bitBoxRoot.addSendable(new SendableFileWithMimeResolver(bitBoxRoot, new File(share)));
	} else {
	bitBoxRoot.addSendable(new SendableFile(bitBoxRoot, new File(share)));
	}
	}
	//bitBoxServer.setSendableRoot(bitBoxRoot);
	
	if (allowedUsers.size() > 0) {
	HttpAuthenticator httpAuthenticator = basicAuthenticator ? new HttpBasicAuthenticator() : new HttpDigestAuthenticator();
	for (String user_pass : allowedUsers) {
	int pos = user_pass.indexOf(':');
	if (pos > -1) {
	httpAuthenticator.addUser(user_pass.substring(0, pos), user_pass.substring(pos + 1));
	}
	}
	bitBoxRoot.setAuthenticator(httpAuthenticator);
	}
	
	if (readable.size() > 0) {
	for (String line : readable) {
	int atPos = line.indexOf('@');
	if (atPos > 0) {
	String users = line.substring(0, atPos);
	String path = line.substring(atPos + 1);
	Sendable serverPath = SendableAdapter.tryToFindSendable(bitBoxRoot, path);
	String[] userArray = users.split(",");
	if (serverPath instanceof SendableAdapter) {
	SendableAdapter sendableServerPath = (SendableAdapter) serverPath;
	if (users.equals("*")) {
	sendableServerPath.setAuthenticator(HttpTools.NULLAUTHENTICATOR);
	} else {
	UserFilter userFilter = new UserFilter(sendableServerPath.getAuthenticator(), userArray);
	sendableServerPath.setAuthenticator(userFilter);
	}
	}
	}
	}
	}
	//        SendableRoot child = new SendableRoot(sl, "dysk c", filter);
	//        sl.addSendable(new SendableFile(sl, new File("/home/dmn/Publiczny/")));
	//        sl.addSendable(new SendableFile(sl, new File("d:/progs/")));
	//        child.addSendable(new SendableFile(child, new File("C:/devenv/")));
	//        child.addSendable(new SendableFile(child, new File("c:\\Documents and Settings\\gorladam\\My Documents\\download\\")));
	//        sl.addSendable(child);
	//        sl.addSendable(new SendableString(sl, "info", "tekst\r\n\r\nlinia 2\r\ntest polskich liter: ęóąśłżźćńĘÓĄŚŁŻŹĆŃ"));
	//        sl.addSendable(new SendableStream(sl, "streamowany plik z dysku, typ text html", "text/html", new FileInputStream("C:\\test.html")));
	//        sl.addSendable(new SendableStream(sl, "streamowany plik z dysku, typ image jpeg", "image/jpeg", new FileInputStream("C:/Documents and Settings/gorladam/My Documents/My Pictures/tapety/abt-scirocco-blue-wallpapers_11586_1600x1200.jpg")));
	//        sl.addSendable(new SendableFile(sl, new File("C:/Documents and Settings/gorladam/My Documents/My Pictures/tapety/1273929104_5e0f05d985_o.jpg")));
	//        fs.setSendableRoot(sl);
	
	//        StringBuffer k = new StringBuffer(100);
	//        for (int i = 0; i < 10; i++) {
	//        for (int j = 0; j < 10; j++) {
	//        k.append("" + i);
	//        }
	//        }
	//        bitBoxRoot.addSendable(new SendableString(bitBoxRoot, "buffertest", k.toString()));
	}
	
	public static void showHelp() {
	final String[] help = new String[]{
	"SendableServer",
	"Example usage:",
	"   java -jar [archivename].jar /home/dmn/public",
	"   java -jar [archivename].jar -p 8080 -t 5 -a 192.168.1.10 -a 192.168.1.11 -u dmn:password -w \"http://dmn.jogger.pl\" -n \"Example Server\" /home/dmn/public /home/dmn/Desktop",
	"   java -jar [archivename].jar -u dmn:password -u 2nd:secret /home/dmn/public /home/dmn/Desktop -r 2nd@/public",
	"Parameters:",
	"  -h, --help        - short help",
	"  -p, --port        - port number, default: 8080",
	"  -t, --threads     - threads number, default: 10",
	"  -a, --allow       - allowed user ip, if none, everyone is allowed",
	"  -u, --user        - allowed user name and password",
	"  -b, --basic       - use http basic authentication type (default: digest)",
	"  -i, --interactive - show confirmation dialog for accepting incoming connections",
	"  -r, --readable    - enumerated users (comma separated) can access given path (and only them).",
	"                      Note that only for root levels users can be assigned.",
	"                      \"*\" mean that path is available for all (without login)",
	"  -m, --nomime      - do not handle mime types",
	"  -d, --directory   - disallow directory listing",
	"  -o, --output      - set output directory for received files",
	"  -l, --link        - add link in menu (format: name|url)",
	"  -w, --homepage    - homepage",
	"  -n, --name        - name of server",};
	for (String s : help) {
	System.out.println(s);
	}
	}
	public static void main(String args[]) throws FileNotFoundException {
	//"c:\Program Files\Apache Software Foundation\Apache2.2\bin\ab.exe" -n 1000 -c 10 http://localhost:8080/
	args = new String[]{"-p", "8080",
	//"-i",
	"-t", "13",
	//"-u", "user:pass2", "-b",
	//"-o", "c:\\Documents and Settings\\gorladam\\Desktop\\",
	//"-o", "/home/dmn/Pulpit",
	"c:/", "/home/dmn/junks"
	};
	}
	 */
}
