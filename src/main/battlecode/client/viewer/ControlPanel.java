package battlecode.client.viewer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import java.awt.event.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

public class ControlPanel extends JPanel
        implements ActionListener, ChangeListener, Controller {

    private static final long serialVersionUID = 0; // don't serialize
    private static final double[][] LAYOUT = {
        {400, 20, 400},
        {30, 30, 30}
    };
    private MatchPlayer player;
    private JPanel panel;
    private JLabel label;
    private JButton start, play, end, next, back, step;
    private JFormattedTextField stepSizeField;
    private NumberFormat stepSizeFmt = NumberFormat.getNumberInstance();
    private JSlider slider;
    private boolean setSliderPrecise = false;
    private String matchCount = "";
    private final ImageIcon playIcon;
    private final ImageIcon pauseIcon;
    private InfoPanel infoPanel = null;
    private Observer timelineObserver = new Observer() {

        public void update(Observable o, Object obj) {
            GameStateTimeline gst = (GameStateTimeline) o;
            if (gst.isActive()) {
                int round = gst.getRound();
                if (round >= 0) {
                    updateRoundLabel(round, gst.getNumRounds());
                    setSliderValue(round);
                }
            } else {
                next.setEnabled(false);
                slider.setEnabled(false);
                setSliderValue(0);
            }
        }
    };

    public ControlPanel() {
        label = new JLabel(matchCount + "Round 0 of 0");
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        if (battlecode.server.Config.getGlobalConfig().getBoolean("bc.client.applet")) {
            ImageIcon playIconSwap = null, pauseIconSwap = null;

            try {
                String fpath = battlecode.server.Config.getGlobalConfig().get("bc.client.applet.path");
                playIconSwap = new ImageIcon(new URL(fpath + "art/icons/playback-start.png"));
                pauseIconSwap = new ImageIcon(new URL(fpath + "art/icons/playback-pause.png"));

                start = createButton(new URL(fpath + "art/icons/skip-backward.png"), "start");
                play = createButton(pauseIconSwap, "pause");
                end = createButton(new URL(fpath + "art/icons/skip-forward.png"), "end");
                next = createButton(new URL(fpath + "art/icons/go-next.png"), "next");
                next.setEnabled(false);

                back = createButton(new URL(fpath + "art/icons/seek-backward.png"), "back");
                step = createButton(new URL(fpath + "art/icons/seek-forward.png"), "step");
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
            playIcon = playIconSwap;
            pauseIcon = pauseIconSwap;
        } else {
            playIcon = new ImageIcon("art/icons/playback-start.png");
            pauseIcon = new ImageIcon("art/icons/playback-pause.png");

            start = createButton("art/icons/skip-backward.png", "start");
            play = createButton(pauseIcon, "pause");
            end = createButton("art/icons/skip-forward.png", "end");
            next = createButton("art/icons/go-next.png", "next");
            next.setEnabled(false);

            back = createButton("art/icons/seek-backward.png", "back");
            step = createButton("art/icons/seek-forward.png", "step");

        }
        stepSizeFmt.setGroupingUsed(false);
        stepSizeField = new JFormattedTextField(stepSizeFmt);
        stepSizeField.setValue(1);
        stepSizeField.setColumns(5);
        stepSizeField.setMinimumSize(new Dimension(50, stepSizeField.getHeight()));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.LINE_AXIS));
        panel.add(start);
        panel.add(play);
        panel.add(end);
        panel.add(next);
        panel.add(back);
        panel.add(step);
        panel.add(stepSizeField);
        panel.add(new JLabel(" rounds"));

        slider = new JSlider(0, 1);
        setSliderValue(0);
        slider.setEnabled(false);
        infoPanel = new InfoPanel();

        setAlignmentX(CENTER_ALIGNMENT);

        /*
        setLayout(new TableLayout(LAYOUT));
        add(label, "0, 0, 0, 0, c, f");
        add(panel, "0, 1, 0, 1, f, t");
        add(slider, "0, 2, 0, 2, f, f");
        add(infoPanel, "2, 0, 2, 2, c, f");
         */

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(label, gbc);
        gbc.gridy = 1;
        add(panel, gbc);
        gbc.gridy = 2;
        add(slider, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridheight = 3;
        add(infoPanel,gbc);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.gridheight = 1;
        add(new JPanel());



        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                setMaximumSize(getSize());
                removeComponentListener(this);
            }
        });

        
    }

    private JButton createButton(String iconPath, String cmd) {
        return createButton(new ImageIcon(iconPath), cmd);
    }

    private JButton createButton(URL iconURL, String cmd) {
        return createButton(new ImageIcon(iconURL), cmd);
    }

    private JButton createButton(ImageIcon icon, String cmd) {
        JButton button = new JButton(icon);
        button.setActionCommand(cmd);
        button.addActionListener(this);
        return button;
    }

    public InfoPanel getInfoPanel() {
        return infoPanel;
    }

    public int getStepSize() {
        return ((Number) stepSizeField.getValue()).intValue();
    }

    public void setPlayEnabled(boolean enabled) {
        if (enabled) {
            play.setActionCommand("play");
            play.setIcon(playIcon);
        } else {
            play.setActionCommand("pause");
            play.setIcon(pauseIcon);
        }
    }

    public void enableNext() {
		// we need to use invokeLater to avoid a deadlock
		// in the 3d client
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
	        	next.setEnabled(true);
			}
		});
    }

    public void setPlayer(MatchPlayer player) {
        if (this.player == null) {
            slider.addChangeListener(this);
        }
        this.player = player;
        GameStateTimeline gst = player.getTimeline();
        gst.addObserver(timelineObserver);
        gst.getMatch().addMatchListener(new MatchListener() {

            public void headerReceived(BufferedMatch match) {
                slider.setMaximum(match.getHeader().getMap().getMaxRounds());
                slider.setEnabled(true);
            }
        });
    }

    public void updateRoundLabel(int round, int max) {
        if (round >= 0) {
            label.setText("Round " + round + " of " + max);
        }
		if (max >= slider.getMaximum()) {
			slider.setMaximum(max);
		}
    }

    public void updateRoundLabel(GameStateTimeline gst) {
        updateRoundLabel(gst.getRound(), gst.getNumRounds());
    }

    private void setSliderValue(int round) {
        setSliderPrecise = true;
        slider.setValue(round);
    }

    public void actionPerformed(ActionEvent e) {
        if (player != null) {
            player.actionPerformed(e);
        }
    }

    public void stateChanged(ChangeEvent e) {
        GameStateTimeline gst = player.getTimeline();
        if (slider.getValueIsAdjusting() && !setSliderPrecise) {
            int round = slider.getValue();
            gst.setRound(round - (round % gst.getRoundsPerKey()));
        }
        setSliderPrecise = false;
        if (slider.getValue() > gst.getNumRounds()) {
            setSliderValue(gst.getNumRounds());
        }
    }
}
