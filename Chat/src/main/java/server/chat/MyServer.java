package server.chat;



import org.apache.log4j.Logger;
import server.ServerApp;
import server.chat.auth.BaseAuthService;
import server.chat.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.sql.*;

public class MyServer {

    private final ServerSocket serverSocket;
    private final BaseAuthService authService;
    private final ArrayList<ClientHandler> clients = new ArrayList<>();

    public Connection connection;
    public Statement stmt;
    public  ResultSet rs;

    public static final Logger logToFile = Logger.getLogger("file");
    public static final Logger logToConsole = Logger.getLogger("console");

    public MyServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.authService = new BaseAuthService();
    }

    public BaseAuthService getAuthService() {
        return authService;
    }

    public void start() throws IOException {
        logToConsole.info("Сервер запущен!");
        logToFile.info("Сервер запущен!");
        try {
            connectionDB();
        } catch (ClassNotFoundException | SQLException e) {
            logToConsole.error("Ошибка с базой данных либо ошибка с поискам класса",e);
            logToFile.error("Ошибка с базой данных либо ошибка с поискам класса",e);
        }

        try {
            while (true) {
                waitAndProcessClientConnection();
            }
        }
        catch(IOException e) {
            logToConsole.error("Произошло исключение ввода-вывода",e);
            logToFile.error("Произошло исключение ввода-вывода",e);
        }

        try {
            disconnectionDB();
        } catch (SQLException e) {
            logToConsole.error("Ошибка с базой данных",e);
            logToFile.error("Ошибка с базой данных",e);
        }
    }


    private void waitAndProcessClientConnection() throws IOException {
        logToConsole.info("Ожидание...");
        logToFile.info("Ожидание...");
        Socket socket = serverSocket.accept();

        processClientConnection(socket);
        logToConsole.info("Клиент уже тут!");
        logToFile.info("Клиент уже тут!");
    }

    private void processClientConnection(Socket socket) throws IOException {
        ClientHandler clientHandler = new ClientHandler(this, socket);
        clientHandler.handle();
    }

    public synchronized void subscribe (ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public synchronized void unSubscribe (ClientHandler clientHandler) {
        logToConsole.info("Клиент покинул чат");
        logToFile.info("Клиент покинул чат");
        clients.remove(clientHandler);
    }

    public synchronized boolean isUserNameBusy (String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastMessage(String message, ClientHandler sender, boolean isServerMessage) throws IOException {
        for (ClientHandler client : clients) {
            if(client == sender) {
                continue;
            }
            client.sendMessage(isServerMessage ? "" : sender.getUsername(), message);
        }
    }

    public synchronized void broadcastMessage(String message, ClientHandler  sender) throws IOException {
        broadcastMessage(message, sender, false);
    }

    public void sendPrivateMessage(String message, ClientHandler sender, String whomSend) throws IOException {
        for (ClientHandler client : clients) {
            if(client.getUsername().equals(whomSend)) {
                client.sendPrivateMessage(sender.getUsername(), message);
            }
        }
    }

    private void connectionDB() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/db/clients.db");
        stmt = connection.createStatement();
    }

    private void disconnectionDB() throws SQLException {
        connection.close();
    }

    public void changeUsername(ClientHandler clientHandler, String newUsername) throws SQLException {
        String oldUsername = clientHandler.getUsername();
        stmt.executeUpdate(String.format("UPDATE clients SET username = '%s' WHERE username = '%s'", newUsername, oldUsername));
    }
}
