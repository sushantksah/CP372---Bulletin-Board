import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List; 



public class BBoard {
    public static void main(String[] args) {
        int port; 
        int boardWidth, noteWidth;
        int boardHeight, noteHeight; 

        // Hardcoding MAX values - NOT SET IN STONE / PLACEHOLDER NUMBERS FIX FIX FIX ! 
        int MAX_NOTE_HEIGHT = 150;
        int MAX_NOTE_WIDTH = 200; 

        // Check for Improper Imports 
        if (args.length < 6){
            System.err.println("Sample Usage: java BBoard <port> <board_w> <board_h> <note_w> <note_h> <color1> ... <colorN>");
            System.exit(1);
            return;
        }

        try {
            port = Integer.parseInt(args[0]);
            boardWidth = Integer.parseInt(args[1]);
            boardHeight = Integer.parseInt(args[2]);
            noteWidth = Integer.parseInt(args[3]);
            noteHeight = Integer.parseInt(args[4]);
        } catch (NumberFormatException e){
            System.err.println("Error: port/width/height values must be integers.");
            System.exit(1);
            return;
        }

        // Port Validity Check
        if (port < 1 || port > 65535) {
            System.err.println("Error: Port must be between 1 and 65535.");
            System.exit(1);
            return; 
        }
        // Ensuring that board/note width/height are >0 
        if (boardWidth <= 0 || boardHeight <= 0 || noteWidth <= 0 || noteHeight <= 0) {
            System.err.println("Error: board_w, board_h, note_w, note_h must all be > 0.");
            System.exit(1);
            return;
        }
        // Note can't be bigger than the board 
        if (noteWidth > boardWidth || noteHeight > boardHeight) {
            System.err.println("Error: note dimensions must fit inside board dimensions.");
            System.exit(1);
            return; 
        }

        // CHECK TO CAP THE SIZE OF THE NOTE 
        if (noteHeight > MAX_NOTE_HEIGHT || noteWidth > MAX_NOTE_WIDTH){
            System.err.println("Error: note exceeds max width/height!");
            System.exit(1);
            return;
        }
        
        // Initalizing Colors 
        List<String> boardColor = new ArrayList<>();
        for (int i = 5; i < args.length; i++) {
            String c = args[i].trim();
            if (!c.isEmpty()) boardColor.add(c);
        }
        if (boardColor.isEmpty()) {
            System.err.println("Error: At least one color must be provided.");
            System.exit(1);
        }

        // Initalizing board 
        Board board = new Board(boardWidth, boardHeight, noteWidth, noteHeight, boardColor);

        // Server Socket!!!!!!!!!!!! SOCKET!!!! SOCKET !!! 
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("BBoard server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread t = new Thread(new ClientHandler(clientSocket, board));
                t.start();
            }

        } catch (Exception e) {
            System.err.println("Fatal server error: " + e.getMessage());
            // Line underneath is used to trace issues in the server/socket system as they occur 
            // e.printStackTrace(); 
            System.exit(1);
        }

}
}