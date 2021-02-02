package chat.server.chat.handler;

import chat.server.chat.MyServer;
import chat.server.chat.auth.AuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.text.MessageFormat;

public class ClientHandler {
    private final MyServer myServer;
    private final Socket clientSocket;
    private DataOutputStream out;
    private DataInputStream in;
    private String username;

    private static final String AUTH_CMD_PREFIX = "/auth";
    private static final String AUTHOK_CMD_PREFIX = "/authok";
    private static final String AUTHERR_CMD_PREFIX = "/autherr";
    private static final String CLIENT_MSG_CMD_PREFIX = "/clientMsg";
    private static final String SERVER_MSG_CMD_PREFIX = "/clientMsg";
    private static final String PRIVATE_MSG_CMD_PREFIX = "/w";
    private static final String END_CMD_PREFIX = "/end";


    public ClientHandler(MyServer myServer, Socket socket) {
        this.myServer = myServer;
        this.clientSocket = socket;
    }

    public void handle() throws IOException {
        out = new DataOutputStream(clientSocket.getOutputStream());
        in = new DataInputStream(clientSocket.getInputStream());

        new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

        }).start();
    }

    private void authentication() throws IOException {
        while (true) {
            //String message = in.readUTF();
            String message = "/auth martin 1111";
            if(message.startsWith(AUTH_CMD_PREFIX)) {
                boolean isSuccess = processAuthCommand(message);
                if(isSuccess) {
                    break;
                }
            } else {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Ошибка авторизации");
            }
        }
    }

    private boolean processAuthCommand(String message) throws IOException {
        String[] parts = message.split("\\s+", 3);
        String login = parts[1];
        String password = parts[2];
        AuthService authService = myServer.getAuthService();

        username = authService.getUsernameByLoginAndPassword(login, password);

        if (username != null) {

            if(myServer.isUserNameBusy(username)) {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Логин уже в сети");
                return false;
            }

            out.writeUTF(AUTHOK_CMD_PREFIX + " " + username);
            myServer.subscribe(this);
        }
        else {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Неверный логин или пароль");
            return false;
        }


        return true;
    }

    private void readMessage() throws IOException {
        while (true) {
            String message = in.readUTF();
            System.out.println("message | " + username + ": " + message);
            if(message.startsWith(END_CMD_PREFIX)) {
                return;
            }
            else if (message.startsWith(PRIVATE_MSG_CMD_PREFIX)) {
                String[] parts = message.split("\\s+",3);
                String whomSend = parts[1];
                myServer.broadcastMessage(message, this, whomSend);
            }
            else {
                myServer.broadcastMessage(message,this);
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String sender, String message) throws IOException {
        out.writeUTF(MessageFormat.format("%s %s %s", CLIENT_MSG_CMD_PREFIX, sender, message));
    }
}
