import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * BBoard client interface. User interacts here; no parsing of greeting or responses.
 * Builds RFC-compliant command strings and calls BBoardClient.sendRequest().
 * Receives: initializeBoard(w, h, ...) to render board size and colors; updateStatus() to show responses;
 * displayError(code) to show user-friendly errors; refreshBoard(notes) to redraw the visual bulletin board.
 */
public class ClientGUI extends JFrame implements BBoardClient.GuiCallback {

    private final BBoardClient networkClient;

    private int boardWidth = 100, boardHeight = 80, noteWidth = 15, noteHeight = 10;
    private BoardCanvas boardCanvas;

    private JTextField hostField;
    private JTextField portField;
    private JCheckBox localTestCheckBox;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JTextField postX, postY;
    private JComboBox<String> colorCombo;
    private JTextField postMessage;
    private JButton postBtn;
    private JCheckBox getColorCheck;
    private JComboBox<String> getColorCombo;
    private JCheckBox getContainsCheck;
    private JTextField getContainsX, getContainsY;
    private JCheckBox getRefersCheck;
    private JTextField getRefersTo;
    private JButton getNotesBtn;
    private JButton getPinsBtn;
    private JTextField pinX, pinY;
    private JButton pinBtn;
    private JButton unpinBtn;
    private JButton shakeBtn;
    private JButton clearBtn;
    private JTextArea outputArea;
    private JLabel statusBar;
    private JPanel boardNotesPanel;
    private JScrollPane boardNotesScroll;

    public ClientGUI(BBoardClient client) {
        this.networkClient = client;
        setTitle("BBoard Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 620);
        setLayout(new BorderLayout(8, 8));
        ((JPanel) getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        top.setBorder(new TitledBorder("Connection"));
        hostField = new JTextField("localhost", 10);
        portField = new JTextField("4554", 5);
        localTestCheckBox = new JCheckBox("Local test (no server)", true);
        connectBtn = new JButton("Connect");
        disconnectBtn = new JButton("Disconnect");
        disconnectBtn.setEnabled(false);
        top.add(new JLabel("Host:"));
        top.add(hostField);
        top.add(new JLabel("Port:"));
        top.add(portField);
        top.add(localTestCheckBox);
        top.add(connectBtn);
        top.add(disconnectBtn);
        add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(6, 6));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JPanel postPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        postPanel.setBorder(new TitledBorder("POST note"));
        postX = new JTextField(4);
        postY = new JTextField(4);
        colorCombo = new JComboBox<>(new String[] { "red", "white", "green", "yellow" });
        postMessage = new JTextField(20);
        postMessage.setToolTipText("1–256 characters");
        postBtn = new JButton("POST");
        postPanel.add(new JLabel("x:"));
        postPanel.add(postX);
        postPanel.add(new JLabel("y:"));
        postPanel.add(postY);
        postPanel.add(new JLabel("color:"));
        postPanel.add(colorCombo);
        postPanel.add(new JLabel("message:"));
        postPanel.add(postMessage);
        postPanel.add(postBtn);
        left.add(postPanel);

        JPanel getPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        getPanel.setBorder(new TitledBorder("GET"));
        getColorCheck = new JCheckBox("color=");
        getColorCombo = new JComboBox<>(new String[] { "red", "white", "green", "yellow" });
        getContainsCheck = new JCheckBox("contains=");
        getContainsX = new JTextField(3);
        getContainsY = new JTextField(3);
        getRefersCheck = new JCheckBox("refersTo=");
        getRefersTo = new JTextField(12);
        getNotesBtn = new JButton("GET notes");
        getPinsBtn = new JButton("GET PINS");
        getPanel.add(getColorCheck);
        getPanel.add(getColorCombo);
        getPanel.add(getContainsCheck);
        getPanel.add(getContainsX);
        getPanel.add(getContainsY);
        getPanel.add(getRefersCheck);
        getPanel.add(getRefersTo);
        getPanel.add(getNotesBtn);
        getPanel.add(getPinsBtn);
        left.add(getPanel);

        JPanel pinPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        pinPanel.setBorder(new TitledBorder("PIN / UNPIN"));
        pinX = new JTextField(4);
        pinY = new JTextField(4);
        pinBtn = new JButton("PIN");
        unpinBtn = new JButton("UNPIN");
        pinPanel.add(new JLabel("x:"));
        pinPanel.add(pinX);
        pinPanel.add(new JLabel("y:"));
        pinPanel.add(pinY);
        pinPanel.add(pinBtn);
        pinPanel.add(unpinBtn);
        left.add(pinPanel);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        actionPanel.setBorder(new TitledBorder("Board actions"));
        shakeBtn = new JButton("SHAKE");
        clearBtn = new JButton("CLEAR");
        actionPanel.add(shakeBtn);
        actionPanel.add(clearBtn);
        left.add(actionPanel);

        center.add(left, BorderLayout.WEST);

        outputArea = new JTextArea(10, 28);
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        center.add(new JScrollPane(outputArea), BorderLayout.CENTER);
        center.setBorder(new TitledBorder("Output / responses"));

        boardCanvas = new BoardCanvas();
        boardCanvas.setPreferredSize(new Dimension(400, 320));
        boardCanvas.setBorder(new TitledBorder("Bulletin board"));
        JScrollPane canvasScroll = new JScrollPane(boardCanvas);

        boardNotesPanel = new JPanel();
        boardNotesPanel.setLayout(new BoxLayout(boardNotesPanel, BoxLayout.Y_AXIS));
        boardNotesScroll = new JScrollPane(boardNotesPanel);
        boardNotesScroll.setBorder(new TitledBorder("Notes list"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, canvasScroll, center);
        split.setResizeWeight(0.5);
        JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, split, boardNotesScroll);
        rightSplit.setResizeWeight(0.7);
        add(rightSplit, BorderLayout.CENTER);

        statusBar = new JLabel("Disconnected. Use \"Local test\" to run without a server.");
        statusBar.setBorder(new EmptyBorder(2, 4, 2, 4));
        add(statusBar, BorderLayout.SOUTH);

        setCommandPanelEnabled(false);

        connectBtn.addActionListener(e -> onConnect());
        disconnectBtn.addActionListener(e -> onDisconnect());
        postBtn.addActionListener(e -> onPostAction());
        getNotesBtn.addActionListener(e -> onGetAction());
        getPinsBtn.addActionListener(e -> onGetPinsAction());
        pinBtn.addActionListener(e -> onPinAction());
        unpinBtn.addActionListener(e -> onUnpinAction());
        shakeBtn.addActionListener(e -> onShakeAction());
        clearBtn.addActionListener(e -> onClearAction());
    }

    /**
     * Called by BBoardClient after handshake. Renders board dimensions and valid colors; does not parse.
     */
    public void initializeBoard(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> validColors) {
        this.boardWidth = Math.max(1, boardWidth);
        this.boardHeight = Math.max(1, boardHeight);
        this.noteWidth = Math.max(1, noteWidth);
        this.noteHeight = Math.max(1, noteHeight);
        colorCombo.setModel(new DefaultComboBoxModel<>(validColors.toArray(new String[0])));
        getColorCombo.setModel(new DefaultComboBoxModel<>(validColors.toArray(new String[0])));
        boardCanvas.setDimensions(this.boardWidth, this.boardHeight, this.noteWidth, this.noteHeight);
        boardCanvas.setNotes(new ArrayList<>());
        boardCanvas.repaint();
    }

    /**
     * Called by BBoardClient to show the last command and response in the output area.
     */
    public void updateStatus(String command, List<String> responseLines) {
        outputArea.append("> " + command + "\n");
        for (String line : responseLines) outputArea.append(line + "\n");
        outputArea.append("\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }

    /**
     * Maps raw server error code to a user-friendly message and displays it (status bar).
     */
    public void displayError(String errorCode) {
        String message = mapErrorToMessage(errorCode);
        statusBar.setText(message);
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
     * Called by BBoardClient after a state-changing OK or GET notes. Redraws the visual board and the notes list.
     */
    public void refreshBoard(List<BBoardClient.NoteData> notes) {
        List<BBoardClient.NoteData> copy = notes != null ? new ArrayList<>(notes) : new ArrayList<>();
        boardCanvas.setNotes(copy);
        boardCanvas.repaint();
        boardNotesPanel.removeAll();
        for (BBoardClient.NoteData n : copy) {
            JLabel l = new JLabel(String.format("(%d,%d) %s: %s %s", n.x, n.y, n.color, n.message, n.pinned ? "[pinned]" : ""));
            l.setBorder(new EmptyBorder(2, 4, 2, 4));
            boardNotesPanel.add(l);
        }
        boardNotesPanel.revalidate();
        boardNotesPanel.repaint();
    }

    /**
     * Maps server color name (string from greeting/response) to java.awt.Color for drawing.
     * Server sends e.g. "red", "white"; we use this to fill note rectangles and draw borders.
     */
    public static Color parseColor(String name) {
        if (name == null || name.isEmpty()) return Color.GRAY;
        switch (name.toLowerCase().trim()) {
            case "red":    return Color.RED;
            case "white":  return Color.WHITE;
            case "green":  return Color.GREEN;
            case "yellow": return Color.YELLOW;
            case "blue":   return Color.BLUE;
            case "black":  return Color.BLACK;
            case "gray":
            case "grey":   return Color.GRAY;
            case "orange": return Color.ORANGE;
            case "pink":   return Color.PINK;
            case "cyan":   return Color.CYAN;
            case "magenta":return Color.MAGENTA;
            case "lightgray":
            case "lightgrey": return Color.LIGHT_GRAY;
            case "darkgray":
            case "darkgrey": return Color.DARK_GRAY;
            default:       return Color.GRAY;
        }
    }

    /** Panel that draws the bulletin board and notes as colored rectangles with text. */
    private static class BoardCanvas extends JPanel {
        private int boardW = 100, boardH = 80, noteW = 15, noteH = 10;
        private List<BBoardClient.NoteData> notes = new ArrayList<>();

        void setDimensions(int boardWidth, int boardHeight, int noteWidth, int noteHeight) {
            this.boardW = Math.max(1, boardWidth);
            this.boardH = Math.max(1, boardHeight);
            this.noteW = Math.max(1, noteWidth);
            this.noteH = Math.max(1, noteHeight);
        }

        void setNotes(List<BBoardClient.NoteData> notes) {
            this.notes = notes != null ? notes : new ArrayList<>();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();
            if (boardW <= 0 || boardH <= 0) return;
            double scaleX = (double) w / boardW;
            double scaleY = (double) h / boardH;
            g2.setColor(new Color(240, 240, 240));
            g2.fillRect(0, 0, w, h);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(0, 0, w - 1, h - 1);
            for (BBoardClient.NoteData n : notes) {
                int px = (int) (n.x * scaleX);
                int py = (int) (n.y * scaleY);
                int pw = Math.max(2, (int) (noteW * scaleX));
                int ph = Math.max(2, (int) (noteH * scaleY));
                Color fill = ClientGUI.parseColor(n.color);
                g2.setColor(fill);
                g2.fillRect(px, py, pw, ph);
                g2.setColor(Color.BLACK);
                g2.drawRect(px, py, pw, ph);
                if (n.pinned) {
                    g2.setColor(Color.BLACK);
                    g2.fillOval(px + pw / 2 - 3, py + ph / 2 - 3, 6, 6);
                }
                g2.setColor(Color.BLACK);
                g2.setFont(getFont().deriveFont(10f));
                FontMetrics fm = g2.getFontMetrics();
                String msg = n.message != null ? n.message : "";
                if (msg.length() > 20) msg = msg.substring(0, 17) + "...";
                int tw = fm.stringWidth(msg);
                int th = fm.getAscent();
                g2.drawString(msg, px + Math.max(0, (pw - tw) / 2), py + (ph + th) / 2 - 2);
            }
        }
    }

    private void onConnect() {
        String host = hostField.getText().trim();
        int port = 4554;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            statusBar.setText("Invalid port number.");
            return;
        }
        boolean useDummy = localTestCheckBox.isSelected();
        boolean ok = networkClient.connect(host, port, useDummy);
        if (!ok) {
            return;
        }
        setCommandPanelEnabled(true);
        connectBtn.setEnabled(false);
        disconnectBtn.setEnabled(true);
        hostField.setEnabled(false);
        portField.setEnabled(false);
        localTestCheckBox.setEnabled(false);
        statusBar.setText(networkClient.isUsingDummy() ? "Local test — connected." : "Connected to " + host + ":" + port);
    }

    private void onDisconnect() {
        networkClient.disconnect();
        setCommandPanelEnabled(false);
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(true);
        hostField.setEnabled(true);
        portField.setEnabled(true);
        localTestCheckBox.setEnabled(true);
        statusBar.setText("Disconnected.");
    }

    private void setCommandPanelEnabled(boolean enabled) {
        postBtn.setEnabled(enabled);
        getNotesBtn.setEnabled(enabled);
        getPinsBtn.setEnabled(enabled);
        pinBtn.setEnabled(enabled);
        unpinBtn.setEnabled(enabled);
        shakeBtn.setEnabled(enabled);
        clearBtn.setEnabled(enabled);
    }

    private void onPostAction() {
        if (!networkClient.isConnected()) return;
        String xs = postX.getText().trim();
        String ys = postY.getText().trim();
        String color = (String) colorCombo.getSelectedItem();
        String message = postMessage.getText();
        if (message == null) message = "";
        if (message.trim().isEmpty()) {
            statusBar.setText("Message must be at least 1 character.");
            return;
        }
        if (message.length() > 256) {
            statusBar.setText("Message must be at most 256 characters.");
            return;
        }
        int x, y;
        try {
            x = Integer.parseInt(xs);
            y = Integer.parseInt(ys);
        } catch (NumberFormatException e) {
            statusBar.setText("x and y must be integers.");
            return;
        }
        if (x < 0 || y < 0) {
            statusBar.setText("Coordinates must be non-negative.");
            return;
        }
        String cmd = "POST " + x + " " + y + " " + color + " " + message;
        networkClient.sendRequest(cmd);
    }

    private void onGetAction() {
        if (!networkClient.isConnected()) return;
        StringBuilder cmd = new StringBuilder("GET");
        if (getColorCheck.isSelected()) cmd.append(" color=").append(getColorCombo.getSelectedItem());
        if (getContainsCheck.isSelected()) {
            try {
                int cx = Integer.parseInt(getContainsX.getText().trim());
                int cy = Integer.parseInt(getContainsY.getText().trim());
                cmd.append(" contains=").append(cx).append(" ").append(cy);
            } catch (NumberFormatException e) {
                statusBar.setText("contains= requires two integers.");
                return;
            }
        }
        if (getRefersCheck.isSelected()) {
            String r = getRefersTo.getText();
            if (r != null && !r.trim().isEmpty()) cmd.append(" refersTo=").append(r.trim());
        }
        String command = cmd.toString();
        networkClient.setLastGetNotesCommand(command);
        networkClient.sendRequest(command);
    }

    private void onGetPinsAction() {
        if (!networkClient.isConnected()) return;
        networkClient.sendRequest("GET PINS");
    }

    private void onPinAction() {
        if (!networkClient.isConnected()) return;
        int x, y;
        try {
            x = Integer.parseInt(pinX.getText().trim());
            y = Integer.parseInt(pinY.getText().trim());
        } catch (NumberFormatException e) {
            statusBar.setText("x and y must be integers.");
            return;
        }
        networkClient.sendRequest("PIN " + x + " " + y);
    }

    private void onUnpinAction() {
        if (!networkClient.isConnected()) return;
        int x, y;
        try {
            x = Integer.parseInt(pinX.getText().trim());
            y = Integer.parseInt(pinY.getText().trim());
        } catch (NumberFormatException e) {
            statusBar.setText("x and y must be integers.");
            return;
        }
        networkClient.sendRequest("UNPIN " + x + " " + y);
    }

    private void onShakeAction() {
        if (!networkClient.isConnected()) return;
        networkClient.sendRequest("SHAKE");
    }

    private void onClearAction() {
        if (!networkClient.isConnected()) return;
        networkClient.sendRequest("CLEAR");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BBoardClient client = new BBoardClient();
            ClientGUI gui = new ClientGUI(client);
            client.setGui(gui);
            gui.setVisible(true);
        });
    }
}
