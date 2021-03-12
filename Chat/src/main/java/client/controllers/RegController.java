package client.controllers;

import client.AlertMessage;
import client.ChatGB;
import client.models.Network;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import server.Logging;

import java.sql.*;

public class RegController {

    @FXML
    private TextField loginField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField passwordField;

    private Network network;
    private ChatGB chatGB;
    private AlertMessage alert = new AlertMessage();
    private Logging log = new Logging();

    public void setNetwork(Network network) {
        this.network = network;
    }
    public void setChatGb(ChatGB chatGB) {
        this.chatGB = chatGB;
    }

    @FXML
    void startRegistration(ActionEvent event) throws ClassNotFoundException, SQLException {
        String login = loginField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:src/main/resources/db/clients.db");
        try( Statement stmt = connection.createStatement()) {
            ResultSet rsLogin = stmt.executeQuery(String.format("SELECT login, username FROM clients WHERE login = '%s' OR username = '%s'", login, username));
            if (rsLogin.next() && (rsLogin.getString("login").equals(login) || rsLogin.getString("username").equals(username))) {
                clearFields();
                alert.showMessage("Ошибка", "Пользователь с таким логином или ником уже есть!");
                log.info("Использовали данные, которые принадлежат другому пользователю");
                return;
            } else if (login.length() == 0 || password.length() == 0 || username.length() == 0) {
                clearFields();
                alert.showMessage("Ошибка", "Поля не должны быть пустыми!");
                return;
            } else {
                stmt.executeUpdate(String.format("INSERT INTO clients(login, password, username) VALUES('%s','%s','%s')", login, password, username));
                alert.showMessage("Поздравляем", "Вы успешно зарегистрировались!");
                log.info("Зарегистрирован новый пользователь!");
                chatGB.openAuthAfterReg();
            }
        }
    }

    private void clearFields(){
        loginField.clear();
        usernameField.clear();
        passwordField.clear();
    }

}
