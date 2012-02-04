package pl.devsite.bitbox.server;

import pl.devsite.bitbox.sendables.SendableError;
import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dmn
 */
public class Server implements Runnable {

    protected ServerSocket server;
    protected int port;
    protected ExecutorService threadPool;
    protected int poolSize;
    private static final Logger logger = Logger.getLogger(Server.class.getName());
    private boolean started;
    private BitBoxConfiguration bitBoxConfiguration = BitBoxConfiguration.getInstance();
    private Thread thread;

    public Server() {
        started = true;
    }

    public Server(int port, int poolSize) {
        this();
        setParameters(port, poolSize);
    }

    private void setParameters(int port, int poolSize) {
        this.port = port;
        this.poolSize = poolSize;
        logger.log(Level.INFO, "Set server parameters: port="+port+", pool="+poolSize);
        if (poolSize < 1 || poolSize > 300) {
            //threadPool = new ThreadPoolExecutor(0, 300, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            threadPool = Executors.newCachedThreadPool();
        } else if (poolSize == 1) {
            threadPool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        } else if (poolSize > 1) {
            threadPool = new ThreadPoolExecutor(poolSize, poolSize, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        }
    }


    public int getPort() {
        return port;
    }

    public boolean isStarted() {
        return started;
    }

    public void setStarted(boolean started) {
        this.started = started;
        if (!started && server != null) {
            try {
                server.close();
                server = null;
            } catch (IOException ex) {
            }
        }
    }

    public static SendableError get503() {
        SendableError s_503 = new SendableError(null, "error", 503, new String[][]{
                    {HttpTools.SERVER, BitBoxConfiguration.getInstance().getProperty(BitBoxConfiguration.PROPERTY_NAME)},
                    {HttpTools.CONTENTTYPE, HttpTools.CONTENTTYPE_TEXT_HTML},
                    {HttpTools.RETRYAFTER, "10"},});
        return s_503;
    }

    protected void protectedRun() throws IOException {
        try {
			String addr = bitBoxConfiguration.getProperty(BitBoxConfiguration.PROPERTY_ADDR);
			InetAddress inetAddress = addr == null ? null : InetAddress.getByName(addr);
			server = inetAddress == null ? new ServerSocket(port) : new ServerSocket(port, 50, inetAddress);
			while (started) {
                Socket client = server.accept();
                //pool.execute(new ServerThread(server.accept(), sendableRoot));
                Runnable t = null;
                if (poolSize > 0 && (threadPool instanceof ThreadPoolExecutor) && (((ThreadPoolExecutor)threadPool).getActiveCount() >= poolSize - 1)) {
                    t = new ServerThread(client, get503());
                    logger.warning("max requests reached (" + poolSize + "), sending 503");
                } else {
                    t = new ServerThread(client, bitBoxConfiguration.getSendableRoot());
                }
                threadPool.execute(t);
            }
        } finally {
            if (server != null) {
                server.close();
            }
        }
    }

    @Override
    public void run() {
        try {
            protectedRun();
        } catch (SocketException ex) {
            if (started) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
    }

    public void startListening() {
        thread = new Thread(this);
        thread.start();
    }

    public void stopListening() {
        if (thread != null) {
            thread.interrupt();
        }
        thread = null;
    }
}
