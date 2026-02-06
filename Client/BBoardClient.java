package Client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// import Server.Board;
// import Server.RequestParser;

/*
BBoardClient class is the main network client for the BBoard protocol
Responsipble for managing the TCP connection, sending commands that are compliant with the RFC, parsing responses, and notifying the GUI
*/

public class BBoardClient {
    private GuiCallback gui;

    // private Connection connection;
    // private boolean usingDummy;
    private String lastGetNotesCommand = "GET";
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public BBoardClient() {
    }

    // Sets up GUI callback
    public void setGui(GuiCallback gui) {
        this.gui = gui;
    }

    //
    public void setLastGetNotesCommand(String cmd) {
        this.lastGetNotesCommand = cmd;
    }

    /* Connect and disconnect methods */
    // Attempts to connect the client to the server
    public boolean connect(String host, int port) {
        // public boolean connect(String host, int port, boolean useDummy) {
        // this.usingDummy = useDummy;

        // if (useDummy) {
        // // local test mode: create an in‑process Board
        // List<String> defaultColors = Arrays.asList("red", "white", "green",
        // "yellow");
        // Board localBoard = new Board(200, 100, 20, 10, defaultColors);
        // connection = new DummyConnection(localBoard);
        // if (gui != null) gui.initializeBoard(200, 100, 20, 10, defaultColors);
        // return true;
        // }

        try {
            int connectTimeoutMs = 3_000; // 3 seconds timeout
            socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // The server sends the board greeting after connection is established
            String greeting = in.readLine();
            if (greeting == null) {
                cleanup();
                if (gui != null)
                    gui.displayError("CONNECTION_ERROR");
                return false;
            }

            // connection = new SocketConnection();

            // Parse greeting to configure the board
            String[] parts = greeting.trim().split("\\s+");
            if (parts.length >= 5) {
                int bw = Integer.parseInt(parts[0]);
                int bh = Integer.parseInt(parts[1]);
                int nw = Integer.parseInt(parts[2]);
                int nh = Integer.parseInt(parts[3]);
                List<String> colors = new ArrayList<>(Arrays.asList(parts).subList(4, parts.length));
                if (gui != null)
                    gui.initializeBoard(bw, bh, nw, nh, colors);
            }
            return true;

        } catch (java.net.ConnectException e) {
            cleanup();
            if (gui != null)
                gui.displayError("CONNECTION_REFUSED");
            return false;
        } catch (java.net.SocketTimeoutException e) {
            cleanup();
            if (gui != null)
                gui.displayError("CONNECTION_TIMEOUT");
            return false;
        } catch (Exception e) {
            cleanup();
            if (gui != null)
                gui.displayError("CONNECTION_ERROR");
            return false;
        }
    }

    // Disconnects the client from the server
    public void disconnect() {
        // if (connection != null && connection.isOpen()) {
        // try {
        // String resp = connection.sendLine("DISCONNECT");
        // if (gui != null) gui.updateStatus("DISCONNECT", parseMultiLine(resp));
        // } catch (Exception ignored) {}
        // }
        // cleanup();
        if (isConnected()) {
            sendRequest("DISCONNECT");
        } else {
            cleanup();
        }
    }

    // Checks if the client is connected to the server
    public boolean isConnected() {
        // return connection != null && connection.isOpen();
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /* Send request */
    public void sendRequest(String cmd) {

        if (!isConnected())
            return;

        try {
            // Send the command
            out.println(cmd);
            out.flush();

            // Get the first line of response
            String first = in.readLine();
            if (first == null)
                throw new IOException("Server closed connection");

            StringBuilder fullResponse = new StringBuilder(first);

            // If response starts with OK and has a count, read the extra lines
            String[] p = first.trim().split("\\s+");
            if (p.length == 2 && p[0].equals("OK")) {
                try {
                    int count = Integer.parseInt(p[1]);
                    for (int i = 0; i < count; i++) {
                        String next = in.readLine();
                        if (next == null)
                            break;
                        fullResponse.append("\n").append(next);
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            List<String> lines = parseMultiLine(fullResponse.toString());
            if (gui != null)
                gui.updateStatus(cmd, lines);

            if (!lines.isEmpty()) {
                String statusLine = lines.get(0);

                // Send error code to the UI if something went wrong
                if (statusLine.startsWith("ERROR ")) {
                    String code = statusLine.substring(6).split("\\s+", 2)[0];
                    if (gui != null)
                        gui.displayError(code);
                    return;
                }

                // If we did something that changes the board, trigger a refresh
                if (statusLine.equals("OK NOTE_POSTED") || statusLine.equals("OK PIN_ADDED")
                        || statusLine.equals("OK PIN_REMOVED") || statusLine.equals("OK SHAKE_COMPLETE")
                        || statusLine.equals("OK CLEAR_COMPLETE")) {
                    autoRefresh();
                    return;
                }

                // If it was a GET command, parse the notes and update the canvas
                if (statusLine.startsWith("OK ") && cmd.startsWith("GET") && !cmd.equals("GET PINS")) {
                    List<NoteData> notes = parseNotes(lines);
                    if (gui != null)
                        gui.refreshBoard(notes);
                }

                if (statusLine.equals("OK DISCONNECTING")) {
                    cleanup();
                }
            }
        } catch (Exception e) {
            if (gui != null)
                gui.displayError("CONNECTION_ERROR");
            cleanup();
        }
        /*
         * if (connection == null || !connection.isOpen()) return;
         * try {
         * String rawResponse = connection.sendLine(cmd);
         * List<String> lines = parseMultiLine(rawResponse);
         * 
         * if (gui != null) gui.updateStatus(cmd, lines);
         * 
         * if (!lines.isEmpty()) {
         * String first = lines.get(0);
         * 
         * // Check for errors
         * if (first.startsWith("ERROR ")) {
         * String code = first.substring(6).split("\\s+", 2)[0];
         * if (gui != null) gui.displayError(code);
         * return;
         * }
         * 
         * // After state-changing OK, auto-refresh board
         * if (first.equals("OK NOTE_POSTED") || first.equals("OK PIN_ADDED")
         * || first.equals("OK PIN_REMOVED") || first.equals("OK SHAKE_COMPLETE")
         * || first.equals("OK CLEAR_COMPLETE")) {
         * autoRefresh();
         * return;
         * }
         * 
         * // GET with data lines: parse notes
         * if (first.startsWith("OK ") && cmd.startsWith("GET") &&
         * !cmd.equals("GET PINS")) {
         * List<NoteData> notes = parseNotes(lines);
         * if (gui != null) gui.refreshBoard(notes);
         * }
         * 
         * if (first.equals("OK DISCONNECTING")) {
         * cleanup();
         * }
         * }
         * } catch (Exception e) {
         * if (gui != null) gui.displayError("CONNECTION_ERROR");
         * cleanup();
         * }
         */
    }

    // Grabs the latest notes so the UI can be updated
    private void autoRefresh() {
        // // Re-issue the last GET command to refresh the visual board
        // if (connection == null || !connection.isOpen())
        // return;
        // try {
        // String cmd = (lastGetNotesCommand != null && !lastGetNotesCommand.isEmpty())
        // ? lastGetNotesCommand : "GET";
        // String rawResponse = connection.sendLine(cmd);
        // List<String> lines = parseMultiLine(rawResponse);
        // if (gui != null)
        // gui.updateStatus(cmd, lines);
        // if (!lines.isEmpty() && lines.get(0).startsWith("OK ")) {
        // List<NoteData> notes = parseNotes(lines);
        // if (gui != null)
        // gui.refreshBoard(notes);
        // }
        // } catch (Exception ignored) {
        // }

        if (!isConnected())
            return;
        sendRequest(lastGetNotesCommand != null ? lastGetNotesCommand : "GET");

    }

    /* Helper methods */
    // Splits multi-line response into a list of lines
    private List<String> parseMultiLine(String raw) {
        if (raw == null || raw.isEmpty())
            return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split("\n", -1)));
    }

    // Parses the notes from the response
    private List<NoteData> parseNotes(List<String> lines) {
        List<NoteData> result = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String l = lines.get(i).trim();
            if (!l.startsWith("NOTE "))
                continue;
            try {
                String[] parts = l.split("\\s+", 5);
                if (parts.length < 5)
                    continue;
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
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    // Cleans up the connection, closes streams and socket
    private void cleanup() {
        try {
            if (out != null)
                out.close();
        } catch (Exception ignored) {
        }
        try {
            if (in != null)
                in.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null)
                socket.close();
        } catch (Exception ignored) {
        }
        out = null;
        in = null;
        socket = null;
        // connection = null;
    }

    // UNUSED

    // Interface for the GUI to receive parsed server data
    public interface GuiCallback {
        void initializeBoard(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> validColors);

        void updateStatus(String command, List<String> responseLines);

        void displayError(String errorCode);

        void refreshBoard(List<NoteData> notes);
    }

    // Holds note data for a note returned by GET command
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

    /*
     * public boolean isUsingDummy() {
     * return usingDummy;
     * }
     */

    /** Real TCP socket connection. */
    /*
     * private class SocketConnection implements Connection {
     * 
     * @Override
     * public String sendLine(String line) throws IOException {
     * out.println(line);
     * out.flush();
     * String first = in.readLine();
     * if (first == null)
     * throw new IOException("Server disconnected");
     * 
     * // If response is "OK <count>", read that many additional lines
     * StringBuilder sb = new StringBuilder(first);
     * try {
     * String[] p = first.trim().split("\\s+");
     * if (p.length == 2 && p[0].equals("OK")) {
     * int count = Integer.parseInt(p[1]);
     * for (int i = 0; i < count; i++) {
     * String next = in.readLine();
     * if (next == null)
     * break;
     * sb.append("\n").append(next);
     * }
     * }
     * } catch (NumberFormatException ignored) {
     * }
     * return sb.toString();
     * }
     * 
     * @Override
     * public void close() {
     * cleanup();
     * }
     * 
     * @Override
     * public boolean isOpen() {
     * return socket != null && socket.isConnected() && !socket.isClosed();
     * }
     * }
     */

    /** Local in-process connection for testing without a server. */
    /*
     * public static class DummyConnection implements Connection {
     * private final Board board;
     * private boolean open = true;
     * 
     * public DummyConnection(Board board) {
     * this.board = board;
     * }
     * 
     * @Override
     * public String sendLine(String line) {
     * if (!open)
     * return "ERROR CONNECTION_ERROR";
     * String resp = RequestParser.parseAndExecute(line.trim(), board);
     * if (resp.equals("OK DISCONNECTING")) {
     * open = false;
     * }
     * return resp;
     * }
     * 
     * @Override
     * public void close() {
     * open = false;
     * }
     * 
     * @Override
     * public boolean isOpen() {
     * return open;
     * }
     * }
     */
}
