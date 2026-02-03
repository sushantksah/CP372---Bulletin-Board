import java.io.*;
import java.net.*;
import java.util.*;

// Client Handler Class 
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Board board;

    public ClientHandler(Socket socket, Board board) {
        this.socket = socket;
        this.board = board;
    }


    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            out.println(board.greetingLine());

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim(); 
                if (line.isEmpty()) {
                    out.println("ERROR INVALID_FORMAT Empty command");
                    continue;
                }

                List<String> responseLines = board.handleCommand(line);

                for (String r : responseLines) out.println(r);

                if (responseLines.size() > 0 && responseLines.get(0).startsWith("OK DISCONNECTING")) {
                    break;
                }
            }
        } catch (Exception e) {
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
