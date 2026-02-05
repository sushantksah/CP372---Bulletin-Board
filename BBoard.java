import java.awt.Color;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class BBoard {

    private static final int MAX_BOARD_DIMENSION = 10000;
    private static final double MAX_NOTE_TO_BOARD_RATIO = 0.5;

    public static void main(String[] args) {
        int port;
        int boardWidth, noteWidth;
        int boardHeight, noteHeight;

        // Check for minimum required arguments
        if (args.length < 6) {
            System.err.println("Usage: java BBoard <port> <board_w> <board_h> <note_w> <note_h> <color1> ... <colorN>");
            System.exit(1);
            return;
        }

        try {
            port = Integer.parseInt(args[0]);
            boardWidth = Integer.parseInt(args[1]);
            boardHeight = Integer.parseInt(args[2]);
            noteWidth = Integer.parseInt(args[3]);
            noteHeight = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
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

        // Ensuring that board/note width/height are > 0
        if (boardWidth <= 0 || boardHeight <= 0 || noteWidth <= 0 || noteHeight <= 0) {
            System.err.println("Error: board_w, board_h, note_w, note_h must all be > 0.");
            System.exit(1);
            return;
        }

        // Cap maximum board size to prevent excessive memory usage
        if (boardWidth > MAX_BOARD_DIMENSION || boardHeight > MAX_BOARD_DIMENSION) {
            System.err.println("Error: Board dimensions must not exceed " + MAX_BOARD_DIMENSION
                    + ". Got " + boardWidth + "x" + boardHeight + ".");
            System.exit(1);
            return;
        }

        // Ratio-based note size cap: note must not exceed MAX_NOTE_TO_BOARD_RATIO of
        // the board
        int maxNoteWidth = (int) (boardWidth * MAX_NOTE_TO_BOARD_RATIO);
        int maxNoteHeight = (int) (boardHeight * MAX_NOTE_TO_BOARD_RATIO);
        if (noteWidth > maxNoteWidth || noteHeight > maxNoteHeight) {
            System.err.println("Error: Note dimensions exceed " + (int) (MAX_NOTE_TO_BOARD_RATIO * 100)
                    + "% of board size. Max note size for this board: "
                    + maxNoteWidth + "x" + maxNoteHeight
                    + ", but got " + noteWidth + "x" + noteHeight + ".");
            System.exit(1);
            return;
        }

        // Validate colours against java.awt.Color using reflection
        List<String> boardColor = new ArrayList<>();
        for (int i = 5; i < args.length; i++) {
            String c = args[i].trim().toLowerCase();
            if (c.isEmpty())
                continue;

            if (isValidSwingColor(c)) {
                boardColor.add(c);
            } else {
                System.err.println("Warning: \"" + args[i].trim()
                        + "\" is not a recognized java.awt.Color name — skipping.");
            }
        }

        if (boardColor.isEmpty()) {
            System.err.println("Error: No valid colors provided. At least one recognized colour is required.");
            System.exit(1);
            return;
        }

        // Initialize board
        Board board = new Board(boardWidth, boardHeight, noteWidth, noteHeight, boardColor);

        // Start server socket
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("BBoard server listening on port " + port);
            System.out.println("Board: " + boardWidth + "x" + boardHeight
                    + "  Note: " + noteWidth + "x" + noteHeight
                    + "  Colors: " + boardColor);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread t = new Thread(new ClientHandler(clientSocket, board));
                t.start();
            }

        } catch (Exception e) {
            System.err.println("Fatal server error: " + e.getMessage());
            System.exit(1);
        }
    }

    // Checks whether a color name corresponds to a static Color field in
    // java.awt.Color.
    private static boolean isValidSwingColor(String colorName) {

        String[] attempts = { colorName.toLowerCase(), colorName, colorName.toUpperCase() };
        for (String attempt : attempts) {
            try {
                Field field = Color.class.getField(attempt);
                if (field.getType() == Color.class)
                    return true;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return false;
    }
}