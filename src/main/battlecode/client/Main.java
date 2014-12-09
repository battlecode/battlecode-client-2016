package battlecode.client;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import battlecode.analysis.AwesomenessAnalyzer;
import battlecode.client.ClientProxy;
import battlecode.client.StreamClientProxy;
import battlecode.client.viewer.MatchViewer;
import battlecode.client.MatchDialog.Choice;
import battlecode.client.MatchDialog.Parameter;
import battlecode.serial.MatchInfo;
import battlecode.server.Config;
import battlecode.server.Server;
import battlecode.server.ServerFactory;
import battlecode.server.State;

public class Main {

    public static JFrame createFrame() {
        final JFrame frame = new JFrame("battlecode");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return frame;
    }

    public static void showViewer(final JFrame frame, final MatchViewer viewer) {
        final GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        // if tournament mode and 2 monitors run viewer on monitor 2 and minimap
        // on monitor one, else run only viewer

        final GraphicsDevice gd = (viewer.isTournamentMode() && MatchViewer.usingTwoScreens())
                ? devices[1] : devices[0];

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
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

                            // resize the frame: we want the height of the frame to be >= 600 (unless the screen can't fit it)
                            int minHeight = 600; 
                            int screenHeight = 0;
                            if (!GraphicsEnvironment.isHeadless()) {
                                screenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
                            }
                            if (screenHeight > 0 && screenHeight < minHeight) {
                                minHeight = screenHeight;
                            }
                            if (frame.getHeight() < minHeight) {
                                frame.setMinimumSize(new Dimension(frame.getWidth(), minHeight));
                                frame.setMinimumSize(null); // make it resizable
                            }
                        }
                    });

                    frame.pack();
                    frame.setVisible(true);
                }
            }
        });
    }

    private static void runLocal(Config options) {
        MatchDialog md = new MatchDialog(null);
        if (!options.getBoolean("bc.dialog.skip"))
            md.setVisible(true);
        Choice choice = md.getChoice();
        md.dispose();

        if (!options.getBoolean("bc.dialog.skip") && md.wasCancelPressed())
            return;

        String saveFile = (md.getSaveChoice() ? md.getSavePath() : null);

        ClientProxy theProxy = null;
        Thread serverThread = null;

        switch (choice) {

            case FILE:
                try {
                    String filePath = md.getSource();

                    if (md.getAnalyzeChoice()) {
                        AwesomenessAnalyzer.analyze(md.getSource());
                        if (new File(filePath + ".analyzed").exists()) {
                            filePath = filePath + ".analyzed";
                        }
                    }

                    theProxy = new StreamClientProxy(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                break;

            case LOCAL:

                theProxy = LocalProxy.INSTANCE;

                Server server = null;

                try {
                    server = ServerFactory.createLocalServer(options,
                            (LocalProxy) theProxy, saveFile);
                } catch (IOException e) {
                    return;
                }
                LocalProxy.INSTANCE.addObserver(server);
                serverThread = new Thread(server);

                server.update(null, new MatchInfo(
                        md.getParameter(Parameter.TEAM_A), md.getParameter(Parameter.TEAM_B), md.getAllMaps().toArray(new String[md.getAllMaps().size()])));
                
                break;

            case REMOTE:
                try {
                    Socket socket = new Socket(md.getSource(), 6370);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    String teamA = md.getParameter(Parameter.TEAM_A), teamB = md.getParameter(Parameter.TEAM_B);
                    String[] maps = md.getAllMaps().toArray(new String[0]);
                    out.writeObject(new MatchInfo(teamA, teamB, maps));
                    out.flush();

                    Thread.sleep(1000);

                    theProxy = new StreamClientProxy(socket.getInputStream(), out);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                break;

        }

		System.out.println("opengl = " + md.getGlClientChoice());
		System.out.println("minimap = " + md.getGlClientChoice());
        options.setBoolean("bc.client.opengl", md.getGlClientChoice());
        //options.setBoolean("bc.client.opengl", true);
        options.setBoolean("bc.client.minimap", md.getMinimapChoice());
        Main.showViewer(createFrame(), new MatchViewer(theProxy, md.getLockstepChoice()));
        if (serverThread != null)
            serverThread.start();
    }

    public static boolean run(Config options) {
				if (options.get("bc.client.match") != null && !options.get("bc.client.match").trim().equals("")) {
						ClientProxy theProxy;
						try {
								theProxy = new StreamClientProxy(options.get("bc.client.match"));
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
        if (battlecode.server.Main.run(options))
            return true;
        return false;
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("os.arch"));

        final Config options = battlecode.server.Main.setupConfig(args);
        if (!run(options)) {
            System.err.println("invalid bc.server.mode");
            System.exit(64);
        }
    }
    
}
