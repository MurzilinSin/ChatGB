package client.controllers;

import client.models.Network;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import server.chat.MyServer;
import server.chat.handler.ClientHandler;

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
    private TextArea windowChat;

    @FXML
    private ListView<String> usersList;

    private Network network;
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd|HH:mm:ss");

    public static final Logger logToFile = Logger.getLogger("file");
    public static final Logger logToConsole = Logger.getLogger("console");

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
            logToConsole.error("Ошибка при отправке сообщения",e);
            logToFile.error("Ошибка при отправке сообщения",e);
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
        textChangeUsername.setVisible(false);
        fieldToUsername.setVisible(false);
    }

    @FXML
    public void changeNick() throws SQLException, ClassNotFoundException {
        String newUsername = fieldToChangeNick.getText().trim();
        if(newUsername.isEmpty()){
            Alert error = new Alert(Alert.AlertType.INFORMATION);
            error.setTitle("Ошибка");
            error.setHeaderText(null);
            error.setContentText("Ник пустым быть не может!");
            error.showAndWait();
            return;
        }
        try {
            network.sendMessage("/change "+ newUsername);
        } catch (IOException e) {
            logToConsole.error("Ошибка при смене ника",e);
            logToFile.error("Ошибка при смене ника",e);
        }
    }

    public void tryChangeName(String newUsername, boolean isChanged) {
        if(isChanged){
            fieldToUsername.setText(newUsername);
            logToConsole.info(network.isUsernameChanged);
            logToFile.info(network.isUsernameChanged);
        }
        else {
            Alert error = new Alert(Alert.AlertType.INFORMATION);
            error.setTitle("Ошибка");
            error.setHeaderText(null);
            error.setContentText("Ник уже занят!");
            error.showAndWait();
            return;
        }
        fieldToChangeNick.clear();
        fieldToChangeNick.setVisible(false);
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
            System.out.println(count);
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
            logToConsole.error("Файл не может быть прочтен",e);
            logToFile.error("Файл не может быть прочтен",e);
        }

    }
}
