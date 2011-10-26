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
    private JLabel energon;
	private JLabel flux;
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
		layoutConstraints.gridx++;
        energon = newLabel();
		layoutConstraints.gridx++;
        flux = newLabel();
		layoutConstraints.gridx++;
		bytecodes = newLabel();
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
        l.setFont(l.getFont().deriveFont(8f));
        add(l, layoutConstraints);
        return l;
    }

    public void clear() {
        for (Component c : getComponents()) {
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setText(null);
                l.setToolTipText(null);
            }
        }

        robotID.setText("No robot selected");
    }

    public void setTargetID(int id) {
        robotID.setText(" Robot " + id + " ");
    }

    public void setRobot(AbstractDrawObject<AbstractAnimation> robot) {
        if (robot == null)
            clear();
        else {
            setEnergon(robot.getEnergon());
			setFlux(robot.getFlux());
            setBytecodesUsed(robot.getBytecodesUsed());
            direction.setText(robot.getDirection().toString());
            for (int i = 0; i < GameConstants.NUMBER_OF_INDICATOR_STRINGS; i++) {
                String ids = robot.getIndicatorString(i);
                if (ids == null)
                    ids = " ";
                setIndicatorString(i, ids);
            }
        }
    }

    private void setEnergon(double amount) {
        energon.setText(String.format(" Energon: %.1f ", amount));
    }

	private void setFlux(double amount) {
		flux.setText(String.format(" Flux: %.1f ", amount));
	}

    private void setBytecodesUsed(int bytecodesUsed) {
        if (bytecodesUsed > 0) {
            bytecodes.setText(" Bytecodes used: " + bytecodesUsed + " ");
        }
    }

    private void setIndicatorString(int index, String str) {
        indicatorStrings[index].setText(str);
        indicatorStrings[index].setToolTipText(str);
    }
}
