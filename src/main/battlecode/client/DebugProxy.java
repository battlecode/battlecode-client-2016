package battlecode.client;

import battlecode.engine.signal.Signal;
import battlecode.serial.notification.Notification;

public interface DebugProxy {

    void writeNotification(Notification n);

    void writeSignal(Signal s);
}
