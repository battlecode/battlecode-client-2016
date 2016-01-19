package battlecode.client;

import battlecode.serial.ServerEvent;
import battlecode.serial.notification.Notification;
import battlecode.serial.notification.NotificationHandler;
import battlecode.server.GameInfo;
import battlecode.server.proxy.Proxy;
import battlecode.server.proxy.ProxyFactory;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Serves as a bidirectional channel between the server and the client.
 * The server writes messages to the client with writeEvent, the client
 * writes messages to the server with writeNotification.
 */
public class LocalProxy implements Proxy, ClientProxy {

    /**
     * Singleton instance; we only support one client in a JVM
     * at a time.
     */
    public static final LocalProxy INSTANCE = new LocalProxy();

    /**
     * "Factory" that returns INSTANCE.
     */
    public static final ProxyFactory FACTORY = info -> INSTANCE;

    private final Queue<ServerEvent> inputQueue;

    private final List<NotificationHandler> outputHandlers;

    /**
     * Create a new LocalProxy.
     */
    private LocalProxy() {
        this.outputHandlers = new ArrayList<>();
        this.inputQueue = new ConcurrentLinkedQueue<>();
    }

    public void addOutputHandler(NotificationHandler outputHandler) {
        this.outputHandlers.add(outputHandler);
    }

    @Override
    public void writeEvent(ServerEvent o) throws IOException {
        inputQueue.add(o);
    }

    @Override
    public void close() throws IOException {}

    @Override
    public ServerEvent readEvent() throws EOFException {
        synchronized (inputQueue) {
            while (inputQueue.isEmpty())
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                }
            return inputQueue.poll();
        }
    }

    @Override
    public ServerEvent peekEvent() throws EOFException {
        assert false : "Local match shouldn't have best-of-3 early termination";
        synchronized (inputQueue) {
            while (inputQueue.isEmpty()) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                }
            }
            return inputQueue.peek();
        }
    }

    @Override
    public boolean isDebuggingAvailable() {
        return true;
    }

    @Override
    public void writeNotification(Notification n) {
        for (NotificationHandler handler : outputHandlers) {
            n.accept(handler);
        }
    }
}
