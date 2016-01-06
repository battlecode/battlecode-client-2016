package battlecode.client;

import battlecode.client.resources.ResourceLoader;
import battlecode.server.Config;
import battlecode.server.PlayerFinder;
import battlecode.server.Server;
import battlecode.server.Version;
import battlecode.world.GameMapIO;
import info.clearthought.layout.TableLayout;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * Displays a modal dialog box for choosing match parameters. It presents the
 * user with options for match type (local, from file, or remote), to save the
 * match to a file, and for match inputs (team A, team B, map).
 * <p>
 * For an example of usage, see the main method below.
 */
public class MatchDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 0; // don't serialize

    private final static int WIDTH = 350, BANNER_HEIGHT = 100;

    private boolean okPressed = false;
    private String popupVersion = null;
    private String currentVersion = null;

    private static final File fields = new File(
            System.getProperty("user.home")
                    + File.separator + ".battlecode.ui");

    private final double[][] LAYOUT = { /* 10 x 21 */
            {20, 30, 70, 5, 100, 5, 55, 5, 40, 20},
            {110, 25, 25, 30, 25, 25, 25, 30, 25, 25, 25, 25, 25, 15,
                    30, 30, 0, 15, 30, 10}
    };

    private final ButtonGroup matchOptionsGroup;

    private final JTextField txtLoadFile, txtSaveFile;
    private final JButton btnLoadBrowse, btnSaveBrowse, btnOK, btnCancel;
    private final JButton btnAdd, btnRemove;
    private final JCheckBox chkLockstep, chkSave, chkNoZombies;
    private final JFileChooser dlgChooser;
    private final JList lstMatches;
    private final DefaultListModel lstMatchesModel;
    private final JLabel lblVersion;

    private final EnumMap<Choice, JRadioButton> choices;
    private final EnumMap<Parameter, JComboBox> parameters;

    private final PlayerFinder finder;

    /**
     * Represents a user's match type choice.
     */
    public enum Choice {
        LOCAL,
        FILE
    }

    /**
     * Represents a match parameter.
     */
    public enum Parameter {
        TEAM_A("Team A"),
        TEAM_B("Team B"),
        MAP("Map");

        private final String label;

        Parameter(String label) {
            this.label = label;
        }

        /**
         * Gets the label for this parameter.
         *
         * @return the parameter's String label
         */
        public final String getLabel() {
            return label;
        }
    }

    private class UpdateTask extends TimerTask {
        public void run() {
            try {
                URL versionURL = new URL("http://battlecode" +
                        ".org/contestants/latest/");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(versionURL.openStream()));

                currentVersion = reader.readLine();

                if (!currentVersion.equals(Version.version)) {
                    lblVersion.setText("<html>" +lblVersion.getText() + " <font color='red'>(OUTDATED)</font></html>");

                    if (!currentVersion.equals(popupVersion)) {
                        popupVersion = currentVersion;
                        JOptionPane.showMessageDialog(MatchDialog.this,
                                "A new version of the BattleCode software" +
                                        "\nhas been released! Please visit " +
                                        "the software" +
                                        "\npage for links and information on " +
                                        "updating" +
                                        "\nyour installation.", "New Version " +
                                        "Available",
                                JOptionPane.WARNING_MESSAGE);
                        saveFields();
                    }
                }
            } catch (IOException e1) {
            }
        }
    }

    /**
     * Creates a match dialog with the given owner.
     *
     * @param owner the parent of this dialog (can be null)
     */
    public MatchDialog(JFrame owner) {

        // Modal JDialog.
        super(owner, Dialog.ModalityType.TOOLKIT_MODAL);

        // Initialize stuff.
        choices = new EnumMap<>(Choice.class);
        parameters = new EnumMap<>(Parameter.class);
        finder = new PlayerFinder();
        matchOptionsGroup = new ButtonGroup();
        dlgChooser = new JFileChooser();
        dlgChooser.setFileFilter(new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith(".rms") || f.isDirectory();
            }

            public String getDescription() {
                return "BattleCode Match Files (*.rms)";
            }

        });

        // Compute the height from the table layout.
        int height = 20;
        for (double d : LAYOUT[1])
            height += (int) d;

        // Set up the dialog window.
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(WIDTH, height);
        setResizable(true);
        setTitle("BattleCode");
        setLayout(new TableLayout(LAYOUT));

        // Native look & feel.
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        // Create and add the banner image.
        JLabel imageLabel = new JLabel(new ImageIcon(ResourceLoader.getUrl("art/banner.png")));
        imageLabel.setMinimumSize(new Dimension(WIDTH, BANNER_HEIGHT));
        add(imageLabel, "0, 0, 9, 0, f, t");

        // Create the match option buttons.
        choices.put(Choice.LOCAL, new JRadioButton("Run match locally"));
        choices.put(Choice.FILE, new JRadioButton("Play back from match file"));
        choices.get(Choice.LOCAL).setSelected(true);

        // Initialize the match option buttons.
        for (int i = 0; i < choices.size(); i++) {
            JRadioButton choice = choices.get(Choice.values()[i]);

            // Add the option button to the group.
            matchOptionsGroup.add(choice);

            // Set up the button for responding to events.
            choice.setActionCommand("choice");
            choice.setMnemonic(choice.getText().charAt(0));
            choice.addActionListener(this);

            // Add to the table.
            int offset = i + 1 + (i > 1 ? 1 : 0);
            add(choice, String.format("1, %d, 8, %d", offset, offset));
        }

        // The file input box.
        txtLoadFile = new JTextField();
        txtLoadFile.setPreferredSize(new Dimension(WIDTH, 20));
        txtLoadFile.setEnabled(false);
        add(txtLoadFile, "2, 3, 4, 3, f, c");

        // The browse button.
        btnLoadBrowse = new JButton("Browse");
        btnLoadBrowse.setActionCommand("browse");
        btnLoadBrowse.addActionListener(this);
        btnLoadBrowse.setEnabled(false);
        add(btnLoadBrowse, "6, 3, 8, 3, f, c");

        // Separator.
        add(new JSeparator(), "1, 4, 8, 4, f, c");

        chkLockstep = new JCheckBox("Compute and view match synchronously");
        add(chkLockstep, "1, 5, 8, 5, f, c");

        // Save to file check box.
        chkSave = new JCheckBox("Save match to file");
        chkSave.setActionCommand("save");
        chkSave.addActionListener(this);
        add(chkSave, "1, 6, 8, 6, f, c");

        // Save to file path field.
        txtSaveFile = new JTextField();
        txtSaveFile.setPreferredSize(new Dimension(WIDTH, 20));
        txtSaveFile.setEnabled(false);
        add(txtSaveFile, "2, 7, 4, 7, f, c");

        // Save to file browse button.
        btnSaveBrowse = new JButton("Browse");
        btnSaveBrowse.setActionCommand("save-browse");
        btnSaveBrowse.addActionListener(this);
        btnSaveBrowse.setEnabled(false);
        add(btnSaveBrowse, "6, 7, 8, 7, f, c");

        
        // Separator.
        add(new JSeparator(), "1, 8, 8, 8, f, c");

        // Create match parameter dropdown boxes.
        parameters.put(Parameter.TEAM_A, new JComboBox());
        parameters.put(Parameter.TEAM_B, new JComboBox());
        parameters.put(Parameter.MAP, new JComboBox());

        // Initialize match parameter dropdown boxes.
        for (int i = 0; i < parameters.size(); i++) {
            Parameter param = Parameter.values()[i];
            JComboBox paramBox = parameters.get(param);
            paramBox.setPreferredSize(new Dimension(WIDTH, 20));
            paramBox.setEditable(true);

            int offset = i + 9;
            add(paramBox, String.format("4, %d, 8, %d, f, c", offset, offset));
            add(new JLabel(param.getLabel()),
                    String.format("1, %d, 2, %d", offset, offset));
        }

        // Disable Zombies check box
        chkNoZombies = new JCheckBox("Disable zombie spawning");
        add(chkNoZombies, "1, 12, 8, 12, f, c");
        
        lstMatchesModel = new DefaultListModel();

        lstMatches = new JList(lstMatchesModel);
        lstMatches.setVisibleRowCount(-1);
        lstMatches.setLayoutOrientation(JList.VERTICAL);
        lstMatches.setAutoscrolls(true);
        JScrollPane scrMatches = new JScrollPane(lstMatches);
        add(scrMatches, "1, 14, 6, 17, f, f");

        // The "queue match" button.
        btnAdd = new JButton();
        btnAdd.setActionCommand("add");
        btnAdd.addActionListener(this);
        btnAdd.setIcon(new ImageIcon(ResourceLoader.getUrl("art/icons/list-add.png")));
        add(btnAdd, "8, 14, f, f");

        btnRemove = new JButton();
        btnRemove.setActionCommand("remove");
        btnRemove.addActionListener(this);
        btnRemove.setIcon(new ImageIcon(ResourceLoader.getUrl("art/icons/list-remove.png")));
        add(btnRemove, "8, 15, f, f");

        // The OK button.
        btnOK = new JButton("OK");
        btnOK.setMnemonic('O');
        btnOK.setActionCommand("ok");
        btnOK.addActionListener(this);
        add(btnOK, "4, 18, f, c");

        // The cancel button.
        btnCancel = new JButton("Cancel");
        btnCancel.setMnemonic('C');
        btnCancel.setActionCommand("cancel");
        btnCancel.addActionListener(this);
        add(btnCancel, "6, 18, 8, 18, f, c");

        int idx;
        if (Version.version == null || (idx = Version.version.indexOf('.')) < 0)
            lblVersion = new JLabel("");
        else
            lblVersion = new JLabel("v" + Version.version.substring(idx + 1));
        lblVersion.setFont(lblVersion.getFont().deriveFont(Font.BOLD));
        lblVersion.setHorizontalAlignment(JLabel.LEFT);
        add(lblVersion, "1, 18, 2, 18, f, c");

        // Restore saved prefs, if any.
        loadFields();

        if (Config.getGlobalConfig().getBoolean("bc.client.check-updates")) {
            Timer t = new Timer(true);
            t.schedule(new UpdateTask(), 25);
        }
    }

    /**
     * Handles UI events from buttons and options.
     * <p>
     * Summary of commands:
     * - ok: the OK button was pressed
     * - cancel: the cancel button was pressed
     * - choice: the user selected a match type radio button
     * - save: the save to file check box was changed
     * - browse: the load from file browse button was clicked
     * - save-browse: the save to file browse button was clicked
     *
     * @param e the event
     */
    public void actionPerformed(ActionEvent e) {

        String cmd = e.getActionCommand();

        // OK: save the current inputs to a file and go hidden
        if ("ok".equals(cmd)) {

            String loadPath = (getChoice() == Choice.FILE ? getSource() : null);
            if (loadPath != null && !new File(loadPath).exists()) {
                JOptionPane.showMessageDialog(this,
                        "Invalid match file path.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                txtLoadFile.selectAll();
                return;
            }

            okPressed = true;
            saveFields();
            setVisible(false);
        }

        // Cancel: don't save the current inputs, go hidden
        else if ("cancel".equals(cmd)) {
            setVisible(false);
        }

        // Radio button change: disable/enable appropriate inputs
        else if ("choice".equals(cmd))
            enableFields();

            // Save to file: disable/enable appropriate inputs
        else if ("save".equals(cmd))
            enableFields();

            // Enqueue a map choice.
        else if ("add".equals(cmd)) {
            String mapName = getParameter(Parameter.MAP);
            if (mapName != null)
                lstMatchesModel.addElement(mapName);
        }

        // Dequeue a map choice.
        else if ("remove".equals(cmd)) {
            int index = lstMatches.getSelectedIndex();
            if (index >= 0) {
                lstMatchesModel.remove(index);
                if (index - 1 >= 0)
                    lstMatches.setSelectedIndex(index - 1);
            } else if (lstMatchesModel.getSize() == 1) {
                // if there is only one, they are trying to clear it
                lstMatchesModel.remove(0);
            }
        }

        // Load from file browse: show the file dialog, fill the path field
        else if ("browse".equals(cmd)) {
            File dir = new File(txtLoadFile.getText());
            if (dir.exists()) {
                if (dir.isDirectory())
                    dlgChooser.setCurrentDirectory(dir);
                else
                    dlgChooser.setCurrentDirectory(dir.getParentFile());
            }
            dlgChooser.showOpenDialog(this);
            File f = dlgChooser.getSelectedFile();
            if (f != null)
                txtLoadFile.setText(f.getAbsolutePath());
        }

        // Save to file browse: show the file dialog, fill the path field
        else if ("save-browse".equals(cmd)) {
            File dir = new File(txtSaveFile.getText());
            if (dir.exists()) {
                if (dir.isDirectory())
                    dlgChooser.setCurrentDirectory(dir);
                else
                    dlgChooser.setCurrentDirectory(dir.getParentFile());
            }
            dlgChooser.showSaveDialog(this);
            File f = dlgChooser.getSelectedFile();
            if (f != null)
                txtSaveFile.setText(f.getAbsolutePath());
        }
    }

    /**
     * Enables/disables text fields based on the current selection. This is
     * invoked on the "choice" action, and also from loadFields(), to ensure
     * that the text fields are always in the right state.
     */
    private void enableFields() {
        if (choices.get(Choice.LOCAL).isSelected()) {
            txtLoadFile.setEnabled(false);
            btnLoadBrowse.setEnabled(false);
            chkLockstep.setEnabled(true);
            chkSave.setEnabled(true);
            btnSaveBrowse.setEnabled(true);
            txtSaveFile.setEnabled(true);
            for (JComboBox box : parameters.values())
                box.setEnabled(true);
            lstMatches.setEnabled(true);
            btnAdd.setEnabled(true);
            btnRemove.setEnabled(true);
        } else if (choices.get(Choice.FILE).isSelected()) {
            txtLoadFile.setEnabled(true);
            btnLoadBrowse.setEnabled(true);
            chkLockstep.setEnabled(false);
            chkSave.setEnabled(false);
            btnSaveBrowse.setEnabled(false);
            txtSaveFile.setEnabled(false);
            for (JComboBox box : parameters.values())
                box.setEnabled(false);
            lstMatches.setEnabled(false);
            btnAdd.setEnabled(false);
            btnRemove.setEnabled(false);
        }

        if (chkSave.isSelected() && chkSave.isEnabled()) {
            btnSaveBrowse.setEnabled(true);
            txtSaveFile.setEnabled(true);
        } else {
            btnSaveBrowse.setEnabled(false);
            txtSaveFile.setEnabled(false);
        }
    }

    /**
     * Saves the user's current choices to a file.
     */
    private void saveFields() {
        Properties p = new Properties();

        p.setProperty("choice", getChoice().toString());
        p.setProperty("file", txtLoadFile.getText());
        p.setProperty("save", String.valueOf(chkSave.isSelected()));
        p.setProperty("save-file", txtSaveFile.getText());
        p.setProperty("lockstep", String.valueOf(chkLockstep.isSelected()));
        p.setProperty("lastVersion", popupVersion);
        p.setProperty("disable-zombies", String.valueOf(chkNoZombies.isSelected()));
        
        // Save parameters.
        for (Parameter param : Parameter.values()) {
            String sel = getParameter(param);
            if (sel != null)
                p.setProperty(param.toString(), sel);
        }

        // Save the map queue.
        StringBuilder mapString = new StringBuilder();
        for (String s : getAllMaps()) {
            if (mapString.length() > 0)
                mapString.append(",");
            mapString.append(s);
        }
        p.setProperty("maps", mapString.toString());

        // Attempt to save.
        try {
            FileOutputStream f = new FileOutputStream(fields);
            p.store(f, "ui options");
            f.close();
        } catch (IOException e) {
            Server.warn("couldn't store ui selections: " + e.getMessage());
        }
    }

    /**
     * Restores the user's current choices from a file.
     */
    private void loadFields() {
        Properties p = new Properties();

        // Attempt to load.
        try {
            FileInputStream f = new FileInputStream(fields);
            p.load(f);
            f.close();
        } catch (IOException e) {
        }

        String choice = p.getProperty("choice", Choice.LOCAL.toString());

        choices.get(Choice.valueOf(choice)).setSelected(true);
        txtLoadFile.setText(p.getProperty("file", ""));
        txtSaveFile.setText(p.getProperty("save-file", ""));
        chkSave.setSelected(Boolean.valueOf(p.getProperty("save", "false")));
        chkLockstep.setSelected(
                Boolean.valueOf(p.getProperty("lockstep", "false")));

        // Fill the parameter dropdowns.
        populateParameters();

        // Restore parameter choices.
        for (Parameter param : Parameter.values())
            parameters.get(param).setSelectedItem(
                    p.getProperty(param.toString(), null));

        // Restore map queue.
        String[] maps = p.getProperty("maps", "").split(",");
        for (String map : maps)
            lstMatchesModel.addElement(map);

        // Restore consistent field state.
        enableFields();

        this.popupVersion = p.getProperty("lastVersion", "");
    }

    /**
     * Fills the dropdown match input boxes with available team and map
     * choices. Either does so locally, if local is selected, or uses an RPC
     * to determine the choices available on the remote host.
     */
    private void populateParameters() {
        String[] matchInputs = finder.findMatchInputsLocally();

        if (matchInputs != null) {
            Arrays.sort(matchInputs);

            // Clear dropdowns.
            for (Map.Entry<Parameter, JComboBox> entries : parameters
                    .entrySet())
                entries.getValue().removeAllItems();

            Set<String> items = new HashSet<>();

            // Add items.
            for (String s : matchInputs) {
                if (items.contains(s))
                    continue;
                items.add(s);
                parameters.get(Parameter.TEAM_A).addItem(s);
                parameters.get(Parameter.TEAM_B).addItem(s);
            }

            items.clear();

            for (String map : GameMapIO.getAvailableMaps(Config.getGlobalConfig().get("bc.game.map-path"))) {
                parameters.get(Parameter.MAP).addItem(map);
            }
        }
    }

    /**
     * Gets the user-selected value for the given match parameter
     * (team A, team B, or map).
     *
     * @param type the type of match parameter to get
     * @return the user-selected value of the given parameter
     */
    public String getParameter(Parameter type) {
        Object sel = parameters.get(type).getSelectedItem();
        if (sel != null)
            return sel.toString();
        return null;
    }

    /**
     * Gets an unmodifiable list of the maps the user has selected in the order
     * of selection.
     *
     * @return the user-selected maps in order of selection
     */
    public List<String> getAllMaps() {
        List<String> maps = new LinkedList<>();
        for (Object obj : lstMatchesModel.toArray()) {
            if (obj instanceof String && ((String) obj).length() > 0)
                maps.add((String) obj);
        }
        if (maps.size() == 0)
            return Collections.singletonList(getParameter(Parameter.MAP));
        return Collections.unmodifiableList(maps);
    }

    /**
     * Gets the user's current match type selection.
     *
     * @return the current selection for match option (local, remote, file)
     */
    public Choice getChoice() {

        for (Map.Entry<Choice, JRadioButton> entry : choices.entrySet()) {
            if (entry.getValue().isSelected())
                return entry.getKey();
        }

        assert (false);
        return null;
    }

    /**
     * Gets the user-selected match source (the file path, if the user's
     * match type choice is FILE, or the remote host if the user's match type
     * choice is REMOTE.
     *
     * @return the select match source (file, host) or null if there isn't one
     */
    public String getSource() {
        Choice r = getChoice();

        if (r == Choice.FILE)
            return txtLoadFile.getText();

        return null;
    }

    /**
     * Gets whether or not the user wants to save the match to a file
     *
     * @return true if the user wants to save the match to a file,
     * false otherwise
     */
    public boolean getSaveChoice() {
        return chkSave.isEnabled() && chkSave.isSelected();
    }

    /**
     * Gets whether or not the users wants lockstep mode.
     *
     * @return true if the user wants lockstep mode, false otherwise
     */
    public boolean getLockstepChoice() {
        return chkLockstep.isEnabled() && chkLockstep.isSelected();
    }

    /**
     * Get whether or not to disable zombie spawning
     * 
     * @return true if zombie spawning is disabled
     */
    public boolean getDisableZombies() {
    	return chkNoZombies.isEnabled() && chkNoZombies.isSelected();
    }
    
    /**
     * Gets the user's save file choice, or null if the user doesn't want to
     * save the match to a file.
     *
     * @return the user's save file choice, or null if there isn't one
     */
    public String getSavePath() {
        if (getSaveChoice())
            return txtSaveFile.getText();

        return null;
    }

    public boolean wasCancelPressed() {
        return !okPressed;
    }
}
