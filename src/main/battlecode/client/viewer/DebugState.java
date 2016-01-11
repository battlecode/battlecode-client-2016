package battlecode.client.viewer;

import battlecode.client.ClientProxy;
import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.MapLocation;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.serial.notification.InjectNotification;
import battlecode.world.signal.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.Observable;

public class DebugState extends Observable implements MouseListener,
        MouseMotionListener {

    private static final String CONTROL_BITS_CMD = "Set control bits";
    private static final String KILL_ROBOT_CMD = "Kill";

    private final ClientProxy proxy;

    private JPopupMenu popupMenu;

    private class MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            doPopupAction(e.getActionCommand());
        }
    }

    private MenuListener menuListener = new MenuListener();
    private JMenuItem controlBitsItem;
    private JMenuItem killRobotItem;
    private Component modalParent = null;

    private Point2D.Float p = new Point2D.Float();//float x, y;
    private float x0, y0;

    private boolean enabled = false;

    private int targetID = -1;
    private int focusID = -1;
    private int dragID = -1;

    private MapLocation targetLoc;
    private MapLocation spawnLoc;
    private long controlBits;

    public DebugState(ClientProxy proxy, Component modalParent) {
        this.proxy = proxy;
        this.modalParent = modalParent;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                popupMenu = new JPopupMenu();
                controlBitsItem = createMenuItem(CONTROL_BITS_CMD);
                killRobotItem = createMenuItem(KILL_ROBOT_CMD);
                popupMenu.add(controlBitsItem);
                popupMenu.add(killRobotItem);
                popupMenu.add(createTeamSpawnMenu(Team.A));
                popupMenu.add(createTeamSpawnMenu(Team.B));
                popupMenu.add(createTeamSpawnMenu(Team.NEUTRAL));
                popupMenu.add(createTeamSpawnMenu(Team.ZOMBIE));
            }
        });
    }

    public float getX() {
        return p.x;
    }

    public float getY() {
        return p.y;
    }

    public boolean isDragging() {
        return dragID != -1;
    }

    public float getDX() {
        return p.x - x0;
    }

    public float getDY() {
        return p.y - y0;
    }

    public int getDragID() {
        return dragID;
    }

    public int getFocusID() {
        return focusID;
    }

    public void setTarget(int id, MapLocation loc, long controlBits) {
        targetID = id;
        targetLoc = loc;
        this.controlBits = controlBits;
    }

    public void setFocusAndUpdate(int id) {
        focusID = id;
        forceUpdate();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private JMenu createTeamSpawnMenu(Team team) {
        String menuString = "Spawn ";
        switch (team) {
            case A: menuString += "red"; break;
            case B: menuString += "blue"; break;
            case NEUTRAL: menuString += "neutral"; break;
            case ZOMBIE: menuString += "zombie"; break;
            default: break;
        }
        JMenu menu = new JMenu(menuString);
        for (RobotType type : RobotType.values()) {
            if (type.isZombie && team != Team.ZOMBIE) {
                continue;
            }
            if (!type.isZombie && team == Team.ZOMBIE) {
                continue;
            }
            menu.add(createMenuItem(team, type));
        }
        return menu;
    }

    private JMenuItem createMenuItem(String cmd) {
        JMenuItem menuItem = new JMenuItem(cmd);
        menuItem.addActionListener(menuListener);
        menuItem.setActionCommand(cmd);
        return menuItem;
    }

    private JMenuItem createMenuItem(Team team, RobotType type) {
        JMenuItem menuItem = new JMenuItem(type.toString().toLowerCase());
        menuItem.addActionListener(menuListener);
        menuItem.setActionCommand(team.toString() + " " + type.toString());
        return menuItem;
    }

    private boolean tryPopupMenu(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return false;
        }
        controlBitsItem.setEnabled(targetID != -1);
        killRobotItem.setEnabled(targetID != -1);
        spawnLoc = new MapLocation((int) Math.floor(p.x), (int) Math.floor(p
                .y));
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
        return true;
    }

    private void doPopupAction(String cmd) {
        switch (cmd) {
            case CONTROL_BITS_CMD:
                String bits = (String) JOptionPane.showInputDialog(modalParent,
                        "Set control bits to (8-byte hexadecimal):", "Input",
                        JOptionPane.PLAIN_MESSAGE, null, null,
                        String.format("%016X", controlBits));
                if (bits == null) {
                    return;
                }
                try {
                    long value = (new java.math.BigInteger(bits, 16)).longValue();
                    proxy.writeNotification(new InjectNotification(new ControlBitsSignal(targetID, value)));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(modalParent,
                            bits + " is not a valid 8-byte hexadecimal.",
                            "Invalid input",
                            JOptionPane.WARNING_MESSAGE);
                }
                break;
            case KILL_ROBOT_CMD:
                proxy.writeNotification(new InjectNotification(new DeathSignal(targetID)));
                break;
            default:
                String[] split = cmd.split(" ");
                Team team = Enum.valueOf(Team.class, split[0]);
                RobotType type = Enum.valueOf(RobotType.class, split[1]);
                proxy.writeNotification(new InjectNotification(new SpawnSignal(spawnLoc, type, team, null, 0)));
                break;
        }
    }

    public void mousePressed(MouseEvent e) {
        if (enabled) {
            if (!tryPopupMenu(e)) {
                if (targetID != -1) {
                    x0 = p.x;
                    y0 = p.y;
                    dragID = targetID;
                }
            }
        }
        focusID = targetID;
        forceUpdate();
    }

    public void mouseReleased(MouseEvent e) {
        if (enabled) {
            tryPopupMenu(e);
        }
        if (dragID != -1) {
            MapLocation loc = new MapLocation(Math.round(targetLoc.x + getDX()),
                    Math.round(targetLoc.y + getDY()));
            if (!loc.equals(targetLoc)) {
                proxy.writeNotification(new InjectNotification(new MovementOverrideSignal(targetID, loc)));
            }
            dragID = -1;
            forceUpdate();
        }
    }

    public void mouseMoved(MouseEvent e) {
        RenderConfiguration.getInstance().getMapCoordinates(e.getX(), e.getY(), p);
        forceUpdate();
    }

    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    public void mouseExited(MouseEvent e) {
        mouseReleased(e);
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    private void forceUpdate() {
        setChanged();
        notifyObservers();
    }

}
