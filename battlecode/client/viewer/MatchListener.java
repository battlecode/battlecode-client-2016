package battlecode.client.viewer;

import battlecode.serial.*;

public abstract class MatchListener {

	public void headerReceived(BufferedMatch match) {}

	public void breakReceived(BufferedMatch match) {}

	public void footerReceived(BufferedMatch match) {}
}
