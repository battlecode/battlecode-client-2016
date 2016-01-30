package battlecode.client.viewer;

import battlecode.client.ClientProxy;
import battlecode.serial.MatchHeader;
import battlecode.serial.notification.PauseNotification;
import battlecode.serial.notification.ResumeNotification;
import battlecode.serial.notification.RunNotification;
import battlecode.server.Config;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

public class MatchPlayer implements Observer, ActionListener {

    private final MatchListener ml = new MatchListener() {

        @Override
        public void headerReceived(BufferedMatch match) {
            battlecode.serial.MatchHeader h = match.getHeader();
            label = "Game " + (h.getMatchNumber() + 1) + " of " + h
                    .getMatchCount() + ": ";
        }

        @Override
        public void footerReceived(BufferedMatch m) {
            label += "Team " + m.getFooter().getWinner() + " Wins! ";
            MatchHeader h = m.getHeader();
            if (h.getMatchNumber() + 1 < h.getMatchCount()) {
                if (m.isEarlyTermination()) {
                    label = "[Best of " + m.getHeader().getMatchCount() + "] " +
                            "" + label;
                } else {
                    controller.enableNext();
                }
            }
        }

        @Override
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
    private ClientProxy proxy;
    private final GameStateTimeline timeline;
    private final BufferedMatch match;
    private final javax.swing.Timer timer;
    private String label = "";
    // value that determines the delay in ticks between timer ticks
    private volatile static MatchPlayer currentPlayer = null;
    public static final int DEFAULT_TIME_DELTA = Config.getGlobalConfig()
            .getInt("bc.client.viewer-delay");
    public static final int NUM_OF_SPEEDS = 10;
    private final int delta = DEFAULT_TIME_DELTA;
    private int fastForward = 1;

    public MatchPlayer(MatchViewer v, Controller c, GameStateTimeline gst,
                       ClientProxy dp, boolean lockstepChoice) {
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
        } else {
            doStep(Integer.MAX_VALUE);
            runSpeed = 1;
        }

        timer = new javax.swing.Timer(delta, e -> timerTick());
        timer.start();

        // set as the current latest player
        currentPlayer = this;
    }

    public void slowdown() {
        if (fastForward < NUM_OF_SPEEDS-1)
            fastForward += 1;
        timer.setDelay((int) (fastForward * delta / (float) NUM_OF_SPEEDS));
    }

    public void speedup() {
        if (fastForward > 0)
            fastForward -= 1;
        timer.setDelay((int) (fastForward * delta / (float) NUM_OF_SPEEDS));
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

    private void requestRounds() {
        if (timeline.isActive() && !match.isFinished()) {
            if (timeline.getRound() + 2 > roundsRequested) {
                roundsRequested = timeline.getRound() + 2;
                if (proxy != null) {
                    proxy.writeNotification(new RunNotification
                            (roundsRequested));
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

    public GameStateTimeline getTimeline() {
        return timeline;
    }

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
        } else {
            timeline.refreshRound();
        }
    }

    public void togglePause() {
        if (isPlaying)
            doPause();
        else
            doPlay();
    }

    private void doPause() {
        isPlaying = false;
        if (timer != null) timer.stop();
        controller.setPlayEnabled(true);
    }

    private void doPlay() {
        doPlay(match.isFinished() ? match.getRoundsAvailable() : Integer
                .MAX_VALUE);
    }

    private void doPlay(int round) {
        maxPlayRound = round;
        isPlaying = true;
        if (timer != null) timer.start();
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
        } else if ("play".equals(e.getActionCommand())) {
            if (isPlaying)
                return;
            resumeServer();
            requestRounds();
            doPlay();
        } else if ("pause".equals(e.getActionCommand())) {
            if (!isPlaying)
                return;
            if (!isStepping) {
                pauseServer();
            }
            doPause();
        } else if ("end".equals(e.getActionCommand())) {
            timeline.setRound(timeline.getNumRounds());
        } else if ("next".equals(e.getActionCommand())) {
            timer.stop();
            timeline.terminate();
        } else {
            if ("back".equals(e.getActionCommand())) {
                if (isStepping) {
                    pauseServer();
                }
                timeline.setRound(timeline.getRound() - controller
                        .getStepSize());
            } else if ("step".equals(e.getActionCommand())) {
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
                javax.swing.SwingUtilities.invokeLater(viewer::setupViewer);
            }
        }
    }
}
