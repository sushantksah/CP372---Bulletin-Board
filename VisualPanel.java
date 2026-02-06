import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class VisualPanel extends JPanel {

    public static class NoteView {
        public final int x, y;
        public final String color;
        public final String message;
        public final boolean pinned;

        public NoteView(int x, int y, String color, String message, boolean pinned) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.message = message;
            this.pinned = pinned;
        }
    }

    private int boardW = 0, boardH = 0, noteW = 1, noteH = 1;
    private List<String> validColors = new ArrayList<>();
    private List<NoteView> notes = new ArrayList<>();
    private List<int[]> pins = new ArrayList<>();

    /** Pin circle radius in pixels (drawn at board coordinates). */
    private static final int PIN_CIRCLE_RADIUS = 5;

    public VisualPanel() {
        setPreferredSize(new Dimension(520, 520));
        setBackground(Color.WHITE);
    }

    public void setBoardConfig(int boardW, int boardH, int noteW, int noteH, List<String> colors) {
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = Math.max(1, noteW);
        this.noteH = Math.max(1, noteH);
        this.validColors = new ArrayList<>(colors);
        repaint();
    }

    public void setNotes(List<NoteView> notes) {
        this.notes = new ArrayList<>(notes);
        repaint();
    }

    public void setPins(List<int[]> pins) {
        this.pins = new ArrayList<>(pins);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (boardW <= 0 || boardH <= 0) {
            drawCenteredText(g, "Connect to server to load board…");
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int pad = 12;
        int w = getWidth() - pad * 2;
        int h = getHeight() - pad * 2;

        int cell = Math.max(6, Math.min(w / boardW, h / boardH)); // cell size in pixels

        int gridWpx = cell * boardW;
        int gridHpx = cell * boardH;

        int startX = (getWidth() - gridWpx) / 2;
        int startY = (getHeight() - gridHpx) / 2;

        // Grid
        g2.setColor(new Color(235, 235, 235));
        for (int x = 0; x <= boardW; x++) {
            int px = startX + x * cell;
            g2.drawLine(px, startY, px, startY + gridHpx);
        }
        for (int y = 0; y <= boardH; y++) {
            int py = startY + y * cell;
            g2.drawLine(startX, py, startX + gridWpx, py);
        }

        // Notes (draw behind text)
        for (NoteView n : notes) {
            Color fill = mapColorName(n.color);
            int px = startX + n.x * cell;
            int py = startY + n.y * cell;
            int nw = noteW * cell;
            int nh = noteH * cell;

            g2.setColor(fill);
            g2.fillRoundRect(px + 1, py + 1, nw - 2, nh - 2, 10, 10);

            // Border
            g2.setColor(n.pinned ? Color.BLACK : new Color(120, 120, 120));
            g2.setStroke(new BasicStroke(n.pinned ? 2.2f : 1.2f));
            g2.drawRoundRect(px + 1, py + 1, nw - 2, nh - 2, 10, 10);

            // Label (color + message snippet)
            g2.setColor(Color.BLACK);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
            String label = n.color + ": " + (n.message == null ? "" : n.message);
            label = shorten(label, 22);
            g2.drawString(label, px + 6, py + 18);

            if (n.pinned) {
                g2.setFont(getFont().deriveFont(Font.BOLD, 12f));
                g2.drawString("📌", px + nw - 20, py + 18);
            }
        }

        // Pins: black circles at each pin (x,y) in board coordinates
        int pinR = Math.min(PIN_CIRCLE_RADIUS, Math.max(2, cell / 3));
        g2.setColor(Color.BLACK);
        for (int[] pin : pins) {
            int px = startX + pin[0] * cell + cell / 2;
            int py = startY + pin[1] * cell + cell / 2;
            g2.fillOval(px - pinR, py - pinR, pinR * 2, pinR * 2);
        }

        // Border around board
        g2.setColor(new Color(180, 180, 180));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(startX, startY, gridWpx, gridHpx);

        g2.dispose();
    }

    private void drawCenteredText(Graphics g, String s) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(80, 80, 80));
        g2.setFont(getFont().deriveFont(Font.PLAIN, 14f));
        FontMetrics fm = g2.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(s)) / 2;
        int y = (getHeight() + fm.getAscent()) / 2;
        g2.drawString(s, x, y);
        g2.dispose();
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private Color mapColorName(String name) {
        if (name == null) return new Color(255, 241, 118); // soft yellow
        String n = name.trim().toLowerCase();

        // Common names
        switch (n) {
            case "red": return new Color(239, 83, 80);
            case "blue": return new Color(66, 165, 245);
            case "green": return new Color(102, 187, 106);
            case "yellow": return new Color(255, 238, 88);
            case "orange": return new Color(255, 167, 38);
            case "purple": return new Color(171, 71, 188);
            case "pink": return new Color(240, 98, 146);
            case "white": return new Color(245, 245, 245);
            case "black": return new Color(33, 33, 33);
        }

        // Deterministic fallback (hash → pastel)
        int h = Math.abs(n.hashCode());
        int r = 120 + (h % 110);
        int g = 120 + ((h / 3) % 110);
        int b = 120 + ((h / 7) % 110);
        return new Color(r, g, b);
    }
}
