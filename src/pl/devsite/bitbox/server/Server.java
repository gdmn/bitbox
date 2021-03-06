package pl.devsite.bitbox.server;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import pl.devsite.bitbox.sendables.Sendable;

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
    protected InetAddress bindAddr;

    public Server() {
        started = true;
    }

    public Server(int port, int poolSize, String bind) {
        this();
        setParameters(port, poolSize, bind);
    }

    private void setParameters(int port, int poolSize, String bind) {
        this.port = port;
        this.poolSize = poolSize;
        this.bindAddr = null;
        if (bind != null) {
            try {
                this.bindAddr = InetAddress.getByName(bind);
            } catch (UnknownHostException e) {
                this.bindAddr = null;
            }
        }

        logger.log(Level.INFO, "Set server parameters: port={0}, pool={1}{2}",
                new Object[]{""+port, ""+poolSize, bindAddr == null ? "" : ", bind=" + bindAddr.toString()});
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

//    public static Sendable get503() {
//        SendableError s_503 = new SendableError(null, "error", 503, new String[][]{
//                    {HttpTools.SERVER, BitBoxConfiguration.getInstance().getProperty(BitBoxConfiguration.PROPERTY_NAME)},
//                    {HttpTools.CONTENTTYPE, HttpTools.CONTENTTYPE_TEXT_HTML},
//                    {HttpTools.RETRYAFTER, "10"},});
//        return s_503;
//    }

    protected void protectedRun() throws IOException {
        try {
            server = bindAddr == null ? new ServerSocket(port) : new ServerSocket(port, 50, bindAddr);
            while (started) {
                Socket client = server.accept();
                //pool.execute(new ServerThread(server.accept(), sendableRoot));
                Runnable t = null;
//                if (poolSize > 0 && (threadPool instanceof ThreadPoolExecutor) && (((ThreadPoolExecutor) threadPool).getActiveCount() >= poolSize - 1)) {
//                    t = new ServerThread(client, get503());
//                    logger.warning("max requests reached (" + poolSize + "), sending 503");
//                } else
				{
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
        } catch (BindException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            System.exit(1);
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
