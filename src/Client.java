import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Класс Клиент
 *
 * @author Александра Малявко
 * @version 2020
 */

public class Client implements Runnable {

    private static final Logger logger = LogManager.getLogger(Client.class.getName());

    private static BufferedReader clientReader = null;
    private static boolean closed = false;

    public static void main(String[] args) {
        int portNumber = 2511;
        String host = "localhost";

        BufferedReader inputLine;
        PrintStream clientPrinter;
        Socket clientSocket;
        try {
            clientSocket = new Socket(host, portNumber);

            // для ввода сообщений с консоли
            inputLine = new BufferedReader(new InputStreamReader(System.in));

            // для записи в клиентский поток
            clientPrinter = new PrintStream(clientSocket.getOutputStream());

            // для чтения получаемых сообщений
            clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        } catch (UnknownHostException e) {
            logger.error("Unknown host: " + host);
            return;
        } catch (IOException e) {
            logger.error("IOException: " + e.getMessage());
            return;
        }

        try {
            new Thread(new Client()).start();

            // пишет в клиентский поток все, что читает из stdin
            while (!closed) {
                clientPrinter.println(inputLine.readLine());
            }

            clientPrinter.close();
            clientReader.close();
            clientSocket.close();
        } catch (IOException e) {
            logger.error("IOException:  " + e.getMessage());
        }
    }

    /**
     * Метод, выводящий в stdin всё, что получает из клиентского потока
     */
    public void run() {
        String line;
        try {
            while ((line = clientReader.readLine()) != null) {
                if (line.equals("Bye")) {
                    closed = true;
                    System.out.println("Closing...");
                    break;
                }
                System.out.println(line);
            }
        } catch (IOException e) {
            logger.error("IOException:  " + e.getMessage());
        }
    }
}