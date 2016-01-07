package battlecode.client.viewer.render;

import battlecode.client.viewer.GameStateTimeline;
import battlecode.client.viewer.MatchPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Observer;


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
        im.put(KeyStroke.getKeyStroke("shift SEMICOLON"), "jump");
        im.put(KeyStroke.getKeyStroke("OPEN_BRACKET"), "jump");
        im.put(KeyStroke.getKeyStroke("shift OPEN_BRACKET"), "jump");
        im.put(KeyStroke.getKeyStroke("CLOSE_BRACKET"), "jump");
        im.put(KeyStroke.getKeyStroke("shift CLOSE_BRACKET"), "jump");
        im.put(KeyStroke.getKeyStroke("shift PERIOD"), "pause");
        im.put(KeyStroke.getKeyStroke("shift COMMA"), "pause");
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
                    String strID = JOptionPane.showInputDialog("Find robot by" +
                            " ID:");
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
        getActionMap().put("jump", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                GameRenderer renderer = getRenderer();
                if (renderer != null) {
                    GameStateTimeline timeline = renderer.getTimeline();
                    String ac = e.getActionCommand();
                    if (":".equals(ac)) {
                        String strRound = JOptionPane.showInputDialog("Jump " +
                                "to round:");
                        int round;
                        try {
                            round = Integer.parseInt(strRound);
                        } catch (NumberFormatException ex) {
                            return;
                        }
                        timeline.setRound(round);
                    } else if ("[".equals(ac)) {
                        timeline.setRound(timeline.getRound() - 1);
                    } else if ("{".equals(ac)) {
                        timeline.setRound(timeline.getRound() - 20);
                    } else if ("]".equals(ac)) {
                        timeline.setRound(timeline.getRound() + 1);
                    } else if ("}".equals(ac)) {
                        timeline.setRound(timeline.getRound() + 20);
                    }
                }
            }
        });
        getActionMap().put("pause", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                MatchPlayer.getCurrent().togglePause();
            }

        });
    }

    protected abstract GameRenderer getRenderer();

    public void setSpaceBarListener(Runnable runnable) {
        spaceBarListener = runnable;
    }

    public abstract void setGraphicsDevice(GraphicsDevice gd);

    public abstract void setRenderer(GameRenderer renderer);

    public abstract void setTournamentMode();

    public abstract void addPaintObserver(Observer o);
}
