package chat.client.controllers;

import chat.client.ChatGB;
import chat.client.models.Network;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

public class AuthController {

    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    private Network network;
    private ChatGB mainChatGB;
    private boolean authentication = false;

    @FXML
    public void initialize() {
         new Thread(() -> {
            TimerTask taskTimer = new TimerTask() {
                @Override
                public void run() {
                    if(!authentication) {
                        System.exit(0);
                    }
                }
            };
            new Timer().schedule(taskTimer,5000);}).start();
    }

    public void tryAuth(ActionEvent actionEvent){
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        if(login.length() == 0 || password.length() == 0) {
            System.out.println("!!Поля не должны быть пустыми");
            return;
        }

        String authErrorMessage = network.sendAuthCommand(login, password);
        if (authErrorMessage == null) {
            authentication = true;
            mainChatGB.openChat();
        }
        else {
            System.out.println("!!Ошибка аутентификации");
        }

    }

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setChatGB(ChatGB chatGB) {
        this.mainChatGB = chatGB;
    }
}