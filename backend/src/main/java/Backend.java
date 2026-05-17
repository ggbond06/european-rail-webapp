import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

/**
 * Backend class that implements BackendInterface. Uses a GraphADT to store
 * European rail data and provide shortest path queries.
 */
public class Backend implements BackendInterface {

    private GraphADT<String, Double> graph;
    private List<String> allLocations;
    private Map<String, Double> edgePrices;
    private Map<String, List<RouteEdge>> outgoingEdges;

    /**
     * Constructor that takes a GraphADT to store graph data.
     * @param graph object to store the backend's graph data
     */
    public Backend(GraphADT<String, Double> graph) {
        this.graph = graph;
        this.allLocations = new ArrayList<>();
        this.edgePrices = new HashMap<>();
        this.outgoingEdges = new HashMap<>();
    }

    /**
     * Loads graph data from a dot file. If a graph was previously loaded, this
     * method first deletes the contents (nodes and edges) of the existing
     * graph before loading a new one.
     * @param filename the path to a dot file to read graph data from
     * @throws IOException if there was any problem reading from this file
     */
    @Override
    public void loadGraphData(String filename) throws IOException {
        // Clear existing graph data by removing all previously tracked nodes
        for (String location : new ArrayList<>(allLocations)) {
            graph.removeNode(location);
        }
        allLocations.clear();
        edgePrices.clear();
        outgoingEdges.clear();

        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            // Look for edge lines in the format: "City1" -> "City2" [minutes=123];
            if (line.contains("->") && line.contains("[minutes=")) {
                // Parse the source city
                int firstQuote = line.indexOf('"');
                int secondQuote = line.indexOf('"', firstQuote + 1);
                String source = line.substring(firstQuote + 1, secondQuote);

                // Parse the destination city
                int thirdQuote = line.indexOf('"', secondQuote + 1);
                int fourthQuote = line.indexOf('"', thirdQuote + 1);
                String destination = line.substring(thirdQuote + 1, fourthQuote);

                Map<String, String> attributes = parseAttributes(line);
                double minutes = Double.parseDouble(attributes.get("minutes"));
                double price = attributes.containsKey("price_eur")
                    ? Double.parseDouble(attributes.get("price_eur"))
                    : estimatePrice(minutes);

                // Insert nodes if not already present
                if (!allLocations.contains(source)) {
                    graph.insertNode(source);
                    allLocations.add(source);
                }
                if (!allLocations.contains(destination)) {
                    graph.insertNode(destination);
                    allLocations.add(destination);
                }

                // Insert the edge (updates weight if edge already exists)
                graph.insertEdge(source, destination, minutes);
                edgePrices.put(edgeKey(source, destination), price);
                outgoingEdges.computeIfAbsent(source, key -> new ArrayList<>())
                        .add(new RouteEdge(destination, minutes, price));
                outgoingEdges.computeIfAbsent(destination, key -> new ArrayList<>());
            }
        }
        reader.close();
    }

    /**
     * Returns a list of all locations in the graph.
     * @return list of all location names
     */
    @Override
    public List<String> getListOfAll() {
        return new ArrayList<>(allLocations);
    }

    /**
     * Return the sequence of locations along the shortest path from start to
     * end, or an empty list if no such path exists.
     * @param start the start of the path
     * @param end the end of the path
     * @return a list with the nodes along the shortest path from start to end,
     *         or an empty list if no such path exists
     */
    @Override
    public List<String> findLocationsOnShortestPath(String start, String end) {
        return findLocationsOnPath(start, end, "time");
    }

    @Override
    public List<String> findLocationsOnPath(String start, String end, String optimizationMode) {
        try {
            return computeOptimizedRoute(start, end, optimizationMode).path;
        } catch (NoSuchElementException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Return the times in minutes between each two nodes on the shortest path
     * from start to end, or an empty list if no such path exists.
     * @param start the start of the path
     * @param end the end of the path
     * @return a list with the times in minutes between two nodes along the
     *         shortest path from start to end, or an empty list if no such
     *         path exists
     */
    @Override
    public List<Double> findTimesOnShortestPath(String start, String end) {
        List<String> path = findLocationsOnShortestPath(start, end);
        return findTimesOnPath(path);
    }

    @Override
    public List<Double> findTimesOnPath(List<String> path) {
        List<Double> times = new ArrayList<>();
        if (path.isEmpty()) {
            return times;
        }
        // Get the edge weight between each consecutive pair on the path
        for (int i = 0; i < path.size() - 1; i++) {
            times.add(graph.getEdge(path.get(i), path.get(i + 1)));
        }
        return times;
    }

    /**
     * Return the estimated ticket prices in euros between each two nodes on
     * the shortest path from start to end, or an empty list if no such path
     * exists.
     */
    @Override
    public List<Double> findPricesOnShortestPath(String start, String end) {
        List<String> path = findLocationsOnShortestPath(start, end);
        return findPricesOnPath(path);
    }

    @Override
    public List<Double> findPricesOnPath(List<String> path) {
        List<Double> prices = new ArrayList<>();
        if (path.isEmpty()) {
            return prices;
        }

        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            Double price = edgePrices.get(edgeKey(from, to));
            if (price == null) {
                price = estimatePrice(graph.getEdge(from, to));
            }
            prices.add(price);
        }
        return prices;
    }

    /**
     * Returns the location that can be reached from all of the specified start
     * locations in the shortest time: minimizing the sum of the times from
     * each start location.
     * @param starts the list of locations to minimize times from
     * @return the location that can be reached in the shortest total time
     *         from all of the specified start locations
     * @throws NoSuchElementException if there is no location that can be
     *         reached from all start locations, or if any start location does
     *         not exist within the graph
     */
    @Override
    public String getClosestLocationFromAll(List<String> starts) throws NoSuchElementException {
        return getClosestLocationFromAll(starts, "time");
    }

    @Override
    public String getClosestLocationFromAll(List<String> starts, String optimizationMode)
            throws NoSuchElementException {
        // Validate that all start locations exist in the graph
        for (String start : starts) {
            if (!graph.containsNode(start)) {
                throw new NoSuchElementException("Start location not found: " + start);
            }
        }

        String bestLocation = null;
        MeetingScore bestScore = null;
        String mode = normalizeMode(optimizationMode);

        // Try every location as a potential meeting point
        for (String candidate : allLocations) {
            MeetingScore score = new MeetingScore();
            boolean reachable = true;

            for (String start : starts) {
                try {
                    RouteResult route = computeOptimizedRoute(start, candidate,
                            mode.equals("fairness") ? "time" : mode);
                    score.addRoute(route);
                } catch (NoSuchElementException e) {
                    reachable = false;
                    break;
                }
            }

            if (reachable && (bestScore == null || compareMeetingScores(score, bestScore, mode) < 0)) {
                bestScore = score;
                bestLocation = candidate;
            }
        }

        if (bestLocation == null) {
            throw new NoSuchElementException(
                "No location reachable from all start locations");
        }

        return bestLocation;
    }

    private RouteResult computeOptimizedRoute(String start, String end, String optimizationMode) {
        if (!graph.containsNode(start) || !graph.containsNode(end)) {
            throw new NoSuchElementException("Start or destination location not found");
        }

        String mode = normalizeMode(optimizationMode);
        PriorityQueue<RouteState> queue = new PriorityQueue<>(
                (left, right) -> compareRouteStates(left, right, mode));
        Map<String, RouteState> best = new HashMap<>();
        RouteState initial = new RouteState(start, null, 0, 0, 0);
        queue.add(initial);
        best.put(start, initial);

        while (!queue.isEmpty()) {
            RouteState current = queue.poll();
            if (best.get(current.city) != current) {
                continue;
            }
            if (current.city.equals(end)) {
                return current.toRouteResult();
            }

            for (RouteEdge edge : outgoingEdges.getOrDefault(current.city, new ArrayList<>())) {
                RouteState next = new RouteState(
                        edge.to,
                        current,
                        current.totalMinutes + edge.minutes,
                        current.totalPrice + edge.price,
                        current.transfers + 1);
                RouteState previousBest = best.get(next.city);
                if (previousBest == null || compareRouteStates(next, previousBest, mode) < 0) {
                    best.put(next.city, next);
                    queue.add(next);
                }
            }
        }

        throw new NoSuchElementException("No path from start to end node");
    }

    private String normalizeMode(String optimizationMode) {
        if (optimizationMode == null) {
            return "time";
        }
        String mode = optimizationMode.trim().toLowerCase();
        if (mode.equals("price") || mode.equals("transfers") || mode.equals("fairness")) {
            return mode;
        }
        return "time";
    }

    private int compareRouteStates(RouteState left, RouteState right, String mode) {
        if (mode.equals("price")) {
            return compareValues(left.totalPrice, right.totalPrice,
                    left.totalMinutes, right.totalMinutes,
                    left.transfers, right.transfers);
        }
        if (mode.equals("transfers")) {
            return compareValues(left.transfers, right.transfers,
                    left.totalMinutes, right.totalMinutes,
                    left.totalPrice, right.totalPrice);
        }
        return compareValues(left.totalMinutes, right.totalMinutes,
                left.totalPrice, right.totalPrice,
                left.transfers, right.transfers);
    }

    private int compareMeetingScores(MeetingScore left, MeetingScore right, String mode) {
        if (mode.equals("price")) {
            return compareValues(left.totalPrice, right.totalPrice,
                    left.totalMinutes, right.totalMinutes,
                    left.totalTransfers, right.totalTransfers);
        }
        if (mode.equals("transfers")) {
            return compareValues(left.totalTransfers, right.totalTransfers,
                    left.totalMinutes, right.totalMinutes,
                    left.totalPrice, right.totalPrice);
        }
        if (mode.equals("fairness")) {
            return compareValues(left.timeSpread(), right.timeSpread(),
                    left.totalMinutes, right.totalMinutes,
                    left.totalPrice, right.totalPrice);
        }
        return compareValues(left.totalMinutes, right.totalMinutes,
                left.totalPrice, right.totalPrice,
                left.totalTransfers, right.totalTransfers);
    }

    private int compareValues(double primaryLeft, double primaryRight,
            double secondaryLeft, double secondaryRight,
            double tertiaryLeft, double tertiaryRight) {
        int primary = Double.compare(primaryLeft, primaryRight);
        if (primary != 0) {
            return primary;
        }
        int secondary = Double.compare(secondaryLeft, secondaryRight);
        if (secondary != 0) {
            return secondary;
        }
        return Double.compare(tertiaryLeft, tertiaryRight);
    }

    private Map<String, String> parseAttributes(String line) {
        Map<String, String> attributes = new HashMap<>();
        int start = line.indexOf('[');
        int end = line.indexOf(']', start);
        if (start < 0 || end < 0) {
            return attributes;
        }

        String[] pairs = line.substring(start + 1, end).split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.trim().split("=", 2);
            if (keyValue.length == 2) {
                attributes.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return attributes;
    }

    private String edgeKey(String source, String destination) {
        return source + "\u001f" + destination;
    }

    private double estimatePrice(double minutes) {
        double rate;
        if (minutes <= 60) {
            rate = 0.22;
        } else if (minutes <= 180) {
            rate = 0.24;
        } else if (minutes <= 300) {
            rate = 0.27;
        } else {
            rate = 0.30;
        }
        return Math.round((6.0 + minutes * rate) * 100.0) / 100.0;
    }

    private static class RouteEdge {
        private final String to;
        private final double minutes;
        private final double price;

        private RouteEdge(String to, double minutes, double price) {
            this.to = to;
            this.minutes = minutes;
            this.price = price;
        }
    }

    private static class RouteState {
        private final String city;
        private final RouteState previous;
        private final double totalMinutes;
        private final double totalPrice;
        private final int transfers;

        private RouteState(String city, RouteState previous,
                double totalMinutes, double totalPrice, int transfers) {
            this.city = city;
            this.previous = previous;
            this.totalMinutes = totalMinutes;
            this.totalPrice = totalPrice;
            this.transfers = transfers;
        }

        private RouteResult toRouteResult() {
            LinkedListBuilder builder = new LinkedListBuilder();
            RouteState current = this;
            while (current != null) {
                builder.addFirst(current.city);
                current = current.previous;
            }
            return new RouteResult(builder.values, totalMinutes, totalPrice, transfers);
        }
    }

    private static class RouteResult {
        private final List<String> path;
        private final double totalMinutes;
        private final double totalPrice;
        private final int transfers;

        private RouteResult(List<String> path, double totalMinutes,
                double totalPrice, int transfers) {
            this.path = path;
            this.totalMinutes = totalMinutes;
            this.totalPrice = totalPrice;
            this.transfers = transfers;
        }
    }

    private static class MeetingScore {
        private double totalMinutes = 0;
        private double totalPrice = 0;
        private double shortestMinutes = Double.MAX_VALUE;
        private double longestMinutes = 0;
        private int totalTransfers = 0;

        private void addRoute(RouteResult route) {
            totalMinutes += route.totalMinutes;
            totalPrice += route.totalPrice;
            totalTransfers += route.transfers;
            shortestMinutes = Math.min(shortestMinutes, route.totalMinutes);
            longestMinutes = Math.max(longestMinutes, route.totalMinutes);
        }

        private double timeSpread() {
            return longestMinutes - shortestMinutes;
        }
    }

    private static class LinkedListBuilder {
        private final List<String> values = new ArrayList<>();

        private void addFirst(String value) {
            values.add(0, value);
        }
    }
}
