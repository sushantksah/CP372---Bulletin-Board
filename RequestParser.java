import java.util.*;

public class RequestParser {

    public static String parseAndExecute(String request, Board board) {
        if (request == null || request.trim().isEmpty()) {
            return "ERROR INVALID_FORMAT";
        }

        String trimmed = request.trim();
        String firstWord = trimmed.split("\\s+")[0].toUpperCase();

        String[] splitRequest;
        if (firstWord.equals("POST")) {
            splitRequest = trimmed.split("\\s+", 5); // x y color message
        } else {
            splitRequest = trimmed.split("\\s+");
        }

        try {
            switch (firstWord) {
                case "POST": //  x y color message
                    return handlePost(splitRequest, board);
                case "GET": // GET PINS orGET [color=<c>] [contains=<x> <y>] [refersTo=<substring>]
                    return handleGet(splitRequest, board);
                case "PIN": //<x> <y>
                    return handlePin(splitRequest, board);
                case "UNPIN": // UNPIN <x> <y>
                    return handleUnpin(splitRequest, board);
                case "SHAKE":
                    return board.shake();
                case "CLEAR":
                    return board.clear();
                case "DISCONNECT":
                    return "DISCONNECTING"; 
                default:
                    return "ERROR INVALID_FORMAT";
            }
        } catch (Exception e) {
            return "ERROR INVALID_FORMAT";
        }
    }

    private static String handlePost(String[] splitRequest, Board board) {

        // not enough parameters 
        if (splitRequest.length < 5) return "ERROR INVALID_FORMAT";
    
        try {
            int x = Integer.parseInt(splitRequest[1]);
            int y = Integer.parseInt(splitRequest[2]);
            String color = splitRequest[3]; // lowkey check if color is valid here instead
            
            if (color == null || color.trim().isEmpty()) {
                return "ERROR INVALID_FORMAT";
            }
            String message = splitRequest[4]; // Extract from the limit-split 
            
            if (message.trim().isEmpty() || message.length() > 256) { 
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
        
        if (splitRequest.length == 2 && splitRequest[1].equalsIgnoreCase("PINS")) {
            return board.getPins();
        }

        String color = null;
        Integer x = null, y = null;
        String refersTo = null;

        for (int i = 1; i < splitRequest.length; i++) {
            String p = splitRequest[i];
            if (p.startsWith("color=")) {
                color = p.substring(6);
            } else if (p.startsWith("contains=")) {
                try {
    
                    x = Integer.parseInt(p.substring(9));
                    y = Integer.parseInt(splitRequest[++i]); 
                } catch (Exception e) { return "ERROR INVALID_FORMAT"; }
            } else if (p.startsWith("refersTo=")) {
                refersTo = p.substring(9);
            }
        }
        return board.get(color, x, y, refersTo);
    }

    private static String handlePin(String[] splitRequest, Board board) {
        // if (splitRequest.length != 3) return "ERROR INVALID_FORMAT";
        // try {
        //     return board.addPin(Integer.parseInt(splitRequest[1]), Integer.parseInt(splitRequest[2]));
        // } catch (NumberFormatException e) {
        //     return "ERROR INVALID_FORMAT";
        // }
    }

    private static String handleUnpin(String[] splitRequest, Board board) {
        // Syntax: UNPIN x y (RFC 7.4)
        // if (splitRequest.length != 3) return "ERROR INVALID_FORMAT";
        // try {
        //     return board.unpin(Integer.parseInt(splitRequest[1]), Integer.parseInt(splitRequest[2]));
        // } catch (NumberFormatException e) {
        //     return "ERROR INVALID_FORMAT";
        // }
    }

}
