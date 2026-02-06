package Server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Board class is the object that contains the board and the notes and pins.
 * Manages the shared bulletin board data, including note placement, pinning logic, and validation.
 */

public class Board {
    private int boardWidth;
    private int boardHeight;
    private int noteWidth;
    private int noteHeight;
    private List<String> validColors;
    private List<Note> notes;

    // Initializes the board dimensions, note size, and allowed colors
    public Board(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> validColors) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.noteWidth = noteWidth;
        this.noteHeight = noteHeight;
        this.validColors = validColors;
        this.notes = new ArrayList<>();
    }

    /* Helper methods */

    // Checks if a note already exists at the exact same x and y coordinates 
    private boolean completelyOverlaps(int x, int y) {
        for (Note note : this.notes) {
            if (note.getX() == x && note.getY() == y) {
                return true;
            }
        }
        return false;
    }

    // Validates if a note of fits within the board boundaries 
    private boolean noteInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x + noteWidth <= this.boardWidth && y + noteHeight <= this.boardHeight;
    }

    // Checks if a point coordinate is within the board boundaries
    private boolean pointInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < this.boardWidth && y < this.boardHeight;
    }

    // Builds the response for a GET command
    private String buildGetResponse(List<Note> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("OK ").append(results.size());

        for (Note note : results) {
            sb.append("\nNOTE ").append(note.getX()).append(" ").append(note.getY()).append(" ").append(note.getColor())
                    .append(" ").append(note.getMessage()).append(" PINNED=").append(note.getPinnedStatus());
        }

        return sb.toString();
    }

    // Builds the response for a GET PINS command 
    private String buildGetPinsResponse(Set<Pin> allPins) {

        StringBuilder sb = new StringBuilder();
        sb.append("OK ").append(allPins.size());

        for (Pin pin : allPins) {
            sb.append("\nPIN ").append(pin.getX()).append(" ").append(pin.getY());
        }

        return sb.toString();
    }

    /* Public methods */

    // Adds a new note to the board if validations pass
    public synchronized String addNote(int x, int y, String color, String message) {
        if (!noteInBounds(x, y)) {
            return "ERROR OUT_OF_BOUNDS";
        }
        if (!validColors.contains(color)) {
            return "ERROR COLOR_NOT_SUPPORTED";
        }
        if (completelyOverlaps(x, y)) {
            return "ERROR COMPLETE_OVERLAP";
        }

        Note note = new Note(x, y, color, message, this.noteWidth, this.noteHeight);
        this.notes.add(note);
        return "OK NOTE_POSTED";
    }

    // Adds a new pin at a specific point to a note if validations pass
    public synchronized String addPin(int pinX, int pinY) {
        if (!pointInBounds(pinX, pinY))
            return "ERROR OUT_OF_BOUNDS";

        boolean noteFound = false;

        for (Note note : this.notes) {
            if (note.containsPoint(pinX, pinY)) {
                noteFound = true;

                if (note.hasPinAt(pinX, pinY)) {
                    continue; // ignore duplicates
                }
                note.addPin(new Pin(pinX, pinY));
            }
        }

        if (!noteFound) {
            return "ERROR NO_NOTE_AT_COORDINATE";
        }

        return "OK PIN_ADDED";
    }

    // Removes a pin at a specific point from a note if validations pass
    public synchronized String unPin(int pinX, int pinY) {
        if (!pointInBounds(pinX, pinY)) {
            return "ERROR OUT_OF_BOUNDS";
        }

        boolean found = false;
        Pin targetPin = new Pin(pinX, pinY);

        for (Note note : this.notes) {
            if (note.getPins().removeIf(p -> p.equals(targetPin))) {
                found = true;
                if (note.getPins().isEmpty()) {
                    note.setPinStatus(false);
                }
            }
        }

        return found ? "OK PIN_REMOVED" : "ERROR PIN_NOT_FOUND";
    }

    // Removes all notes with no pins from the board
    public synchronized String shake() {

        this.notes.removeIf(note -> note.getPins().isEmpty());
        return "OK SHAKE_COMPLETE";

    }

    // Removes all pins and all notes on the board
    public synchronized String clear() {
        for (Note note : this.notes) {
            note.clearPins();
        }

        this.notes.clear();
        return "OK CLEAR_COMPLETE";

    }

    // Gets notes on the board that match the given filters, returns all notes if no filters are provided
    public synchronized String get(String colorFilter, Integer containsX, Integer containsY, String refersToSubstring) {
        List<Note> results = new ArrayList<>(this.notes);

        if (colorFilter != null) {
            if (!validColors.contains(colorFilter)) {
                return "ERROR COLOR_NOT_SUPPORTED";
            }
            results.removeIf(note -> !note.getColor().equals(colorFilter));
        }

        if (containsX != null && containsY != null) {
            if (!pointInBounds(containsX, containsY)) {
                return "ERROR OUT_OF_BOUNDS";
            }
            results.removeIf(note -> !note.containsPoint(containsX, containsY));
        }

        if (refersToSubstring != null) {
            results.removeIf(note -> !note.getMessage().contains(refersToSubstring));
        }

        return buildGetResponse(results);
    }

    // Helper wrapper class for GET, used by RequestParser
    public String getNotes(String color, Integer x, Integer y, String refersTo) {
        return get(color, x, y, refersTo);
    }

    // Gets all pins on the board
    public synchronized String getPins() {
        Set<Pin> allPins = new HashSet<>();

        for (Note note : this.notes) {
            allPins.addAll(note.getPins());
        }

        return buildGetPinsResponse(allPins);
    }

    // Builds the greeting line for the board for newly connected clients
    public String greetingLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(boardWidth).append(" ").append(boardHeight).append(" ")
                .append(noteWidth).append(" ").append(noteHeight);
        for (String c : validColors)
            sb.append(" ").append(c);
        return sb.toString();
    }

    // Handles a command from a client, returns a list of response lines
    public List<String> handleCommand(String line) {
        String resp = RequestParser.parseAndExecute(line, this);
        return Arrays.asList(resp.split("\n", -1));
    }

}
