package battlecode.client;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import battlecode.serial.*;
import battlecode.serial.notification.Notification;
import battlecode.server.proxy.Proxy;
import battlecode.engine.signal.Signal;

public class LocalProxy extends Proxy implements ClientProxy {

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

	protected OutputStream getOutputStream() throws IOException {
		return null;
	}

	public void writeObject(Object o) throws IOException {
		queue.add(o);
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
		assert false: "Local match shouldn't have best-of-3 early termination";
		synchronized (queue) {
			while (queue.isEmpty()) {
				try {
					Thread.sleep(250);
				}
				catch (InterruptedException e) {
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
