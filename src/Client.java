import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client implements Runnable {

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
            inputLine = new BufferedReader(new InputStreamReader(System.in));

            clientPrinter = new PrintStream(clientSocket.getOutputStream());
            clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Unknown host: " + host);
            return;
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return;
        }

        try {
            new Thread(new Client()).start();
            while (!closed) {
                clientPrinter.println(inputLine.readLine());
            }

            clientPrinter.close();
            clientReader.close();
            clientSocket.close();
        } catch (IOException e) {
            System.err.println("IOException:  " + e.getMessage());
        }
    }

    public void run() {
        String responseLine;
        try {
            while ((responseLine = clientReader.readLine()) != null) {
                if (responseLine.equals("Bye")) {
                    closed = true;
                    System.out.println("Closing...");
                    break;
                }
                System.out.println(responseLine);
            }
        } catch (IOException e) {
            System.err.println("IOException:  " + e.getMessage());
        }
    }
}