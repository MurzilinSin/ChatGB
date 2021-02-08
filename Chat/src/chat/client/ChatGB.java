package chat.client;

import chat.client.controllers.AuthController;
import chat.client.controllers.ChatController;
import chat.client.models.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class ChatGB extends Application {

    private Network network;
    private Stage primaryStage;

    public Stage getAuthStage() {
        return authStage;
    }

    private Stage authStage;
    private ChatController chatController;

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        network = new Network();
        network.connect();

        openAuthDialog();
        createChatDialog();
    }

    private void openAuthDialog() throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(ChatGB.class.getResource("views/aut-view.fxml"));

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
        loader.setLocation(ChatGB.class.getResource("views/chat-view.fxml"));

        Parent root = loader.load();

        primaryStage.setTitle("Chat");
        primaryStage.setScene(new Scene(root));

        chatController = loader.getController();
        chatController.setNetwork(network);
    }

    public static void main(String[] args) {
        launch(args);
    }

    public void openChat() {
        authStage.close();
        primaryStage.show();
        primaryStage.setTitle(network.getUsername());
        primaryStage.setAlwaysOnTop(true);
        network.waitMessage(chatController);
    }


}
