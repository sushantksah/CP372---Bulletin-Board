import java.util.List;
import java.util.ArrayList;

public class Board {
    private int boardWidth;
    private int boardHeight;
    private int noteWidth;
    private int noteHeight;
    private List<String> validColors;
    private List<Note> notes;

    public Board(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> validColors) {

        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.noteWidth = noteWidth;
        this.noteHeight = noteHeight;
        this.validColors = validColors;
        this.notes = new ArrayList<>();
    }

    private boolean completelyOverlaps(int x, int y) {
        for (Note note : this.notes) {
            if (note.contains(x, y, this.noteWidth, this.noteHeight)) {
                return true;
            }
        }
        return false;
    }

    private boolean noteInBounds(int x, int y) {
        // can't have notes on the very edge? 
        return x + noteWidth < this.boardWidth && y + noteHeight < this.boardHeight && x > 0 && y > 0;

    }

    private boolean pinInBounds(int x, int y) {
        // can't have notes on the very edge? 
        return x < this.boardWidth && y < this.boardHeight && x > 0 && y > 0;
    }

 
    // Methoddssss

    public synchronized String addNote(Note note) {
        if (!noteInBounds(note.x, note.y)) {
            return "ERROR OUT_OF_BOUNDS";
        } else if (completelyOverlaps(note.x, note.y)) {
            return "ERROR COMPLETE_OVERLAP";
        } else if (!validColors.contains(note.color)) {
            // ERROR COLOR_NOT_VALID
            return "ERROR COLOR_NOT_VALID";
        }  else {
            this.notes.add(note);
            // OK NOTE_POSTED
            return "OK NOTE_POSTED";
        }
        
    }

    public synchronized String addPin(int pinX, int pinY){
        if (!pinInBounds(pinX, pinY)) {
            return "ERROR OUT_OF_BOUNDS";
        } else {
            for (Note note : this.notes) {
                if (note.contains(pinX, pinY)) {
                    note.addPin(new Pin(pinX, pinY));
                    return "OK PIN_POSTED";
                } else {
                    return "ERROR OUT_OF_BOUNDS"; // don't know if this is the right error message
                }
            }
            return "OK PIN_POSTED";
        }
    }

    public synchronized String unPin(int pinX, int pinY) {
        if (!pinInBounds(pinX, pinY)) {
            return "ERROR OUT_OF_BOUNDS";
        } else {
            for (Note note : this.notes) {
                for (Pin pin : note.pins) {
                    if (pin.equals(new Pin(pinX, pinY))) {
                        note.removePin(pin); // remove pin method
                        return "OK PIN_REMOVED";
                    }
                }
            }
            return "ERROR PIN_NOT_FOUND";
        }
    }

    public synchronized String shake() {
        List<Note> notesToRemove = new ArrayList<>();
        for (Note note : this.notes) {
            if (note.pins.isEmpty()) {
                notesToRemove.add(note);
            }
        }
        this.notes.removeAll(notesToRemove); // does this remove the pins too?
        return "OK SHAKE_COMPLETE";
        // any error cases here?
    }

    public synchronized String clear() {
        for (Note note : this.notes) {
            note.pins.clear();
        }

        this.notes.clear();

        return "OK CLEAR_COMPLETE";
        // any error cases here?
    }

    // General GET
    // GET [color=<c>] [contains=<x> <y>] [refersTo=<substring>]
    // Parameters
    // color=<c>: note color must equal c
    // contains=<x> <y>: the point (x,y) must lie inside the note rectangle.
    // refersTo=<substring>: the note message must contain the substring as a
    // case-sensitive match
    // Success Response
    // OK <count>
    // NOTE x y color message PINNED=true|false
    // NOTE …

    // Start with all notes
    // Apply filters sequentially:

    // color filter: keep only notes matching color
    // contains filter: keep only notes where (x, y) is inside rectangle
    // refersTo filter: keep only notes where message.contains(substring)

    // Build response: "OK <count>\n" + NOTE lines
    // Return response string

    // how do we want to pass in filters? i feel like the parser should categorize
    // the filters itself?
    // check if parameters are valid here?

    public synchronized String get(String colorFilter, Integer containsX, Integer containsY, String refersToFilter) {
        List<Note> results = new ArrayList<>(this.notes);

        if (colorFilter != null) {
            // if colour is in the valid color list
            results.removeIf(note -> !note.getColor().equals(colorFilter));
        }

        if (containsX != null && containsY != null) {
            // if coordinates are out of bounds
            if ()
            results.removeIf(note -> !noteContainsPoint(note, containsX, containsY));
        }

        if (refersToFilter != null) {
            // if valid string??
            results.removeIf(note -> !note.getMessage().contains(refersToFilter));
        }

        String response = buildGetResponse(results);

        return response;
    }

    // GET PINS
    // Returns coordinates of all pins on the board.
    // Success Response
    // OK <count>
    // PIN x y
    // PIN x y
    // …
    // Build response: "OK <count>\n" + PIN lines
    public synchronized String getPins() {

        // List<Pin> allPins = new ArrayList<>();
        // for (Note note : this.notes) {
        //     allPins.addAll(note.pins);
        // }

        // StringBuilder sb = new StringBuilder();
        // sb.append("OK ").append(allPins.size());
        // for (Pin pin : allPins) {
        //     sb.append("\nPIN ").append(pin.x).append(" ").append(pin.y);
        // }

        // return sb.toString();
    }

    // Semantics
    // If a parameter is missing, the server ignores that filter.
    // For example, if no color is provided, then notes of all colors are returned.
    // If the coordinate is outside the board, the server must respond with ERROR
    // OUT_OF_BOUNDS.
    // Each filter type may appear at most once.
    // If a filter type appears multiple times, the behaviour is undefined.
    // The server MAY reject with INVALID_FORMAT or MAY use the last occurrence.

}
