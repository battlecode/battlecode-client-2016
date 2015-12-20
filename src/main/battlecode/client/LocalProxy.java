package battlecode.client;

import battlecode.world.signal.Signal;
import battlecode.serial.notification.Notification;
import battlecode.server.proxy.Proxy;

import java.io.EOFException;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LocalProxy implements Proxy, ClientProxy {

    public static final LocalProxy INSTANCE = new LocalProxy();

    private final Queue<Object> queue;

    private final Notifier notifier;

    private static class Notifier extends Observable {
        public void notify(Object obj) {
            this.setChanged();
            this.notifyObservers(obj);
            this.clearChanged();
        }
    }

    // Can't instantiate.
    private LocalProxy() {
        queue = new ConcurrentLinkedQueue<Object>();
        notifier = new Notifier();
    }

    @Override
    public void writeObject(Object o) throws IOException {
        queue.add(o);
    }

    @Override
    public void close() throws IOException {
    }

    public Object readObject() throws EOFException {
        synchronized (queue) {
            while (queue.isEmpty())
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                }
            return queue.poll();
        }
    }

    public Object peekObject() throws EOFException {
        assert false : "Local match shouldn't have best-of-3 early termination";
        synchronized (queue) {
            while (queue.isEmpty()) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                }
            }
            return queue.peek();
        }
    }

    public boolean isDebuggingAvailable() {
        return true;
    }

    public void writeNotification(Notification n) {
        this.notifier.notify(n);
    }

    public void writeSignal(Signal s) {
        this.notifier.notify(s);
    }

    public void addObserver(Observer o) {
        this.notifier.addObserver(o);
    }

}
