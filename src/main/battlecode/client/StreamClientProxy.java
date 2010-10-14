package battlecode.client;

import battlecode.serial.notification.Notification;

import battlecode.engine.signal.Signal;

import java.io.*;
import battlecode.server.Config;
import battlecode.server.proxy.XStreamProxy;

public final class StreamClientProxy implements ClientProxy {

	private ObjectInputStream  ois;
	private ObjectOutputStream oos = null;

	private Object peekBuffer;
	private boolean peeked = false;

	public StreamClientProxy(InputStream stream) throws IOException {
		if(Config.getGlobalConfig().getBoolean("bc.server.output-xml")) {
			ois = XStreamProxy.getXStream().createObjectInputStream(stream);	
		}
		else {
			ois = new ObjectInputStream(stream);
		}
	}

	public StreamClientProxy(InputStream is, ObjectOutputStream os) throws IOException {
		this(is);
		oos = os;
	}

	public StreamClientProxy(String path) throws IOException {
		this(new java.util.zip.GZIPInputStream(new FileInputStream(path)));
	}

	public Object readObject() throws EOFException {
		if (peeked) {
			peeked = false;
			//System.out.println("SP " + peekBuffer);
			return peekBuffer;
		}
		try {

			Object o = ois.readObject();
			//System.out.println("SP " + o);
                        return o;
		}
		catch (EOFException e) {
			throw e;
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Deserialization failed");
		}
	}

	public Object peekObject() throws EOFException {
		if (!peeked) {
			peekBuffer = readObject();
			peeked = true;
		}
		return peekBuffer;
	}

	public boolean isDebuggingAvailable() {
		return (oos != null);
	}

	public void writeNotification(Notification n) {
		writeObject(n);
	}

	public void writeSignal(Signal s) {
		writeObject(s);
	}

	private void writeObject(Object o) {
		assert isDebuggingAvailable();
		try {
			oos.writeObject(o);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void finalize() throws Throwable {
		if (ois != null) ois.close();
	}
}
