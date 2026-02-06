package Server;
import java.util.ArrayList;
import java.util.List;

/*
Note class is the object that contains the note and the pins on the note.
Manages note data, including position, color, message, and pins.
*/
public class Note {
    public final int x, y;
    public final int noteWidth, noteHeight;
    public final String color;
    public final String message;
    private List<Pin> pins;
    private boolean isPinned;

    // Maximum length of the message for a note to prevent excessive memory usage (RFC - 12)
    public static final int MAX_MESSAGE_LENGTH = 256;

    // Initializes the note position, color, message, and pins, initially not pinned
    public Note(int x, int y, String color, String message, int noteWidth, int noteHeight) {
        this.x = x;
        this.y = y;
        this.noteWidth = noteWidth;
        this.noteHeight = noteHeight;
        this.color = color;
        this.message = message;
        this.pins = new ArrayList<>();
        this.isPinned = false;
    }

    /* Getters */
    public int getX() { return this.x; }
    public int getY() { return this.y; }
    public String getColor() { return this.color; }
    public String getMessage() { return this.message; }
    public List<Pin> getPins() { return this.pins; }
    public boolean getPinnedStatus() { return this.isPinned; }

    /* Setters */
    // Updates the pinned status of the note
    public void setPinStatus(boolean bool) { this.isPinned = bool; }

    // Adds a pin to the note
    public void addPin(Pin pin) {
        this.pins.add(pin);
        if (this.pins.size() == 1) this.isPinned = true;
    }

    // Removes a pin from the note
    public void removePin(Pin pin) {
        this.pins.remove(pin);
        if (this.pins.size() == 0) this.isPinned = false;
    }

    // Checks if a pin is at a specific point on the note
    public boolean hasPinAt(int pinX, int pinY) {
        for (Pin p : pins) {
            if (p.getX() == pinX && p.getY() == pinY) return true;
        }
        return false;
    }

    // Clears all pins from the note
    public void clearPins() {
        this.pins.clear();
        this.isPinned = false;
    }

    /* Helper methods */
    // Checks if a point is on the note, pins cannot be on the edge of the note (as specified on Piazza)
    public boolean containsPoint(int pointX, int pointY) {
        return this.x < pointX && pointX < (this.noteWidth + this.x)
            && this.y < pointY && pointY < (noteHeight + this.y);
    }
}