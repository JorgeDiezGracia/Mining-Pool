package server;

import common.Protocol;
import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArrayList;

public class MiningServer {

    private static final int PORT = 9000;
    private static final int RANGE_PER_CLIENT = 1000000;

    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicBoolean solutionFound = new AtomicBoolean(false);
    private String currentBlock = "";
    private final int difficulty = 4;

    public static void main(String[] args) throws IOException {
        new MiningServer().start();
    }

    public void start() throws IOException {
        System.out.println("[Server] Starting on port " + PORT);
        ServerSocket serverSocket = new ServerSocket(PORT);

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                this::broadcastNewBlock, 5, 10, TimeUnit.SECONDS
        );

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("[Server] New connection: " + clientSocket.getInetAddress());
            ClientHandler handler = new ClientHandler(clientSocket, this, clients.size() + 1);
            new Thread(handler).start();
        }
    }

    public synchronized void registerClient(ClientHandler client) {
        clients.add(client);
        client.sendMessage(Protocol.ACK + " - " + clients.size() + " total clients");
        System.out.println("[Server] Client " + client.getId() + " registered. Total: " + clients.size());
        if (!currentBlock.isEmpty()) {
            int i = clients.indexOf(client);
            assignRange(client, i * RANGE_PER_CLIENT, (i + 1) * RANGE_PER_CLIENT - 1);
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("[Server] Client " + client.getId() + " removed. Total: " + clients.size());
    }

    public synchronized void broadcastNewBlock() {
        if (clients.isEmpty()) {
            System.out.println("[Server] No clients connected, skipping.");
            return;
        }
        solutionFound.set(false);
        currentBlock = BlockGenerator.generateBlock(5);
        System.out.println("[Server] New block generated: " + currentBlock);

        for (int i = 0; i < clients.size(); i++) {
            assignRange(clients.get(i), i * RANGE_PER_CLIENT, (i + 1) * RANGE_PER_CLIENT - 1);
        }
    }

    private void assignRange(ClientHandler client, int start, int end) {
        client.setRange(start, end);
        client.sendMessage(Protocol.NEW_REQUEST + " " + start + "-" + end + " " + currentBlock);
        System.out.println("[Server] Range " + start + "-" + end + " → client " + client.getId());
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
                System.out.println("[Server] Valid! Salt=" + salt + " Hash=" + hash);
                for (ClientHandler c : clients) c.sendMessage(Protocol.END + " " + salt);
            } else {
                System.out.println("[Server] Invalid solution from client " + finder.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
