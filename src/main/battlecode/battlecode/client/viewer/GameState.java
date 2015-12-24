package battlecode.client.viewer;

import battlecode.world.signal.AutoSignalHandler;
import battlecode.world.signal.Signal;
import battlecode.serial.RoundDelta;

public abstract class GameState extends AutoSignalHandler {

    public synchronized void apply(RoundDelta rd) {
        preUpdateRound();
        for (Signal signal : rd.getSignals()) {
            visitSignal(signal);
        }
        postUpdateRound();
    }

    public synchronized void apply(Signal signal) {
        visitSignal(signal);
    }

    protected abstract void preUpdateRound();

    protected abstract void postUpdateRound();
}
