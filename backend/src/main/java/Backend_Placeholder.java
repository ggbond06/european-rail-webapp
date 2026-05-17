import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * This is a placeholder for the fully working Backend that will be developed
 * by one of your teammates this week and then integrated with your role code
 * in a future week.  It is designed to help develop and test the functionality
 * of your own Frontend role code this week.  Note the limitations described
 * below.
 */
public class Backend_Placeholder implements BackendInterface {

    // Presumably this placeholder is using a placeholder graph that is itself
    // not fully functional.
    GraphADT<String,Double> graph;
    List<String> nodes = new ArrayList<>(List.of("Union South",
        "Computer Sciences and Statistics", "Weeks Hall for Geological Sciences"));
    public Backend_Placeholder(GraphADT<String,Double> graph) {
        this.graph = graph;
    }

    // this method adds a single extra location to the graph when called
    public void loadGraphData(String filename) throws IOException {
        graph.insertNode("Alpha_Centauri_A");
    }
    
    public List<String> getListOfAll() {
        return nodes;
    }

    public List<String> findLocationsOnShortestPath(String start, String end) {
        return graph.shortestPathData(start,end);
    }

    public List<String> findLocationsOnPath(String start, String end, String optimizationMode) {
        return findLocationsOnShortestPath(start,end);
    }

    // returns list of increasing values
    public List<Double> findTimesOnShortestPath(String start, String end) {
        List<String> locations = graph.shortestPathData(start,end);
        return findTimesOnPath(locations);
    }

    public List<Double> findTimesOnPath(List<String> locations) {
        List<Double> times = new ArrayList<>();
        for(int i=0;i<locations.size()-1;i++) times.add(i+1.0);
        return times;
    }

    public List<Double> findPricesOnShortestPath(String start, String end) {
        List<String> locations = graph.shortestPathData(start,end);
        return findPricesOnPath(locations);
    }

    public List<Double> findPricesOnPath(List<String> locations) {
        List<Double> prices = new ArrayList<>();
        for(int i=0;i<locations.size()-1;i++) prices.add(i+2.0);
        return prices;
    }

    // always returns last location
    public String getClosestLocationFromAll(List<String> starts) throws NoSuchElementException {
        List<String> all = nodes;
        return all.get(all.size()-1);
    }

    public String getClosestLocationFromAll(List<String> starts, String optimizationMode)
            throws NoSuchElementException {
        return getClosestLocationFromAll(starts);
    }
}
