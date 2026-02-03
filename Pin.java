import java.util.Objects;

public class Pin {
    public final int x, y;

    public Pin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pin)) return false;
        Pin other = (Pin)o;
        return this.x == other.x && this.y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}