import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Класс Сервер
 *
 * @author Александра Малявко
 * @version 2020
 */

public class Server {

    private static final Logger logger = LogManager.getLogger(Server.class.getName());

    private static final ArrayList<ClientThread> clientThreads = new ArrayList<>();
    private static final String FILENAME = "src\\clients.txt";
    private static int clientID = 1;

    public static void main(String[] args) throws IOException {
        logger.debug("==========================================");

        File f = new File(FILENAME);
        if (f.exists()) {
            logger.debug("File " + FILENAME + " exists");
            if (!f.delete()) {
                logger.error("Cannot delete file " + FILENAME);
                return;
            }
            logger.debug("File " + FILENAME + " deleted");
        }
        if (!f.createNewFile()) {
            logger.error("Cannot create file " + FILENAME);
            return;
        }
        logger.debug("File " + FILENAME + " created");

        int portNumber = 2511;
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(portNumber);
        } catch (IOException e) {
            logger.error("Cannot create server socket with port " + portNumber);
            e.printStackTrace();
            return;
        }

        logger.info("Server running!");

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

                var newThread = new ClientThread(clientSocket, clientThreads, clientID);
                newThread.start();
                clientThreads.add(newThread);

                logger.info("New client with ID " + (clientID++) + " connected!");
            } catch (IOException e) {
                logger.error("IOException " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}