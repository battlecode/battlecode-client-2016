package battlecode.client;

import battlecode.engine.signal.Signal;
import battlecode.serial.notification.Notification;
import battlecode.server.Config;
import battlecode.server.serializer.JavaSerializerFactory;
import battlecode.server.serializer.Serializer;
import battlecode.server.serializer.XStreamSerializerFactory;

import java.io.*;

public final class StreamClientProxy implements ClientProxy {

    private final boolean debug;

    private final Serializer inSerializer;
    private final Serializer outSerializer;

    private Object peekBuffer;
    private boolean peeked = false;

    public StreamClientProxy(InputStream stream) throws IOException {
        debug = false;
        if (Config.getGlobalConfig().getBoolean("bc.server.output-xml")) {
            inSerializer = new XStreamSerializerFactory().createSerializer
                    (null, stream);
        } else {
            inSerializer = new JavaSerializerFactory().createSerializer(null,
                    stream);
        }
        outSerializer = null;
    }

    public StreamClientProxy(InputStream is, ObjectOutputStream os) throws
            IOException {
        debug = true;
        // This is necessary because we may have different types of
        // serialization coming in and going out.
        if (Config.getGlobalConfig().getBoolean("bc.server.output-xml")) {
            inSerializer = new XStreamSerializerFactory().createSerializer
                    (null, is);
        } else {
            inSerializer = new JavaSerializerFactory().createSerializer(null,
                    is);
        }
        outSerializer = new JavaSerializerFactory().createSerializer(os, null);
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

            Object o = inSerializer.deserialize();
            //System.out.println("SP " + o);
            return o;
        } catch (EOFException e) {
            throw e;
        } catch (Exception e) {
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
        return debug;
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
            outSerializer.serialize(o);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void finalize() throws Throwable {
        if (outSerializer != null) {
            outSerializer.close();
        }
    }
}
