package server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MiningServerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/server-view.fxml"));
        Scene scene = new Scene(loader.load(), 700, 500);
        stage.setTitle("Mining Pool - Server");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> System.exit(0));
        stage.show();

        MiningServerController controller = loader.getController();
        controller.startServer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}