package chat.client.models;

import chat.client.controllers.ChatController;
import javafx.application.Platform;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class Network {
    private static final String AUTH_CMD_PREFIX = "/auth";
    private static final String AUTHOK_CMD_PREFIX = "/authok";
    private static final String AUTHERR_CMD_PREFIX = "/autherr";
    private static final String CLIENT_MSG_CMD_PREFIX = "/clientMsg";
    private static final String SERVER_MSG_CMD_PREFIX = "/clientMsg";
    private static final String PRIVATE_MSG_CMD_PREFIX = "/w";
    private static final String END_CMD_PREFIX = "/end";

    private static final int DEFAULT_SERVER_SOCKET = 8888;
    private static final String DEFAULT_SERVER_HOST = "localhost";

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    private final int port;
    private final String host;

    public Network(String host, int port){
        this.host = host;
        this.port = port;
    }

    public Socket getSocket() {
        return socket;
    }

    public Network() {
        this.port = DEFAULT_SERVER_SOCKET;
        this.host = DEFAULT_SERVER_HOST;
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8888);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.out.println("Соединение не установлено");
            e.printStackTrace();
        }
    }

    public DataOutputStream getOut() {
        return out;
    }

    public String getUsername() {
        return username;
    }


    public void waitMessage(ChatController chatController) {
        Thread thread = new Thread(() -> {
           try {
               while (true){
                   String message = in.readUTF();
                   if (message.startsWith(CLIENT_MSG_CMD_PREFIX)) {
                       String[] parts = message.split("\\s+", 3);
                       String sender = parts[1];
                       String messageFromUser = parts[2];
                       Platform.runLater(() -> chatController.appendMessage(String.format("%s: %s", sender, messageFromUser)));
                   }
                   else if (message.startsWith(SERVER_MSG_CMD_PREFIX)) {
                       String[] parts = message.split("\\s+", 2);

                       String messageFromUser = parts[2];

                       Platform.runLater(() -> chatController.appendMessage(messageFromUser));
                   }
                   else {
                       Platform.runLater(() -> System.out.println("НЕИЗВЕСТНАЯ ОШИБКА СЕРВЕРА"));
                   }
               }
           } catch (IOException e){
               System.err.println("Ошибка подключения");
           }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public String sendAuthCommand(String login, String password) {
        try {
            out.writeUTF(String.format("%s %s %s", AUTH_CMD_PREFIX, login, password));
            String response = in.readUTF();
            if (response.startsWith(AUTHOK_CMD_PREFIX)) {
                this.username = response.split("\\s+", 2)[1];
                return null;
            } else {
                return response.split("\\s+", 2)[1];
            }
        } catch (IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }
    public void sendPrivateMessage(String message, String recipient) throws IOException {
        String command = String.format("%s,%s,%s", PRIVATE_MSG_CMD_PREFIX, recipient, message);
        sendMessage(command);
    }
}
