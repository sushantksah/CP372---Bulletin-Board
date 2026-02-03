public class Board {
    private int boardWidth;
    private int boardHeight;
    private int noteWidth;
    private int noteHeight;
    private List<String> colors;
    private List<Note> notes;

    public Board(int boardWidth, int boardHeight, int noteWidth, int noteHeight, List<String> colors) {

        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.noteWidth = noteWidth;
    }
    // }

    // public void update(Graphics g) {
    // this.draw(g);
    // }

    // public void resize(int newWidth, int newHeight) {
    // this.boardWidth = newWidth;
    // this.boardHeight = newHeight;
    // this.notes.clear();
    // for (int i = 0; i < boardWidth; i++) {
    // for (int j = 0; j < boardHeight; j++) {
    // this.notes.add(new Note(i * noteWidth, j * noteHeight, colors.get(i %
    // colors.size()), ""));
    // }
    // }
    // }

    // public void addNote(Note note) {
    // this.notes.add(note);
    // }

    // public void removeNote(Note note) {
    // this.notes.remove(note);
    // }

    // public void draw(Graphics g) {
    // for (Note note : this.notes) {
    // g.setColor(note.color);
    // g.fillRect(note.x, note.y, note.width, note.height);
    // }
    // }

    // public void update(Graphics g) {
    // this.draw(g);
    // }

    // public void resize(int newWidth, int newHeight) {
    // this.boardWidth = newWidth;
    // this.boardHeight = newHeight;
    // this.notes.clear();
    // for (int i = 0; i < boardWidth; i++) {
    // for (int j = 0; j < boardHeight; j++) {
    // this.notes.add(new Note(i * noteWidth, j * noteHeight, colors.get(i %
    // colors.size()), ""));
    // }
    // }
    // }

    // public void addNote(Note note) {
    // this.notes.add(note);
    // }

    // public void removeNote(Note note) {
    // this.notes.remove(note);
    // }

    // public void draw(Graphics g) {
    // for (Note note : this.notes) {
    // g.setColor(note.color);
    // g.fillRect(note.x, note.y, note.width, note.height);
    // }
    // }

    // public void update(Graphics g) {
    // this.draw(g);
    // }
}
