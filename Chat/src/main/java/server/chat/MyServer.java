package server.chat;



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

    public MyServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.authService = new BaseAuthService();
    }

    public BaseAuthService getAuthService() {
        return authService;
    }

    public void start() throws IOException {
        System.out.println("Сервер запущен!");
        try {
            connectionDB();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        try {
            while (true) {
                waitAndProcessClientConnection();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }

        try {
            disconnectionDB();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    private void waitAndProcessClientConnection() throws IOException {
        System.out.println("Ожидание...");
        Socket socket = serverSocket.accept();

        processClientConnection(socket);
        System.out.println("Клиент уже тут!");
    }

    private void processClientConnection(Socket socket) throws IOException {
        ClientHandler clientHandler = new ClientHandler(this, socket);
        clientHandler.handle();
    }

    public synchronized void subscribe (ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public synchronized void unSubscribe (ClientHandler clientHandler) {
        System.out.println("Клиент покинул чат");
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
