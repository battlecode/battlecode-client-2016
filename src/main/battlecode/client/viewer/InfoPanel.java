package battlecode.client.viewer;

import battlecode.common.ComponentType;
import battlecode.common.GameConstants;


import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

public class InfoPanel extends JPanel {

    private static final long serialVersionUID = 0; // don't serialize
    private JLabel[] indicatorStrings;
    private JPanel componentPanel;
    private ArrayList<JLabel> componentLabels = new ArrayList<JLabel>(); 
	private JLabel robotID;
	private JLabel bytecodes;
	private JLabel energon; 
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
		
		layoutConstraints.gridx = 1;
		energon = newLabel();
		
		layoutConstraints.gridx = 2;
		bytecodes = newLabel();
		
		layoutConstraints.gridx = 0;
        for (int i = 0; i < indicatorStrings.length; i++) {
            layoutConstraints.gridy = i+1;
            layoutConstraints.gridwidth = 3;
            indicatorStrings[i] = newLabel();
        }
        componentPanel = new JPanel();
		componentPanel.setLayout(new GridLayout(2,0));
		
		layoutConstraints.gridx = 0;
        layoutConstraints.gridy = indicatorStrings.length+3;
        layoutConstraints.gridwidth = 3;
        layoutConstraints.gridheight = 2;
		add(componentPanel, layoutConstraints);
		
		for(int i = 0; i < 10; i++){
			JLabel componentLabel = new JLabel();
			componentLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
			componentLabels.add(componentLabel);
			componentPanel.add(componentLabel);
		}
		
		clear();
    }

	private JLabel newLabel() {
		JLabel l = new JLabel();
		add(l, layoutConstraints);
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
        
        for(Component c : componentPanel.getComponents()){
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
			setComponentTypes(robot.getComponents());
		}
	}
	
	private void setComponentTypes(ArrayList<ComponentType> components){
		for(JLabel label : componentLabels){
			label.setText("");
		}
		int labelCount = 0;
		for(ComponentType component : components){
			
			JLabel componentLabel = componentLabels.get(labelCount);
			componentLabel.setText(" " + component.toString() + " ");
			labelCount++;
		}//It would probably be more efficient to set up the labels, then just change their texts.
	
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
