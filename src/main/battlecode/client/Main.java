package battlecode.client;

import battlecode.client.MatchDialog.Choice;
import battlecode.client.MatchDialog.Parameter;
import battlecode.client.viewer.MatchViewer;
import battlecode.server.GameInfo;
import battlecode.serial.notification.GameNotification;
import battlecode.server.Config;
import battlecode.server.Server;
import battlecode.server.proxy.FileProxy;
import battlecode.server.proxy.Proxy;
import battlecode.serial.serializer.JavaSerializerFactory;
import battlecode.serial.serializer.SerializerFactory;
import battlecode.serial.serializer.XStreamSerializerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    public static JFrame createFrame() {
        final JFrame frame = new JFrame("battlecode");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        return frame;
    }

    public static void showViewer(final JFrame frame, final MatchViewer
            viewer) {
        final GraphicsDevice[] devices = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getScreenDevices();
        // if tournament mode and 2 monitors run viewer on monitor 2 and minimap
        // on monitor one, else run only viewer

        final GraphicsDevice gd = (viewer.isTournamentMode() && MatchViewer
                .usingTwoScreens())
                ? devices[1] : devices[0];
        viewer.getCanvas().setGraphicsDevice(gd);

        SwingUtilities.invokeLater(() -> {
            frame.getContentPane().add(viewer.getComponent());
            if (viewer.isTournamentMode() && gd.isFullScreenSupported()) {
                //frame.setIgnoreRepaint(true);
                frame.setResizable(false);
                frame.setUndecorated(true);
                gd.setFullScreenWindow(frame);
                /*
                // TESTING!!
                frame.setResizable(true);
                frame.setUndecorated(false);
                 */

                viewer.getCanvas().setVisible(true);
            } else {
                viewer.getCanvas().addComponentListener(new ComponentAdapter() {

                    public void componentShown(ComponentEvent e) {
                        frame.pack();
                    }
                });

                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    private static void runLocal(Config options) {
        MatchDialog md = new MatchDialog(null);
        if (!options.getBoolean("bc.dialog.skip"))
            md.setVisible(true);
        Choice choice = md.getChoice();
        md.dispose();
        
        options.setBoolean("bc.game.disable-zombies", md.getDisableZombies());

        if (!options.getBoolean("bc.dialog.skip") && md.wasCancelPressed())
            return;

        String saveFile = (md.getSaveChoice() ? md.getSavePath() : null);

        ClientProxy theProxy = null;
        Thread serverThread = null;

        switch (choice) {

            case FILE:
                try {
                    String filePath = md.getSource();

                    theProxy = new StreamClientProxy(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                break;

            case LOCAL:
                LocalProxy localProxy = new LocalProxy();

                final Server server;

                List<Proxy> proxies = new ArrayList<>();

                try {

                    if (saveFile != null) {
                        final SerializerFactory serializerFactory;
                        if (options.getBoolean("bc.server.output-xml")) {
                            serializerFactory = new XStreamSerializerFactory();
                        } else {
                            serializerFactory = new JavaSerializerFactory();
                        }
                        proxies.add(new FileProxy(saveFile, serializerFactory));
                    }

                    proxies.add(localProxy);

                    server = new Server(options, true);

                    localProxy.addOutputHandler(server);

                } catch (IOException e) {
                    return;
                }

                serverThread = new Thread(server);

                new GameNotification(new GameInfo(
                        md.getParameter(Parameter.TEAM_A),
                        null,
                        md.getParameter(Parameter.TEAM_B),
                        null,
                        md.getAllMaps().toArray(new String[md.getAllMaps().size()]),
                        proxies.toArray(new Proxy[proxies.size()]),
                        false
                )).accept(server);

                theProxy = localProxy;

                break;

        }

        Main.showViewer(createFrame(), new MatchViewer(theProxy, md
                .getLockstepChoice()));

        if (serverThread != null)
            serverThread.start();
    }

    public static boolean run(Config options) {
        if (options.get("bc.client.match") != null && !options.get("bc.client" +
                ".match").trim().equals("")) {
            ClientProxy theProxy;
            try {
                theProxy = new StreamClientProxy(options.get("bc.client" +
                        ".match"));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            Main.showViewer(createFrame(), new MatchViewer(theProxy, true));
            return true;
        }
        if (options.get("bc.server.mode").equalsIgnoreCase("LOCAL")) {
            runLocal(options);
            return true;
        }
        return battlecode.server.Main.run(options);
    }

    public static void main(String[] args) {
        final Config options = battlecode.server.Main.setupConfig(args);
        if (!run(options)) {
            System.err.println("invalid bc.server.mode");
            System.exit(64);
        }
    }

}
