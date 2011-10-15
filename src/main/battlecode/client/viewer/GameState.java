package battlecode.client.viewer;

import battlecode.serial.RoundDelta;
import battlecode.serial.RoundStats;

import battlecode.engine.signal.Signal;
import battlecode.engine.signal.AutoSignalHandler;

public abstract class GameState extends AutoSignalHandler {

	public synchronized void apply(RoundDelta rd) {
		for (Signal signal: rd.getSignals()) {
			signal.accept(this);
		}
		updateRound();
	}

	public synchronized void apply(Signal signal) {
		signal.accept(this);
	}

	public synchronized void apply(RoundStats stats) {
	}

	protected abstract void updateRound();

	//public abstract AbstractDrawObject getDrawObject(int id);
}
