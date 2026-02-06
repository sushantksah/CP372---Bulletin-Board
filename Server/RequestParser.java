package Server;

/*
RequestParser class acts as a middleman between the client and the board. 
Parses the request and executes the command.
*/

public class RequestParser {

    // Parses the request, checks if valid, and executes the command
    public static String parseAndExecute(String request, Board board) {
        // Request cannot be null or empty
        if (request == null || request.trim().isEmpty()) {
            return "ERROR INVALID_FORMAT";
        }

        // Leading and trailing whitespace is ignored by the server (RFC - 6.1)
        String trimmed = request.trim();
        String firstWord = trimmed.split("\\s+")[0]; // not uppercase to force client to use correct case

        String[] splitRequest;
        if (firstWord.equals("POST")) {
            splitRequest = trimmed.split("\\s+", 5); // POST has 5 (including first word)
        } else {
            splitRequest = trimmed.split("\\s+"); // GET, PIN, UNPIN, SHAKE, CLEAR, DISCONNECT
        }

        try {
            switch (firstWord) {
                case "POST":
                    return handlePost(splitRequest, board);
                case "GET":
                    return handleGet(splitRequest, board);
                case "PIN":
                    return handlePin(splitRequest, board);
                case "UNPIN":
                    return handleUnpin(splitRequest, board);
                case "SHAKE":
                    if (splitRequest.length != 1)
                        return "ERROR INVALID_FORMAT"; // no parameters for SHAKE
                    return board.shake();
                case "CLEAR":
                    if (splitRequest.length != 1) // no parameters for CLEAR
                        return "ERROR INVALID_FORMAT";
                    return board.clear();
                case "DISCONNECT":
                    if (splitRequest.length != 1) // no parameters for DISCONNECT
                        return "ERROR INVALID_FORMAT";
                    return "OK DISCONNECTING";
                default: // lowercase will be ignored as invalid format
                    return "ERROR INVALID_FORMAT";
            }
        } catch (Exception e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    /* Helper methods */
    // Handles the POST command
    private static String handlePost(String[] splitRequest, Board board) {

        // POST needs at least 5 parameters (x y color message)
        if (splitRequest.length < 5)
            return "ERROR INVALID_FORMAT";

        try {
            int x = Integer.parseInt(splitRequest[1]);
            int y = Integer.parseInt(splitRequest[2]);
            String color = splitRequest[3];

            // Color can't be null or empty
            if (color == null || color.trim().isEmpty()) {
                return "ERROR INVALID_FORMAT";
            }
            String message = splitRequest[4];

            // Message can't be null, empty or more than 256 characters
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

    // Handles the GET command
    private static String handleGet(String[] splitRequest, Board board) {
        // GET with no arguments returns all notes
        if (splitRequest.length == 1) {
            return board.getNotes(null, null, null, null);
        }

        // Handles GET PINS
        if (splitRequest.length == 2 && splitRequest[1].equals("PINS")) {
            return board.getPins();
        }

        // Use filters to get specific notes
        String color = null;
        Integer x = null, y = null;
        String refersTo = null;

        for (int i = 1; i < splitRequest.length; i++) {
            String p = splitRequest[i];

            if (p.startsWith("color=")) {
                if (color != null)
                    return "ERROR INVALID_FORMAT";
                color = p.substring(6);
                if (color.isEmpty())
                    return "ERROR INVALID_FORMAT";

            } else if (p.startsWith("contains=")) {
                if (x != null)
                    return "ERROR INVALID_FORMAT";
                if (i + 1 >= splitRequest.length)
                    return "ERROR INVALID_FORMAT";

                try {
                    x = Integer.parseInt(p.substring(9));
                    y = Integer.parseInt(splitRequest[++i]);
                } catch (Exception e) {
                    return "ERROR INVALID_FORMAT";
                }

            } else if (p.startsWith("refersTo=")) {
                if (refersTo != null)
                    return "ERROR INVALID_FORMAT";

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

    // Handles the PIN command
    private static String handlePin(String[] splitRequest, Board board) {
        // PIN needs 2 parameters (x y)
        if (splitRequest.length != 3) {
            return "ERROR INVALID_FORMAT";
        }

        try {
            return board.addPin(Integer.parseInt(splitRequest[1]), Integer.parseInt(splitRequest[2]));
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    // Handles the UNPIN command
    private static String handleUnpin(String[] splitRequest, Board board) {
        // UNPIN needs 2 parameters (x y)
        if (splitRequest.length != 3)
            return "ERROR INVALID_FORMAT";

        try {
            return board.unPin(Integer.parseInt(splitRequest[1]), Integer.parseInt(splitRequest[2]));
        } catch (NumberFormatException e) {
            return "ERROR INVALID_FORMAT";
        }
    }
}