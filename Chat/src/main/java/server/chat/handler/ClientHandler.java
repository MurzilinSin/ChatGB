package server.chat.handler;

import org.apache.log4j.Logger;
import org.w3c.dom.ls.LSOutput;
import server.chat.MyServer;
import server.chat.auth.AuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClientHandler {
    private final MyServer myServer;
    private final Socket clientSocket;
    private DataOutputStream out;
    private DataInputStream in;

    private static final String AUTH_CMD_PREFIX = "/auth"; // + login + pass
    private static final String AUTHOK_CMD_PREFIX = "/authok"; // + username
    private static final String AUTHERR_CMD_PREFIX = "/autherr"; // + error message
    private static final String CLIENT_MSG_CMD_PREFIX = "/clientMsg"; // + msg
    private static final String SERVER_MSG_CMD_PREFIX = "/serverMsg"; // + msg
    private static final String PRIVATE_MSG_CMD_PREFIX = "/w"; //sender + p + msg
    private static final String END_CMD_PREFIX = "/end"; //
    private static final String CHANGE_USERNAME_CMD_PREFIX = "/change"; // префикс для изменнения никнейма
    public static final String CHANGE_ERROR_CMD_PREFIX = "/changerr"; // префикс, если никнейм нельзя изменить

    private String username;
    public static final Logger logToFile = Logger.getLogger("file");
    public static final Logger logToConsole = Logger.getLogger("console");

    public ClientHandler(MyServer myServer, Socket socket) {
        this.myServer = myServer;
        this.clientSocket = socket;
    }


    public void handle() throws IOException {

        out = new DataOutputStream(clientSocket.getOutputStream());
        in = new DataInputStream(clientSocket.getInputStream());

        new Thread(() -> {
            try {
                try {
                    authentication();
                } catch (SQLException e) {
                    logToConsole.error("Ошибка с базой данных",e);
                    logToFile.error("Ошибка с базой данных",e);
                }
                readMessage();
            } catch (IOException e) {
                logToConsole.error("Произошло исключение ввода-вывода",e);
                logToFile.error("Произошло исключение ввода-вывода",e);
            }
        }).start();
    }

    private void authentication() throws IOException, SQLException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith(AUTH_CMD_PREFIX)) {
                boolean isSuccessAuth = processAuthCommand(message);
                if(isSuccessAuth) {
                    break;
                }
            } else {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Ошибка авторизации");
            }
        }
    }

    private boolean processAuthCommand(String message) throws IOException, SQLException {
        String[] parts = message.split("\\s+", 3);
        String login = parts[1];
        String password = parts[2];

        try{
            myServer.rs = myServer.stmt.executeQuery(String.format("SELECT username FROM clients WHERE login = '%s' AND password = '%s'", login, password));
            System.out.println(myServer.rs.getString("username") + " ЭТИ данные взяты из БД!");
            username = myServer.rs.getString("username");
        }
        catch (SQLException e){
            out.writeUTF(AUTHERR_CMD_PREFIX + " Логин или пароль не соответствуют действительности");
            logToConsole.error("Ошибка авторизации");
            logToFile.error("Ошибка авторизации");
            return false;
        }

        if (username != null) {
            if (myServer.isUserNameBusy(username)) {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Логин уже используется");
                logToConsole.info("Логин уже используется");
                logToFile.info("Логин уже используется");
                return false;
            }
            out.writeUTF(AUTHOK_CMD_PREFIX + " " + username);
            myServer.broadcastMessage(String.format(">>> %s присоединился к чату", username), this, true);
            myServer.subscribe(this);
            return true;
        }
        else {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Логин или пароль не соответствуют действительности");
            return false;
        }
    }

    private void readMessage() throws IOException {
        while (true) {
            String message = in.readUTF();
            logToConsole.info("message | " + username + ": " + message);
            logToFile.info("message | " + username + ": " + message);
            if (message.startsWith(END_CMD_PREFIX)) {
                return;
            }
            else if (message.startsWith(PRIVATE_MSG_CMD_PREFIX)) {
                String[] parts = message.split("\\s+", 3);
                String recipient = parts[1];
                String privateMessage = parts[2];
                myServer.sendPrivateMessage(privateMessage, this, recipient);
            }else if(message.startsWith(CHANGE_USERNAME_CMD_PREFIX)){
                String[] parts = message.split("\\s+",2);
                String newUsername = parts[1];
                List<String> nameClients = new ArrayList<>();
                try {
                    ResultSet resultSet = myServer.stmt.executeQuery("SELECT username FROM clients");
                    while (resultSet.next()){
                        nameClients.add(resultSet.getString("username"));
                    }
                    System.out.println(nameClients);
                    boolean isUsernameBusy = false;
                    for (int i = 0; i < nameClients.size(); i++) {
                        if(newUsername.equals(nameClients.get(i))){
                            out.writeUTF(CHANGE_ERROR_CMD_PREFIX);
                            logToConsole.error("Такой никнейм уже есть");
                            logToFile.error("Такой никнейм уже есть");
                            isUsernameBusy = true;
                        }
                    }
                    if(isUsernameBusy){
                        continue;
                    }

                    myServer.stmt.executeUpdate(String.format("UPDATE clients SET username = '%s' WHERE username = '%s'", newUsername, getUsername()));
                    String oldUsername = username;
                    username = newUsername;

                    out.writeUTF(String.format("%s %s", CHANGE_USERNAME_CMD_PREFIX, username));
                    logToConsole.info("Никнейм сменился на " + username);
                    logToFile.info("Никнейм сменился " + username);
                    myServer.broadcastMessage(String.format(">>> %s изменил свой никнейм на %s", oldUsername, newUsername), this, true);

                } catch (SQLException e) {
                    out.writeUTF(CHANGE_ERROR_CMD_PREFIX);
                    logToConsole.error("Ошибка изменений никнейма",e);
                    logToFile.error("Ошибка изменений никнейма",e);
                }
            }
            else {
                myServer.broadcastMessage(message, this);
            }
        }
    }

    public String getUsername() {
        return username;
    }

    public void sendMessage(String sender, String message) throws IOException {
        out.writeUTF(String.format("%s %s %s", CLIENT_MSG_CMD_PREFIX, sender, message));
    }
    public void sendPrivateMessage(String sender, String message) throws IOException {
        out.writeUTF(String.format("%s %s %s", PRIVATE_MSG_CMD_PREFIX, sender, message));
    }

}
