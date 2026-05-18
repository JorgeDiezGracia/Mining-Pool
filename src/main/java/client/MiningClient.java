package client;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import java.security.MessageDigest;

public class MiningClient {

    private static final String HOST = "localhost";
    private static final int PORT = 9000;

    private PrintWriter out;
    private volatile boolean running = true;
    private volatile boolean mining  = false;
    private final int difficulty = 2;

    public static void main(String[] args) {
        new MiningClient().start();
    }

    public void start() {
        try (Socket socket = new Socket(HOST, PORT);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {

            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(Protocol.CONNECT);
            System.out.println("[Client] Sent: connect");

            String line;
            while (running && (line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            System.out.println("[Client] Error: " + e.getMessage());
        }
    }

    private void handleMessage(String message) {
        System.out.println("[Server→Client] " + message);

        if (message.startsWith(Protocol.ACK)) {
            System.out.println("[Client] Login confirmed.");

        } else if (message.startsWith(Protocol.NEW_REQUEST)) {
            out.println(Protocol.ACK);
            mining = true;
            String[] parts = message.split(" ", 3);
            String[] range = parts[1].split("-");
            int start = Integer.parseInt(range[0]);
            int end   = Integer.parseInt(range[1]);
            String blockData = parts[2];
            mine(blockData, start, end);

        } else if (message.startsWith(Protocol.END)) {
            System.out.println("[Client] Solution found by someone. Stopping.");
            mining  = false;
            running = false;
        }
    }

    private void mine(String blockData, int start, int end) {
        System.out.println("[Client] Mining " + start + " to " + end);
        String prefix = "0".repeat(difficulty);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int salt = start; salt <= end && mining; salt++) {
                byte[] hashBytes = digest.digest((blockData + salt).getBytes("UTF-8"));
                StringBuilder hex = new StringBuilder();
                for (byte b : hashBytes) hex.append(String.format("%02x", b));
                if (hex.toString().startsWith(prefix)) {
                    System.out.println("[Client] FOUND! Salt=" + salt);
                    out.println(Protocol.SOL + " " + salt);
                    mining = false;
                    return;
                }
            }
            System.out.println("[Client] Range exhausted, nothing found.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}