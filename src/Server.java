import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {

    private static final ArrayList<ClientThread> clientThreads = new ArrayList<>();
    private static final String FILENAME = "src\\clients.txt";
    private static int clientID = 1;

    public static void main(String[] args) throws IOException {
        File f = new File(FILENAME);
        if (f.exists()) {
            if (!f.delete()) {
                System.err.println("Cannot delete file " + FILENAME);
                return;
            }
        }
        if (!f.createNewFile()) {
            System.err.println("Cannot create file " + FILENAME);
            return;
        }

        int portNumber = 2511;
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            System.err.println("Cannot create server socket with port " + portNumber);
            e.printStackTrace();
            return;
        }

        System.out.println("Server running!");

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                var newThread = new ClientThread(clientSocket, clientThreads, clientID++);
                newThread.start();
                clientThreads.add(newThread);

                System.out.println("New client with ID " + (clientID - 1) + " connected!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}