import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This is the interface that a backend developer will implement, so that a
 * frontend developer's code can make use of this functionality. It makes use of
 * a GraphADT to perform shortest path computations.
 */
public interface BackendInterface {

    /*
    * Implementing classes should support the constructor below.
    * @param graph object to store the backend's graph data
    */
    // public Backend(GraphADT<String,Double> graph);

    /**
     * Loads graph data from a dot file. If a graph was previously loaded, this
     * method should first delete the contents (nodes and edges) of the existing
     * graph before loading a new one.
     * @param filename the path to a dot file to read graph data from
     * @throws IOException if there was any problem reading from this file
     */
    public void loadGraphData(String filename) throws IOException;

    /**
     * Returns a list of all locations in the graph.
     * @return list of all location names
     */
    public List<String> getListOfAll();

    /**
     * Return the sequence of locations along the shortest path from start to 
     * end, or an empty list if no such path exists.
     * @param start the start of the path
     * @param end the end of the path
     * @return a list with the nodes along the shortest path from start to end,
     *         or an empty list if no such path exists
     */
    public List<String> findLocationsOnShortestPath(String start, String end);

    /**
     * Return the sequence of locations along an optimized path from start to
     * end. Supported optimization modes are time, price, and transfers.
     * Unknown modes should behave like time.
     * @param start the start of the path
     * @param end the end of the path
     * @param optimizationMode the mode used to choose the path
     * @return a list with the nodes along the optimized path
     */
    public List<String> findLocationsOnPath(String start, String end, String optimizationMode);

    /**
     * Return the times in minutes between each two nodes on the shortest path 
     * from start to end, or an empty list if no such path exists.
     * @param start the start of the path
     * @param end the end of the path
     * @return a list with the times in minutes between two nodes along the 
     * shortest path from start to end, or an empty list if no such path exists
     */
    public List<Double> findTimesOnShortestPath(String start, String end);

    /**
     * Return the times in minutes between each two nodes along the provided
     * path.
     * @param path ordered list of city names
     * @return edge times for each segment in path
     */
    public List<Double> findTimesOnPath(List<String> path);

    /**
     * Return the estimated ticket prices in euros between each two nodes on
     * the shortest path from start to end, or an empty list if no such path
     * exists.
     * @param start the start of the path
     * @param end the end of the path
     * @return a list with the estimated prices in euros between two nodes
     *         along the shortest path from start to end
     */
    public List<Double> findPricesOnShortestPath(String start, String end);

    /**
     * Return the estimated ticket prices in euros between each two nodes along
     * the provided path.
     * @param path ordered list of city names
     * @return edge prices for each segment in path
     */
    public List<Double> findPricesOnPath(List<String> path);

    /**
     * Returns the location that can be reached from all of the specified start 
     * locations in the shortest time: minimizing the sum of the times from 
     * each start location.
     * @param starts the list of locations to minimize times from
     * @return the location that can be reached in the shortest total time 
     *         from all of the specified start locations
     * @throws NoSuchElementException if there is no location that can be 
     *         reached from all start locations, or if any start locations does
     *         not exist within the graph
     */
    public String getClosestLocationFromAll(List<String> starts) throws NoSuchElementException;

    /**
     * Returns the location that best satisfies the provided optimization mode
     * for all start locations. Supported modes are time, price, transfers, and
     * fairness. Fairness minimizes the difference between the longest and
     * shortest travel times, then breaks ties by total travel time.
     * @param starts the list of locations to optimize from
     * @param optimizationMode the optimization mode
     * @return the best shared destination
     * @throws NoSuchElementException if no shared destination can be reached
     */
    public String getClosestLocationFromAll(List<String> starts, String optimizationMode)
            throws NoSuchElementException;
}
