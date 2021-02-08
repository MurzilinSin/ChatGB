package chat.server.chat;

import chat.server.chat.auth.BaseAuthService;
import chat.server.chat.handler.ClientHandler;
import com.sun.xml.internal.ws.api.model.wsdl.WSDLOutput;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MyServer {

    private final ServerSocket serverSocket;
    private final BaseAuthService authService;
    private final ArrayList<ClientHandler> clients = new ArrayList<>();

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
            while (true) {
                waitAndProcessClientConnection();
            }
        }
        catch(IOException e) {
            e.printStackTrace();
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
                 client.sendMessage(sender.getUsername(), message);
             }
         }
     }



}
