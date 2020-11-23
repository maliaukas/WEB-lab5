import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Класс Клиентский поток. Проводит рассылку сообщений всем клиентам,
 * обновляет данные в файле со списком клиентов.
 *
 * @author Александра Малявко
 * @version 2020
 */

class ClientThread extends Thread {

    private static final Logger logger = LogManager.getLogger(ClientThread.class.getName());

    private static final String FILENAME = "src\\clients.txt";
    private static final String exitMessage = "/exit";

    private final ArrayList<ClientThread> clientThreads;
    private final Socket clientSocket;
    private final int clientID;
    private String clientName = null;
    private PrintStream clientPrinter = null;
    private BufferedReader clientReader = null;

    /**
     * Конструктор
     *
     * @param clientSocket клиентский сокет
     * @param threads      список всех клиентских потоков
     * @param clientID     идентификатор клиента
     */
    public ClientThread(Socket clientSocket, ArrayList<ClientThread> threads, int clientID) {
        this.clientSocket = clientSocket;
        this.clientThreads = threads;
        this.clientID = clientID;
    }

    /**
     * Метод, осуществляющий логику потока
     */
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
            logger.error("Client thread " + clientID + " stopped");
        } catch (IOException e) {
            logger.error("IOException: " + e.getMessage());
        }
    }

    /**
     * Метод, запрашивающий у пользователя имя
     *
     * @return имя пользователя
     * @throws IOException в случае неудачного чтения
     */
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

    /**
     * Метод, добавляющий в файл строку вида имя-клиента_ID
     *
     * @throws IOException в случае неудачной записи
     */
    private void appendNameToFile() throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILENAME, true))) {
            bw.write(clientName + "_" + clientID + "\n");
        }
        logger.debug("Client <" + clientName + "> added to file");
    }

    /**
     * Метод, выводящий список клиентов, находящихся онлайн
     *
     * @throws IOException в случае неудачного чтения из файла
     */
    private void printPeopleOnline() throws IOException {
        clientPrinter.println("\nPeople online:");
        try (BufferedReader in = new BufferedReader(new FileReader(FILENAME))) {
            String nickname;
            while ((nickname = in.readLine()) != null) {
                clientPrinter.println("\t- " +
                        nickname.substring(0, nickname.lastIndexOf("_")));
            }
        }
    }

    /**
     * Метод, выполняющий чтение и отправку сообщения
     *
     * @return true - если сообщение отправлено, false - если введено exitMessage
     * @throws IOException в случае неудачного чтения
     */
    private boolean sendMessage() throws IOException {
        String line;
        while (true) {
            line = clientReader.readLine().trim();
            if (line.isBlank()) {
                clientPrinter.println("Message should not be empty!");
            } else break;
        }

        if (line.equals(exitMessage)) {
            return false;
        }

        synchronized (this) {
            String message = clientName + ": " + line;
            for (var thr : clientThreads) {
                thr.clientPrinter.println(message);
            }
            logger.debug(message);
        }
        return true;
    }

    /**
     * Метод, удаляющий из файла со списком клиентов строку,
     * соответсвующую текущему клиенту
     */
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
                logger.error("Could not delete file " + inFile);
                return;
            }

            if (!tempFile.renameTo(inFile)) {
                logger.error("Could not rename file " + tempFile);
            }
        } catch (IOException ex) {
            logger.error("IOException: " + ex.getMessage());
        }
    }

    /**
     * Метод, уведомляющий всех клиентов о подключении/отключении клиента
     *
     * @param leave false - в случае подключения, true - в случае отключения
     */
    private synchronized void notifyEveryone(boolean leave) {
        String msg = clientName + (leave ? " left chat!" : " connected!");
        for (var thr : clientThreads) {
            if (thr != this) {
                thr.clientPrinter.println(msg);
            }
        }
        logger.debug(msg);
    }
}