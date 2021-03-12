package server.chat;

import server.Logging;
import server.chat.auth.BaseAuthService;
import server.chat.handler.ClientHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.sql.*;
import java.util.List;

public class MyServer {

    private final ServerSocket serverSocket;
    private final BaseAuthService authService;
    private final List<ClientHandler> clients = new ArrayList<>();
    private Logging log = new Logging();

    public MyServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
        this.authService = new BaseAuthService();
    }

    public BaseAuthService getAuthService() {
        return authService;
    }

    public void start() throws IOException {
        log.info("Сервер запущен");
        try {
            while (true) {
                waitAndProcessClientConnection();
            }
        }
        catch(IOException e) {
            log.error("Произошло исключение ввода-вывода",e);
        }
    }


    private void waitAndProcessClientConnection() throws IOException {
        log.info("Ожидание");
        Socket socket = serverSocket.accept();
        processClientConnection(socket);
        log.info("Кто-то хочет присоединиться!");
    }

    private void processClientConnection(Socket socket) throws IOException {
        ClientHandler clientHandler = new ClientHandler(this, socket);
        clientHandler.handle();
    }

    public synchronized void subscribe (ClientHandler clientHandler) {
        log.info("Клиент уже тут!");
        clients.add(clientHandler);
        authService.usernames.add(clientHandler.getUsername());
    }

    public synchronized void unSubscribe (ClientHandler clientHandler) {
        log.info("Клиент покинул чат");
        clients.remove(clientHandler);
        authService.usernames.remove(clientHandler.getUsername());
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
            if(isServerMessage){
                client.sendServerMessage(message);
            }
            else{
                client.sendMessage(sender.getUsername(), message);
            }
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

    public void broadcastChangedList() throws IOException {
        for (ClientHandler client : clients) {
            client.sendChangedList();
        }
    }
}
