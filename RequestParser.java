import java.util.*;

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
                    return board.shake();
                case "CLEAR":
                    return board.clear();
                case "DISCONNECT":
                    return "DISCONNECTING";
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
        // if only get pins check for case then get pins
        if (splitRequest.length == 2) {
            if (splitRequest[1].equals("PINS")) {
                return board.getPins();
            } else {
                return "ERROR INVALID_FORMAT"; // message
            }

            String color = null;
            Integer x = null, y = null;
            String refersTo = null;

            // allows for different argument ordering and duplicate filters
            for (int i = 1; i < splitRequest.length; i++) {
                String p = splitRequest[i];

                if (p.startsWith("color=")) {
                    if (color != null)
                        return "ERROR INVALID_FORMAT"; // Duplicate check
                    color = p.substring(6);
                    if (color.isEmpty())
                        return "ERROR INVALID_FORMAT";
                } else if (p.startsWith("contains=")) {
                    if (x != null)
                        return "ERROR INVALID_FORMAT"; // Duplicate check
                    // ex if you had xy
                    if (i + 1 >= splitRequest.length) {
                        return "ERROR INVALID_FORMAT";
                    }
                    // would reject something like x*y or 10 20abc
                    try {
                        x = Integer.parseInt(p.substring(9));
                        y = Integer.parseInt(splitRequest[++i]);
                    } catch (Exception e) {
                        return "ERROR INVALID_FORMAT";
                    }
                } else if (p.startsWith("refersTo=")) {
                    if (refersTo != null)
                        return "ERROR INVALID_FORMAT"; // Duplicate check

                    StringBuilder sb = new StringBuilder(p.substring(9));

                    for (int j = i + 1; j < splitRequest.length; j++) {
                        sb.append(" ").append(splitRequest[j]);
                    }

                    refersTo = sb.toString();
                    if (refersTo.trim().isEmpty() || refersTo.length() > Note.MAX_MESSAGE_LENGTH) {
                        return "ERROR INVALID_FORMAT";
                    }
                    // VERY IMPORTANT: Tell the main loop we are done
                    break;

                } else {
                    // This catches "incorrect argument ordering"
                    // e.g., if they put '10 20' without 'contains='
                    return "ERROR INVALID_FORMAT";
                }
            }
            return board.getNotes(color, x, y, refersTo);
        }

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