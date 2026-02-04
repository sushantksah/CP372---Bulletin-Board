import java.io.*;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

/**
 * BBoard client engine. Converts GUI actions into RFC-compliant strings, sends them to the server,
 * parses the greeting and responses, and drives GUI updates (initializeBoard, updateStatus, displayError, refreshBoard).
 *
 * Data flow:
 * - connect(ip, port) → socket connect with timeout → handleHandshake() parses greeting → gui.initializeBoard(w, h, ...).
 * - GUI builds raw command strings and calls sendRequest(cmd) → client writes to server, reads response →
 *   on ERROR calls gui.displayError(code); on OK for state-changing commands does auto GET and gui.refreshBoard(notes);
 *   always calls gui.updateStatus(response).
 */
public class BBoardClient {

    /** Callback interface so either ClientGUI or BBoardGUI can be used. */
    public interface GuiCallback {
        void initializeBoard(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> validColors);
        void updateStatus(String command, List<String> responseLines);
        void displayError(String errorCode);
        void refreshBoard(List<NoteData> notes);
    }

    public static final boolean USE_SERVER = false;

    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int SO_TIMEOUT_MS = 10000;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private GuiCallback gui;

    private int boardWidth;
    private int boardHeight;
    private int noteWidth;
    private int noteHeight;
    private List<String> validColors = new ArrayList<>();

    /** Last GET notes command (with filters) for auto-refresh after POST/PIN/UNPIN/SHAKE/CLEAR. */
    private String lastGetNotesCommand = "GET";

    /** Connection for dummy (local test); null when using real server. */
    private Connection dummyConnection;

    /** Optional dummy config (same as server args): used when connecting in local-test mode. */
    private int dummyBoardW = 100, dummyBoardH = 80, dummyNoteW = 15, dummyNoteH = 10;
    private List<String> dummyColors = Arrays.asList("red", "white", "green", "yellow");

    public void setGui(GuiCallback gui) {
        this.gui = gui;
    }

    /**
     * Set dummy (local test) board config to simulate server args: port is ignored, rest match server.
     * Example: setDummyConfig(4554, 200, 100, 20, 10, Arrays.asList("red", "white", "green", "yellow")).
     */
    public void setDummyConfig(int port, int boardW, int boardH, int noteW, int noteH, List<String> colors) {
        this.dummyBoardW = Math.max(1, boardW);
        this.dummyBoardH = Math.max(1, boardH);
        this.dummyNoteW = Math.max(1, noteW);
        this.dummyNoteH = Math.max(1, noteH);
        this.dummyColors = colors != null && !colors.isEmpty() ? new ArrayList<>(colors) : Arrays.asList("red", "white", "green", "yellow");
    }

    /**
     * Connect to server (or dummy). Uses socket.connect with timeout.
     * On success, parses greeting and calls gui.initializeBoard(...). GUI does not parse the greeting.
     */
    @SuppressWarnings("unused") // server branch used when USE_SERVER is true
    public boolean connect(String ip, int port, boolean useDummy) {
        disconnect();
        if (useDummy || !USE_SERVER) {
            dummyConnection = new DummyConnection(dummyBoardW, dummyBoardH, dummyNoteW, dummyNoteH, new ArrayList<>(dummyColors));
            if (!dummyConnection.connect(ip, port)) {
                dummyConnection = null;
                return false;
            }
            String greeting = dummyConnection.getGreeting();
            if (greeting == null || !handleHandshake(greeting)) {
                if (dummyConnection != null) dummyConnection.disconnect();
                dummyConnection = null;
                return false;
            }
            return true;
        }
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(SO_TIMEOUT_MS);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
            String greeting = in.readLine();
            if (greeting == null || !handleHandshake(greeting)) {
                closeSocket();
                return false;
            }
            return true;
        } catch (SocketTimeoutException e) {
            if (gui != null) gui.displayError("CONNECTION_TIMEOUT");
            return false;
        } catch (ConnectException e) {
            if (gui != null) gui.displayError("CONNECTION_REFUSED");
            return false;
        } catch (IOException e) {
            if (gui != null) gui.displayError("CONNECTION_ERROR");
            return false;
        }
    }

    /**
     * Parse the greeting line (boardW boardH noteW noteH color1 color2 ...) and call gui.initializeBoard.
     * Returns true if parsing succeeded.
     */
    private boolean handleHandshake(String greetingLine) {
        if (greetingLine == null || greetingLine.isEmpty()) return false;
        String[] parts = greetingLine.trim().split("\\s+");
        if (parts.length < 5) return false;
        try {
            boardWidth = Integer.parseInt(parts[0]);
            boardHeight = Integer.parseInt(parts[1]);
            noteWidth = Integer.parseInt(parts[2]);
            noteHeight = Integer.parseInt(parts[3]);
            validColors.clear();
            for (int i = 4; i < parts.length; i++)
                if (!parts[i].isEmpty()) validColors.add(parts[i]);
            if (validColors.isEmpty()) return false;
            if (gui != null) gui.initializeBoard(boardWidth, boardHeight, noteWidth, noteHeight, new ArrayList<>(validColors));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void disconnect() {
        if (dummyConnection != null) {
            try { dummyConnection.sendCommand("DISCONNECT"); } catch (Exception ignored) {}
            dummyConnection.disconnect();
            dummyConnection = null;
        }
        closeSocket();
    }

    private void closeSocket() {
        try {
            if (out != null) out.println("DISCONNECT");
        } catch (Exception ignored) {}
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
        socket = null;
        in = null;
        out = null;
    }

    public boolean isConnected() {
        if (dummyConnection != null) return dummyConnection.isConnected();
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public boolean isUsingDummy() {
        return dummyConnection != null;
    }

    /** Set the current GET notes command (used for auto-refresh). Call this when user sends a GET with filters. */
    public void setLastGetNotesCommand(String command) {
        if (command != null && command.trim().toUpperCase().startsWith("GET") && !command.trim().toUpperCase().contains("GET PINS"))
            lastGetNotesCommand = command.trim();
    }

    /**
     * Send raw RFC command to the server. Reads response, then:
     * - On ERROR: calls gui.displayError(code).
     * - On OK for state-changing commands: sends GET (with last filters), parses NOTE lines, calls gui.refreshBoard(notes).
     * - Always calls gui.updateStatus(responseLines).
     */
    public void sendRequest(String command) {
        List<String> response = sendRequestRaw(command);
        if (response.isEmpty()) return;
        if (gui != null) gui.updateStatus(command, response);

        String first = response.get(0);
        if (first.startsWith("ERROR ")) {
            String code = first.length() > 6 ? first.substring(6).split("\\s+")[0] : first.substring(6);
            if (gui != null) gui.displayError(code);
            return;
        }

        if (first.startsWith("OK ")) {
            String tag = first.substring(3).split("\\s+")[0];
            boolean stateChanging = "NOTE_POSTED".equals(tag) || "PIN_ADDED".equals(tag) || "PIN_REMOVED".equals(tag)
                    || "SHAKE_COMPLETE".equals(tag) || "CLEAR_COMPLETE".equals(tag);
            if (stateChanging) {
                List<String> getResponse = sendRequestRaw(lastGetNotesCommand);
                List<NoteData> notes = parseNoteLines(getResponse);
                if (gui != null) gui.refreshBoard(notes);
            } else if (isGetNotesCommand(command) && response.size() > 1) {
                List<NoteData> notes = parseNoteLines(response);
                if (gui != null) gui.refreshBoard(notes);
            }
        }
    }

    private static boolean isGetNotesCommand(String cmd) {
        if (cmd == null) return false;
        String c = cmd.trim().toUpperCase();
        return c.startsWith("GET") && !c.startsWith("GET PINS");
    }

    /**
     * Send command and return raw response lines. Does not update GUI or trigger refresh.
     */
    private List<String> sendRequestRaw(String command) {
        if (dummyConnection != null) {
            if (!dummyConnection.isConnected()) return Collections.singletonList("ERROR Not connected");
            return dummyConnection.sendCommand(command);
        }
        if (socket == null || out == null || in == null)
            return Collections.singletonList("ERROR Not connected");
        List<String> lines = new ArrayList<>();
        try {
            out.println(command);
            String first = in.readLine();
            if (first == null) {
                lines.add("ERROR Connection closed");
                return lines;
            }
            first = first.trim();
            lines.add(first);
            if (first.startsWith("OK ")) {
                String[] parts = first.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        int count = Integer.parseInt(parts[1]);
                        for (int i = 0; i < count; i++) {
                            String line = in.readLine();
                            if (line != null) lines.add(line.trim());
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) {
            lines.clear();
            lines.add("ERROR " + e.getMessage());
        }
        return lines;
    }

    /** Parse response lines from GET (notes) into NoteData list. NOTE x y color message PINNED=true|false */
    private List<NoteData> parseNoteLines(List<String> lines) {
        List<NoteData> notes = new ArrayList<>();
        if (lines.size() < 2) return notes;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (!line.startsWith("NOTE ")) continue;
            NoteData nd = parseNoteLine(line);
            if (nd != null) notes.add(nd);
        }
        return notes;
    }

    private static NoteData parseNoteLine(String line) {
        String rest = line.substring(5).trim();
        int pinnedIdx = rest.lastIndexOf(" PINNED=");
        if (pinnedIdx < 0) return null;
        String pinnedStr = rest.substring(pinnedIdx + 8).trim();
        rest = rest.substring(0, pinnedIdx).trim();
        String[] parts = rest.split("\\s+", 4);
        if (parts.length < 4) return null;
        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            String color = parts[2];
            String message = parts[3];
            boolean pinned = "true".equalsIgnoreCase(pinnedStr);
            return new NoteData(x, y, color, message, pinned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Data for one note (parsed from server GET response). */
    public static class NoteData {
        public final int x, y;
        public final String color, message;
        public final boolean pinned;

        public NoteData(int x, int y, String color, String message, boolean pinned) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.message = message;
            this.pinned = pinned;
        }
    }

    // ----- Dummy connection (local test, no server) -----

    private interface Connection {
        boolean connect(String host, int port);
        void disconnect();
        boolean isConnected();
        String getGreeting();
        List<String> sendCommand(String command);
    }

    private static class DummyConnection implements Connection {
        private final int boardW, boardH, noteW, noteH;
        private final List<String> colors;
        private Board board;
        private String greeting;
        private boolean connected;

        DummyConnection(int boardW, int boardH, int noteW, int noteH, List<String> colors) {
            this.boardW = Math.max(1, boardW);
            this.boardH = Math.max(1, boardH);
            this.noteW = Math.max(1, noteW);
            this.noteH = Math.max(1, noteH);
            this.colors = colors != null && !colors.isEmpty() ? colors : Arrays.asList("red", "white", "green", "yellow");
        }

        @Override
        public boolean connect(String host, int port) {
            board = new Board(boardW, boardH, noteW, noteH, new ArrayList<>(this.colors));
            StringBuilder sb = new StringBuilder();
            sb.append(boardW).append(" ").append(boardH).append(" ").append(noteW).append(" ").append(noteH);
            for (String c : this.colors) sb.append(" ").append(c);
            greeting = sb.toString();
            connected = true;
            return true;
        }

        @Override
        public void disconnect() {
            connected = false;
            board = null;
            greeting = null;
        }

        @Override
        public boolean isConnected() {
            return connected && board != null;
        }

        @Override
        public String getGreeting() {
            return greeting;
        }

        @Override
        public List<String> sendCommand(String command) {
            if (!isConnected()) return Collections.singletonList("ERROR Not connected");
            String line = command == null ? "" : command.trim();
            if (line.isEmpty()) return Collections.singletonList("ERROR INVALID_FORMAT");
            String response = parseAndExecute(line, board);
            return Arrays.asList(response.split("\n"));
        }

        private static String parseAndExecute(String request, Board board) {
            String trimmed = request.trim();
            String firstWord = trimmed.split("\\s+")[0];
            String[] parts;
            if (firstWord.equals("POST")) parts = trimmed.split("\\s+", 5);
            else parts = trimmed.split("\\s+");
            try {
                switch (firstWord) {
                    case "POST": return handlePost(parts, board);
                    case "GET": return handleGet(parts, board);
                    case "PIN": return handlePin(parts, board);
                    case "UNPIN": return handleUnpin(parts, board);
                    case "SHAKE": return board.shake();
                    case "CLEAR": return board.clear();
                    case "DISCONNECT": return "OK DISCONNECTING";
                    default: return "ERROR INVALID_FORMAT";
                }
            } catch (Exception e) {
                return "ERROR INVALID_FORMAT";
            }
        }

        private static String handlePost(String[] p, Board board) {
            if (p.length < 5) return "ERROR INVALID_FORMAT";
            try {
                int x = Integer.parseInt(p[1]), y = Integer.parseInt(p[2]);
                String color = p[3], message = p[4];
                if (message.trim().isEmpty() || message.length() > 256) return "ERROR INVALID_FORMAT";
                return board.addNote(x, y, color, message);
            } catch (NumberFormatException e) { return "ERROR INVALID_FORMAT"; }
        }

        private static String handleGet(String[] p, Board board) {
            if (p.length == 2 && p[1].equals("PINS")) return board.getPins();
            String color = null; Integer cx = null, cy = null; String refersTo = null;
            for (int i = 1; i < p.length; i++) {
                if (p[i].startsWith("color=")) { color = p[i].substring(6); if (color.isEmpty()) return "ERROR INVALID_FORMAT"; }
                else if (p[i].startsWith("contains=")) {
                    if (i + 1 >= p.length) return "ERROR INVALID_FORMAT";
                    try { cx = Integer.parseInt(p[i].substring(9)); cy = Integer.parseInt(p[i + 1]); i++; }
                    catch (NumberFormatException e) { return "ERROR INVALID_FORMAT"; }
                } else if (p[i].startsWith("refersTo=")) {
                    refersTo = p[i].substring(9);
                    for (int j = i + 1; j < p.length; j++) refersTo += " " + p[j];
                    refersTo = refersTo.trim();
                    if (refersTo.isEmpty()) return "ERROR INVALID_FORMAT";
                    break;
                } else return "ERROR INVALID_FORMAT";
            }
            return board.get(color, cx, cy, refersTo);
        }

        private static String handlePin(String[] p, Board board) {
            if (p.length != 3) return "ERROR INVALID_FORMAT";
            try {
                return board.addPin(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            } catch (NumberFormatException e) { return "ERROR INVALID_FORMAT"; }
        }

        private static String handleUnpin(String[] p, Board board) {
            if (p.length != 3) return "ERROR INVALID_FORMAT";
            try {
                return board.unPin(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            } catch (NumberFormatException e) { return "ERROR INVALID_FORMAT"; }
        }
    }

    /**
     * Run the client. Optional args (same order as server): port board_w board_h note_w note_h color1 color2 ...
     * Example: java BBoardClient 4554 200 100 20 10 red white green yellow
     * If 6+ args are given, dummy/local-test mode uses this board config when you click Connect with "Local test" checked.
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
        javax.swing.SwingUtilities.invokeLater(() -> {
            ClientGUI gui = new ClientGUI(client);
            client.setGui(gui);
            gui.setVisible(true);
        });
    }
}
