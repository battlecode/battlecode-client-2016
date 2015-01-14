package battlecode.client.viewer;

import battlecode.serial.RoundDelta;
import battlecode.serial.RoundStats;

import battlecode.engine.signal.Signal;
import battlecode.engine.signal.AutoSignalHandler;

public abstract class GameState extends AutoSignalHandler {

	public synchronized void apply(RoundDelta rd) {
	    preUpdateRound();
		for (Signal signal: rd.getSignals()) {
			signal.accept(this);
		}
		postUpdateRound();
	}

	public synchronized void apply(Signal signal) {
		signal.accept(this);
	}

	public synchronized void apply(RoundStats stats) {
	}

    protected abstract void preUpdateRound();
	protected abstract void postUpdateRound();

	//public abstract AbstractDrawObject getDrawObject(int id);
}
