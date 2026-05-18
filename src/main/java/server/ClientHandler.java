package server;

import common.Protocol;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final MiningServer server;
    private final int id;
    private PrintWriter out;
    private int rangeStart;
    private int rangeEnd;

    public ClientHandler(Socket socket, MiningServer server, int id) {
        this.socket = socket;
        this.server = server;
        this.id = id;
    }

    public void sendMessage(String message) {
        if (out != null) out.println(message);
    }

    public int getId() { return id; }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);
            String line;
            while ((line = in.readLine()) != null) {
                handleMessage(line.trim());
            }
        } catch (IOException e) {
            System.out.println("[Server] Client " + id + " disconnected.");
        } finally {
            server.removeClient(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void handleMessage(String message) {
        System.out.println("[Client " + id + "] " + message);

        if (message.equals(Protocol.CONNECT)) {
            server.registerClient(this);

        } else if (message.equals(Protocol.ACK)) {
            System.out.println("[Server] Client " + id + " acknowledged.");

        } else if (message.startsWith(Protocol.SOL)) {
            String[] parts = message.split(" ");
            if (parts.length == 2) {
                int salt = Integer.parseInt(parts[1]);
                server.validateSolution(this, salt);
            }
        }
    }

    public void setRange(int start, int end) {
        this.rangeStart = start;
        this.rangeEnd = end;
    }
}
