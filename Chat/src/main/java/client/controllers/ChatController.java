package client.controllers;

import client.AlertMessage;
import client.models.Network;
import javafx.collections.*;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import org.apache.log4j.Logger;
import server.Logging;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatController {

    @FXML
    private TextField textField;
    @FXML
    private TextField fieldToChangeNick;
    @FXML
    private Label textChangeUsername;
    @FXML
    public Label fieldToUsername;
    @FXML
    public Button cancel;
    @FXML
    private TextArea windowChat;
    public ObservableList<String> users = FXCollections.observableArrayList();
    @FXML
    private ListView<String> usersList = new ListView<>(users);
    private Network network;
    private String selectedRecipient;
    private AlertMessage alert = new AlertMessage();
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd|HH:mm:ss");
    private Logging log = new Logging();
    private static final String PRIVATE_MSG_CMD_PREFIX = "/w";

    public void setNetwork(Network network) {
        this.network = network;
    }

    @FXML
    public void initialize() {
        displayListview();
        usersList.setCellFactory(lv -> {
            MultipleSelectionModel<String> selectionModel = usersList.getSelectionModel();
            ListCell<String> cell = new ListCell<>();
            cell.textProperty().bind(cell.itemProperty());
            cell.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                usersList.requestFocus();
                if (!cell.isEmpty()) {
                    int index = cell.getIndex();
                    if (selectionModel.getSelectedIndices().contains(index)) {
                        selectionModel.clearSelection(index);
                        selectedRecipient = null;
                    } else {
                        selectionModel.select(index);
                        selectedRecipient = cell.getItem();
                    }
                    event.consume();
                }
            });
            return cell;
        });

    }

    @FXML
    void enterMessage() {
        String message = textField.getText().trim();
        if(message.isEmpty()) {
            return;
        }
        if(message.startsWith(PRIVATE_MSG_CMD_PREFIX)){
            String[] parts = message.split("\\s+",3);
            textField.setStyle("-fx-text-fill: green; -fx-font-size: 16px;");
            appendMessage("Приватное соощение для " + parts[1] +  ": " + parts[2]);
        }else {
            appendMessage("Я: " + message);
        }
        try {
            if(selectedRecipient != null) {
                network.sendPrivateMessage(message,selectedRecipient);
            }
            else{
                network.sendMessage(message);
            }
        } catch (IOException e) {
            log.error("Ошибка при отправке сообщения",e);
        }
        textField.clear();
    }

    public void appendMessage(String message) {
        File file = new File(String.format("src/main/resources/lib/'%s'.chatHistory.txt",network.getLogin()));
        try(FileWriter writer = new FileWriter(file,true)){
            LocalDateTime now = LocalDateTime.now();
            writer.write(dtf.format(now));
            writer.write("\n");
            writer.write(message);
            writer.write("\n");
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        LocalDateTime now = LocalDateTime.now();
        windowChat.appendText(dtf.format(now));
        windowChat.appendText(System.lineSeparator());
        windowChat.appendText(message);
        windowChat.appendText(System.lineSeparator());
        windowChat.appendText(System.lineSeparator());
    }
    @FXML
    public void showTextfield(){
        fieldToChangeNick.setVisible(true);
        cancel.setVisible(true);
        textChangeUsername.setVisible(false);
        fieldToUsername.setVisible(false);
    }

    @FXML
    public void changeNick() {
        String newUsername = fieldToChangeNick.getText().trim();
        if(newUsername.isEmpty()){
            alert.showMessage("Ошибка","Ник пустым быть не может!");
            return;
        }
        try {
            network.sendMessage("/change "+ newUsername);
        } catch (IOException e) {
            log.error("Ошибка при смене ника",e);
        }
    }

    @FXML
    public void cancelChange(){
        hideEverythingAndShowSomething();
    }

    public void tryChangeName(String newUsername, boolean isChanged) {
        if(isChanged){
            fieldToUsername.setText(newUsername);
            log.info("Ник сменился: " + network.isUsernameChanged);
        }
        else {
            alert.showMessage("Ошибка","Ник уже занят!");
            return;
        }
        hideEverythingAndShowSomething();
    }

    public void hideEverythingAndShowSomething(){
        fieldToChangeNick.clear();
        fieldToChangeNick.setVisible(false);
        cancel.setVisible(false);
        textChangeUsername.setVisible(true);
        fieldToUsername.setVisible(true);
    }

    public void chatHistory() {
        File file = new File(String.format("src/main/resources/lib/'%s'.chatHistory.txt",network.getLogin()));
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try(BufferedReader reader = new BufferedReader(new FileReader(file))) {
            long count = Files.lines(Path.of(String.format("src/main/resources/lib/'%s'.chatHistory.txt", network.getLogin()))).count();
            String strline;
            if(count <= 100){
                while ((strline = reader.readLine()) != null) {
                    windowChat.appendText(strline);
                    windowChat.appendText(System.lineSeparator());
                }
            }
            else if (count > 100) {
                long startPoint = count - 100;
                long j = 0;
                while ((strline = reader.readLine()) != null){
                    if(j <= startPoint){
                        j++;
                        continue;
                    }
                    windowChat.appendText(strline);
                    windowChat.appendText(System.lineSeparator());
                }
            }
        } catch (IOException e) {
            log.error("Файл не может быть прочтен",e);
        }
    }
    public void displayListview(String[] incomeUsers){
        users.clear();
        for (String str :incomeUsers) {
            str = str.trim();
            users.add(str);
        }
        usersList.getItems().clear();
        for (String user : users) {
            usersList.getItems().add(user);
        }
    }

    public void displayListview(){
        for (String str :users) {
            str = str.trim();
            users.add(str);
        }
        usersList.getItems().clear();
        for (String user : users) {
            usersList.getItems().add(user);
        }
    }
}
