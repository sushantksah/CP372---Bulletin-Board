import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

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

    private boolean pointInBounds(int x, int y) {
        // can't have notes on the very edge?
        return x < this.boardWidth && y < this.boardHeight && x > 0 && y > 0;
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

    public synchronized String addNote(Note note) {
        if (!noteInBounds(note.x, note.y)) {
            return "ERROR OUT_OF_BOUNDS";
        } else if (completelyOverlaps(note.x, note.y)) {
            return "ERROR COMPLETE_OVERLAP";
        } else if (!validColors.contains(note.color)) {
            // ERROR COLOR_NOT_VALID
            return "ERROR COLOR_NOT_VALID";
        } else {
            this.notes.add(note);
            // OK NOTE_POSTED
            return "OK NOTE_POSTED";
        }

    }

    public synchronized String addPin(int pinX, int pinY) {
        if (!pointInBounds(pinX, pinY)) {
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
        if (!pointInBounds(pinX, pinY)) {
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
            results.removeIf(note -> !noteContainsPoint(note, containsX, containsY));
        }

        if (refersToSubstring != null) {
            // if valid string??
            results.removeIf(note -> !note.getMessage().contains(refersToSubstring));
        }

        return buildGetResponse(results);
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
