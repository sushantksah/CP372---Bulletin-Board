import java.util.*;

public class RequestParser {

    public static String parseAndExecute(String rawLine, Board board) {
        if (rawLine == null || rawLine.trim().isEmpty()) {
            return "ERROR INVALID_FORMAT";
        }

        String[] parts = rawLine.trim().split("\\s+");
        String command = parts[0].toUpperCase();

        try {
            switch (command) {
                case "POST":
                    return handlePost(parts, board);
                case "GET":
                    return handleGet(parts, board);
                case "PIN":
                    return handlePin(parts, board);
                case "UNPIN":
                    return handleUnpin(parts, board);
                case "SHAKE":
                    return board.shake();
                case "CLEAR":
                    return board.clear();
                case "DISCONNECT":
                    return "DISCONNECTING"; // Handled by ClientHandler loop
                default:
                    return "ERROR INVALID_FORMAT";
            }
        } catch (Exception e) {
            // General safety net to ensure server robustness (Goal 1.3)
            return "ERROR INVALID_FORMAT";
        }
    }

    private static String handlePost(String[] parts, Board board) {
        // Syntax: POST x y color message (RFC 7.1)
        if (parts.length < 5) return "ERROR INVALID_FORMAT";

        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            String color = parts[3];
            
            // Reconstruct message (can contain spaces, extend to end of line)
            StringBuilder msgBuilder = new StringBuilder();
            for (int i = 4; i < parts.length; i++) {
                msgBuilder.append(parts[i]).append(i == parts.length - 1 ? "" : " ");
            }
            String message = msgBuilder.toString();

            return board.addNote(x, y, color, message); 

        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    private static String handlePin(String[] parts, Board board) {
        // Syntax: PIN x y (RFC 7.3)
        if (parts.length != 3) return "ERROR INVALID_FORMAT";
        try {
            return board.addPin(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    private static String handleUnpin(String[] parts, Board board) {
        // Syntax: UNPIN x y (RFC 7.4)
        if (parts.length != 3) return "ERROR INVALID_FORMAT";
        try {
            return board.unpin(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    private static String handleGet(String[] parts, Board board) {
        
        if (parts.length == 2 && parts[1].equalsIgnoreCase("PINS")) {
            return board.getPins();
        }

        String color = null;
        Integer x = null, y = null;
        String refersTo = null;

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.startsWith("color=")) {
                color = p.substring(6);
            } else if (p.startsWith("contains=")) {
                try {
    
                    x = Integer.parseInt(p.substring(9));
                    y = Integer.parseInt(parts[++i]); 
                } catch (Exception e) { return "ERROR INVALID_FORMAT"; }
            } else if (p.startsWith("refersTo=")) {
                refersTo = p.substring(9);
            }
        }
        return board.get(color, x, y, refersTo);
    }
}
