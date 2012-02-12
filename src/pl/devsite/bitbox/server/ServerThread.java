package pl.devsite.bitbox.server;

import pl.devsite.bitbox.server.renderers.Router;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Renderer;
import pl.devsite.bitbox.sendables.Sendable;

/**
 *
 * @author dmn
 */
public class ServerThread implements Runnable {

	private static final Logger logger = Logger.getLogger(ServerThread.class.getName());
	private Socket socket;
	private Sendable root;
	private BitBoxConfiguration config = BitBoxConfiguration.getInstance();
	private Parser parser;
	private Processor processor;
	private Router router;

	public ServerThread(final Socket socket_, final Sendable root) {
		this.socket = socket_;
		this.root = root;
	}

	@Override
	public void run() {
		RequestContext context = null;
		try {
			parser = new Parser(socket, root);
			context = parser.getContext();
			processor = new Processor(context);
			processor.process();
			router = new Router(context);
			router.getRenderer().send();

		} catch (Exception ex) {
			String m = "";
			StackTraceElement[] stackTrace = ex.getStackTrace();
			String err = "";
			if (stackTrace != null && stackTrace.length > 1) {
				for (int i = stackTrace.length-1; i >=0; i--) {
					if (stackTrace[i].getClassName().startsWith("pl.devsite.")) {
						err = " @" + stackTrace[i].toString();
					}
				}
			}
			m = m + err;
			logger.log(Level.SEVERE, ex.toString() + m, ex);
		} finally {
			if (context != null) {
				if (context.getClientOut() != null) {
					try {
						context.getClientOut().flush();
						context.getClientOut().close();
					} catch (IOException ex) {
						logger.log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
				if (context.getClientIn() != null) {
					try {
						context.getClientIn();
						context.getClientIn().close();
					} catch (IOException ex) {
						logger.log(Level.SEVERE, ex.getMessage(), ex);
					}
				}
			}
			try {
				socket.close();
			} catch (IOException ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}

	}
}
