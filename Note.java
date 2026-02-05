import java.util.ArrayList;
import java.util.List;

public class Note {
    public final int x, y;
    public final int noteWidth, noteHeight;
    public final String color;
    public final String message;
    private List<Pin> pins;
    private boolean isPinned;

    // MUST be static if other classes call Note.MAX_MESSAGE_LENGTH
    public static final int MAX_MESSAGE_LENGTH = 256;

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

    public int getX() { return this.x; }
    public int getY() { return this.y; }

    public String getColor() { return this.color; }
    public String getMessage() { return this.message; }
    public List<Pin> getPins() { return this.pins; }
    public boolean getPinnedStatus() { return this.isPinned; }

    public void setPinStatus(boolean bool) { this.isPinned = bool; }

    public void addPin(Pin pin) {
        this.pins.add(pin);
        if (this.pins.size() == 1) this.isPinned = true;
    }

    public void removePin(Pin pin) {
        this.pins.remove(pin);
        if (this.pins.size() == 0) this.isPinned = false;
    }

    public boolean hasPinAt(int pinX, int pinY) {
        for (Pin p : pins) {
            if (p.getX() == pinX && p.getY() == pinY) return true;
        }
        return false;
    }

    public void clearPins() {
        this.pins.clear();
        this.isPinned = false;
    }

    // Pin cannot be on the edge of the note (as specified on Piazza)
    public boolean containsPoint(int pointX, int pointY) {
        return this.x < pointX && pointX < (this.noteWidth + this.x)
            && this.y < pointY && pointY < (noteHeight + this.y);
    }
}