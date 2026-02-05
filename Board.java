import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            if (note.getX() == x && note.getY() == y) {
                return true;
            }
        }
        return false;
    }

    private boolean noteInBounds(int x, int y) { // Note occupies (x, y) to (x+noteWidth-1, y+noteHeight-1), must be within board
        return x >= 0 && y >= 0 && x + noteWidth <= this.boardWidth && y + noteHeight <= this.boardHeight;
    }

    private boolean pointInBounds(int x, int y) {
       
        return x >= 0 && y >= 0 && x < this.boardWidth && y < this.boardHeight;
    }

    private String buildGetResponse(List<Note> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("OK ").append(results.size());

        for (Note note : results) {
            sb.append("\nNOTE ").append(note.getX()).append(" ").append(note.getY()).append(" ").append(note.getColor())
                    .append(" ").append(note.getMessage()).append(" PINNED=").append(note.getPinnedStatus());
        }

        return sb.toString();
    }

    private String buildGetPinsResponse(Set<Pin> allPins) {

        // OK <count>
        // PIN x y
        // PIN x y
        // …

        StringBuilder sb = new StringBuilder();
        sb.append("OK ").append(allPins.size());

        for (Pin pin : allPins) {
            sb.append("\nPIN ").append(pin.getX()).append(" ").append(pin.getY());
        }

        return sb.toString();
    }

    // Methoddssss

    public synchronized String addNote(int x, int y, String color, String message) {

        // Validate in RFC-specified order: bounds, color, overlap
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

    public synchronized String shake() {
        // List<Note> notesToRemove = new ArrayList<>();
        // for (Note note : this.notes) {
        // if (note.pins.isEmpty()) {
        // notesToRemove.add(note);
        // }
        // }
        // this.notes.removeAll(notesToRemove);

        this.notes.removeIf(note -> note.getPins().isEmpty());

        return "OK SHAKE_COMPLETE";
        // any error cases here?
    }

    public synchronized String clear() {
        for (Note note : this.notes) {
            note.clearPins();
        }

        this.notes.clear();

        return "OK CLEAR_COMPLETE";
        // any error cases here?
    }

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
            // if valid string?? - should be checked here or somehwere else
            // get message can be empty? how are we approaching empty again?
            results.removeIf(note -> !note.getMessage().contains(refersToSubstring));
        }

        return buildGetResponse(results);
    }

    // Wrapper class for GET
    public String getNotes(String color, Integer x, Integer y, String refersTo) {
        return get(color, x, y, refersTo);
    }

    public String greetingLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(boardWidth).append(" ").append(boardHeight).append(" ")
          .append(noteWidth).append(" ").append(noteHeight);
        for (String c : validColors) sb.append(" ").append(c);
        return sb.toString();
    }

    public List<String> handleCommand(String line) {
    // RequestParser returns a multi-line String (likely)
    String resp = RequestParser.parseAndExecute(line, this);// if your method name differs, adjust
    return Arrays.asList(resp.split("\n", -1));
}

    public synchronized String getPins() {

        // ensures no duplicates
        Set<Pin> allPins = new HashSet<>();

        for (Note note : this.notes) {
            allPins.addAll(note.getPins());
        }

        return buildGetPinsResponse(allPins);
    }
}
