package battlecode.client;

import battlecode.serial.ServerEvent;
import battlecode.serial.notification.Notification;

import java.io.EOFException;

/**
 * The "reading end" of a battlecode.server.proxy.Proxy.
 */
public interface ClientProxy {

    ServerEvent readEvent() throws EOFException;

    ServerEvent peekEvent() throws EOFException;

    boolean isDebuggingAvailable();

    void writeNotification(Notification n);
}
