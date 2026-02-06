package Server;
import java.util.Objects;

/*
Pin class is the object that contains the pin and the coordinates of the pin.
Manages pin coordinates and equality.
*/

public class Pin {
    private final int x;
    private final int y;

    // Initializes the pin and its coordinates
    public Pin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /* Getters */
    public int getX() { return x; }
    public int getY() { return y; }

    /* Overrides */
    // Checks if two pins are equal if they have the same coordinates
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pin)) return false;
        Pin other = (Pin)o;
        return x == other.x && y == other.y;
    }

    // Returns the hash code of the pin
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}