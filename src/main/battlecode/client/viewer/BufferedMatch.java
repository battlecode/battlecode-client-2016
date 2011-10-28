package battlecode.client.viewer;

import java.io.EOFException;
import java.util.*;

import battlecode.client.*;
import battlecode.serial.*;
import battlecode.serial.notification.*;
import battlecode.engine.signal.Signal;
import battlecode.world.GameMap;

public final class BufferedMatch {

	

	private ClientProxy proxy;

	private MatchHeader header = null;
	private MatchFooter footer = null;

	public String teamA = null;
	public String teamB = null;
	private String[] mapNames = null;

	private List<RoundDelta> deltas;
	private List<RoundStats> stats;
	private int roundsAvailable = 0;
	private List<Signal> currentBreak = null;
	private boolean paused = false;

	private List<MatchListener> matchListeners;
	private List<MatchListener> matchPausedListeners;

	private boolean earlyTermination = false;

	public BufferedMatch(ClientProxy proxy) {
		assert proxy != null;
		this.proxy = proxy;

		matchListeners = new LinkedList<MatchListener>();
		matchPausedListeners = new LinkedList<MatchListener>();

		(new Thread() { public void run() { readMatch(); } }).start();
	}

	public MatchHeader getHeader() {
		return header;
	}

	public int getRoundsAvailable() {
		return roundsAvailable;
	}
  
	public RoundDelta getRound(int round) {
		if (deltas != null && round < roundsAvailable) {
			return deltas.get(round);
		}
		return null;
	}

	public RoundStats getRoundStats(int round) {
		return stats.get(round);
	}

	public List<Signal> getDebugSignals(int round) {
		if (deltas != null) {
			synchronized(deltas) {
				if (round == roundsAvailable) return currentBreak;
			}
		}
		return null;
	}

	public MatchFooter getFooter() {
		return footer;
	}

	private void readMatch() {
		Object obj;
		do {
			try {
				obj = proxy.readObject();
			}
			catch (EOFException e) {
				//System.err.println("Unexpected end of line at match header");
				return;
			}
		} while (!(obj instanceof MatchHeader));
		synchronized(this) {
			header = (MatchHeader) obj;
			for (MatchListener listener: matchListeners) {
				listener.headerReceived(this);
			}
		}
		deltas = new Vector<RoundDelta>();
		stats  = new Vector<RoundStats>();
		while (true) {
			try {
				obj = proxy.readObject();
			}
			catch (EOFException e) {
				System.err.println("Unexpected end of line at round " + roundsAvailable);
				return;
			}
			if (obj instanceof Notification) {
				handleNotification((Notification) obj);
			}
			else if (obj instanceof RoundDelta) {
				handleRoundDelta((RoundDelta) obj);
			}
			else if (obj instanceof RoundStats) {
				handleRoundStats((RoundStats) obj);
			}
			else if (obj instanceof Signal[]) {
				handleSignals((Signal[]) obj);
			}
			else if (obj instanceof ExtensibleMetadata) {
				handleExtensibleMetadata((ExtensibleMetadata) obj);
			}
			else if (obj instanceof MatchFooter) {
				break;
			}
		}
		if (!proxy.isDebuggingAvailable()) {
			try {
				proxy.peekObject();
			}
			catch (EOFException e) {
				earlyTermination = true;
			}
		}
		synchronized(this) {
			footer = (MatchFooter) obj;
			for (MatchListener listener: matchListeners) {
				listener.footerReceived(this);
			}
			matchPausedListeners = null;
		}
		matchListeners = null;
		System.out.println("Stop buffering match");
	}

	private void handleNotification(Notification n) {
		assert (n instanceof PauseNotification);
		paused = true;
		synchronized(matchPausedListeners) {
			for (MatchListener listener: matchPausedListeners) {
				listener.breakReceived(this);
			}
		}
	}

	private void handleRoundDelta(RoundDelta roundDelta) {
		assert roundDelta != null: "Null delta at round " + roundsAvailable;
		paused = false;
		if (currentBreak == null) {
			deltas.add(roundDelta);
			roundsAvailable++;
		}
		else {
			Signal[] signals = roundDelta.getSignals();
			Signal[] merged = new Signal[currentBreak.size() + signals.length];
			for (int i = 0; i < currentBreak.size(); i++) {
				merged[i] = currentBreak.get(i);
			}
			for (int i = 0; i < signals.length; i++) {
				merged[currentBreak.size() + i] = signals[i];
			}
			synchronized(deltas) {
				currentBreak = null;
				deltas.add(new RoundDelta(merged));
				roundsAvailable++;
			}
		}
	}

	private void handleRoundStats(RoundStats roundStats) {
		stats.add(roundStats);
	}

	private void handleSignals(Signal[] signals) {
		if (currentBreak == null) {
			currentBreak = new ArrayList<Signal>();
		}
		for (int i = 0; i < signals.length; i++) {
			currentBreak.add(signals[i]);
		}
	}

	private void handleExtensibleMetadata(ExtensibleMetadata metadata) {
		
		if ("header".equals(metadata.get("type", null))) {
			teamA = (String) metadata.get("team-a", null);
			teamB = (String) metadata.get("team-b", null);
			mapNames = (String[]) metadata.get("maps", null);
		}
		System.out.println("metadata: " + teamA + " " + teamB);
	}

	public String getTeamA() { return teamA; }
	public String getTeamB() { return teamB; }
	public String[] getMapNames() { return mapNames; }

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
			synchronized(matchPausedListeners) {
				matchPausedListeners.add(listener);
			}
		}
	}

	public DebugProxy getDebugProxy() {
		if (proxy.isDebuggingAvailable()) {
			return proxy;
		}
		else {
			return null;
		}
	}

	public boolean isEarlyTermination() { return earlyTermination; }
}
