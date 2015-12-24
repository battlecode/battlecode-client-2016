package battlecode.client;

import battlecode.serial.ServerEvent;
import battlecode.serial.notification.Notification;
import battlecode.server.Config;
import battlecode.server.serializer.JavaSerializerFactory;
import battlecode.server.serializer.Serializer;
import battlecode.server.serializer.XStreamSerializerFactory;

import java.io.*;
import java.util.zip.GZIPInputStream;

public final class StreamClientProxy implements ClientProxy {

    private final boolean debug;

    private final Serializer serializer;

    private ServerEvent peekBuffer;
    private boolean peeked = false;

    public StreamClientProxy(InputStream stream) throws IOException {
        debug = false;
        if (Config.getGlobalConfig().getBoolean("bc.server.output-xml")) {
            serializer = new XStreamSerializerFactory().createSerializer
                    (null, stream, ServerEvent.class);
        } else {
            serializer = new JavaSerializerFactory().createSerializer(null,
                    stream, ServerEvent.class);
        }
    }

    public StreamClientProxy(String path) throws IOException {
        this(new GZIPInputStream(new FileInputStream(path)));
    }

    public ServerEvent readEvent() throws EOFException {
        if (peeked) {
            peeked = false;
            //System.out.println("SP " + peekBuffer);
            return peekBuffer;
        }
        try {
            return (ServerEvent) serializer.deserialize();
        } catch (EOFException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Deserialization failed");
        }
    }

    public ServerEvent peekEvent() throws EOFException {
        if (!peeked) {
            peekBuffer = readEvent();
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

    private void writeObject(Object o) {
        throw new RuntimeException("Can't write to StreamClientProxy");
    }

    protected void finalize() throws Throwable {
        serializer.close();
    }
}
