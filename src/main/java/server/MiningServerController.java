package server;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MiningServerController {

    @FXML private TextArea logArea;
    @FXML private ListView<String> clientListView;
    @FXML private TextArea blockArea;
    @FXML private Label solutionLabel;
    @FXML private Label totalClientsLabel;

    private MiningServer server;

    public void startServer() {
        server = new MiningServer(this);
        new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                log("[Server] Error: " + e.getMessage());
            }
        }).start();
    }

    public void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    public void updateClients(java.util.List<String> clients) {
        Platform.runLater(() -> {
            clientListView.getItems().setAll(clients);
            totalClientsLabel.setText("Total clients: " + clients.size());
        });
    }

    public void updateBlock(String block) {
        Platform.runLater(() -> blockArea.setText(block));
    }

    public void updateSolution(int salt, String hash) {
        Platform.runLater(() ->
                solutionLabel.setText("Last solution → Salt: " + salt + " | Hash: " + hash.substring(0, 16) + "...")
        );
    }
}