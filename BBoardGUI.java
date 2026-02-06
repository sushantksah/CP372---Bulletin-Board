import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class BBoardGUI extends JFrame {

    // Network
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Auto-refresh
    private static final int REFRESH_INTERVAL_MS = 3000; // 3 seconds
    private Timer refreshTimer;

    // UI fields
    private JTextField hostField = new JTextField("localhost");
    private JTextField portField = new JTextField("8080");
    private JButton connectBtn = new JButton("Connect");
    private JButton disconnectBtn = new JButton("Disconnect");
    private VisualPanel visualPanel = new VisualPanel();
    private java.util.List<VisualPanel.NoteView> lastNotes = new ArrayList<>();

    private JLabel greetingLabel = new JLabel("Not connected");

    private JComboBox<String> colorBox = new JComboBox<>();
    private JTextField xField = new JTextField("0");
    private JTextField yField = new JTextField("0");
    private JTextField messageField = new JTextField("");

    private JButton postBtn = new JButton("POST");
    private JButton getBtn = new JButton("GET");
    private JButton getPinsBtn = new JButton("GET PINS");
    private JButton pinBtn = new JButton("PIN");
    private JButton unpinBtn = new JButton("UNPIN");
    private JButton shakeBtn = new JButton("SHAKE");
    private JButton clearBtn = new JButton("CLEAR");

    private JTextField getColorFilterField = new JTextField("");      // optional: color filter
    private JTextField getContainsXField = new JTextField("");        // optional: contains x
    private JTextField getContainsYField = new JTextField("");        // optional: contains y
    private JTextField getRefersToField = new JTextField("");         // optional: refersTo text

    private JTextArea logArea = new JTextArea(18, 60);

    // Parsed greeting info (optional for display/validation)
    private Integer boardW, boardH, noteW, noteH;
    private List<String> validColors = new ArrayList<>();

    public BBoardGUI() {
        super("BBoard Client GUI");
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
        p.add(connectBtn, c);
        c.gridx = 5; c.gridy = 0;
        p.add(disconnectBtn, c);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 6;
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
        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> sendDisconnect());

        postBtn.addActionListener(e -> doPost());
        getBtn.addActionListener(e -> doGet());
        getPinsBtn.addActionListener(e -> sendCommand("GET PINS"));
        pinBtn.addActionListener(e -> doPin());
        unpinBtn.addActionListener(e -> doUnpin());
        shakeBtn.addActionListener(e -> sendCommand("SHAKE"));
        clearBtn.addActionListener(e -> sendCommand("CLEAR"));
    }

    private void connect() {
        if (isConnected()) return;

        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException ex) {
            appendLog("CLIENT: invalid port");
            return;
        }

        appendLog("CLIENT: connecting to " + host + ":" + port + " ...");

        // Connect + read greeting on a background thread so UI doesn’t freeze
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

                String greeting = in.readLine(); // server greeting
                appendLog("SERVER: " + greeting);

                parseGreeting(greeting);
                SwingUtilities.invokeLater(() -> {
                    greetingLabel.setText("Greeting: " + greeting);
                    setConnected(true);
                });

                refreshBoardFromServer();
                startAutoRefresh();

            } catch (Exception ex) {
                appendLog("CLIENT: connect failed: " + ex.getMessage());
                cleanup();
                SwingUtilities.invokeLater(() -> setConnected(false));
            }
        }).start();
    }

    private void parseGreeting(String greeting) {
        boardW = boardH = noteW = noteH = null;
        validColors.clear();

        if (greeting == null) return;
        String[] parts = greeting.trim().split("\\s+");
        if (parts.length < 5) return; // must have dims + at least 1 color

        try {
            boardW = Integer.parseInt(parts[0]);
            boardH = Integer.parseInt(parts[1]);
            noteW  = Integer.parseInt(parts[2]);
            noteH  = Integer.parseInt(parts[3]);

            validColors.addAll(Arrays.asList(parts).subList(4, parts.length));

            SwingUtilities.invokeLater(() -> {
                visualPanel.setBoardConfig(boardW, boardH, noteW, noteH, validColors);
                visualPanel.setNotes(lastNotes);
            });
            SwingUtilities.invokeLater(() -> {
                colorBox.removeAllItems();
                for (String c : validColors) colorBox.addItem(c);
                if (colorBox.getItemCount() > 0) colorBox.setSelectedIndex(0);
            });

        } catch (NumberFormatException ignored) {
            // If greeting format differs, GUI still works for manual commands
        }
    }

    private void doPost() {
        if (!isConnected()) return;

        String x = xField.getText().trim();
        String y = yField.getText().trim();
        String msg = messageField.getText(); // allow spaces
        Object colorObj = colorBox.getSelectedItem();
        String color = (colorObj == null) ? "" : colorObj.toString();

        if (msg == null) msg = "";

        String cmd = "POST " + x + " " + y + " " + color + " " + msg;
        sendCommand(cmd);
        // Note: sendCommand() auto-sends GET on POST success, no need to call again
    }

    private void doGet() {
        if (!isConnected()) return;

        List<String> parts = new ArrayList<>();
        String c = getColorFilterField.getText().trim();
        if (!c.isEmpty()) parts.add("color=" + c);

        String cx = getContainsXField.getText().trim();
        String cy = getContainsYField.getText().trim();
        if (!cx.isEmpty() && !cy.isEmpty()) parts.add("contains=" + cx + " " + cy);
        else if (!cx.isEmpty() || !cy.isEmpty()) {
            appendLog("CLIENT: contains requires both x and y (or leave both blank).");
            return;
        }

        String ref = getRefersToField.getText();
        if (ref != null) ref = ref.trim();
        if (ref != null && !ref.isEmpty()) parts.add("refersTo=" + ref);

        String cmd = parts.isEmpty() ? "GET" : "GET " + String.join(" ", parts);
        sendCommand(cmd);
    }

    private void doPin() {
        if (!isConnected()) return;
        String x = xField.getText().trim();
        String y = yField.getText().trim();
        sendCommand("PIN " + x + " " + y);
    }

    private void doUnpin() {
        if (!isConnected()) return;
        String x = xField.getText().trim();
        String y = yField.getText().trim();
        sendCommand("UNPIN " + x + " " + y);
    }

    private void sendDisconnect() {
        if (!isConnected()) return;
        stopAutoRefresh();
        sendCommand("DISCONNECT");
        // Server may close after OK DISCONNECTING; we also close locally.
        new Thread(() -> {
            try { Thread.sleep(150); } catch (InterruptedException ignored) {}
            cleanup();
            SwingUtilities.invokeLater(() -> {
                setConnected(false);
                greetingLabel.setText("Not connected");
                lastNotes.clear();
                visualPanel.setNotes(lastNotes);
            });
        }).start();
    }

    // ---- Auto-refresh: keeps board in sync with server (sees other clients' changes) ----

    private void startAutoRefresh() {
        stopAutoRefresh(); // safety: cancel any previous timer
        refreshTimer = new Timer(REFRESH_INTERVAL_MS, e -> {
            if (isConnected()) {
                new Thread(() -> {
                    synchronized (socketLock) {
                        refreshBoardFromServer();
                    }
                }).start();
            }
        });
        refreshTimer.setRepeats(true);
        refreshTimer.start();
    }

    private void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
    }

    private void refreshBoardFromServer() {
        try {
            if (!isConnected()) return;
            out.println("GET");
            String first = in.readLine();
            if (first == null) return;

            int count = parseOkCount(first);
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String l = in.readLine();
                if (l == null) break;
                lines.add(l);
            }

            List<VisualPanel.NoteView> parsed = parseNotesFromLines(lines);
            lastNotes = parsed;
            SwingUtilities.invokeLater(() -> visualPanel.setNotes(parsed));
        } catch (Exception ignored) {}
    }

    // Lock object for thread-safe sequential access to the socket
    private final Object socketLock = new Object();

    private void sendCommand(String cmd) {
        if (!isConnected()) return;

        appendLog("CLIENT: " + cmd);

        new Thread(() -> {
            synchronized (socketLock) {
                try {
                    if (!isConnected()) return;
                    out.println(cmd);

                    // Read at least one line response
                    String first = in.readLine();
                    if (first == null) {
                        appendLog("SERVER: <disconnected>");
                        cleanup();
                        SwingUtilities.invokeLater(() -> setConnected(false));
                        return;
                    }
                    appendLog("SERVER: " + first);

                    // If response is "OK <number>", read that many additional lines
                    int extra = parseOkCount(first);
                    List<String> lines = new ArrayList<>();
                    for (int i = 0; i < extra; i++) {
                        String l = in.readLine();
                        if (l == null) break;
                        lines.add(l);
                        appendLog("SERVER: " + l);
                    }

                    // If this was a GET (not GET PINS), render notes
                    if (cmd.equals("GET") || cmd.startsWith("GET ")) {
                        if (!cmd.equals("GET PINS")) {
                            List<VisualPanel.NoteView> parsed = parseNotesFromLines(lines);
                            lastNotes = parsed;
                            SwingUtilities.invokeLater(() -> visualPanel.setNotes(parsed));
                        }
                    }

                    // After a state-changing success, auto-refresh the board
                    if (first.equals("OK NOTE_POSTED") || first.equals("OK PIN_ADDED")
                            || first.equals("OK PIN_REMOVED") || first.equals("OK SHAKE_COMPLETE")
                            || first.equals("OK CLEAR_COMPLETE")) {
                        // Send GET inline (already holding lock)
                        out.println("GET");
                        appendLog("CLIENT: GET");
                        String gFirst = in.readLine();
                        if (gFirst != null) {
                            appendLog("SERVER: " + gFirst);
                            int gExtra = parseOkCount(gFirst);
                            List<String> gLines = new ArrayList<>();
                            for (int i = 0; i < gExtra; i++) {
                                String l = in.readLine();
                                if (l == null) break;
                                gLines.add(l);
                                appendLog("SERVER: " + l);
                            }
                            List<VisualPanel.NoteView> parsed = parseNotesFromLines(gLines);
                            lastNotes = parsed;
                            SwingUtilities.invokeLater(() -> visualPanel.setNotes(parsed));
                        }
                    }

                    if (first.equals("OK DISCONNECTING")) {
                        cleanup();
                        SwingUtilities.invokeLater(() -> setConnected(false));
                    }

                } catch (Exception ex) {
                    appendLog("CLIENT: error: " + ex.getMessage());
                    cleanup();
                    SwingUtilities.invokeLater(() -> setConnected(false));
                }
            }
        }).start();
    }

    private int parseOkCount(String line) {
        // Accept "OK <n>" where n is integer
        // If your server uses different formatting, this safely returns 0.
        try {
            String[] p = line.trim().split("\\s+");
            if (p.length == 2 && p[0].equals("OK")) {
                return Integer.parseInt(p[1]);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && in != null && out != null;
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

    private void cleanup() {
        stopAutoRefresh();
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        out = null; in = null; socket = null;
    }

    private void appendLog(String s) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(s + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BBoardGUI().setVisible(true));
    }

    private List<VisualPanel.NoteView> parseNotesFromLines(List<String> lines) {
        List<VisualPanel.NoteView> out = new ArrayList<>();
    
        for (String l : lines) {
            // Expected format from your Board.get():
            // NOTE x y color message... PINNED=true/false
            if (l == null) continue;
            l = l.trim();
            if (!l.startsWith("NOTE ")) continue;
    
            try {
                // Split first 4 tokens: NOTE x y color
                String[] first4 = l.split("\\s+", 5);
                if (first4.length < 5) continue;
    
                int x = Integer.parseInt(first4[1]);
                int y = Integer.parseInt(first4[2]);
                String color = first4[3];
    
                // remaining contains "message... PINNED=..."
                String rest = first4[4];
    
                boolean pinned = false;
                String message = rest;
    
                int idx = rest.lastIndexOf(" PINNED=");
                if (idx >= 0) {
                    message = rest.substring(0, idx);
                    String pv = rest.substring(idx + " PINNED=".length()).trim();
                    pinned = pv.startsWith("true");
                }
    
                out.add(new VisualPanel.NoteView(x, y, color, message, pinned));
            } catch (Exception ignored) {
            }
        }
    
        return out;
    }

}