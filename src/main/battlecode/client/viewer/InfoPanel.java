package battlecode.client.viewer;

import battlecode.common.GameConstants;


import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

public class InfoPanel extends JPanel {

  private static final long serialVersionUID = 0; // don't serialize
  private JLabel[] indicatorStrings;
  private JLabel robotID;
  private JLabel bytecodes;
  private JLabel delay;
  private JLabel energon;
  private JLabel flux;
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
    forceMinimumSize(energon, " Health: 100.0 ");
    flux = newLabel();
    layoutConstraints.gridx++;
    delay = newLabel();
    forceMinimumSize(delay, " Actiondelay: 100.0 ");
    layoutConstraints.gridx++;
    bytecodes = newLabel();
    forceMinimumSize(bytecodes, " Bytecodes used: 10000 ");
    layoutConstraints.gridx++;
    location = newLabel();
    forceMinimumSize(location, " Location: [99, 99] ");
    layoutConstraints.gridx++;
    direction = newLabel();

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
                                 int x, int y, double density) {
    if (robot == null) {
      clear();
      robotID.setText("Cows: " + Double.toString(density));
      indicatorStrings[0].setText("Location: " + Integer.toString(x) + ", " + Integer.toString(y));
      //robotID.setText("No robot selected");
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
    setFlux(robot.getFlux());
    setBytecodesUsed(robot.getBytecodesUsed());
    setActionDelay(robot.getActionDelay());
    location.setText(String.format(" Location: %s ",robot.getLocation()));
    direction.setText("");
    //direction.setText(robot.getDirection().toString());
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

  private void setFlux(double amount) {
    flux.setText("");
    //flux.setText(String.format(" Flux: %.1f ", amount));
  }

  private void setBytecodesUsed(int bytecodesUsed) {
    bytecodes.setText(String.format(" Bytecodes used: %d ", bytecodesUsed));
  }

  private void setActionDelay(double actionDelay) {
    delay.setText(String.format(" Actiondelay: %.1f ", actionDelay));
  }

  private void setIndicatorString(int index, String str) {
    indicatorStrings[index].setText(str);
    indicatorStrings[index].setToolTipText(str);
  }
}
