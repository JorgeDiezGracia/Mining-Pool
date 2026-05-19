package client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MiningClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/client-view.fxml"));
        Scene scene = new Scene(loader.load(), 600, 400);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); // añade aquí
        stage.setTitle("Mining Pool - Client");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}