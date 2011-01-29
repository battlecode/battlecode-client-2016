package battlecode.client.viewer;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;

import battlecode.client.viewer.render.BaseCanvas;
import battlecode.client.viewer.render.BaseRenderer;
import battlecode.client.viewer.render.GameCanvas;
import battlecode.client.viewer.render.GameRenderer;
import battlecode.common.Team;

public class MinimapViewer {

    private JFrame frame;
    private BaseCanvas canvas;
    private BaseRenderer bracket;
    private BaseRenderer renderer;

    public MinimapViewer(boolean fullscreen) {
        frame = new JFrame("Minimap");
        GameCanvas gc = new GameCanvas();
        canvas = gc;
        bracket = gc.bracketRenderer;

        if (fullscreen) {
            frame.setUndecorated(true);
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[1];
            Rectangle secondWindow = gd.getDefaultConfiguration().getBounds();
            frame.setBounds(secondWindow);
        } else {
            frame.setUndecorated(false);
            frame.setBounds(new Rectangle(500, 340));
            frame.setAlwaysOnTop(true);
        }

        frame.getContentPane().add(canvas);
        frame.setVisible(true);
        frame.toFront();
    }

    public void setNull() {
        canvas.setRenderer(null);
    }

    public void setBracket() {
        canvas.setRenderer(bracket);
    }

    public void resetMatches() {
        if (renderer != null)
            renderer.resetMatches();
    }

    public void addWin(Team t) {
        if (renderer != null)
            renderer.addWin(t);
    }

    public void setTimeline(GameStateTimeline master) {
        renderer = new GameRenderer(master.getMatch());
        renderer.getTimeline().setMasterTimeline(master);
        renderer.setDebugState(new DebugState(master.getMatch().getDebugProxy(), canvas));
        canvas.setRenderer(renderer);
    }

    public void repaint() {
        canvas.repaint();
    }
}
