import java.util.List;
import java.util.ArrayList;
// import Note.java;
// import Pin.java;

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

    private boolean pinInNote(int pinX, int pinY, int noteX, int noteY) {
        // can't have pins on the edge of the note
        return this.boardWidth < pinX &&  pinX < (this.boardWidth + noteWidth) && this.boardHeight < pinY &&  pinY < (this.boardHeight + noteHeight);

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
                if (pinInNote(pinX, pinY, note.x, note.y)) {
                    note.addPin(new Pin(pinX, pinY));
                    return "OK PIN_POSTED";
                } else {
                    return "ERROR OUT_OF_BOUNDS"; // don't know if this is the right error message
                }
            }
            return "OK PIN_POSTED";
        }
    }
}
