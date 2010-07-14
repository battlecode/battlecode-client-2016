package battlecode.client.viewer;

import battlecode.client.DebugProxy;

import battlecode.serial.*;
import battlecode.serial.notification.*;

import info.clearthought.layout.TableLayout;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import battlecode.client.util.OBJFile;


public class MatchPlayer implements Observer, ActionListener {

	private MatchListener ml = new MatchListener() {
		public void headerReceived(BufferedMatch match) {
			battlecode.serial.MatchHeader h = match.getHeader();
			label = "Game "+(h.getMatchNumber() + 1)+" of "+h.getMatchCount()+": ";
		}
		public void footerReceived(BufferedMatch m) {
			label += "Team " + m.getFooter().getWinner() + " Wins! ";
			MatchHeader h = m.getHeader();
			if (h.getMatchNumber() + 1 < h.getMatchCount()) {
				if (m.isEarlyTermination()) {
					label = "[Best of " + m.getHeader().getMatchCount() + "] " + label;
				}
				else { controller.enableNext(); }
			}
		}
		public void breakReceived(BufferedMatch m) {
			breakRound = m.getRoundsAvailable();
			roundsRequested = breakRound;
			maxPlayRound = breakRound;
		}
	};

	private int maxPlayRound = 0;
	private int roundsRequested = 0;

	private int breakRound = -1; // round at which a break occurred

	private boolean isPlaying = true;
	private boolean isStepping = false;
	private int runSpeed = 1;

	private final MatchViewer viewer;
	private final Controller controller;
	private DebugProxy proxy;

	private GameStateTimeline timeline;
	private BufferedMatch match;
	private javax.swing.Timer timer;

	private String label = "";
	private int stepSize = 1;
	
	// value that determines the delay in ticks between timer ticks
	private volatile static MatchPlayer currentPlayer = null;
	public static final int DEFAULT_TIME_DELTA = 15000000;
	private int timeDeltaMax = DEFAULT_TIME_DELTA;

	public MatchPlayer(MatchViewer v, Controller c, GameStateTimeline gst,
	                   DebugProxy dp, boolean lockstepChoice) {
//System.out.println("Start");
  //  	OBJFile.convertToBCMs();
		viewer = v;
		timeline = gst;
		timeline.addObserver(this);
		controller = c;
		controller.setPlayer(this);
		proxy = dp;
		match = gst.getMatch();
		match.addMatchListener(ml);
		match.addMatchPausedListener(ml);

		if (lockstepChoice) {
			roundsRequested = 2;
			if (proxy != null) {
				proxy.writeNotification(new RunNotification(roundsRequested));
			}
			doPlay();
		}
		else {
			doStep(Integer.MAX_VALUE);
			runSpeed = 1;
		}
		timer = new javax.swing.Timer(DEFAULT_TIME_DELTA/1000000, new ActionListener() {
			public void actionPerformed(ActionEvent e) { timerTick(); }
		});
		timer.start();
		
		// set as the current latest player
		currentPlayer = this;
	}

	// get the latest match player
	public static MatchPlayer getCurrent() {
		return currentPlayer;
	}
	private void pauseServer() {
		if (proxy != null) {
			proxy.writeNotification(PauseNotification.INSTANCE);
		}
	}

	private void resumeServer() {
		if (proxy != null && breakRound != -1) {
			breakRound = -1;
			proxy.writeNotification(ResumeNotification.INSTANCE);
		}
	}

	// set the number of ticks before a round switches
	public void setTimeDelta(int max) {
		timer.setDelay(max/1000000);
		//timeDeltaMax = max;
	}
	
	private void requestRounds() {
		if (timeline.isActive() && !match.isFinished()) {
			if (timeline.getRound() + 2 > roundsRequested) {
				roundsRequested = timeline.getRound() + 2;
				if (proxy != null) {
					proxy.writeNotification(new RunNotification(roundsRequested));
				}
			}
		}
	}

	private void runServerUntil(int round) {
		if (round > roundsRequested) {
			if (proxy != null) {
				proxy.writeNotification(new RunNotification(round));
			}
			roundsRequested = round;
		}
	}

	public GameStateTimeline getTimeline() { return timeline; }

	private void timerTick() {
		controller.updateRoundLabel(timeline);
		if (isPlaying) {
			if (timeline.getRound() == maxPlayRound) {
				isStepping = false;
				doPause();
				return;
			}
			if (isStepping) {
				if (match.isFinished()) {
					isStepping = false;
					if (maxPlayRound > match.getRoundsAvailable()) {
						maxPlayRound = match.getRoundsAvailable();
					}
					proxy = null;
				}
			}
			timeline.setRound(timeline.getRound() + runSpeed);
			if (breakRound == -1 && !isStepping) {
				requestRounds();
			}
		}
		else {
			timeline.refreshRound();
		}
	}

	private void doPause() {
		isPlaying = false;
		controller.setPlayEnabled(true);
	}

	private void doPlay() {
		doPlay(match.isFinished() ? match.getRoundsAvailable() : Integer.MAX_VALUE);
	}

	private void doPlay(int round) {
		maxPlayRound = round;
		isPlaying = true;
		controller.setPlayEnabled(false);
		runSpeed = 1;
	}

	private void doStep(int stepRounds) {
		isStepping = true;
		resumeServer();
		doPlay(timeline.getRound() + stepRounds);
		runServerUntil(maxPlayRound);
		runSpeed = stepRounds;
	}

	public void actionPerformed(ActionEvent e) {
		if ("start".equals(e.getActionCommand())) {
			timeline.setRound(0);
		}
		else if ("play".equals(e.getActionCommand())) {
			if (isPlaying) return;
			resumeServer();
			requestRounds();
			doPlay();
		}
		else if ("pause".equals(e.getActionCommand())) {
			if (!isPlaying) return;
			if (!isStepping) {
				pauseServer();
			}
			doPause();
		}
		else if ("end".equals(e.getActionCommand())) {
			timeline.setRound(timeline.getNumRounds());
		}
		else if ("next".equals(e.getActionCommand())) {
			timer.stop();
			timeline.terminate();
		}
		else {
			if ("back".equals(e.getActionCommand())) {
				if (isStepping) {
					pauseServer();
				}
				timeline.setRound(timeline.getRound() - controller.getStepSize());
			}
			else if ("step".equals(e.getActionCommand())) {
				doStep(controller.getStepSize());
			}
		}
	}

	public void update(Observable o, Object arg) {
		if (!timeline.isActive()) {
			if (timer != null) {
				timer.stop();
			}
			if (viewer != null) {
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					public void run() { viewer.setupViewer(); }
				});
			}
		}
	}
}
