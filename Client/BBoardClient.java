package Client;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import Server.Board;
import Server.RequestParser;

/**
 * Network client for the BBoard protocol.
 * Manages TCP connection, sends RFC-compliant commands, parses responses,
 * and notifies the GUI via the GuiCallback interface.
 */
public class BBoardClient {

    /** Callback interface for the GUI to receive parsed server data. */
    public interface GuiCallback {
        void initializeBoard(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> validColors);
        void updateStatus(String command, List<String> responseLines);
        void displayError(String errorCode);
        void refreshBoard(List<NoteData> notes);
    }

    /** Data transfer object for a note returned by GET. */
    public static class NoteData {
        public final int x, y;
        public final String color;
        public final String message;
        public final boolean pinned;

        public NoteData(int x, int y, String color, String message, boolean pinned) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.message = message;
            this.pinned = pinned;
        }
    }

    /** Abstraction over a server connection (real or dummy). */
    public interface Connection {
        String sendLine(String line) throws IOException;
        void close();
        boolean isOpen();
    }

    // ---- fields ----
    private GuiCallback gui;
    private Connection connection;
    private boolean usingDummy;
    private String lastGetNotesCommand = "GET";

    // real‑socket fields
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public BBoardClient() {}

    public void setGui(GuiCallback gui) {
        this.gui = gui;
    }

    public void setLastGetNotesCommand(String cmd) {
        this.lastGetNotesCommand = cmd;
    }

    // ---- connection ----

    public boolean connect(String host, int port, boolean useDummy) {
        this.usingDummy = useDummy;

        if (useDummy) {
            // local test mode: create an in‑process Board
            List<String> defaultColors = Arrays.asList("red", "white", "green", "yellow");
            Board localBoard = new Board(200, 100, 20, 10, defaultColors);
            connection = new DummyConnection(localBoard);
            if (gui != null) gui.initializeBoard(200, 100, 20, 10, defaultColors);
            return true;
        }

        try {
            int connectTimeoutMs = 10_000; // 10 seconds — avoid hanging on unreachable host
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            String greeting = in.readLine();
            if (greeting == null) {
                cleanup();
                if (gui != null) gui.displayError("CONNECTION_ERROR");
                return false;
            }

            connection = new SocketConnection();

            // Parse greeting: boardW boardH noteW noteH color1 ... colorN
            String[] parts = greeting.trim().split("\\s+");
            if (parts.length >= 5) {
                int bw = Integer.parseInt(parts[0]);
                int bh = Integer.parseInt(parts[1]);
                int nw = Integer.parseInt(parts[2]);
                int nh = Integer.parseInt(parts[3]);
                List<String> colors = new ArrayList<>(Arrays.asList(parts).subList(4, parts.length));
                if (gui != null) gui.initializeBoard(bw, bh, nw, nh, colors);
            }
            return true;

        } catch (java.net.ConnectException e) {
            cleanup();
            if (gui != null) gui.displayError("CONNECTION_REFUSED");
            return false;
        } catch (java.net.SocketTimeoutException e) {
            cleanup();
            if (gui != null) gui.displayError("CONNECTION_TIMEOUT");
            return false;
        } catch (Exception e) {
            cleanup();
            if (gui != null) gui.displayError("CONNECTION_ERROR");
            return false;
        }
    }

    public void disconnect() {
        if (connection != null && connection.isOpen()) {
            try {
                String resp = connection.sendLine("DISCONNECT");
                if (gui != null) gui.updateStatus("DISCONNECT", parseMultiLine(resp));
            } catch (Exception ignored) {}
        }
        cleanup();
    }

    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }

    public boolean isUsingDummy() {
        return usingDummy;
    }

    // ---- send request ----

    public void sendRequest(String cmd) {
        if (connection == null || !connection.isOpen()) return;
        try {
            String rawResponse = connection.sendLine(cmd);
            List<String> lines = parseMultiLine(rawResponse);

            if (gui != null) gui.updateStatus(cmd, lines);

            if (!lines.isEmpty()) {
                String first = lines.get(0);

                // Check for errors
                if (first.startsWith("ERROR ")) {
                    String code = first.substring(6).split("\\s+", 2)[0];
                    if (gui != null) gui.displayError(code);
                    return;
                }

                // After state-changing OK, auto-refresh board
                if (first.equals("OK NOTE_POSTED") || first.equals("OK PIN_ADDED")
                        || first.equals("OK PIN_REMOVED") || first.equals("OK SHAKE_COMPLETE")
                        || first.equals("OK CLEAR_COMPLETE")) {
                    autoRefresh();
                    return;
                }

                // GET with data lines: parse notes
                if (first.startsWith("OK ") && cmd.startsWith("GET") && !cmd.equals("GET PINS")) {
                    List<NoteData> notes = parseNotes(lines);
                    if (gui != null) gui.refreshBoard(notes);
                }

                if (first.equals("OK DISCONNECTING")) {
                    cleanup();
                }
            }
        } catch (Exception e) {
            if (gui != null) gui.displayError("CONNECTION_ERROR");
            cleanup();
        }
    }

    // ---- helpers ----

    private void autoRefresh() {
        // Re-issue the last GET command to refresh the visual board
        if (connection == null || !connection.isOpen()) return;
        try {
            String cmd = (lastGetNotesCommand != null && !lastGetNotesCommand.isEmpty()) ? lastGetNotesCommand : "GET";
            String rawResponse = connection.sendLine(cmd);
            List<String> lines = parseMultiLine(rawResponse);
            if (gui != null) gui.updateStatus(cmd, lines);
            if (!lines.isEmpty() && lines.get(0).startsWith("OK ")) {
                List<NoteData> notes = parseNotes(lines);
                if (gui != null) gui.refreshBoard(notes);
            }
        } catch (Exception ignored) {}
    }

    private List<String> parseMultiLine(String raw) {
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split("\n", -1)));
    }

    private List<NoteData> parseNotes(List<String> lines) {
        List<NoteData> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) { // skip "OK <count>"
            String l = lines.get(i).trim();
            if (!l.startsWith("NOTE ")) continue;
            try {
                String[] parts = l.split("\\s+", 5);
                if (parts.length < 5) continue;
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                String color = parts[3];
                String rest = parts[4];
                boolean pinned = false;
                String message = rest;
                int idx = rest.lastIndexOf(" PINNED=");
                if (idx >= 0) {
                    message = rest.substring(0, idx);
                    String pv = rest.substring(idx + " PINNED=".length());
                    pinned = pv.startsWith("true");
                }
                result.add(new NoteData(x, y, color, message, pinned));
            } catch (Exception ignored) {}
        }
        return result;
    }

    private void cleanup() {
        try { if (out != null) out.close(); } catch (Exception ignored) {}
        try { if (in != null) in.close(); } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        out = null; in = null; socket = null;
        connection = null;
    }

    // ---- Connection implementations ----

    /** Real TCP socket connection. */
    private class SocketConnection implements Connection {
        @Override
        public String sendLine(String line) throws IOException {
            out.println(line);
            out.flush();
            String first = in.readLine();
            if (first == null) throw new IOException("Server disconnected");

            // If response is "OK <count>", read that many additional lines
            StringBuilder sb = new StringBuilder(first);
            try {
                String[] p = first.trim().split("\\s+");
                if (p.length == 2 && p[0].equals("OK")) {
                    int count = Integer.parseInt(p[1]);
                    for (int i = 0; i < count; i++) {
                        String next = in.readLine();
                        if (next == null) break;
                        sb.append("\n").append(next);
                    }
                }
            } catch (NumberFormatException ignored) {}
            return sb.toString();
        }

        @Override
        public void close() {
            cleanup();
        }

        @Override
        public boolean isOpen() {
            return socket != null && socket.isConnected() && !socket.isClosed();
        }
    }

    /** Local in-process connection for testing without a server. */
    public static class DummyConnection implements Connection {
        private final Board board;
        private boolean open = true;

        public DummyConnection(Board board) {
            this.board = board;
        }

        @Override
        public String sendLine(String line) {
            if (!open) return "ERROR CONNECTION_ERROR";
            String resp = RequestParser.parseAndExecute(line.trim(), board);
            if (resp.equals("OK DISCONNECTING")) {
                open = false;
            }
            return resp;
        }

        @Override
        public void close() {
            open = false;
        }

        @Override
        public boolean isOpen() {
            return open;
        }
    }
}
