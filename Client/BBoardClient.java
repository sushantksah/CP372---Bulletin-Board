package Client;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
BBoardClient class is the main network client for the BBoard protocol
Responsipble for managing the TCP connection, sending commands that are compliant with the RFC, parsing responses, and notifying the GUI
*/

public class BBoardClient {

    // GUI uses this to react to server data
    public interface GuiCallback {
        void initializeBoard(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> validColors);

        void updateStatus(String command, List<String> responseLines);

        void displayError(String errorCode);

        void refreshBoard(List<NoteData> notes);
    }

    // Hold note data for the UI
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

    private GuiCallback gui;
    private String lastGetNotesCommand = "GET";
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Constructor
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
        if (isConnected()) {
            sendRequest("DISCONNECT");
        } else {
            cleanup();
        }
    }

    // Checks if the client is connected to the server
    public boolean isConnected() {
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

                // Trigger a refresh if something changes
                if (statusLine.equals("OK NOTE_POSTED") || statusLine.equals("OK PIN_ADDED")
                        || statusLine.equals("OK PIN_REMOVED") || statusLine.equals("OK SHAKE_COMPLETE")
                        || statusLine.equals("OK CLEAR_COMPLETE")) {
                    autoRefresh();
                    return;
                }

                // GET command: parse the notes and update the canvas
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

    }

    // Grabs the latest notes so the UI can be updated
    private void autoRefresh() {
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
    }
}
