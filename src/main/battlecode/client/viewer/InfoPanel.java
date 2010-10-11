package battlecode.client.viewer;

import battlecode.common.GameConstants;

import java.awt.*;
import javax.swing.*;

public class InfoPanel extends JPanel {

    private static final long serialVersionUID = 0; // don't serialize
    private static final int NUM_LINES = 4;
    private JLabel[] labels;

    public InfoPanel() {
        int rows = NUM_LINES + GameConstants.NUMBER_OF_INDICATOR_STRINGS;
        setLayout(new GridLayout(rows, 0));
        labels = new JLabel[rows];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new JLabel();
            add(labels[i]);
        }
    }

    public void clear() {
        for (JLabel label : labels) {
            label.setText(null);
            label.setToolTipText(null);
        }
        labels[0].setText("No robot selected");
    }

    public void setTargetID(int id) {
        labels[0].setText("Robot " + id);
    }

	public void setRobot(AbstractDrawObject robot) {
		if(robot==null) clear();
		else {
			setEnergon(robot.getEnergon());
			setFlux(robot.getFlux());
			setBytecodesUsed(robot.getBytecodesUsed());
			for(int i=0;i<GameConstants.NUMBER_OF_INDICATOR_STRINGS;i++) {
				setIndicatorString(i,robot.getIndicatorString(i));
			}
		}
	}

    private void setEnergon(double amount) {
        labels[1].setText(String.format("Energon: %.1f", amount));
    }

    private void setFlux(double amount) {
        labels[3].setText(String.format("Flux: %.0f", amount));
    }

    private void setBytecodesUsed(int bytecodesUsed) {
        if (bytecodesUsed > 0) {
            labels[2].setText("Bytecodes used: " + bytecodesUsed);
        }
    }

    private void setIndicatorString(int index, String str) {
        labels[index + NUM_LINES].setText(str);
        labels[index + NUM_LINES].setToolTipText(str);
    }
}
