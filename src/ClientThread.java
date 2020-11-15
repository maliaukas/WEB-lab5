import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

class ClientThread extends Thread {

    private static final String FILENAME = "src\\clients.txt";
    private static final String exitMessage = "/exit";

    private final ArrayList<ClientThread> clientThreads;
    private final Socket clientSocket;
    private final int clientID;
    private String clientName = null;
    private PrintStream clientPrinter = null;
    private BufferedReader clientReader = null;

    public ClientThread(Socket clientSocket, ArrayList<ClientThread> threads, int clientID) {
        this.clientSocket = clientSocket;
        this.clientThreads = threads;
        this.clientID = clientID;
    }

    private String askClientName() throws IOException {
        String name;
        while (true) {
            clientPrinter.println("Enter your name:");
            name = clientReader.readLine().trim();

            if (name.isBlank()) {
                clientPrinter.println("Name should not be empty!");
            } else break;
        }
        clientPrinter.println("Hello, " + name + "!");
        return name;
    }

    private void appendNameToFile() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILENAME, true))) {
            bw.write(clientName + "_" + clientID + "\n");
        }
    }

    private void printPeopleOnline() throws IOException {
        clientPrinter.println("People online:");
        try (BufferedReader in = new BufferedReader(new FileReader(FILENAME))) {
            String nickname;
            while ((nickname = in.readLine()) != null) {
                clientPrinter.println(nickname.substring(0, nickname.lastIndexOf("_")));
            }
        }
    }

    private boolean sendMessage() throws IOException {
        String line;
        while (true) {
            line = clientReader.readLine();
            if (line.trim().isBlank()) {
                clientPrinter.println("Message should not be empty!");
            } else break;
        }

        if (line.trim().equals(exitMessage)) {
            return false;
        }

        synchronized (this) {
            for (var thr : clientThreads) {
                thr.clientPrinter.println(clientName + ": " + line);
            }
        }
        return true;
    }

    public synchronized void removeNameFromFile() {
        try {
            File inFile = new File(FILENAME);

            File tempFile = new File(inFile.getAbsolutePath() + ".tmp");
            BufferedReader bufferedReader = new BufferedReader(new FileReader(FILENAME));
            PrintWriter printWriter = new PrintWriter(new FileWriter(tempFile));

            String nickname;
            while ((nickname = bufferedReader.readLine()) != null) {
                if (!nickname.trim().equals(clientName + "_" + clientID)) {
                    printWriter.println(nickname);
                    printWriter.flush();
                }
            }

            printWriter.close();
            bufferedReader.close();

            if (!inFile.delete()) {
                System.err.println("Could not delete file " + inFile);
                return;
            }

            if (!tempFile.renameTo(inFile)) {
                System.err.println("Could not rename file " + tempFile);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private synchronized void notifyEveryone(boolean leave) {
        for (var thr : clientThreads) {
            if (thr != this) {
                thr.clientPrinter.println(clientName + (leave ? " left chat!" : " connected!"));
            }
        }
    }

    public void run() {
        try {
            clientReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    clientSocket.getInputStream()));
            clientPrinter = new PrintStream(clientSocket.getOutputStream());
            clientName = askClientName();

            appendNameToFile();

            printPeopleOnline();

            clientPrinter.println("\nWelcome to chat! " +
                    "You can now write messages." +
                    "\nEnter " + exitMessage + " to exit.");

            notifyEveryone(false);

            while (true) {
                if (!sendMessage()) break;
            }

            removeNameFromFile();

            clientPrinter.println("Bye");

            notifyEveryone(true);

            synchronized (this) {
                clientThreads.remove(this);
            }

            clientReader.close();
            clientPrinter.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}