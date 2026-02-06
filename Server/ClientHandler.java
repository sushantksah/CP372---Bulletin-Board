package Server;
import java.io.*;
import java.net.*;
import java.util.*;

/*
ClientHandler class is the object that handles the client connection.
Manages the client connection, reads the request, and sends the response.
*/

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Board board;

    // Initializes the client handler and its socket and board
    public ClientHandler(Socket socket, Board board) {
        this.socket = socket;
        this.board = board;
    }

    // Runs the client handler
    @Override
    public void run() {
        // Try to get I/O streams from the socket
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            out.println(board.greetingLine());

            String line;
            while ((line = in.readLine()) != null) {
                line = line.strip(); 
                if (line.isEmpty()) {
                    out.println("ERROR INVALID_FORMAT");
                    continue;
                }

                List<String> responseLines = board.handleCommand(line);

                for (String r : responseLines) out.println(r);

                if (responseLines.size() > 0 && responseLines.get(0).startsWith("OK DISCONNECTING")) {
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
