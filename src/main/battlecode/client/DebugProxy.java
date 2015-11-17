package battlecode.client;

import battlecode.serial.notification.Notification;
import battlecode.engine.signal.Signal;

public interface DebugProxy {

	void writeNotification(Notification n);

	void writeSignal(Signal s);
}
