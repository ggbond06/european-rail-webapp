import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Persistent cache for route segment prices.
 *
 * The current price provider is a date-adjusted estimate based on stored DOT
 * segment fares. This cache isolates that source so a licensed live fare API
 * can be plugged in later without changing the web API or frontend.
 */
public class PriceCache {
    private static final long CACHE_TTL_MILLIS = 6L * 60L * 60L * 1000L;
    private static final String HEADER = "travel_date\tfrom\tto\tprice_eur\tupdated_at";

    private final Path cacheFile;
    private final Map<String, CacheEntry> entries = new HashMap<>();

    public PriceCache(Path cacheFile) {
        this.cacheFile = cacheFile;
        load();
    }

    public synchronized CachedPriceResult getPrices(
            List<String> path,
            List<Double> basePrices,
            LocalDate travelDate,
            double multiplier) {
        List<Double> prices = new ArrayList<>();
        boolean usedCachedPrice = false;
        boolean refreshedAnyPrice = false;

        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            String key = key(travelDate, from, to);
            CacheEntry entry = entries.get(key);

            if (entry != null && !entry.isExpired()) {
                prices.add(entry.price);
                usedCachedPrice = true;
                continue;
            }

            double refreshedPrice = roundCurrency(basePrices.get(i) * multiplier);
            entries.put(key, new CacheEntry(travelDate, from, to, refreshedPrice, Instant.now()));
            prices.add(refreshedPrice);
            refreshedAnyPrice = true;
        }

        if (refreshedAnyPrice) {
            save();
        }

        String status = refreshedAnyPrice
                ? "refreshed"
                : usedCachedPrice ? "cache_hit" : "not_cached";
        return new CachedPriceResult(prices, status);
    }

    private void load() {
        if (!Files.exists(cacheFile)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(cacheFile, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank() || line.equals(HEADER)) {
                    continue;
                }

                String[] parts = line.split("\t", -1);
                if (parts.length != 5) {
                    continue;
                }

                try {
                    CacheEntry entry = new CacheEntry(
                            LocalDate.parse(parts[0]),
                            parts[1],
                            parts[2],
                            Double.parseDouble(parts[3]),
                            Instant.parse(parts[4]));
                    entries.put(key(entry.travelDate, entry.from, entry.to), entry);
                } catch (RuntimeException ignored) {
                    // Skip malformed cache rows.
                }
            }
        } catch (IOException ignored) {
            // A missing or unreadable cache should not prevent route searches.
        }
    }

    private void save() {
        try {
            Path parent = cacheFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            List<String> lines = new ArrayList<>();
            lines.add(HEADER);
            for (CacheEntry entry : entries.values()) {
                StringJoiner row = new StringJoiner("\t");
                row.add(entry.travelDate.toString());
                row.add(entry.from);
                row.add(entry.to);
                row.add(formatCurrency(entry.price));
                row.add(entry.updatedAt.toString());
                lines.add(row.toString());
            }
            Files.write(cacheFile, lines, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Cache persistence is helpful but not required for a response.
        }
    }

    private String key(LocalDate travelDate, String from, String to) {
        return travelDate + "\u001f" + from + "\u001f" + to;
    }

    private static double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static String formatCurrency(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static class CacheEntry {
        private final LocalDate travelDate;
        private final String from;
        private final String to;
        private final double price;
        private final Instant updatedAt;

        private CacheEntry(LocalDate travelDate, String from, String to, double price, Instant updatedAt) {
            this.travelDate = travelDate;
            this.from = from;
            this.to = to;
            this.price = price;
            this.updatedAt = updatedAt;
        }

        private boolean isExpired() {
            return updatedAt.plusMillis(CACHE_TTL_MILLIS).isBefore(Instant.now());
        }
    }

    public static class CachedPriceResult {
        private final List<Double> prices;
        private final String cacheStatus;

        private CachedPriceResult(List<Double> prices, String cacheStatus) {
            this.prices = prices;
            this.cacheStatus = cacheStatus;
        }

        public List<Double> getPrices() {
            return prices;
        }

        public String getCacheStatus() {
            return cacheStatus;
        }
    }
}
