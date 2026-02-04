import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * BBoard client GUI (migrated from ClientGUI). Uses BBoardClient as the engine: no direct socket or parsing.
 * Same logic as ClientGUI: connect (with Local test dummy), initializeBoard/updateStatus/displayError/refreshBoard
 * called by BBoardClient; builds raw RFC strings and calls networkClient.sendRequest().
 */
public class BBoardGUI extends JFrame implements BBoardClient.GuiCallback {

    private final BBoardClient networkClient;

    private JTextField hostField = new JTextField("localhost");
    private JTextField portField = new JTextField("4554");
    private JCheckBox localTestCheckBox = new JCheckBox("Local test (no server)", true);
    private JButton connectBtn = new JButton("Connect");
    private JButton disconnectBtn = new JButton("Disconnect");
    private JLabel greetingLabel = new JLabel("Not connected");

    private VisualPanel visualPanel = new VisualPanel();

    private JComboBox<String> colorBox = new JComboBox<>();
    private JTextField xField = new JTextField("0");
    private JTextField yField = new JTextField("0");
    private JTextField messageField = new JTextField("");

    private JButton postBtn = new JButton("POST");
    private JButton getBtn = new JButton("GET");
    private JButton getPinsBtn = new JButton("GET PINS");
    private JTextField pinXField = new JTextField("0", 4);
    private JTextField pinYField = new JTextField("0", 4);
    private JButton pinBtn = new JButton("PIN");
    private JButton unpinBtn = new JButton("UNPIN");
    private JButton shakeBtn = new JButton("SHAKE");
    private JButton clearBtn = new JButton("CLEAR");

    private JTextField getColorFilterField = new JTextField("");
    private JTextField getContainsXField = new JTextField("");
    private JTextField getContainsYField = new JTextField("");
    private JTextField getRefersToField = new JTextField("");

    private JTextArea logArea = new JTextArea(18, 60);

    public BBoardGUI(BBoardClient client) {
        super("BBoard Client GUI");
        this.networkClient = client;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(root, BorderLayout.CENTER);

        root.add(buildConnectionPanel(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildMainPanel(), visualPanel);
        split.setResizeWeight(0.55);
        root.add(split, BorderLayout.CENTER);
        root.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        setConnected(false);
        pack();
        setSize(700, 620);
        setLocationRelativeTo(null);

        wireActions();
    }

    private JPanel buildConnectionPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0;
        p.add(new JLabel("Host:"), c);
        c.gridx = 1; c.gridy = 0; c.weightx = 1.0;
        p.add(hostField, c);

        c.gridx = 2; c.gridy = 0; c.weightx = 0;
        p.add(new JLabel("Port:"), c);
        c.gridx = 3; c.gridy = 0; c.weightx = 0.5;
        p.add(portField, c);

        c.gridx = 4; c.gridy = 0; c.weightx = 0;
        p.add(localTestCheckBox, c);
        c.gridx = 5; c.gridy = 0; c.weightx = 0;
        p.add(connectBtn, c);
        c.gridx = 6; c.gridy = 0;
        p.add(disconnectBtn, c);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 7;
        greetingLabel.setOpaque(true);
        greetingLabel.setBackground(new Color(245, 245, 245));
        greetingLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
        p.add(greetingLabel, c);

        return p;
    }

    private JPanel buildMainPanel() {
        JPanel p = new JPanel(new BorderLayout(10, 10));

        JPanel commands = new JPanel(new GridLayout(1, 2, 10, 10));
        commands.add(buildPostPanel());
        commands.add(buildGetPanel());

        JPanel quick = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        quick.add(new JLabel("PIN/UNPIN x:"));
        quick.add(pinXField);
        quick.add(new JLabel("y:"));
        quick.add(pinYField);
        quick.add(pinBtn);
        quick.add(unpinBtn);
        quick.add(getPinsBtn);
        quick.add(shakeBtn);
        quick.add(clearBtn);

        p.add(commands, BorderLayout.CENTER);
        p.add(quick, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildPostPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("POST a Note"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; p.add(new JLabel("x:"), c);
        c.gridx = 1; c.gridy = 0; p.add(xField, c);

        c.gridx = 0; c.gridy = 1; p.add(new JLabel("y:"), c);
        c.gridx = 1; c.gridy = 1; p.add(yField, c);

        c.gridx = 0; c.gridy = 2; p.add(new JLabel("color:"), c);
        c.gridx = 1; c.gridy = 2; p.add(colorBox, c);

        c.gridx = 0; c.gridy = 3; p.add(new JLabel("message:"), c);
        c.gridx = 1; c.gridy = 3; p.add(messageField, c);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 2;
        p.add(postBtn, c);

        return p;
    }

    private JPanel buildGetPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("GET filters (optional)"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; p.add(new JLabel("color=<c>"), c);
        c.gridx = 1; c.gridy = 0; p.add(getColorFilterField, c);

        c.gridx = 0; c.gridy = 1; p.add(new JLabel("contains=<x> <y>"), c);
        JPanel containsPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        containsPanel.add(getContainsXField);
        containsPanel.add(getContainsYField);
        c.gridx = 1; c.gridy = 1; p.add(containsPanel, c);

        c.gridx = 0; c.gridy = 2; p.add(new JLabel("refersTo=<text>"), c);
        c.gridx = 1; c.gridy = 2; p.add(getRefersToField, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 2;
        p.add(getBtn, c);

        return p;
    }

    private void wireActions() {
        connectBtn.addActionListener(e -> onConnect());
        disconnectBtn.addActionListener(e -> onDisconnect());

        postBtn.addActionListener(e -> doPost());
        getBtn.addActionListener(e -> doGet());
        getPinsBtn.addActionListener(e -> doGetPins());
        pinBtn.addActionListener(e -> doPin());
        unpinBtn.addActionListener(e -> doUnpin());
        shakeBtn.addActionListener(e -> doShake());
        clearBtn.addActionListener(e -> doClear());
    }

    private void onConnect() {
        String host = hostField.getText().trim();
        int port = 4554;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            greetingLabel.setText("Invalid port number.");
            return;
        }
        boolean useDummy = localTestCheckBox.isSelected();
        boolean ok = networkClient.connect(host, port, useDummy);
        if (!ok) {
            return;
        }
        setConnected(true);
        hostField.setEnabled(false);
        portField.setEnabled(false);
        localTestCheckBox.setEnabled(false);
        greetingLabel.setText(networkClient.isUsingDummy() ? "Local test — connected." : "Connected to " + host + ":" + port);
    }

    private void onDisconnect() {
        networkClient.disconnect();
        setConnected(false);
        hostField.setEnabled(true);
        portField.setEnabled(true);
        localTestCheckBox.setEnabled(true);
        greetingLabel.setText("Not connected");
    }

    // --- Called by BBoardClient (same contract as ClientGUI) ---

    /**
     * Called by BBoardClient after handshake. Renders board dimensions and valid colors; does not parse.
     */
    public void initializeBoard(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> validColors) {
        colorBox.removeAllItems();
        for (String c : validColors) colorBox.addItem(c);
        if (colorBox.getItemCount() > 0) colorBox.setSelectedIndex(0);
        visualPanel.setBoardConfig(boardWidth, boardHeight, noteWidth, noteHeight, validColors);
        visualPanel.setNotes(new ArrayList<>());
    }

    /**
     * Called by BBoardClient to show the last command and response in the log.
     */
    public void updateStatus(String command, List<String> responseLines) {
        logArea.append("> " + command + "\n");
        for (String line : responseLines) logArea.append(line + "\n");
        logArea.append("\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Maps raw server error code to a user-friendly message and displays it.
     */
    public void displayError(String errorCode) {
        String message = mapErrorToMessage(errorCode);
        greetingLabel.setText(message);
    }

    private static String mapErrorToMessage(String code) {
        if (code == null) return "An error occurred.";
        switch (code) {
            case "OUT_OF_BOUNDS":
                return "Coordinates are off the board!";
            case "INVALID_FORMAT":
                return "Invalid command format.";
            case "COLOR_NOT_SUPPORTED":
            case "COLOR_NOT_VALID":
                return "That color is not supported.";
            case "COMPLETE_OVERLAP":
                return "A note already exists at that position.";
            case "NO_NOTE_AT_COORDINATE":
                return "No note at that coordinate to pin.";
            case "PIN_NOT_FOUND":
                return "No pin at that coordinate to remove.";
            case "CONNECTION_TIMEOUT":
                return "Connection timed out. Is the server running?";
            case "CONNECTION_REFUSED":
                return "Connection refused. Check host and port.";
            case "CONNECTION_ERROR":
                return "Connection error.";
            default:
                return "Error: " + code;
        }
    }

    /**
     * Called by BBoardClient after a state-changing OK or GET notes. Redraws the visual board.
     */
    public void refreshBoard(List<BBoardClient.NoteData> notes) {
        List<VisualPanel.NoteView> views = new ArrayList<>();
        if (notes != null) {
            for (BBoardClient.NoteData n : notes) {
                views.add(new VisualPanel.NoteView(n.x, n.y, n.color, n.message, n.pinned));
            }
        }
        visualPanel.setNotes(views);
    }

    // --- Command builders: same logic as ClientGUI, delegate to BBoardClient ---

    private void doPost() {
        if (!networkClient.isConnected()) return;

        String xs = xField.getText().trim();
        String ys = yField.getText().trim();
        String msg = messageField.getText();
        Object colorObj = colorBox.getSelectedItem();
        String color = (colorObj == null) ? "" : colorObj.toString();

        if (msg == null) msg = "";
        if (msg.trim().isEmpty()) {
            greetingLabel.setText("Message must be at least 1 character.");
            return;
        }
        if (msg.length() > 256) {
            greetingLabel.setText("Message must be at most 256 characters.");
            return;
        }
        int x, y;
        try {
            x = Integer.parseInt(xs);
            y = Integer.parseInt(ys);
        } catch (NumberFormatException e) {
            greetingLabel.setText("x and y must be integers.");
            return;
        }
        if (x < 0 || y < 0) {
            greetingLabel.setText("Coordinates must be non-negative.");
            return;
        }

        String cmd = "POST " + x + " " + y + " " + color + " " + msg;
        networkClient.sendRequest(cmd);
    }

    private void doGet() {
        if (!networkClient.isConnected()) return;

        List<String> parts = new ArrayList<>();
        String c = getColorFilterField.getText().trim();
        if (!c.isEmpty()) parts.add("color=" + c);

        String cx = getContainsXField.getText().trim();
        String cy = getContainsYField.getText().trim();
        if (!cx.isEmpty() && !cy.isEmpty()) parts.add("contains=" + cx + " " + cy);
        else if (!cx.isEmpty() || !cy.isEmpty()) {
            greetingLabel.setText("contains= requires both x and y (or leave both blank).");
            return;
        }

        String ref = getRefersToField.getText();
        if (ref != null) ref = ref.trim();
        if (ref != null && !ref.isEmpty()) parts.add("refersTo=" + ref);

        String cmd = parts.isEmpty() ? "GET" : "GET " + String.join(" ", parts);
        networkClient.setLastGetNotesCommand(cmd);
        networkClient.sendRequest(cmd);
    }

    private void doGetPins() {
        if (!networkClient.isConnected()) return;
        networkClient.sendRequest("GET PINS");
    }

    private void doPin() {
        if (!networkClient.isConnected()) return;
        String xs = pinXField.getText().trim();
        String ys = pinYField.getText().trim();
        int x, y;
        try {
            x = Integer.parseInt(xs);
            y = Integer.parseInt(ys);
        } catch (NumberFormatException e) {
            greetingLabel.setText("PIN x and y must be integers.");
            return;
        }
        networkClient.sendRequest("PIN " + x + " " + y);
    }

    private void doUnpin() {
        if (!networkClient.isConnected()) return;
        String xs = pinXField.getText().trim();
        String ys = pinYField.getText().trim();
        int x, y;
        try {
            x = Integer.parseInt(xs);
            y = Integer.parseInt(ys);
        } catch (NumberFormatException e) {
            greetingLabel.setText("UNPIN x and y must be integers.");
            return;
        }
        networkClient.sendRequest("UNPIN " + x + " " + y);
    }

    private void doShake() {
        if (!networkClient.isConnected()) return;
        networkClient.sendRequest("SHAKE");
    }

    private void doClear() {
        if (!networkClient.isConnected()) return;
        networkClient.sendRequest("CLEAR");
    }

    private void setConnected(boolean connected) {
        connectBtn.setEnabled(!connected);
        disconnectBtn.setEnabled(connected);

        postBtn.setEnabled(connected);
        getBtn.setEnabled(connected);
        getPinsBtn.setEnabled(connected);
        pinBtn.setEnabled(connected);
        unpinBtn.setEnabled(connected);
        shakeBtn.setEnabled(connected);
        clearBtn.setEnabled(connected);

        hostField.setEnabled(!connected);
        portField.setEnabled(!connected);
    }

    /**
     * Launch with BBoardClient (supports server-style args for dummy).
     * Example: java BBoardClient 4554 200 100 20 10 red white green yellow
     */
    public static void main(String[] args) {
        final BBoardClient client = new BBoardClient();
        if (args.length >= 6) {
            try {
                int port = Integer.parseInt(args[0]);
                int boardW = Integer.parseInt(args[1]);
                int boardH = Integer.parseInt(args[2]);
                int noteW = Integer.parseInt(args[3]);
                int noteH = Integer.parseInt(args[4]);
                List<String> colors = new ArrayList<>();
                for (int i = 5; i < args.length; i++)
                    if (args[i] != null && !args[i].trim().isEmpty()) colors.add(args[i].trim());
                if (colors.isEmpty()) colors.add("red");
                client.setDummyConfig(port, boardW, boardH, noteW, noteH, colors);
            } catch (NumberFormatException e) {
                System.err.println("Invalid args; use: port board_w board_h note_w note_h color1 ... (using defaults)");
            }
        }
        SwingUtilities.invokeLater(() -> {
            BBoardGUI gui = new BBoardGUI(client);
            client.setGui(gui);
            gui.setVisible(true);
        });
    }
}
