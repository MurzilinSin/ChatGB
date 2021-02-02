package chat.client;

import chat.client.controllers.ViewController;
import chat.client.models.Network;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import sun.reflect.generics.scope.Scope;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.net.Socket;

public class EchoClient extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(EchoClient.class.getResource("views/sample.fxml"));

        Parent root = loader.load();
        //Parent root = loader.load(getClass().getResource("views/sample.fxml"));
        primaryStage.setTitle("Chat");
        primaryStage.setScene(new Scene(root));

        Network network = new Network();
        network.connect();

        ViewController viewController = loader.getController();
        viewController.setNetwork(network);
        network.waitMessage(viewController);

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
