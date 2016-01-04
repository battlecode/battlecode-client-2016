package battlecode.client.viewer;

import battlecode.world.signal.AutoSignalHandler;
import battlecode.world.signal.InternalSignal;
import battlecode.serial.RoundDelta;

public abstract class GameState extends AutoSignalHandler {

    public synchronized void apply(RoundDelta rd) {
        preUpdateRound();
        for (InternalSignal internalSignal : rd.getInternalSignals()) {
            visitSignal(internalSignal);
        }
        postUpdateRound();
    }

    public synchronized void apply(InternalSignal internalSignal) {
        visitSignal(internalSignal);
    }

    protected abstract void preUpdateRound();

    protected abstract void postUpdateRound();
}
