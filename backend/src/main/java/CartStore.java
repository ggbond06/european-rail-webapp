import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * File-backed cart storage keyed by user email.
 */
public class CartStore {
    private static final String HEADER =
            "id\temail\tstart\tend\ttravel_date\ttotal_minutes\ttotal_price_eur\tpath_summary\tcreated_at";

    private final Path cartsFile;
    private final List<CartItem> items = new ArrayList<>();

    public CartStore(Path cartsFile) {
        this.cartsFile = cartsFile;
        load();
    }

    public synchronized CartItem addItem(String email, CartItem item) {
        CartItem saved = new CartItem(
                UUID.randomUUID().toString(),
                email,
                item.start,
                item.end,
                item.travelDate,
                item.totalMinutes,
                item.totalPriceEuros,
                item.pathSummary,
                Instant.now().toString());
        items.add(saved);
        save();
        return saved;
    }

    public synchronized List<CartItem> getItems(String email) {
        List<CartItem> userItems = new ArrayList<>();
        for (CartItem item : items) {
            if (item.email.equals(email)) {
                userItems.add(item);
            }
        }
        return userItems;
    }

    public synchronized boolean removeItem(String email, String itemId) {
        boolean removed = items.removeIf(item -> item.email.equals(email) && item.id.equals(itemId));
        if (removed) {
            save();
        }
        return removed;
    }

    public synchronized CheckoutResult checkout(String email) {
        List<CartItem> userItems = getItems(email);
        double total = 0;
        for (CartItem item : userItems) {
            total += item.totalPriceEuros;
        }
        items.removeIf(item -> item.email.equals(email));
        save();
        return new CheckoutResult(userItems.size(), roundCurrency(total));
    }

    private void load() {
        if (!Files.exists(cartsFile)) {
            return;
        }

        try {
            for (String line : Files.readAllLines(cartsFile, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.equals(HEADER)) {
                    continue;
                }
                String[] parts = line.split("\t", -1);
                if (parts.length == 9) {
                    items.add(new CartItem(
                            parts[0],
                            parts[1],
                            parts[2],
                            parts[3],
                            parts[4],
                            Double.parseDouble(parts[5]),
                            Double.parseDouble(parts[6]),
                            parts[7],
                            parts[8]));
                }
            }
        } catch (IOException | NumberFormatException ignored) {
            // Start with an empty cart if the saved file is unreadable.
        }
    }

    private void save() {
        try {
            Path parent = cartsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            for (CartItem item : items) {
                lines.add(String.join("\t",
                        item.id,
                        item.email,
                        item.start,
                        item.end,
                        item.travelDate,
                        Double.toString(item.totalMinutes),
                        Double.toString(item.totalPriceEuros),
                        item.pathSummary,
                        item.createdAt));
            }
            Files.write(cartsFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Cart persistence is best effort for this local demo.
        }
    }

    private static double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static class CartItem {
        private final String id;
        private final String email;
        private final String start;
        private final String end;
        private final String travelDate;
        private final double totalMinutes;
        private final double totalPriceEuros;
        private final String pathSummary;
        private final String createdAt;

        public CartItem(String start, String end, String travelDate,
                double totalMinutes, double totalPriceEuros, String pathSummary) {
            this("", "", start, end, travelDate, totalMinutes, totalPriceEuros, pathSummary, "");
        }

        private CartItem(String id, String email, String start, String end, String travelDate,
                double totalMinutes, double totalPriceEuros, String pathSummary, String createdAt) {
            this.id = id;
            this.email = email;
            this.start = start;
            this.end = end;
            this.travelDate = travelDate;
            this.totalMinutes = totalMinutes;
            this.totalPriceEuros = totalPriceEuros;
            this.pathSummary = pathSummary;
            this.createdAt = createdAt;
        }

        public String getId() {
            return id;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }

        public String getTravelDate() {
            return travelDate;
        }

        public double getTotalMinutes() {
            return totalMinutes;
        }

        public double getTotalPriceEuros() {
            return totalPriceEuros;
        }

        public String getPathSummary() {
            return pathSummary;
        }

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public static class CheckoutResult {
        private final int itemCount;
        private final double totalPriceEuros;

        private CheckoutResult(int itemCount, double totalPriceEuros) {
            this.itemCount = itemCount;
            this.totalPriceEuros = totalPriceEuros;
        }

        public int getItemCount() {
            return itemCount;
        }

        public double getTotalPriceEuros() {
            return totalPriceEuros;
        }
    }
}
