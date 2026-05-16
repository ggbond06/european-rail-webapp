import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Backend class that implements BackendInterface. Uses a GraphADT to store
 * European rail data and provide shortest path queries.
 */
public class Backend implements BackendInterface {

    private GraphADT<String, Double> graph;
    private List<String> allLocations;

    /**
     * Constructor that takes a GraphADT to store graph data.
     * @param graph object to store the backend's graph data
     */
    public Backend(GraphADT<String, Double> graph) {
        this.graph = graph;
        this.allLocations = new ArrayList<>();
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

                // Parse the minutes value
                int minutesStart = line.indexOf("minutes=") + "minutes=".length();
                int minutesEnd = line.indexOf(']', minutesStart);
                double minutes = Double.parseDouble(line.substring(minutesStart, minutesEnd));

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
        try {
            return graph.shortestPathData(start, end);
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
        // Validate that all start locations exist in the graph
        for (String start : starts) {
            if (!graph.containsNode(start)) {
                throw new NoSuchElementException("Start location not found: " + start);
            }
        }

        String bestLocation = null;
        double bestTotalCost = Double.MAX_VALUE;

        // Try every location as a potential meeting point
        for (String candidate : allLocations) {
            double totalCost = 0;
            boolean reachable = true;

            for (String start : starts) {
                try {
                    totalCost += graph.shortestPathCost(start, candidate);
                } catch (NoSuchElementException e) {
                    reachable = false;
                    break;
                }
            }

            if (reachable && totalCost < bestTotalCost) {
                bestTotalCost = totalCost;
                bestLocation = candidate;
            }
        }

        if (bestLocation == null) {
            throw new NoSuchElementException(
                "No location reachable from all start locations");
        }

        return bestLocation;
    }
}
