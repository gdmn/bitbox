package pl.devsite.bitbox.server;

import java.beans.ExceptionListener;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.Sendable;
import pl.devsite.bitbox.server.renderers.Renderer;

/**
 *
 * @author dmn
 */
public class ServerThread implements Runnable, ExceptionListener {

	private static final Logger logger = Logger.getLogger(ServerThread.class.getName());
	private Socket socket;
	private Sendable root;
	private BitBoxConfiguration config = BitBoxConfiguration.getInstance();

	public ServerThread(final Socket socket_, final Sendable root) {
		this.socket = socket_;
		this.root = root;
	}

//	public static Operation stringToOperation(Object s) {
//		if (s == null) {
//			return null;
//		}
//		if (s instanceof Operation) {
//			return (Operation) s;
//		}
//		if (s instanceof String) {
//			return new Operation((String) s);
//		}
//		return null;
//	}
	@Override
	public void run() {
		RequestContext context = new RequestContext();
		context.setExceptionListener(this);
		context.setSocket(socket);
		context.setSendableRoot(root);

		HttpRequestParser parser;
		AuthProcessor processor;
		Router router;
		Object op;

		try {
			parser = new HttpRequestParser();
			parser.initialize(context);
			String requestedString = parser.execute();

			op = requestedString;

//			processor = new AuthProcessor();
//			processor.initialize(context);
//			processor.execute();

			do {
				do {
					router = new Router();
					router.initialize(context);
					Object nextRoute = router.execute(op);
					if (nextRoute == op) {
						break;
					}
					op = nextRoute;

//					if (op == Processor.LAST) {
//						break;
//					}

					if (op instanceof Processor) {
						Processor opProcessor = (Processor) op;
						op = opProcessor.execute();
					}
//
//				if (op.getType().equals(Operation.Type.READY)) {
//					op = stringToOperation(context.getRenderer().execute());
//					if (op.getType().equals(Operation.Type.DONE)) {
//						break;
//					}
//				}

				} while (true);

				if (op instanceof Sendable) {
					context.setSendableResponse((Sendable) op);
					break;
				} else {
					op = "error:routing failed";
				}
			} while (true);
			
			Renderer renderer = new Renderer();
			renderer.initialize(context);
			renderer.execute();
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
