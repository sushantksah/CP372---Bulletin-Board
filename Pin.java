import java.util.Objects;

public class Pin {
    private final int x;
    private final int y;

    public Pin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pin)) return false;
        Pin other = (Pin)o;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}