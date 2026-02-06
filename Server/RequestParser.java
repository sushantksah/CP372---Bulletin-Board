package Server;
public class RequestParser {

    public static String parseAndExecute(String request, Board board) {
        if (request == null || request.trim().isEmpty()) {
            return "ERROR INVALID_FORMAT";
        }

        String trimmed = request.trim();
        String firstWord = trimmed.split("\\s+")[0]; // not uppercase to force correct case

        String[] splitRequest;
        if (firstWord.equals("POST")) {
            splitRequest = trimmed.split("\\s+", 5); // x y color message
        } else {
            splitRequest = trimmed.split("\\s+");
        }

        try {
            switch (firstWord) {
                case "POST": // x y color message
                    return handlePost(splitRequest, board);
                case "GET": // GET PINS orGET [color=<c>] [contains=<x> <y>] [refersTo=<substring>]
                    return handleGet(splitRequest, board);
                case "PIN": // <x> <y>
                    return handlePin(splitRequest, board);
                case "UNPIN": // UNPIN <x> <y>
                    return handleUnpin(splitRequest, board);
                case "SHAKE":
                    if (splitRequest.length != 1) return "ERROR INVALID_FORMAT";
                    return board.shake();
                case "CLEAR":
                    if (splitRequest.length != 1) return "ERROR INVALID_FORMAT";
                    return board.clear();
                case "DISCONNECT":
                    if (splitRequest.length != 1) return "ERROR INVALID_FORMAT";
                    return "OK DISCONNECTING";
                default: // lowercase will be ignored as invalid format
                    return "ERROR INVALID_FORMAT";
            }
        } catch (Exception e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    private static String handlePost(String[] splitRequest, Board board) {

        // not enough parameters
        if (splitRequest.length < 5)
            return "ERROR INVALID_FORMAT";

        try {
            int x = Integer.parseInt(splitRequest[1]);
            int y = Integer.parseInt(splitRequest[2]);
            String color = splitRequest[3]; // lowkey check if color is valid here instead

            if (color == null || color.trim().isEmpty()) {
                return "ERROR INVALID_FORMAT";
            }
            String message = splitRequest[4]; // Extract from the limit-split

            if (message.trim().isEmpty() || message.length() > Note.MAX_MESSAGE_LENGTH) {
                return "ERROR INVALID_FORMAT";
            }

            return board.addNote(x, y, color, message);

        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT";
        } catch (Exception e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    private static String handleGet(String[] splitRequest, Board board) {

        // "GET" with no args
        if (splitRequest.length == 1) {
            return board.getNotes(null, null, null, null);
        }
    
        // "GET PINS"
        if (splitRequest.length == 2 && splitRequest[1].equals("PINS")) {
            return board.getPins();
        }
    
        // Otherwise: GET [color=<c>] [contains=<x> <y>] [refersTo=<substring>]
        String color = null;
        Integer x = null, y = null;
        String refersTo = null;
    
        for (int i = 1; i < splitRequest.length; i++) {
            String p = splitRequest[i];
    
            if (p.startsWith("color=")) {
                if (color != null) return "ERROR INVALID_FORMAT";
                color = p.substring(6);
                if (color.isEmpty()) return "ERROR INVALID_FORMAT";
    
            } else if (p.startsWith("contains=")) {
                if (x != null) return "ERROR INVALID_FORMAT";
                if (i + 1 >= splitRequest.length) return "ERROR INVALID_FORMAT";
    
                try {
                    x = Integer.parseInt(p.substring(9));
                    y = Integer.parseInt(splitRequest[++i]);
                } catch (Exception e) {
                    return "ERROR INVALID_FORMAT";
                }
    
            } else if (p.startsWith("refersTo=")) {
                if (refersTo != null) return "ERROR INVALID_FORMAT";
    
                StringBuilder sb = new StringBuilder(p.substring(9));
                for (int j = i + 1; j < splitRequest.length; j++) {
                    sb.append(" ").append(splitRequest[j]);
                }
    
                refersTo = sb.toString();
                if (refersTo.trim().isEmpty() || refersTo.length() > Note.MAX_MESSAGE_LENGTH) {
                    return "ERROR INVALID_FORMAT";
                }
                break;
    
            } else {
                return "ERROR INVALID_FORMAT";
            }
        }
    
        return board.getNotes(color, x, y, refersTo);
    }

    private static String handlePin(String[] splitRequest, Board board) {
        // <x> <y> as parameters
        if (splitRequest.length != 3) {
            return "ERROR INVALID_FORMAT";
        }

        try {
            return board.addPin(Integer.parseInt(splitRequest[1]), Integer.parseInt(splitRequest[2]));
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT";
        }

    }

    private static String handleUnpin(String[] splitRequest, Board board) {
        // Syntax: UNPIN x y 
        if (splitRequest.length != 3) return "ERROR INVALID_FORMAT";
        
        try {
            return board.unPin(Integer.parseInt(splitRequest[1]), Integer.parseInt(splitRequest[2]));
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT";
        }
    }
}