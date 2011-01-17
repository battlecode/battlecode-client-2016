package battlecode.client.viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.event.HierarchyEvent;

import java.util.Observable;
import java.util.Observer;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import battlecode.client.ClientProxy;
import battlecode.client.viewer.render.BaseCanvas;
import battlecode.client.viewer.render.BaseRenderer;
import battlecode.client.viewer.render.GameCanvas;
import battlecode.client.viewer.render.GameRenderer;
import battlecode.client.viewer.renderer3d.GLGameCanvas;
import battlecode.client.viewer.renderer3d.GLGameRenderer;
import battlecode.client.viewer.sound.AudioPlayer;
import battlecode.serial.notification.StartNotification;
import battlecode.server.Config;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.HierarchyBoundsListener;

public class MatchViewer {

    private final ClientProxy proxy;
    private final JPanel panel = new JPanel();
    private final Controller controller;
    private boolean lockstepChoice = false;
    private boolean tournamentMode = false;
    //private final GameCanvas gc = new GameCanvas();
    //private final GLGameCanvas gc = new GLGameCanvas();
    private final BaseCanvas bc; //= new GameCanvas();
    // configuration options
    private Config cfg = Config.getGlobalConfig();
    //private GameRenderer gr;
    //private GLGameRenderer gr;
    private BaseRenderer br;
    private final AudioPlayer audio = new AudioPlayer();
    private InfoPanel info;
    private MinimapViewer minimap = null;
    private DebugState dbg;

    public MatchViewer() {

        proxy = null;
        controller = null;
        bc = null;
    }

    public MatchViewer(ClientProxy proxy, boolean lockstepChoice) {
        if (cfg.getBoolean("bc.client.opengl"))
            bc = new GLGameCanvas();
        else
            bc = new GameCanvas();
        System.out.println("Matchviewer 2");
        this.proxy = proxy;
        this.lockstepChoice = lockstepChoice;
        final ControlPanel cpanel = new ControlPanel();
        info = cpanel.getInfoPanel();
        //panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        GridBagLayout l = new GridBagLayout();
        panel.setLayout(l);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(cpanel,gbc);

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;
        panel.add(new JPanel());
        
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 3;
        gbc.weighty = 1;
        panel.add(bc, gbc);



        cpanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        controller = cpanel;

        if (cfg.getBoolean("bc.client.opengl") && cfg.getBoolean("bc.client.minimap")) {
            minimap = new MinimapViewer(false);
        }

        setupViewer();

    }

    public MatchViewer(ClientProxy proxy) {
        if (cfg.getBoolean("bc.client.opengl"))
            bc = new GLGameCanvas();
        else
            bc = new GameCanvas();

        this.proxy = proxy;
        tournamentMode = true;
        controller = new Controller.ControlAdapter();
        panel.setLayout(new BorderLayout());
        //panel.add(gc, BorderLayout.CENTER);
        //gc.setTournamentMode();
        panel.add(bc, BorderLayout.CENTER);
        bc.setTournamentMode();

        if (GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length > 1) {
            minimap = new MinimapViewer(true);
        }
        TournamentTimer timer = new TournamentTimer(this);
        //gc.setSpaceBarListener(timer.getSpaceBarListener());
        bc.setSpaceBarListener(timer.getSpaceBarListener());
    }

    public BaseRenderer setupViewer() {
        if (proxy.isDebuggingAvailable()) {
            proxy.writeNotification(StartNotification.INSTANCE);
        }
        final BufferedMatch bufferedMatch = new BufferedMatch(proxy);
        if (cfg.getBoolean("bc.client.opengl")) {
            br = new GLGameRenderer(bufferedMatch, bc.getGraphics());
        } else {
            br = new GameRenderer(bufferedMatch);
        }

        dbg = new DebugState(bufferedMatch.getDebugProxy(), bc.getParent());
        br.setDebugState(dbg);
        bc.setRenderer(br);

        if (info != null) {
            Observer paintObserver = new Observer() {

                public void update(Observable o, Object arg) {
                    dbg.setEnabled(bufferedMatch.isPaused());
                    info.setTargetID(dbg.getFocusID());
                    info.setRobot(br.getRobotByID(dbg.getFocusID()));
                }
            };
            bc.addPaintObserver(paintObserver);
        }

        if (cfg.getBoolean("bc.client.sound-on")) {
            audio.setTimeline(br.getTimeline());
            battlecode.client.viewer.sound.GameSoundBank.preload();
        }

        if (tournamentMode) {
        } else {
            setupDevelViewer();
        }

        if (cfg.getBoolean("bc.client.opengl") && cfg.getBoolean("bc.client.minimap")) {
            minimap.setTimeline(br.getTimeline());
        }

        return br;
    }

    private void setupDevelViewer() {
        ClientProxy tmpProxy = (proxy.isDebuggingAvailable() ? proxy : null);
        MatchPlayer mp = new MatchPlayer(this, controller, br.getTimeline(),
                tmpProxy, lockstepChoice);
        //br.setInfoPanel(info);
    }

    public JComponent getComponent() {
        return panel;
    }

    public BaseCanvas getCanvas() {
        return bc;
    }

    public MinimapViewer getMinimap() {
        return minimap;
    }

    public boolean isTournamentMode() {
        return tournamentMode;
    }
}
