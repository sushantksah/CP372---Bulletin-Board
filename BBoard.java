
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class BBoard {
    public static void main(String[] args) throws Exception {
        if (args.length < 6){
            System.err.println("Usage: java Bulletin Board <port> <board_w> <board_h> <note_w> <note_h> <color1> ... <colorN>\"");
            return; 
        }

        int port = Integer.parseInt(args[0]); 
        int boardWidth = Integer.parseInt(args[1]);
        int boardHeight = Integer.parseInt(args[2]);
        int noteWidth = Integer.parseInt(args[3]);
        int noteHeight = Integer.parseInt(args[4]);



        List<String> colors = new ArrayList<>();
        for (int i =5; i< args.length; i++)
            colors.add(args[i]);


        Board board = new Board(boardWidth, boardHeight, noteWidth, noteHeight, colors);

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("BBoard server listening on port " + port);

        while (true) {
            Socket client = serverSocket.accept();
            Thread t = new Thread(new ClientHandler(client, board));
            t.start();
        
    }

}
}
