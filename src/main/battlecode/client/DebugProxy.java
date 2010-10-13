package battlecode.client;

import battlecode.serial.notification.Notification;
import battlecode.engine.signal.Signal;

public interface DebugProxy {

	public void writeNotification(Notification n);

	public void writeSignal(Signal s);
}
