import java.util.List;
import java.util.ArrayList;

public class Note {
    public final int x, y;
    public final int noteWidth, noteHeight;
    public final String color;
    public final String message;
    private List<Pin> pins;
    private boolean isPinned;
    public final int MAX_MESSAGE_LENGTH = 256;


// i can't remember if note width and height are passed in here or not
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

    // Getters
    public Integer getX() {
        return this.x;
    }

    public Integer getY() {
        return this.y;
    }

    public String getColor() {
        return this.color;
    }

    public String getMessage() {
        return this.message;
    }

    public List<Pin> getPins() {
        return this.pins;
    }

    public boolean getPinnedStatus() {
        return this.isPinned;
    }

    // Setters
    public void setPinStatus(boolean bool) {
        this.isPinned = bool;
    }

    public void addPin(Pin pin) {
        this.pins.add(pin);

        // no longer 0 pins
        if (this.pins.size() == 1) {
            this.isPinned = true;
        }
    }

    public void removePin(Pin pin) {
        this.pins.remove(pin);

        // no longer 0 pins
        if (this.pins.size() == 0) {
            this.isPinned = false;
        }
    }

    // Containment, ensuring that the points lie within the note rectangle
    // pins cannot be on the edge of the note
    public boolean containsPoint(int pointX, int pointY) {
        return this.x < pointX && pointX < (this.noteWidth + this.x) && this.y < pointY
                && pointY < (noteHeight + this.y);
    }
}