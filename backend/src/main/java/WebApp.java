import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * HTTP server for the European Rail Navigator.
 *
 * The Java side owns the rail graph and shortest-path computation. The React
 * frontend calls these JSON endpoints:
 *   GET /api/locations
 *   GET /api/shortest-path?start=Amsterdam&end=Vienna
 *   GET /api/closest?from=Amsterdam,Paris,Berlin
 */
public class WebApp {
    private static final String GRAPH_FILE = findExistingPath(
            "data/europeanRail.dot",
            "europeanRail.dot",
            "../data/europeanRail.dot");
    private static final Path DIST_ROOT = Paths.get(findExistingPath(
            "frontend/dist",
            "dist",
            "../frontend/dist"));
    private static final BackendInterface BACKEND = createBackend();
    private static final PriceCache PRICE_CACHE = new PriceCache(Paths.get(findExistingPath(
            "data/pricing-cache.tsv",
            "pricing-cache.tsv",
            "../data/pricing-cache.tsv")));
    private static final UserStore USER_STORE = new UserStore(Paths.get(findExistingPath(
            "data/users.tsv",
            "users.tsv",
            "../data/users.tsv")));
    private static final CartStore CART_STORE = new CartStore(Paths.get(findExistingPath(
            "data/carts.tsv",
            "carts.tsv",
            "../data/carts.tsv")));

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Pass one argument: the port to run the server on.");
        }

        int portNumber = Integer.parseInt(args[0]);
        HttpServer server = HttpServer.create(new InetSocketAddress(portNumber), 8);
        HttpContext context = server.createContext("/");
        context.setHandler(WebApp::requestHandler);
        System.out.println("European Rail Navigator running at http://localhost:" + portNumber);
        server.start();
    }

    public static void requestHandler(HttpExchange exchange) {
        try {
            addCommonHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 204, "", "text/plain; charset=utf-8");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/locations")) {
                handleLocations(exchange);
            } else if (path.equals("/api/shortest-path")) {
                handleShortestPath(exchange);
            } else if (path.equals("/api/closest")) {
                handleClosest(exchange);
            } else if (path.equals("/api/register")) {
                handleRegister(exchange);
            } else if (path.equals("/api/login")) {
                handleLogin(exchange);
            } else if (path.equals("/api/cart")) {
                handleCart(exchange);
            } else if (path.equals("/api/cart/checkout")) {
                handleCheckout(exchange);
            } else {
                serveReactApp(exchange, path);
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendJson(exchange, 500, "{\"error\":\"Internal server error.\"}");
            } catch (IOException ignored) {
                // The client may already be gone.
            }
        }
    }

    private static void handleRegister(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) {
            return;
        }

        Map<String, String> body = parseJsonObject(readBody(exchange));
        try {
            UserStore.AuthResult result = USER_STORE.register(
                    body.get("name"),
                    body.get("email"),
                    body.get("password"));
            sendJson(exchange, 200, authResultToJson(result));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":" + quote(e.getMessage()) + "}");
        }
    }

    private static void handleLogin(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) {
            return;
        }

        Map<String, String> body = parseJsonObject(readBody(exchange));
        try {
            UserStore.AuthResult result = USER_STORE.login(body.get("email"), body.get("password"));
            sendJson(exchange, 200, authResultToJson(result));
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 401, "{\"error\":" + quote(e.getMessage()) + "}");
        }
    }

    private static void handleCart(HttpExchange exchange) throws IOException {
        UserStore.User user = authenticatedUser(exchange);
        if (user == null) {
            sendJson(exchange, 401, "{\"error\":\"Please log in first.\"}");
            return;
        }

        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            sendJson(exchange, 200, cartItemsToJson(CART_STORE.getItems(user.getEmail())));
        } else if ("POST".equalsIgnoreCase(method)) {
            Map<String, String> body = parseJsonObject(readBody(exchange));
            try {
                CartStore.CartItem item = new CartStore.CartItem(
                        clean(body.get("start")),
                        clean(body.get("end")),
                        clean(body.get("travelDate")),
                        parseDouble(body.get("totalMinutes")),
                        parseDouble(body.get("totalPriceEuros")),
                        clean(body.get("pathSummary")));
                CartStore.CartItem saved = CART_STORE.addItem(user.getEmail(), item);
                sendJson(exchange, 200, "{\"item\":" + cartItemToJson(saved) + "}");
            } catch (IllegalArgumentException e) {
                sendJson(exchange, 400, "{\"error\":\"Could not add this trip to the cart.\"}");
            }
        } else if ("DELETE".equalsIgnoreCase(method)) {
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            boolean removed = CART_STORE.removeItem(user.getEmail(), clean(query.get("id")));
            sendJson(exchange, removed ? 200 : 404, "{\"removed\":" + removed + "}");
        } else {
            sendJson(exchange, 405, "{\"error\":\"Unsupported cart method.\"}");
        }
    }

    private static void handleCheckout(HttpExchange exchange) throws IOException {
        if (!requireMethod(exchange, "POST")) {
            return;
        }

        UserStore.User user = authenticatedUser(exchange);
        if (user == null) {
            sendJson(exchange, 401, "{\"error\":\"Please log in first.\"}");
            return;
        }

        CartStore.CheckoutResult result = CART_STORE.checkout(user.getEmail());
        String json = "{"
                + "\"purchased\":" + result.getItemCount() + ","
                + "\"totalPriceEuros\":" + formatNumber(result.getTotalPriceEuros())
                + "}";
        sendJson(exchange, 200, json);
    }

    private static BackendInterface createBackend() {
        try {
            GraphADT<String, Double> graph = new DijkstraGraph<>();
            BackendInterface backend = new Backend(graph);
            backend.loadGraphData(GRAPH_FILE);
            return backend;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load " + GRAPH_FILE, e);
        }
    }

    private static void handleLocations(HttpExchange exchange) throws IOException {
        List<String> locations = new ArrayList<>(BACKEND.getListOfAll());
        Collections.sort(locations);
        sendJson(exchange, 200, "{\"locations\":" + stringListToJson(locations) + "}");
    }

    private static void handleShortestPath(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String start = clean(query.get("start"));
        String end = clean(query.get("end"));
        String dateValue = clean(query.get("date"));
        String optimizationMode = normalizeOptimizationMode(clean(query.get("mode")));

        if (start.isEmpty() || end.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Choose both a start and destination city.\"}");
            return;
        }

        LocalDate travelDate;
        try {
            travelDate = parseTravelDate(dateValue);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Choose a valid travel date that is not in the past.\"}");
            return;
        }

        List<String> path = BACKEND.findLocationsOnPath(start, end, optimizationMode);
        List<Double> times = BACKEND.findTimesOnPath(path);
        List<Double> basePrices = BACKEND.findPricesOnPath(path);
        double priceMultiplier = datePriceMultiplier(travelDate);
        if (path.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"No rail path was found between those cities.\"}");
            return;
        }
        PriceCache.CachedPriceResult cachedPrices =
                PRICE_CACHE.getPrices(path, basePrices, travelDate, priceMultiplier);
        List<Double> prices = cachedPrices.getPrices();

        double total = sum(times);
        double totalPrice = roundCurrency(sum(prices));
        String json = "{"
                + "\"start\":" + quote(start) + ","
                + "\"end\":" + quote(end) + ","
                + "\"path\":" + stringListToJson(path) + ","
                + "\"times\":" + numberListToJson(times) + ","
                + "\"prices\":" + numberListToJson(prices) + ","
                + "\"travelDate\":" + quote(travelDate.toString()) + ","
                + "\"optimizationMode\":" + quote(optimizationMode) + ","
                + "\"optimizationLabel\":" + quote(optimizationLabel(optimizationMode)) + ","
                + "\"pricingMode\":\"date_adjusted_estimate\","
                + "\"priceCacheStatus\":" + quote(cachedPrices.getCacheStatus()) + ","
                + "\"priceMultiplier\":" + formatNumber(priceMultiplier) + ","
                + "\"totalMinutes\":" + formatNumber(total) + ","
                + "\"totalPriceEuros\":" + formatNumber(totalPrice)
                + "}";
        sendJson(exchange, 200, json);
    }

    private static void handleClosest(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String from = clean(query.get("from"));
        String optimizationMode = normalizeMeetingMode(clean(query.get("mode")));
        List<String> starts = splitLocations(from);

        if (starts.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Enter at least one starting city.\"}");
            return;
        }

        try {
            String closest = BACKEND.getClosestLocationFromAll(starts, optimizationMode);
            String pathMode = optimizationMode.equals("fairness") ? "time" : optimizationMode;
            double total = 0;
            double totalPrice = 0;
            int totalTransfers = 0;
            double shortest = Double.MAX_VALUE;
            double longest = 0;
            List<Double> travelTimes = new ArrayList<>();
            for (String start : starts) {
                List<String> path = BACKEND.findLocationsOnPath(start, closest, pathMode);
                List<Double> times = BACKEND.findTimesOnPath(path);
                List<Double> prices = BACKEND.findPricesOnPath(path);
                double routeMinutes = sum(times);
                total += routeMinutes;
                totalPrice += sum(prices);
                totalTransfers += Math.max(0, path.size() - 1);
                shortest = Math.min(shortest, routeMinutes);
                longest = Math.max(longest, routeMinutes);
                travelTimes.add(routeMinutes);
            }
            String json = "{"
                    + "\"starts\":" + stringListToJson(starts) + ","
                    + "\"closest\":" + quote(closest) + ","
                    + "\"optimizationMode\":" + quote(optimizationMode) + ","
                    + "\"optimizationLabel\":" + quote(optimizationLabel(optimizationMode)) + ","
                    + "\"totalMinutes\":" + formatNumber(total) + ","
                    + "\"totalPriceEuros\":" + formatNumber(roundCurrency(totalPrice)) + ","
                    + "\"totalTransfers\":" + totalTransfers + ","
                    + "\"timeSpreadMinutes\":" + formatNumber(longest - shortest) + ","
                    + "\"travelTimes\":" + numberListToJson(travelTimes)
                    + "}";
            sendJson(exchange, 200, json);
        } catch (NoSuchElementException e) {
            sendJson(exchange, 404, "{\"error\":\"No shared reachable city was found.\"}");
        }
    }

    private static void serveReactApp(HttpExchange exchange, String requestPath) throws IOException {
        Path distRoot = DIST_ROOT.toAbsolutePath().normalize();
        Path file = requestPath.equals("/")
                ? distRoot.resolve("index.html")
                : distRoot.resolve(requestPath.substring(1)).normalize();

        if (!file.startsWith(distRoot) || !Files.exists(file) || Files.isDirectory(file)) {
            file = distRoot.resolve("index.html");
        }

        if (!Files.exists(file)) {
            String message = "React app has not been built yet. Run: npm install && npm run build";
            sendText(exchange, 404, message, "text/plain; charset=utf-8");
            return;
        }

        byte[] bytes = Files.readAllBytes(file);
        exchange.getResponseHeaders().set("Content-Type", contentType(file));
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String findExistingPath(String... candidates) {
        for (String candidate : candidates) {
            if (Files.exists(Paths.get(candidate))) {
                return candidate;
            }
        }
        return candidates[0];
    }

    private static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }

        for (String arg : rawQuery.split("&")) {
            String[] pair = arg.split("=", 2);
            if (pair.length == 2) {
                String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                map.put(key, value);
            }
        }
        return map;
    }

    private static Map<String, String> parseJsonObject(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null) {
            return map;
        }

        int index = 0;
        while (index < json.length()) {
            int keyStart = json.indexOf('"', index);
            if (keyStart < 0) {
                break;
            }
            int keyEnd = findStringEnd(json, keyStart + 1);
            if (keyEnd < 0) {
                break;
            }
            String key = unescapeJson(json.substring(keyStart + 1, keyEnd));
            int colon = json.indexOf(':', keyEnd);
            if (colon < 0) {
                break;
            }
            int valueStart = colon + 1;
            while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
                valueStart++;
            }

            String value;
            if (valueStart < json.length() && json.charAt(valueStart) == '"') {
                int valueEnd = findStringEnd(json, valueStart + 1);
                if (valueEnd < 0) {
                    break;
                }
                value = unescapeJson(json.substring(valueStart + 1, valueEnd));
                index = valueEnd + 1;
            } else {
                int valueEnd = valueStart;
                while (valueEnd < json.length()
                        && ",}".indexOf(json.charAt(valueEnd)) < 0) {
                    valueEnd++;
                }
                value = json.substring(valueStart, valueEnd).trim();
                index = valueEnd;
            }
            map.put(key, value);
        }
        return map;
    }

    private static int findStringEnd(String json, int start) {
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String unescapeJson(String value) {
        return value.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static List<String> splitLocations(String from) {
        List<String> starts = new ArrayList<>();
        for (String part : from.split(",")) {
            String location = clean(part);
            if (!location.isEmpty()) {
                starts.add(location);
            }
        }
        return starts;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static double sum(List<Double> values) {
        double total = 0;
        for (Double value : values) {
            total += value;
        }
        return total;
    }

    private static double roundCurrency(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing number");
        }
        return Double.parseDouble(value);
    }

    private static LocalDate parseTravelDate(String dateValue) {
        LocalDate date;
        if (dateValue.isEmpty()) {
            date = LocalDate.now();
        } else {
            try {
                date = LocalDate.parse(dateValue);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid travel date");
            }
        }

        if (date.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Travel date is in the past");
        }
        return date;
    }

    private static double datePriceMultiplier(LocalDate travelDate) {
        long daysAway = ChronoUnit.DAYS.between(LocalDate.now(), travelDate);
        double multiplier;
        if (daysAway == 0) {
            multiplier = 1.60;
        } else if (daysAway <= 3) {
            multiplier = 1.45;
        } else if (daysAway <= 13) {
            multiplier = 1.25;
        } else if (daysAway <= 29) {
            multiplier = 1.10;
        } else if (daysAway <= 59) {
            multiplier = 1.00;
        } else {
            multiplier = 0.92;
        }

        DayOfWeek day = travelDate.getDayOfWeek();
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            multiplier += 0.08;
        }

        int month = travelDate.getMonthValue();
        int dayOfMonth = travelDate.getDayOfMonth();
        if (month == 7 || month == 8) {
            multiplier += 0.06;
        } else if (month == 12 && dayOfMonth >= 15) {
            multiplier += 0.12;
        }

        return roundCurrency(multiplier);
    }

    private static String normalizeOptimizationMode(String mode) {
        if (mode == null) {
            return "time";
        }
        if (mode.equals("price") || mode.equals("transfers")) {
            return mode;
        }
        return "time";
    }

    private static String normalizeMeetingMode(String mode) {
        if (mode == null) {
            return "time";
        }
        if (mode.equals("price") || mode.equals("transfers") || mode.equals("fairness")) {
            return mode;
        }
        return "time";
    }

    private static String optimizationLabel(String mode) {
        if (mode.equals("price")) {
            return "lowest total ticket price";
        }
        if (mode.equals("transfers")) {
            return "fewest transfers";
        }
        if (mode.equals("fairness")) {
            return "most equal travel time";
        }
        return "shortest total travel time";
    }

    private static String stringListToJson(List<String> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(quote(values.get(i)));
        }
        return json.append("]").toString();
    }

    private static String numberListToJson(List<Double> values) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(formatNumber(values.get(i)));
        }
        return json.append("]").toString();
    }

    private static String authResultToJson(UserStore.AuthResult result) {
        return "{"
                + "\"token\":" + quote(result.getToken()) + ","
                + "\"user\":{"
                + "\"name\":" + quote(result.getUser().getName()) + ","
                + "\"email\":" + quote(result.getUser().getEmail())
                + "}}";
    }

    private static String cartItemsToJson(List<CartStore.CartItem> items) {
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(cartItemToJson(items.get(i)));
        }
        return json.append("]}").toString();
    }

    private static String cartItemToJson(CartStore.CartItem item) {
        return "{"
                + "\"id\":" + quote(item.getId()) + ","
                + "\"start\":" + quote(item.getStart()) + ","
                + "\"end\":" + quote(item.getEnd()) + ","
                + "\"travelDate\":" + quote(item.getTravelDate()) + ","
                + "\"totalMinutes\":" + formatNumber(item.getTotalMinutes()) + ","
                + "\"totalPriceEuros\":" + formatNumber(item.getTotalPriceEuros()) + ","
                + "\"pathSummary\":" + quote(item.getPathSummary()) + ","
                + "\"createdAt\":" + quote(item.getCreatedAt())
                + "}";
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                escaped.append('\\').append(c);
            } else if (c == '\n') {
                escaped.append("\\n");
            } else if (c == '\r') {
                escaped.append("\\r");
            } else if (c == '\t') {
                escaped.append("\\t");
            } else {
                escaped.append(c);
            }
        }
        return escaped.append("\"").toString();
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }

    private static void addCommonHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static boolean requireMethod(HttpExchange exchange, String method) throws IOException {
        if (method.equalsIgnoreCase(exchange.getRequestMethod())) {
            return true;
        }
        sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
        return false;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static UserStore.User authenticatedUser(HttpExchange exchange) {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return null;
        }
        try {
            return USER_STORE.requireUser(auth.substring("Bearer ".length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        sendText(exchange, status, json, "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange exchange, int status, String body, String contentType)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (name.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }
}
