package battlecode.client.viewer;

import battlecode.common.GameConstants;
import battlecode.common.RobotType;


import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

public class InfoPanel extends JPanel {

  private static final long serialVersionUID = 0; // don't serialize
  private JLabel[] indicatorStrings;
  private JLabel robotID;
  private JLabel bytecodes;
  private JLabel movementDelay;
  private JLabel attackDelay;
  private JLabel energon;
  private JLabel supplyLevel;
  private JLabel extraInformation;
  private JLabel location;
  private JLabel direction;
  private GridBagConstraints layoutConstraints;

  public InfoPanel() {
    setLayout(new GridBagLayout());
    indicatorStrings = new JLabel[GameConstants.NUMBER_OF_INDICATOR_STRINGS];
    layoutConstraints = new GridBagConstraints();
    layoutConstraints.fill = GridBagConstraints.HORIZONTAL;

    layoutConstraints.gridx = 0;
    layoutConstraints.gridy = 0;
    layoutConstraints.weightx = 0.33;
    robotID = newLabel();
    forceMinimumSize(robotID, " Robot 10000 NOISETOWER ");
    layoutConstraints.gridx++;
    energon = newLabel();
    layoutConstraints.gridx++;
    forceMinimumSize(energon, " Health: 1000.0 ");
    supplyLevel = newLabel();
    forceMinimumSize(supplyLevel, " Supply: 100000.0 ");
    layoutConstraints.gridx++;
    movementDelay = newLabel();
    forceMinimumSize(movementDelay, " Movement: 100.0 ");
    layoutConstraints.gridx++;
    attackDelay = newLabel();
    forceMinimumSize(attackDelay, "Attack: 100,0");
    layoutConstraints.gridx++;
    bytecodes = newLabel();
    forceMinimumSize(bytecodes, " Bytecodes used: 10000 ");
    layoutConstraints.gridx++;
    location = newLabel();
    forceMinimumSize(location, " Location: [-99999, 99999] ");
    layoutConstraints.gridx++;
    direction = newLabel();
    forceMinimumSize(direction, " NORTH_EAST ");
    layoutConstraints.gridx++;
    extraInformation = newLabel();
    forceMinimumSize(extraInformation, " 6 missiles ready ");

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

  public void updateDebugChanges(AbstractDrawObject<AbstractAnimation> robot,
                                 int x, int y, double ore) {
    if (robot == null) {
      clear();
      robotID.setText("Ore: " + ore);
      indicatorStrings[0].setText("Location: " + Integer.toString(x) + ", " + Integer.toString(y));
    }
    else {
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

  public void setRobot(AbstractDrawObject<AbstractAnimation> robot) {
    robotID.setText(robotID.getText()+robot.getType()+" ");
    setEnergon(robot.getEnergon(), robot.getShields());
    setBytecodesUsed(robot.getBytecodesUsed());
    setAttackDelay(robot.getAttackDelay());
    setMovementDelay(robot.getMovementDelay());
    setSupplyLevel(robot.getSupplyLevel());
    if (robot.getType() == RobotType.COMMANDER) {
        setExtraInformation(String.format("%d XP", robot.getXP()));
    } else if (robot.getType() == RobotType.LAUNCHER) {
        setExtraInformation(String.format("%d missiles ready", robot.getMissileCount()));
    } else {
        setExtraInformation(".....");
    }
    location.setText(String.format(" Location: %s ",robot.getLocation()));
    direction.setText("");
    direction.setText(robot.getDirection().toString());
    for (int i = 0; i < GameConstants.NUMBER_OF_INDICATOR_STRINGS; i++) {
      String ids = robot.getIndicatorString(i);
      if (ids == null)
        ids = " ";
      setIndicatorString(i, ids);
    }
  }

private void setEnergon(double amount, double shields) {
    if (amount > Integer.MAX_VALUE / 2)
      energon.setText(String.format(" Health: lots ", amount));
    else
      energon.setText(String.format(" Health: %.1f ", amount));
  }

  private void setSupplyLevel(double amount) {
    supplyLevel.setText(String.format(" Supply: %.1f ", amount));
  }

  private void setBytecodesUsed(int bytecodesUsed) {
    bytecodes.setText(String.format(" Bytecodes used: %d ", bytecodesUsed));
  }

  private void setMovementDelay(double delay) {
    movementDelay.setText(String.format(" Movement: %.1f ", delay));
  }
  
  private void setAttackDelay(double delay){
	attackDelay.setText(String.format(" Attack: %.1f ", delay));
  }

  private void setExtraInformation(String s) {
    extraInformation.setText(s);
  }
  

  private void setIndicatorString(int index, String str) {
    indicatorStrings[index].setText(str);
    indicatorStrings[index].setToolTipText(str);
  }
}
