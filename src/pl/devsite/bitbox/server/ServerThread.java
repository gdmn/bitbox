package pl.devsite.bitbox.server;

import java.beans.ExceptionListener;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.server.renderers.Router;

/**
 *
 * @author dmn
 */
public class ServerThread implements Runnable, ExceptionListener {

	private static final Logger logger = Logger.getLogger(ServerThread.class.getName());
	private Socket socket;
	private Sendable root;
	private BitBoxConfiguration config = BitBoxConfiguration.getInstance();
	private int exceptionCounter = 0;

	public ServerThread(final Socket socket_, final Sendable root) {
		this.socket = socket_;
		this.root = root;
	}

	@Override
	public void run() {
		RequestContext context = new RequestContext();
		context.setExceptionListener(this);
		context.setSocket(socket);
		context.setSendableRoot(root);

		Parser parser;
		AuthProcessor processor;
		Router router;

		try {
			parser = new Parser();
			parser.initialize(context);
			parser.execute();
			
			processor = new AuthProcessor();
			processor.initialize(context);
			processor.execute();
			
			router = new Router();
			router.initialize(context);
			router.execute();
			
			context.getRenderer().execute();

		} catch (Exception ex) {
			exceptionThrown(ex);
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

	@Override
	public void exceptionThrown(Exception ex) {
		String m = "";
		StackTraceElement[] stackTrace = ex.getStackTrace();
		String err = "";
		if (stackTrace != null && stackTrace.length > 1) {
			for (int i = stackTrace.length - 1; i >= 0; i--) {
				if (stackTrace[i].getClassName().startsWith("pl.devsite.")) {
					err = " @" + stackTrace[i].toString();
				}
			}
		}
		m = m + err;
		logger.log(Level.SEVERE, ex.toString() + m, ex);
	}
}
