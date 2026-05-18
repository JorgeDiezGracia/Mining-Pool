package server;

import java.util.Random;

public class BlockGenerator {

    private static final String[] ACCOUNTS = {
            "ACC001", "ACC002", "ACC003", "ACC004", "ACC005",
            "ACC006", "ACC007", "ACC008", "ACC009", "ACC010"
    };
    private static final Random random = new Random();

    public static String generateBlock(int numTransactions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < numTransactions; i++) {
            String origin = ACCOUNTS[random.nextInt(ACCOUNTS.length)];
            String dest;
            do {
                dest = ACCOUNTS[random.nextInt(ACCOUNTS.length)];
            } while (dest.equals(origin));
            double amount = Math.round(random.nextDouble() * 1000 * 100.0) / 100.0;
            sb.append("mv|").append(origin).append("|").append(dest).append("|").append(amount);
            if (i < numTransactions - 1) sb.append(";");
        }
        return sb.toString();
    }
}