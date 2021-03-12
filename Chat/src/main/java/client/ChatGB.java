package client;

import client.controllers.*;
import client.models.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.stage.*;
import org.apache.log4j.PropertyConfigurator;
import server.Logging;

import java.io.IOException;

public class ChatGB extends Application {

    private Network network;
    private Stage primaryStage;
    private Stage authStage;
    private Stage regStage;
    private ChatController chatController;
    private Logging log = new Logging();

    @Override
    public void start(Stage primaryStage) throws Exception{
        PropertyConfigurator.configure("src/main/resources/log/config/log4j.properties");
        this.primaryStage = primaryStage;
        primaryStage.setAlwaysOnTop(false);
        network = new Network();
        network.connect();
        openAuthDialog();
        openRegDialog();
        createChatDialog();
        primaryStage.setOnCloseRequest(windowEvent -> {
            try {
                network.exitForReal();
                network.getSocket().close();
                System.exit(0);
            } catch (IOException e) {
                log.error("Сокет не закрывается",e);
            }
        });
    }

    private void openAuthDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ChatGB.class.getResource("auth-view.fxml"));
        Parent root = loader.load();
        authStage = new Stage();
        authStage.setTitle("Authentication");
        authStage.setScene(new Scene(root));
        authStage.initModality(Modality.WINDOW_MODAL);
        authStage.initOwner(primaryStage);
        authStage.show();
        AuthController authController = loader.getController();
        authController.setNetwork(network);
        authController.setChatGB(this);
        authStage.setOnCloseRequest(windowEvent -> {
            try {
                network.exitForReal();
                network.getSocket().close();
                System.exit(0);
            } catch (IOException e) {
                log.error("Сокет не закрывается",e);
            }
        });
    }

    private void openRegDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ChatGB.class.getResource("reg-view.fxml"));
        Parent root = loader.load();
        regStage = new Stage();
        regStage.setTitle("Регистрация");
        regStage.setScene(new Scene(root));
        regStage.initModality(Modality.WINDOW_MODAL);
        regStage.initOwner(authStage);
        RegController regController = loader.getController();
        regController.setNetwork(network);
        regController.setChatGb(this);
        regStage.setOnCloseRequest(windowEvent -> {
            regStage.close();
            try {
                openAuthDialog();
            } catch (IOException e) {
                log.error("Сокет не закрывается",e);
            }
        });
    }

    private void createChatDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ChatGB.class.getResource("chat-view.fxml"));
        Parent root = loader.load();
        primaryStage.setTitle("Проклятый чат");
        primaryStage.setScene(new Scene(root));
        chatController = loader.getController();
        chatController.setNetwork(network);
    }

    public void openChat() {
        authStage.close();
        chatController.fieldToUsername.setText(network.getUsername());
        primaryStage.show();
        primaryStage.setAlwaysOnTop(false);
        network.waitMessage(chatController);
        chatController.chatHistory();
    }

    public void openRegistration(){
        authStage.close();
        regStage.show();
    }

    public void openAuthAfterReg(){
        regStage.close();
        authStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}