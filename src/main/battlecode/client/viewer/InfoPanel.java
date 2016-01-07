package battlecode.client.viewer;

import battlecode.client.viewer.render.DrawObject;
import battlecode.common.GameConstants;

import javax.swing.*;
import java.awt.*;

public class InfoPanel extends JPanel {

    private static final long serialVersionUID = 0; // don't serialize
    private final JLabel[] indicatorStrings;
    private final JLabel robotID;
    private final JLabel bytecodes;
    private final JLabel movementDelay;
    private final JLabel attackDelay;
    private final JLabel health;
    private final JLabel viperInfectedTurns;
    private final JLabel zombieInfectedTurns;
    private final JLabel location;
    private final GridBagConstraints layoutConstraints;

    public InfoPanel() {
        setLayout(new GridBagLayout());
        indicatorStrings = new JLabel[GameConstants
                .NUMBER_OF_INDICATOR_STRINGS];
        layoutConstraints = new GridBagConstraints();
        layoutConstraints.fill = GridBagConstraints.HORIZONTAL;

        layoutConstraints.gridx = 0;
        layoutConstraints.gridy = 0;
        layoutConstraints.weightx = 0.33;
        robotID = newLabel();
        forceMinimumSize(robotID, " Robot 10000 NOISETOWER ");
        layoutConstraints.gridx++;
        health = newLabel();
        forceMinimumSize(health, " Health: 1000.0 ");
        layoutConstraints.gridx++;
        movementDelay = newLabel();
        forceMinimumSize(movementDelay, " Core delay: 100.0 ");
        layoutConstraints.gridx++;
        attackDelay = newLabel();
        forceMinimumSize(attackDelay, "Weapon delay: 100.0");
        layoutConstraints.gridx++;
        bytecodes = newLabel();
        forceMinimumSize(bytecodes, " Bytecodes used: 10000 ");
        layoutConstraints.gridx++;
        location = newLabel();
        forceMinimumSize(location, " Location: [999, 999] ");
        layoutConstraints.gridx++;
        zombieInfectedTurns = newLabel();
        forceMinimumSize(zombieInfectedTurns, " Zombie infection: 99 turns " +
                "left ");
        layoutConstraints.gridx++;
        viperInfectedTurns = newLabel();
        forceMinimumSize(viperInfectedTurns, " Viper infection: 99 turns " +
                "left ");

        layoutConstraints.gridx = 0;
        for (int i = 0; i < indicatorStrings.length; i++) {
            layoutConstraints.gridy = i + 1;
            layoutConstraints.gridwidth = GridBagConstraints.REMAINDER;
            indicatorStrings[i] = newLabel();
        }

        clear();
    }

    private JLabel newLabel() {
        JLabel l = new JLabel();
        l.setFont(l.getFont().deriveFont(10f));
        add(l, layoutConstraints);
        return l;
    }

    private void forceMinimumSize(JLabel label, String str) {
        FontMetrics fm = label.getFontMetrics(label.getFont());
        Dimension size = new Dimension(fm.stringWidth(str), fm.getHeight());
        label.setMinimumSize(size);
        label.setPreferredSize(size);
    }

    public void updateDebugChanges(DrawObject robot,
                                   int x, int y, double parts, double rubble) {
        if (robot == null) {
            clear();
            robotID.setText("Parts: " + parts);
            indicatorStrings[1].setText("Rubble: " + rubble);
            indicatorStrings[2].setText("Location: " + Integer.toString(x) +
                    ", " + Integer.toString(y));
        } else {
            setRobot(robot);
        }
    }


    public void clear() {
        for (Component c : getComponents()) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setText(null);
                l.setToolTipText(null);
            }
        }
    }

    public void setTargetID(int id) {
        robotID.setText(" Robot " + id + " ");
    }

    public void setRobot(DrawObject robot) {
        robotID.setText(robotID.getText() + robot.getType() + " ");
        setHealth(robot.getHealth(), robot.getShields());
        setBytecodesUsed(robot.getBytecodesUsed());
        setAttackDelay(robot.getAttackDelay());
        setMovementDelay(robot.getMovementDelay());
        setZombieInfectedTurns(robot.getZombieInfectedTurns());
        setViperInfectedTurns(robot.getViperInfectedTurns());
        location.setText(String.format(" Location: %s ", robot.getLocation()));
        for (int i = 0; i < GameConstants.NUMBER_OF_INDICATOR_STRINGS; i++) {
            String ids = robot.getIndicatorString(i);
            if (ids == null)
                ids = " ";
            setIndicatorString(i, ids);
        }
    }

    private void setHealth(double amount, double shields) {
        if (amount > Integer.MAX_VALUE / 2)
            health.setText(" Health: lots ");
        else
            health.setText(String.format(" Health: %.1f ", amount));
    }

    private void setZombieInfectedTurns(int t) {
        zombieInfectedTurns.setText(String.format(" Zombie infection: %d " +
                "turns left ", t));
    }

    private void setViperInfectedTurns(int t) {
        viperInfectedTurns.setText(String.format(" Viper infection: %d " +
                "turns left ", t));
    }

    private void setBytecodesUsed(int bytecodesUsed) {
        bytecodes.setText(String.format(" Bytecodes used: %d ", bytecodesUsed));
    }

    private void setMovementDelay(double delay) {
        movementDelay.setText(String.format(" Core delay: %.1f ", delay));
    }

    private void setAttackDelay(double delay) {
        attackDelay.setText(String.format(" Weapon delay: %.1f ", delay));
    }

    private void setIndicatorString(int index, String str) {
        indicatorStrings[index].setText(str);
        indicatorStrings[index].setToolTipText(str);
    }
}
