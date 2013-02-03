package battlecode.client.viewer;

import battlecode.client.viewer.render.BaseRenderer;
import battlecode.client.viewer.render.GameRenderer;
import battlecode.client.viewer.sound.GameSoundBank;
import battlecode.client.viewer.sound.MetaGameLoop;
import battlecode.common.Team;

public class TournamentTimer {

    private final MatchViewer mv;
    private MetaGameLoop loop;
    private final Controller controller = new Controller.ControlAdapter();
    //private GameRenderer gr;
    //private GLGameRenderer gr;
    private BaseRenderer br;
    private volatile boolean continueCued = false;
	public static volatile boolean waitBetweenMatches; 
	private Runnable spaceBarListener = new Runnable() {

        public void run() {
            continueCued = true;
        }
    };
    Team winner = Team.NEUTRAL;
    int aWins = 0, bWins = 0;
    private Thread thread = new Thread() {

				private long wakeNanoTime;

				public void run() {
            final MinimapViewer minimap = mv.getMinimap();
            while (true) {
                loop = new MetaGameLoop();
								winner = null;

                loop.start();
                
                br = mv.setupViewer();

                GameStateTimeline gst = br.getTimeline();
                gst.getMatch().addMatchListener(new MatchListener() {

										public void headerReceived(BufferedMatch match) {
												if (match.getHeader().getMatchNumber() == 0) {
                            //loop.start();
														aWins = bWins = 0;
                            if (minimap != null) {
                                minimap.resetMatches();
                                minimap.setBracket();
                            }
                        }
                        //else if (minimap != null) { minimap.setNull(); }
                    }

                    public void footerReceived(BufferedMatch match) {
                        winner = match.getFooter().getWinner();
                    }
                });
                continueCued = !waitBetweenMatches;
								
								while (!(gst.getMatch().isFinished() && continueCued)) {
										if (minimap != null) {
												minimap.repaint();
										}
										doSleep(100);
								}

                if (minimap != null) {
                    minimap.setTimeline(gst);
                }
                if (minimap != null) {
                    if (aWins > 0)
                        minimap.addWin(Team.A);
                    if (bWins > 0)
                        minimap.addWin(Team.B);
                } else {
                    if (aWins > 0)
                        br.addWin(Team.A);
                    if (bWins > 0)
                        br.addWin(Team.B);
								}
                br.beginIntroCutScene(loop.stop());
                while (loop.isPlaying()) {
                    doSleep(100);
										mv.getCanvas().repaint();
                }
                doSleep(2000);
                br.setCutSceneVisible(false);
                new MatchPlayer(null, controller, gst, null, false);
                while (gst.getRound() < gst.getNumRounds() || winner == null) {
                    doSleep(100);
                }
								if (minimap == null)
										br.addWin(winner);
                doSleep(1000);
                br.setCutSceneVisible(true);
                br.fadeOutCutScene();
								System.out.println("sleep "+Thread.currentThread());
								for(int i=0;i<60;i++) {
										doSleep(50);
										mv.getCanvas().repaint();
								}
								if (winner == Team.A)
										aWins++;
								else if (winner == Team.B)
										bWins++;
				
								if (minimap != null) {
										minimap.addWin(winner);
								}
								gst.terminate();
            }
        }

        private void doSleep(long millis) {
            if (br != null) {
                wakeNanoTime = System.nanoTime() + 1000000 * millis;
                do {
                    //mv.getCanvas().forceRepaint();
                    try {
                        sleep(10);
                    } catch (InterruptedException e) {
                    }
                } while (System.nanoTime() < wakeNanoTime);
            } else {
                try {
                    sleep(millis);
                } catch (InterruptedException e) {
                }
            }
        }
    };

    public TournamentTimer(MatchViewer mv) {
        this.mv = mv;
        GameRenderer.preloadGraphics();
        GameSoundBank.preload();
        thread.start();
    }

    public Runnable getSpaceBarListener() {
        return spaceBarListener;
    }
}
