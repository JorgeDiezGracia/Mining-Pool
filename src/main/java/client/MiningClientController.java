package client;

import common.Protocol;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MiningClientController {

    @FXML private TextArea logArea;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private Button connectButton;
    @FXML private Label saltLabel;

    private static final String HOST = "localhost";
    private static final int PORT = 9000;
    private static final int NUM_THREADS = 5; // hilos de minado

    private PrintWriter out;
    private volatile boolean running = false;
    private volatile boolean mining  = false;
    private final int difficulty = 4;
    private final AtomicBoolean localSolutionFound = new AtomicBoolean(false);
    private ExecutorService miningPool;

    @FXML
    public void onConnect() {
        connectButton.setDisable(true);
        statusLabel.setText("Connecting...");
        progressBar.setProgress(0.5);
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
            localSolutionFound.set(false);

            String[] parts = message.split(" ", 3);
            String[] range = parts[1].split("-");
            int start = Integer.parseInt(range[0]);
            int end   = Integer.parseInt(range[1]);
            String blockData = parts[2];

            setStatus("Mining " + start + " - " + end + "...");
            setProgress(0.0);
            Platform.runLater(() -> saltLabel.setText("Salt: -"));

            mineWithThreads(blockData, start, end);

        } else if (message.startsWith(Protocol.END)) {
            mining = false;
            running = false;
            if (miningPool != null) miningPool.shutdownNow();
            setStatus("Done ✓ Solution found!");
            setProgress(1.0);
        }
    }

    private void mineWithThreads(String blockData, int start, int end) {
        int total = end - start + 1;
        int chunkSize = total / NUM_THREADS;

        miningPool = Executors.newFixedThreadPool(NUM_THREADS);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < NUM_THREADS; i++) {
            int chunkStart = start + i * chunkSize;
            int chunkEnd = (i == NUM_THREADS - 1) ? end : chunkStart + chunkSize - 1;
            final int threadId = i + 1;

            log("[Client] Thread " + threadId + " assigned range " + chunkStart + "-" + chunkEnd);

            futures.add(miningPool.submit(() ->
                    mineChunk(blockData, chunkStart, chunkEnd, threadId, total)
            ));
        }

        // Hilo monitor que espera a que terminen todos
        new Thread(() -> {
            for (Future<?> f : futures) {
                try { f.get(); } catch (Exception ignored) {}
            }
            miningPool.shutdown();
            if (!localSolutionFound.get() && mining) {
                log("[Client] All ranges exhausted, nothing found.");
                setProgress(1.0);
            }
        }).start();
    }

    private void mineChunk(String blockData, int start, int end, int threadId, int totalRange) {
        String prefix = "0".repeat(difficulty);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int salt = start; salt <= end && mining && !localSolutionFound.get(); salt++) {
                byte[] hashBytes = digest.digest((blockData + salt).getBytes("UTF-8"));
                StringBuilder hex = new StringBuilder();
                for (byte b : hashBytes) hex.append(String.format("%02x", b));

                if ((salt - start) % 1000 == 0) {
                    double progress = (double)(salt - start) / (totalRange / NUM_THREADS);
                    setProgress(Math.min(progress, 1.0));
                }

                if (hex.toString().startsWith(prefix)) {
                    if (localSolutionFound.compareAndSet(false, true)) {
                        final int foundSalt = salt;
                        log("[Client] Thread " + threadId + " FOUND! Salt=" + foundSalt);
                        Platform.runLater(() -> saltLabel.setText("Salt: " + foundSalt));
                        out.println(Protocol.SOL + " " + foundSalt);
                        mining = false;
                        if (miningPool != null) miningPool.shutdownNow();
                    }
                    return;
                }
            }
        } catch (Exception e) {
            if (mining) log("[Client] Thread " + threadId + " error: " + e.getMessage());
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