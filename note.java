public class Note {
    public final int x, y;
    public final String color;
    public final String msg;

    public Note(int x, int y, String color, String msg) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.msg = msg;
    }

    // Containment, ensuring that the points lie within the note rectangle 
    public boolean contains(int pos_x, int pos_y, int noteWidth, int noteHeight) {
        return pos_x >= x && pos_x < (x + noteWidth) && pos_y >= y && pos_y < (y + noteHeight);
    }
}