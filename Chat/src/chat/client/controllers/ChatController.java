package chat.client.controllers;

import chat.client.models.Network;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatController {

    @FXML
    private TextField textField;

    @FXML
    private TextArea windowChat;

    @FXML
    private ListView<String> usersList;

    private Network network;

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd|HH:mm:ss");

    public void setNetwork(Network network) {
        this.network = network;
    }

    @FXML
    void enterMessage() {
        String message = textField.getText().trim();
        if(message.isEmpty()) {
            return;
        }
        appendMessage("Я: " + message);
        try {
            network.sendMessage(message);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка при отправке сообщения");
        }


        textField.clear();
    }

    public void appendMessage(String message) {
        LocalDateTime now = LocalDateTime.now();
        windowChat.appendText(dtf.format(now));
        windowChat.appendText(System.lineSeparator());
        windowChat.appendText(message);
        windowChat.appendText(System.lineSeparator());
        windowChat.appendText(System.lineSeparator());
    }

    @FXML
    void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Input About");
        alert.setHeaderText("Lesson 4");
        alert.setContentText("Online Chat v0.1");
        alert.show();
    }

    @FXML
    void exit() {
        System.exit(0);
    }
}
