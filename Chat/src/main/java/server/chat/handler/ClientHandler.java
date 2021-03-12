package server.chat.handler;

import server.Logging;
import server.chat.MyServer;
import server.chat.auth.AuthService;
import server.chat.auth.BaseAuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
    private static final String CHANGE_ERROR_CMD_PREFIX = "/changerr"; // префикс, если никнейм нельзя изменить
    private static final String CHANGE_USER_LIST = "/ListViewUserList"; // префикс для смены листвью пользователя
    private static final String FIRST_REQUEST_LISTVIEW = "/firstRequest"; // нужно для отображение listview с самого начала работы программы
    private String username;
    private Logging log = new Logging();
    private BaseAuthService authService;

    public ClientHandler(MyServer myServer, Socket socket) {
        this.myServer = myServer;
        this.clientSocket = socket;
    }

    public String getUsername() {
        return username;
    }

    public void handle() throws IOException {

        out = new DataOutputStream(clientSocket.getOutputStream());
        in = new DataInputStream(clientSocket.getInputStream());

        new Thread(() -> {
            try {
                try {
                    authentication();
                } catch (SQLException | EOFException e) {
                    if(e instanceof SQLException){
                        log.error("Ошибка с базой данных",e);
                    }
                    else if(e instanceof EOFException){
                        log.error("Этот кто-то передумал",e);
                    }
                }
                readMessage();
            } catch (IOException e) {
                log.error("Произошло исключение ввода-вывода",e);
            }
        }).start();
    }

    private void authentication() throws IOException, SQLException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith(AUTH_CMD_PREFIX)) {
                boolean isSuccessAuth = processAuthCommand(message);
                if (isSuccessAuth) {
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
            authService = myServer.getAuthService();
            username = authService.getUsernameByLoginAndPassword(login,password);
        }
        catch (SQLException e){
            out.writeUTF(AUTHERR_CMD_PREFIX + " Логин или пароль не соответствуют действительности");
            log.error("Ошибка авторизации",e);
            return false;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (username != null) {
            if (myServer.isUserNameBusy(username)) {
                out.writeUTF(AUTHERR_CMD_PREFIX + " Логин уже используется");
                log.info("Логин уже используется");
                return false;
            }
            out.writeUTF(AUTHOK_CMD_PREFIX + " " + username);
            myServer.broadcastMessage(String.format("Сервер>>> %s присоединился к чату", username), this, true);
            myServer.subscribe(this);
            myServer.broadcastChangedList();
            return true;
        }
        else {
            out.writeUTF(AUTHERR_CMD_PREFIX + " Логин или пароль не соответствуют действительности");
            return false;
        }
    }

    private void readMessage() throws IOException {
        while (true) {
            try {
                String message = in.readUTF();
                log.info("message | " + username + ": " + message);
                if (message.startsWith(END_CMD_PREFIX)) {
                    myServer.broadcastMessage(String.format("Сервер>>> %s покинул чат", username), this, true);
                    myServer.unSubscribe(this);
                    myServer.broadcastChangedList();
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
                    try {
                        authService = myServer.getAuthService();
                        boolean isUsernameBusy = authService.isUsernameBusy(newUsername);
                        if(isUsernameBusy){
                            log.info("Такой пользователь уже есть!");
                            out.writeUTF(CHANGE_ERROR_CMD_PREFIX);
                            continue;
                        }
                        authService.changeNickname(username,newUsername);
                        String oldUsername = username;
                        username = newUsername;
                        out.writeUTF(String.format("%s %s", CHANGE_USERNAME_CMD_PREFIX, username));
                        log.info("Никнейм сменился на " + username);
                        myServer.broadcastMessage(String.format("Сервер>>> %s изменил свой никнейм на %s", oldUsername, newUsername), this, true);
                        myServer.broadcastChangedList();
                    } catch (SQLException | ClassNotFoundException e) {
                        out.writeUTF(CHANGE_ERROR_CMD_PREFIX);
                        log.error("Ошибка изменений никнейма",e);
                    }
                }
                else if(message.startsWith(FIRST_REQUEST_LISTVIEW)){
                    out.writeUTF(CHANGE_USER_LIST + "|" + listToString(authService.usernames));
                }
                else {
                    myServer.broadcastMessage(message, this);
                }
            }
            catch (SocketException e) {
                myServer.broadcastMessage(String.format("Сервер>>> %s покинул чат из-за проблем с интернетом", username), this, true);
                myServer.unSubscribe(this);
                myServer.broadcastChangedList();
                return;
            }
        }
    }

    public void sendMessage(String sender, String message) throws IOException {
        out.writeUTF(String.format("%s %s %s", CLIENT_MSG_CMD_PREFIX, sender, message));
    }
    public void sendPrivateMessage(String sender, String message) throws IOException {
        out.writeUTF(String.format("%s %s %s", PRIVATE_MSG_CMD_PREFIX, sender, message));
    }

    public void sendServerMessage(String message) throws IOException {
        out.writeUTF(String.format("%s %s", SERVER_MSG_CMD_PREFIX, message));
    }

    public void sendChangedList() throws IOException {
        out.writeUTF(CHANGE_USER_LIST + " | " + listToString(authService.usernames));
    }

    public String listToString(List<String> names) {
        String stroka = "";
        for (String name : names){
            stroka += name + ",";
        }
        stroka = stroka.substring(0, stroka.length() - 1);
        return stroka;
    }


}
