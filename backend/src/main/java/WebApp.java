import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        if (start.isEmpty() || end.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Choose both a start and destination city.\"}");
            return;
        }

        List<String> path = BACKEND.findLocationsOnShortestPath(start, end);
        List<Double> times = BACKEND.findTimesOnShortestPath(start, end);
        if (path.isEmpty()) {
            sendJson(exchange, 404, "{\"error\":\"No rail path was found between those cities.\"}");
            return;
        }

        double total = sum(times);
        String json = "{"
                + "\"start\":" + quote(start) + ","
                + "\"end\":" + quote(end) + ","
                + "\"path\":" + stringListToJson(path) + ","
                + "\"times\":" + numberListToJson(times) + ","
                + "\"totalMinutes\":" + formatNumber(total)
                + "}";
        sendJson(exchange, 200, json);
    }

    private static void handleClosest(HttpExchange exchange) throws IOException {
        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String from = clean(query.get("from"));
        List<String> starts = splitLocations(from);

        if (starts.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Enter at least one starting city.\"}");
            return;
        }

        try {
            String closest = BACKEND.getClosestLocationFromAll(starts);
            double total = 0;
            for (String start : starts) {
                total += sum(BACKEND.findTimesOnShortestPath(start, closest));
            }
            String json = "{"
                    + "\"starts\":" + stringListToJson(starts) + ","
                    + "\"closest\":" + quote(closest) + ","
                    + "\"totalMinutes\":" + formatNumber(total)
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
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
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
