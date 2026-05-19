package client;

import common.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;

public class MiningClientController {

    @FXML private TextArea logArea;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button connectButton;
    @FXML private Label saltLabel;

    private static final String HOST = "localhost";
    private static final int PORT = 9000;

    private PrintWriter out;
    private volatile boolean running = false;
    private volatile boolean mining  = false;
    private final int difficulty = 4;

    @FXML
    public void onConnect() {
        connectButton.setDisable(true);
        statusLabel.setText("Connecting...");
        progressBar.setProgress(0.1);
        running = true;
        new Thread(this::startConnection).start();
    }

    private void startConnection() {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(Protocol.CONNECT);
            log("[Client] Sent: connect");

            String line;
            while (running && (line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            log("[Client] Error: " + e.getMessage());
        } finally {
            Platform.runLater(() -> {
                connectButton.setDisable(false);
                statusLabel.setText("Disconnected");
            });
        }
    }

    private void handleMessage(String message) {
        log("[Server] " + message);

        if (message.startsWith(Protocol.ACK)) {
            setStatus("Connected ✓");

        } else if (message.startsWith(Protocol.NEW_REQUEST)) {
            out.println(Protocol.ACK);
            mining = true;
            String[] parts = message.split(" ", 3);
            String[] range = parts[1].split("-");
            int start = Integer.parseInt(range[0]);
            int end   = Integer.parseInt(range[1]);
            String blockData = parts[2];
            setStatus("Mining " + start + " - " + end + "...");
            setProgress(0.0);
            Platform.runLater(() -> saltLabel.setText("Salt: -"));
            new Thread(() -> mine(blockData, start, end)).start();

        } else if (message.startsWith(Protocol.END)) {
            mining  = false;
            running = false;
            setStatus("Done ✓ Solution found!");
            setProgress(1.0);
        }
    }

    private void mine(String blockData, int start, int end) {
        String prefix = "0".repeat(difficulty);
        int total = end - start;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int salt = start; salt <= end && mining; salt++) {
                byte[] hashBytes = digest.digest((blockData + salt).getBytes("UTF-8"));
                StringBuilder hex = new StringBuilder();
                for (byte b : hashBytes) hex.append(String.format("%02x", b));

                if ((salt - start) % 1000 == 0) {
                    double progress = (double)(salt - start) / total;
                    setProgress(progress);
                }

                if (hex.toString().startsWith(prefix)) {
                    final int foundSalt = salt;
                    log("[Client] FOUND! Salt=" + foundSalt);
                    Platform.runLater(() -> saltLabel.setText("Salt: " + foundSalt));
                    out.println(Protocol.SOL + " " + foundSalt);
                    mining = false;
                    return;
                }
            }
            log("[Client] Range exhausted, nothing found.");
            setProgress(1.0);
        } catch (Exception e) {
            log("[Client] Mining error: " + e.getMessage());
        }
    }

    private void log(String message) {
        Platform.runLater(() -> logArea.appendText(message + "\n"));
    }

    private void setStatus(String status) {
        Platform.runLater(() -> statusLabel.setText(status));
    }

    private void setProgress(double value) {
        Platform.runLater(() -> progressBar.setProgress(value));
    }
}