import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.Jedis;

public class PriceCache {
    private static final int CACHE_EXPIRATION_SECONDS = 6 * 60 * 60; // 6 hours
    private final JedisPool jedisPool;

    public PriceCache(String redisHost, int redisPort) {
        this.jedisPool = new JedisPool(new JedisPoolConfig(), redisHost, redisPort);
    }
    
    private String key(LocalDate travelDate, String from, String to) {
        StringBuilder sb = new StringBuilder();
        sb.append(travelDate.toString());
        sb.append(":");
        sb.append(from);
        sb.append(":");
        sb.append(to);
        return sb.toString();
    }

    public CachedPriceResult getCachedPrices(List<String> path, List<Double> basePrices, LocalDate travelData, double multiplier) {

        List<Double> prices = new ArrayList<>();
        boolean usedCachedPrice = false;
        boolean refreshedAny = false;

        try (Jedis jedis = jedisPool.getResource()) {
            for (int i = 0; i < path.size() - 1; i++) {
                String from = path.get(i);
                String to = path.get(i + 1);
                String key = key(travelData, from, to);
                String cachedPriceStr = jedis.get(key);

                if (cachedPriceStr != null) {
                    double cachedPrice = Double.parseDouble(cachedPriceStr);
                    prices.add(cachedPrice);
                    usedCachedPrice = true;
                } else {
                    double basePrice = basePrices.get(i);
                    double newPrice = basePrice * multiplier;
                    prices.add(newPrice);
                    jedis.set(key, String.format(Locale.US, "%.2f", newPrice), SetParams.setParams().ex(CACHE_EXPIRATION_SECONDS));
                    refreshedAny = true;
                }
            }
            if (refreshedAny) {
                return new CachedPriceResult(prices, "refreshed");
            } else if (usedCachedPrice) {
                return new CachedPriceResult(prices, "cached");
            } else {
                return new CachedPriceResult(prices, "new");
            }
        }
    }

    public static class CachedPriceResult {

        private final List<Double> prices;
        private final String cachedStatus;

        public CachedPriceResult(List<Double> prices, String chachedStatus) {
            this.prices = prices;
            this.cachedStatus = chachedStatus;
        }

        public List<Double> getPrices() {
            return prices;
        }

        public String getCachedStatus() {
            return cachedStatus;
        }
    }

    public void close() {
        jedisPool.close();
    }
}
