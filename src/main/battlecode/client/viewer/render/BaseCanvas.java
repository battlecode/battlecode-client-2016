package battlecode.client.viewer.render;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.Observer;

import javax.swing.*;

import battlecode.client.viewer.BufferedMatch;
import battlecode.client.viewer.GameStateTimeline;
import battlecode.engine.signal.Signal;
import battlecode.world.signal.DeathSignal;

public abstract class BaseCanvas extends JPanel {

    protected Runnable spaceBarListener = null;

    public BaseCanvas() {
        super();
        setVisible(false);
        addHierarchyListener(new HierarchyListener() {

            public void hierarchyChanged(HierarchyEvent e) {
                if (isDisplayable()) {
                    initKeyBindings();
                    removeHierarchyListener(this);
                }
            }
        });
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                if (getRenderer() != null) {
                    getRenderer().setCanvasSize(getSize());
                }
            }
        });
        setAlignmentX(CENTER_ALIGNMENT);
        setBackground(Color.BLACK);
        setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
        setPreferredSize(new Dimension(800, 600));
        setOpaque(true);

    }

    protected void initKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke("ESCAPE"), "exit");
        for (char c = 'A'; c <= 'Z'; c++) {
            im.put(KeyStroke.getKeyStroke(String.valueOf(c)), "toggle");
        }
        if (RenderConfiguration.isTournamentMode()) {
            im.put(KeyStroke.getKeyStroke("SPACE"), "space");
        }
        im.put(KeyStroke.getKeyStroke("SLASH"), "find");
		im.put(KeyStroke.getKeyStroke("shift PERIOD"), "searcharchon");
		im.put(KeyStroke.getKeyStroke("shift COMMA"), "searcharchon");
        getActionMap().put("toggle", new AbstractAction() {

            private static final long serialVersionUID = 0; // don't serialize

            public void actionPerformed(ActionEvent e) {
                if (getRenderer() != null) {
                    getRenderer().handleAction(e.getActionCommand().charAt(0));
                    repaint();
                }
            }
        });
        getActionMap().put("space", new AbstractAction() {

            private static final long serialVersionUID = 0; // don't serialize

            public void actionPerformed(ActionEvent e) {
                if (spaceBarListener != null) {
                    spaceBarListener.run();
                }
            }
        });
        getActionMap().put("exit", new AbstractAction() {

            private static final long serialVersionUID = 0; // don't serialize

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        getActionMap().put("find", new AbstractAction() {

            private static final long serialVersionUID = 0; // don't serialize

            public void actionPerformed(ActionEvent e) {
                if (getRenderer() != null) {
                    String strID = JOptionPane.showInputDialog("Find robot by ID:");
                    int robotID;
                    try {
                        robotID = Integer.parseInt(strID);
                    } catch (NumberFormatException ex) {
                        return;
                    }
                    getRenderer().getDebugState().setFocusAndUpdate(robotID);
                }
            }
        });
		getActionMap().put("searcharchon", new AbstractAction() {

			private static final long serialVersionUID = 0;

			public void actionPerformed(ActionEvent e) {
				BaseRenderer renderer = getRenderer();
				if (renderer != null) {
					int dt = "<".equals(e.getActionCommand()) ? -1 : 1;
					GameStateTimeline timeline = renderer.getTimeline();
					BufferedMatch match = timeline.getMatch();
					int t;
					for(t=timeline.getRound()+dt;t>0&&t<=timeline.getNumRounds();t+=dt) {
						for(Signal s : match.getRound(t-1).getSignals()) {
							if((s instanceof DeathSignal)) {
								int id = ((DeathSignal)s).getObjectID();
								if(id<=100) {
									timeline.setRound(t);
									renderer.getDebugState().setFocusAndUpdate(id);
									return;
								}
							}
						}
					}
				}
			}

		});
    }

    protected abstract BaseRenderer getRenderer();

    public void setSpaceBarListener(Runnable runnable) {
        spaceBarListener = runnable;
    }

    public abstract void setRenderer(BaseRenderer renderer);

    public abstract void setTournamentMode();

    public abstract void addPaintObserver(Observer o);
}
