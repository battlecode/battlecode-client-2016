package battlecode.client.viewer;

import battlecode.common.GameConstants;

import java.awt.*;
import javax.swing.*;

public class InfoPanel extends JPanel {

    private static final long serialVersionUID = 0; // don't serialize
    private JLabel[] indicatorStrings;
	private JLabel robotID;
	private JLabel bytecodes;
	private JLabel energon; 

    public InfoPanel() {
        setLayout(new GridLayout(0,1));
        indicatorStrings = new JLabel[GameConstants.NUMBER_OF_INDICATOR_STRINGS];
		robotID = newLabel();
		energon = newLabel();
		bytecodes = newLabel();
        for (int i = 0; i < indicatorStrings.length; i++) {
            indicatorStrings[i] = newLabel();
        }
		clear();
    }

	private JLabel newLabel() {
		JLabel l = new JLabel();
		add(l);
		return l;
	}

    public void clear() {
        for (Component c : getComponents()) {
			if(c instanceof JLabel) {
				JLabel l = (JLabel)c;
            	l.setText(null);
            	l.setToolTipText(null);
			}
        }
        robotID.setText("No robot selected");
    }

    public void setTargetID(int id) {
        robotID.setText("Robot " + id);
    }

	public void setRobot(AbstractDrawObject robot) {
		if(robot==null) clear();
		else {
			setEnergon(robot.getEnergon());
			setBytecodesUsed(robot.getBytecodesUsed());
			for(int i=0;i<GameConstants.NUMBER_OF_INDICATOR_STRINGS;i++) {
				setIndicatorString(i,robot.getIndicatorString(i));
			}
		}
	}

    private void setEnergon(double amount) {
        energon.setText(String.format("Energon: %.1f", amount));
    }

    private void setBytecodesUsed(int bytecodesUsed) {
        if (bytecodesUsed > 0) {
            bytecodes.setText("Bytecodes used: " + bytecodesUsed);
        }
    }

    private void setIndicatorString(int index, String str) {
        indicatorStrings[index].setText(str);
        indicatorStrings[index].setToolTipText(str);
    }
}
