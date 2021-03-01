package client;

import client.controllers.AuthController;
import client.controllers.ChatController;
import client.models.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;

public class ChatGB extends Application {

    private Network network;
    private Stage primaryStage;

    private Stage authStage;
    private ChatController chatController;

    public static final Logger logToFile = Logger.getLogger("file");
    public static final Logger logToConsole = Logger.getLogger("console");

    @Override
    public void start(Stage primaryStage) throws Exception{
        PropertyConfigurator.configure("src/main/resources/log/config/log4j.properties");
        this.primaryStage = primaryStage;
        primaryStage.setAlwaysOnTop(false);
        network = new Network();
        network.connect();

        openAuthDialog();
        createChatDialog();
        primaryStage.setOnCloseRequest(windowEvent -> {
            try {
                System.exit(0);
                network.getSocket().close();
            } catch (IOException e) {
                logToConsole.error("Сокет не закрывается",e);
                logToFile.error("Сокет не закрывается",e);
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

    public static void main(String[] args) {
        launch(args);
    }

    public void openChat() {
        authStage.close();
        chatController.fieldToUsername.setText(network.getUsername());
        primaryStage.show();
        primaryStage.setAlwaysOnTop(false);
        network.waitMessage(chatController);
        chatController.chatHistory();
    }


}