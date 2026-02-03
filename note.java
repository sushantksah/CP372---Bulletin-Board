import java.util.ArrayList;
import java.util.List;

public class Note {
    public final int x, y;
    public int noteWidth, noteHeight;
    public final String color;
    public final String message;
    public  List<Pin> pins;
    public boolean isPinned;

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

    // Containment, ensuring that the points lie within the note rectangle 
    // pins cannot be on the edge of the note
    public boolean contains(int pinX, int pinY) {
        return this.x < pinX &&  pinX < (this.noteWidth + this.x) && this.y < pinY &&  pinY < (noteHeight + this.y);
    }

    public void addPin(Pin pin) {
        this.pins.add(pin);

        // no longer 0 pins
        if (this.pins.size() == 1) {
            this.isPinned = true;
        }
    }
}