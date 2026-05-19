package server;

import common.Protocol;
import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MiningServer {

    private static final int PORT = 9000;
    private static final int RANGE_PER_CLIENT = 1000000;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicBoolean solutionFound = new AtomicBoolean(false);
    private String currentBlock = "";
    private final int difficulty = 4;
    private MiningServerController controller;

    // Constructor sin UI (para usar sin JavaFX)
    public MiningServer() {}

    // Constructor con UI
    public MiningServer(MiningServerController controller) {
        this.controller = controller;
    }

    public static void main(String[] args) throws IOException {
        new MiningServer().start();
    }

    public void start() throws IOException {
        log("[Server] Starting on port " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::broadcastNewBlock, 5, 10, TimeUnit.SECONDS
        );

        while (true) {
            Socket clientSocket = serverSocket.accept();
            log("[Server] New connection: " + clientSocket.getInetAddress());
            ClientHandler handler = new ClientHandler(clientSocket, this, clients.size() + 1);
            new Thread(handler).start();
        }
    }

    public synchronized void registerClient(ClientHandler client) {
        clients.add(client);
        client.sendMessage(Protocol.ACK + " - " + clients.size() + " total clients");
        log("[Server] Client " + client.getId() + " registered. Total: " + clients.size());
        updateClientList();
        if (!currentBlock.isEmpty()) {
            int i = clients.indexOf(client);
            assignRange(client, i * RANGE_PER_CLIENT, (i + 1) * RANGE_PER_CLIENT - 1);
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        log("[Server] Client " + client.getId() + " removed. Total: " + clients.size());
        updateClientList();
    }

    public synchronized void broadcastNewBlock() {
        if (clients.isEmpty()) {
            log("[Server] No clients connected, skipping.");
            return;
        }
        solutionFound.set(false);
        currentBlock = BlockGenerator.generateBlock(5);
        log("[Server] New block: " + currentBlock);
        if (controller != null) controller.updateBlock(currentBlock);

        for (int i = 0; i < clients.size(); i++) {
            assignRange(clients.get(i), i * RANGE_PER_CLIENT, (i + 1) * RANGE_PER_CLIENT - 1);
        }
    }

    private void assignRange(ClientHandler client, int start, int end) {
        client.setRange(start, end);
        client.sendMessage(Protocol.NEW_REQUEST + " " + start + "-" + end + " " + currentBlock);
        log("[Server] Range " + start + "-" + end + " → client " + client.getId());
    }

    public synchronized void validateSolution(ClientHandler finder, int salt) {
        if (solutionFound.get()) return;
        String prefix = "0".repeat(difficulty);
        try {
            String toHash = currentBlock + salt;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(toHash.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) hex.append(String.format("%02x", b));
            String hash = hex.toString();

            if (hash.startsWith(prefix)) {
                solutionFound.set(true);
                log("[Server] Valid! Salt=" + salt + " Hash=" + hash);
                if (controller != null) controller.updateSolution(salt, hash);
                for (ClientHandler c : clients) c.sendMessage(Protocol.END + " " + salt);
            } else {
                log("[Server] Invalid solution from client " + finder.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println(message);
        if (controller != null) controller.log(message);
    }

    private void updateClientList() {
        if (controller != null) {
            List<String> names = clients.stream()
                    .map(c -> "Client " + c.getId())
                    .collect(Collectors.toList());
            controller.updateClients(names);
        }
    }
}