package Client;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GUI extends JFrame {

    private BBoardClient client;

    private static final int REFRESH_INTERVAL_MS = 3000;
    private Timer refreshTimer;

    // Connection panel components
    private JTextField hostField = new JTextField("localhost");
    private JTextField portField = new JTextField("8080");
    private JButton connectBtn = new JButton("Connect");
    private JButton disconnectBtn = new JButton("Disconnect");
    private JLabel greetingLabel = new JLabel("Not connected");

    // Visual display
    private VisualPanel visualPanel = new VisualPanel();

    // POST components
    private JTextField xField = new JTextField("0", 4);
    private JTextField yField = new JTextField("0", 4);
    private JComboBox<String> postColorBox = new JComboBox<>();
    private JTextField messageField = new JTextField("");
    private JButton postBtn = new JButton("POST");

    // GET components
    private JComboBox<String> getColorBox = new JComboBox<>();
    private JTextField getContainsXField = new JTextField("", 4);
    private JTextField getContainsYField = new JTextField("", 4);
    private JTextField getRefersToField = new JTextField("", 6);
    private JButton getBtn = new JButton("GET");
    private JButton getPinsBtn = new JButton("GET PINS");

    // PIN/UNPIN components
    private JTextField pinXField = new JTextField("0", 4);
    private JTextField pinYField = new JTextField("0", 4);
    private JButton pinBtn = new JButton("PIN");
    private JButton unpinBtn = new JButton("UNPIN");

    // SHAKE/CLEAR components
    private JButton shakeBtn = new JButton("SHAKE");
    private JButton clearBtn = new JButton("CLEAR");

    // Log area
    private JTextArea logArea = new JTextArea(18, 60);

    // Board configuration
    // private Integer boardW, boardH, noteW, noteH;
    // private List<String> validColors = new ArrayList<>();

    // Prevents logging refresh commands
    private volatile boolean suppressLogging = false; 

    // Constructor
    public GUI() {
        super("Bulletin Board Client");

        client = new BBoardClient();
        client.setGui(new GuiCallbackImpl());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(root, BorderLayout.CENTER);

        root.add(buildConnectionPanel(), BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildCommandPanel(),
                visualPanel);
        split.setResizeWeight(0.55);
        root.add(split, BorderLayout.CENTER);
        root.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        wireActions();
        setConnected(false);
        pack();
        setLocationRelativeTo(null);
    }

    /* Build UI */
    private JPanel buildConnectionPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("Host:"), c);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 1.0;
        p.add(hostField, c);

        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        p.add(new JLabel("Port:"), c);
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 0.5;
        p.add(portField, c);

        c.gridx = 4;
        c.gridy = 0;
        c.weightx = 0;
        p.add(connectBtn, c);
        c.gridx = 5;
        c.gridy = 0;
        p.add(disconnectBtn, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 6;
        greetingLabel.setOpaque(true);
        greetingLabel.setBackground(new Color(245, 245, 245));
        greetingLabel.setBorder(new EmptyBorder(6, 8, 6, 8));
        p.add(greetingLabel, c);

        return p;
    }

    private JPanel buildCommandPanel() {
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

        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("x:"), c);
        c.gridx = 1;
        c.gridy = 0;
        p.add(xField, c);

        c.gridx = 0;
        c.gridy = 1;
        p.add(new JLabel("y:"), c);
        c.gridx = 1;
        c.gridy = 1;
        p.add(yField, c);

        c.gridx = 0;
        c.gridy = 2;
        p.add(new JLabel("color:"), c);
        c.gridx = 1;
        c.gridy = 2;
        p.add(postColorBox, c);

        c.gridx = 0;
        c.gridy = 3;
        p.add(new JLabel("message:"), c);
        c.gridx = 1;
        c.gridy = 3;
        p.add(messageField, c);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        p.add(postBtn, c);

        return p;
    }

    private JPanel buildGetPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder("GET filters (optional)"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        p.add(new JLabel("color=<c>"), c);
        c.gridx = 1;
        c.gridy = 0;
        p.add(getColorBox, c);

        c.gridx = 0;
        c.gridy = 1;
        p.add(new JLabel("contains=<x> <y>"), c);
        JPanel containsPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        containsPanel.add(getContainsXField);
        containsPanel.add(getContainsYField);
        c.gridx = 1;
        c.gridy = 1;
        p.add(containsPanel, c);

        c.gridx = 0;
        c.gridy = 2;
        p.add(new JLabel("refersTo=<text>"), c);
        c.gridx = 1;
        c.gridy = 2;
        p.add(getRefersToField, c);

        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        p.add(getBtn, c);

        return p;
    }

    private void wireActions() {
        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());

        postBtn.addActionListener(e -> doPost());
        getBtn.addActionListener(e -> doGet());
        getPinsBtn.addActionListener(e -> sendCommand("GET PINS"));
        pinBtn.addActionListener(e -> doPin());
        unpinBtn.addActionListener(e -> doUnpin());
        shakeBtn.addActionListener(e -> sendCommand("SHAKE"));
        clearBtn.addActionListener(e -> sendCommand("CLEAR"));
    }

    /* Connection methods - delegated to BBoardClient */
    private void connect() {
        if (client.isConnected()) {
            appendLog("CLIENT: already connected");
            return;
        }

        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            appendLog("CLIENT: invalid port number");
            return;
        }

        appendLog("CLIENT: connecting to " + host + ":" + port + " ...");

        new Thread(() -> {
            boolean success = client.connect(host, port);
            SwingUtilities.invokeLater(() -> {
                if (success) {
                    appendLog("CLIENT: Connected successfully");
                    setConnected(true);
                    startAutoRefresh();
                } else {
                    appendLog("CLIENT: Connection failed");
                    setConnected(false);
                }
            });
        }).start();
    }

    // Disconnect from the server
    private void disconnect() {
        if (!client.isConnected())
            return;

        stopAutoRefresh();
        appendLog("CLIENT: Disconnecting...");

        new Thread(() -> {
            client.disconnect();
            SwingUtilities.invokeLater(() -> {
                // reset UI
                setConnected(false);
                greetingLabel.setText("Not connected");
                visualPanel.setNotes(new ArrayList<>());
                appendLog("CLIENT: Disconnected");
            });
        }).start();
    }

    /* Command methods */
    // Send a POST command with user input
    private void doPost() {
        if (!client.isConnected())
            return;

        String x = xField.getText().trim();
        String y = yField.getText().trim();
        String msg = messageField.getText();
        Object colorObj = postColorBox.getSelectedItem();
        String color = (colorObj == null) ? "" : colorObj.toString();

        if (msg == null)
            msg = "";

        String cmd = "POST " + x + " " + y + " " + color + " " + msg;
        sendCommand(cmd);
    }

    // Send a GET command
    private void doGet() {
        if (!client.isConnected())
            return;

        List<String> parts = new ArrayList<>();

        // Get color filter
        Object cObj = getColorBox.getSelectedItem();
        if (cObj != null) {
            String c = cObj.toString().trim();
            if (!c.isEmpty()) {
                parts.add("color=" + c);
            }
        }

        // Get contains filter
        String cx = getContainsXField.getText().trim();
        String cy = getContainsYField.getText().trim();
        if (!cx.isEmpty() && !cy.isEmpty()) {
            parts.add("contains=" + cx + " " + cy);
        } else if (!cx.isEmpty() || !cy.isEmpty()) {
            appendLog("CLIENT: contains requires both x and y, or neither");
            return;
        }

        // Get refersTo filter
        String ref = getRefersToField.getText();
        if (ref != null)
            ref = ref.trim();
        if (ref != null && !ref.isEmpty()) {
            parts.add("refersTo=" + ref);
        }

        String cmd = parts.isEmpty() ? "GET" : "GET " + String.join(" ", parts);
        client.setLastGetNotesCommand(cmd); // this command is stored for auto refresh
        sendCommand(cmd);
    }

    private void doPin() {
        if (!client.isConnected())
            return;

        String xs = pinXField.getText().trim();
        String ys = pinYField.getText().trim();

        if (xs.isEmpty() || ys.isEmpty()) {
            appendLog("CLIENT: enter x and y for PIN");
            return;
        }

        try {
            int x = Integer.parseInt(xs);
            int y = Integer.parseInt(ys);
            sendCommand("PIN " + x + " " + y);
        } catch (NumberFormatException e) {
            appendLog("CLIENT: x and y must be integers for PIN");
        }
    }

    // Unpin a note
    private void doUnpin() {
        if (!client.isConnected())
            return;

        String xs = pinXField.getText().trim();
        String ys = pinYField.getText().trim();

        if (xs.isEmpty() || ys.isEmpty()) {
            appendLog("CLIENT: enter x and y for UNPIN");
            return;
        }

        try {
            int x = Integer.parseInt(xs);
            int y = Integer.parseInt(ys);
            sendCommand("UNPIN " + x + " " + y);
        } catch (NumberFormatException e) {
            appendLog("CLIENT: x and y must be integers for UNPIN");
        }
    }

    // Send a command to the server
    private void sendCommand(String cmd) {
        if (!client.isConnected())
            return;

        appendLog("CLIENT: " + cmd);

        new Thread(() -> {
            client.sendRequest(cmd);
        }).start();
    }

    /* Auto refresh methods */
    // Start auto refresh timer
    private void startAutoRefresh() {
        stopAutoRefresh(); 
        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> {
            if (client.isConnected()) {
                new Thread(() -> {
                    suppressLogging = true;  
                    client.sendRequest("GET");
                    suppressLogging = false;
                }).start();
            }
        });
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }

    // Stop auto refresh timer
    private void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }

    /* UI state management */
    // Set the connected state
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

    // Add message to the log
    private void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /* Callback */
    // Callback
    private class GuiCallbackImpl implements BBoardClient.GuiCallback {

        @Override
        public void initializeBoard(int boardWidth, int boardHeight, int noteWidth,
                int noteHeight, List<String> colors) {
            SwingUtilities.invokeLater(() -> {
        
                // boardW = boardWidth;
                // boardH = boardHeight;
                // noteW = noteWidth;
                // noteH = noteHeight;
                // validColors = new ArrayList<>(colors);

                visualPanel.setBoardConfig(boardWidth, boardHeight, noteWidth, noteHeight, colors);
                postColorBox.removeAllItems();
                getColorBox.removeAllItems();

                for (String col : colors) {
                    postColorBox.addItem(col);
                }

                getColorBox.addItem(""); // blank option for "no filter"
                for (String col : colors) {
                    getColorBox.addItem(col);
                }

                if (postColorBox.getItemCount() > 0) {
                    postColorBox.setSelectedIndex(0);
                }
                getColorBox.setSelectedIndex(0);

                // Update greeting label
                greetingLabel.setText(String.format("Board: %dx%d  Note: %dx%d  Colors: %s",
                        boardWidth, boardHeight, noteWidth, noteHeight, String.join(", ", colors)));

                appendLog("CLIENT: Board initialized - " + boardWidth + "x" + boardHeight);
            });
        }
        // Log all response lines
        @Override
        public void updateStatus(String command, List<String> responseLines) {
            if (suppressLogging) return;
            SwingUtilities.invokeLater(() -> {   
                for (String line : responseLines) {
                    
                    appendLog("SERVER: " + line);
                }
            });
        }

        @Override
        public void displayError(String errorCode) {
            SwingUtilities.invokeLater(() -> {
                appendLog("ERROR: " + errorCode);

                // Optionally show a dialog for critical errors
                if (errorCode.equals("CONNECTION_ERROR") ||
                        errorCode.equals("CONNECTION_REFUSED") ||
                        errorCode.equals("CONNECTION_TIMEOUT")) {
                    JOptionPane.showMessageDialog(
                            GUI.this,
                            "Connection error: " + errorCode,
                            "Connection Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
        }

        // Refresh the board
        @Override
        public void refreshBoard(List<BBoardClient.NoteData> notes) {
            SwingUtilities.invokeLater(() -> {
                List<VisualPanel.NoteView> views = new ArrayList<>();
                for (BBoardClient.NoteData note : notes) {
                    views.add(new VisualPanel.NoteView(
                            note.x, note.y, note.color, note.message, note.pinned));
                }

                // Update
                visualPanel.setNotes(views);
            });
        }
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GUI gui = new GUI();
            gui.setVisible(true);
        });
    }
}