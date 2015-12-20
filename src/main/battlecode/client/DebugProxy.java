package battlecode.client;

import battlecode.world.signal.Signal;
import battlecode.serial.notification.Notification;

public interface DebugProxy {

    void writeNotification(Notification n);

    void writeSignal(Signal s);
}
