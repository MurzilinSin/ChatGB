module Chat.v03 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens client.controllers to javafx.fxml;
    exports client;
}