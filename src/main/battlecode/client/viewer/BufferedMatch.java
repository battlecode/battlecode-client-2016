package battlecode.client.viewer;

import battlecode.client.ClientProxy;
import battlecode.world.DominationFactor;
import battlecode.world.signal.Signal;
import battlecode.serial.*;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class BufferedMatch {

    private ClientProxy proxy;

    private MatchHeader header = null;
    private MatchFooter footer = null;

    public String teamA = null;
    public String teamB = null;
    private String[] mapNames = null;

    private final List<RoundDelta> deltas = new ArrayList<>();
    private DominationFactor dominationFactor = null;
    private List<Signal> currentBreak = null;
    private boolean paused = false;

    private List<MatchListener> matchListeners;
    private List<MatchListener> matchPausedListeners;

    private boolean earlyTermination = false;

    public BufferedMatch(ClientProxy proxy) {
        assert proxy != null;
        this.proxy = proxy;

        matchListeners = new LinkedList<>();
        matchPausedListeners = new LinkedList<>();

        new Thread(this::readMatch).start();
    }

    public MatchHeader getHeader() {
        return header;
    }

    public int getRoundsAvailable() {
        return deltas.size();
    }

    public RoundDelta getRound(int round) {
        if (round < deltas.size())
            return deltas.get(round);
        return null;
    }

    public List<Signal> getDebugSignals(int round) {
        if (deltas != null) {
            synchronized (deltas) {
                if (round == deltas.size()) return currentBreak;
            }
        }
        return null;
    }

    public MatchFooter getFooter() {
        return footer;
    }

    public DominationFactor getDominationFactor() {
        return dominationFactor;
    }

    private void readMatch() {
        ServerEvent event;
        do {
            try {
                event = proxy.readEvent();
            } catch (EOFException e) {
                //System.err.println("Unexpected end of line at match header");
                return;
            }
        } while (!(event instanceof MatchHeader));
        synchronized (this) {
            header = (MatchHeader) event;
            for (MatchListener listener : matchListeners) {
                listener.headerReceived(this);
            }
        }
        deltas.clear();
        while (true) {
            try {
                event = proxy.readEvent();
            } catch (EOFException e) {
                System.err.println("Unexpected end of line at round " +
                        deltas.size());
                return;
            }

            if (event instanceof RoundDelta) {
                handleRoundDelta((RoundDelta) event);
            } else if (event instanceof InjectDelta) {
                handleInjectDelta((InjectDelta) event);
            } else if (event instanceof ExtensibleMetadata) {
                handleExtensibleMetadata((ExtensibleMetadata) event);
            } else if (event instanceof GameStats) {
                handleGameStats((GameStats) event);
            } else if (event instanceof PauseEvent) {
                handlePauseEvent();
            } else if (event instanceof MatchFooter) {
                handleMatchFooter((MatchFooter) event);
                break;
            } else {
                throw new RuntimeException("Unhandled object read: "+event);
            }
        }
        if (!proxy.isDebuggingAvailable()) {
            try {
                proxy.peekEvent();
            } catch (EOFException e) {
                earlyTermination = true;
            }
        }
        synchronized (this) {
            footer = (MatchFooter) event;
            for (MatchListener listener : matchListeners) {
                listener.footerReceived(this);
            }
            matchPausedListeners = null;
        }
        matchListeners = null;
        System.out.println("Stop buffering match");
    }

    private void handlePauseEvent() {
        paused = true;
        synchronized (matchPausedListeners) {
            for (MatchListener listener : matchPausedListeners) {
                listener.breakReceived(this);
            }
        }
    }

    private void handleRoundDelta(RoundDelta roundDelta) {
        assert roundDelta != null : "Null delta at round " + deltas.size();
        paused = false;
        if (currentBreak == null) {
            deltas.add(roundDelta);
        } else {
            Signal[] signals = roundDelta.getSignals();
            Signal[] merged = new Signal[currentBreak.size() + signals.length];
            for (int i = 0; i < currentBreak.size(); i++) {
                merged[i] = currentBreak.get(i);
            }
            for (int i = 0; i < signals.length; i++) {
                merged[currentBreak.size() + i] = signals[i];
            }
            synchronized (deltas) {
                currentBreak = null;
                deltas.add(new RoundDelta(merged));
            }
        }
    }

    // TODO: we could be doing more with these.
    // For the final competition, we should show this information on the screen.
    // For now, this will just print to console the results of the game.
    private void handleGameStats(GameStats gameStats) {
        DominationFactor dom = gameStats.getDominationFactor();
        String s = "";
        if (dom == DominationFactor.DESTROYED)
            s = "The winning team won by TODO something here.";
        else if (dom == DominationFactor.PWNED)
            s = "The winning team won on TODO something here.";
        else if (dom == DominationFactor.OWNED)
            s = "The winning team won on TODO something here.";
        else if (dom == DominationFactor.BARELY_BEAT)
            s = "The winning team won on TODO something here.";
        else if (dom == DominationFactor.WON_BY_DUBIOUS_REASONS)
            s = "The winning team won arbitrarily.";
        System.out.println(s);

        dominationFactor = dom;
    }

    private void handleInjectDelta(InjectDelta delta) {
        final Signal[] signals = delta.getSignals();

        if (currentBreak == null) {
            currentBreak = new ArrayList<>();
        }
        Collections.addAll(currentBreak, signals);
    }

    private void handleExtensibleMetadata(ExtensibleMetadata metadata) {

        if ("header".equals(metadata.get("type", null))) {
            teamA = (String) metadata.get("team-a", null);
            teamB = (String) metadata.get("team-b", null);
            mapNames = (String[]) metadata.get("maps", null);
        }
        System.out.println("metadata: " + teamA + " " + teamB);
    }

    private void handleMatchFooter(MatchFooter matchFooter) {
        System.out.println("Team " + matchFooter.getWinner() + " wins!");
    }

    public String getTeamA() {
        return teamA;
    }

    public String getTeamB() {
        return teamB;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isFinished() {
        return (footer != null);
    }

    public synchronized void addMatchListener(MatchListener listener) {
        if (header != null) {
            listener.headerReceived(this);
            if (footer != null) {
                listener.footerReceived(this);
                return;
            }
        }
        matchListeners.add(listener);
    }

    public synchronized void addMatchPausedListener(MatchListener listener) {
        if (matchPausedListeners != null) {
            synchronized (matchPausedListeners) {
                matchPausedListeners.add(listener);
            }
        }
    }

    public ClientProxy getDebugProxy() {
        if (proxy.isDebuggingAvailable()) {
            return proxy;
        } else {
            return null;
        }
    }

    public boolean isEarlyTermination() {
        return earlyTermination;
    }
}
