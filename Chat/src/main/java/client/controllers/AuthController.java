package client.controllers;

import client.AlertMessage;
import client.ChatGB;
import client.models.Network;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.util.Timer;
import java.util.TimerTask;

public class AuthController {

    @FXML
    public TextField loginField;
    @FXML
    public PasswordField passwordField;
    private Network network;
    private ChatGB mainChatGB;
    private AlertMessage alert = new AlertMessage();
    private boolean authentication = false;

    public void setNetwork(Network network) {
        this.network = network;
    }

    public void setChatGB(ChatGB chatGB) {
        this.mainChatGB = chatGB;
    }

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
            new Timer().schedule(taskTimer,15000000);}).start();
    }

    public void tryAuth(){
        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();

        if(login.length() == 0 || password.length() == 0) {
            alert.showMessage("Ошибка","Поля не должны быть пустыми");
            return;
        }
        String authErrorMessage = network.sendAuthCommand(login, password);
        if (authErrorMessage == null) {
            authentication = true;
            network.setLogin(login);
            mainChatGB.openChat();
        }
        else {
            alert.showMessage("Ошибка","Неправильно введены данные. Перепроверьте и попробуйте еще раз.");
        }

    }

    @FXML
    void registration(ActionEvent event) {
        mainChatGB.openRegistration();
    }
}
