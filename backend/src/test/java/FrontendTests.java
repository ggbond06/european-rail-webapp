import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class FrontendTests {

    /**
     * This test case tests that the shortest path prompt HTML has the required
     * start input field, end input field, and submit button.
     */
    @Test
    public void roleTest1() {
        Frontend frontend = new Frontend(new Backend_Placeholder(new Graph_Placeholder()));

        String html = frontend.generateShortestPathPromptHTML();
        assertTrue(html.contains("id=\"start\""));
        assertTrue(html.contains("id=\"end\""));
        assertTrue(html.contains("Find Shortest Path"));
    }

    /**
     * This test case tests that the shortest path response HTML correctly displays
     * the locations in the correct order and the total time, for the path returned by the placeholder
     */
    @Test
    public void roleTest2() {
        Frontend frontend = new Frontend(new Backend_Placeholder(new Graph_Placeholder()));

        String html = frontend.generateShortestPathResponseHTML("Union South", "Weeks Hall for Geological Sciences");

        assertTrue(html.contains("Shortest path from Union South to Weeks Hall for Geological Sciences:"));
        assertTrue(html.contains("<ol>"));
        assertTrue(html.contains("<li>Union South</li>"));
        assertTrue(html.contains("<li>Computer Sciences and Statistics</li>"));
        assertTrue(html.contains("<li>Weeks Hall for Geological Sciences</li>"));
        assertTrue(html.contains("</ol>"));
        assertTrue(html.contains("Total time: 6.0 minutes"));
    }

    /**
     * This test case tests that the closest from all prompt HTML has the required
     * input fields and buttons, and the response HTML correctly displays the locations
     * in the correct order and the total time, for the path returned by the placeholder
     */
    @Test
    public void roleTest3() {
        Frontend frontend = new Frontend(new Backend_Placeholder(new Graph_Placeholder()));

        String prompt = frontend.generateClosestLocationsFromAllPromptHTML();
        assertTrue(prompt.contains("id=\"from\""));
        assertTrue(prompt.contains("Closest From All"));

        String response = frontend.generateClosestLocationsFromAllResponseHTML("Union South, Computer Sciences and Statistics");
        assertTrue(response.contains("<ul>"));
        assertTrue(response.contains("<li>Union South</li>"));
        assertTrue(response.contains("<li>Computer Sciences and Statistics</li>"));
        assertTrue(response.contains("Weeks Hall for Geological Sciences"));
        assertTrue(response.contains("Total time"));
    }

    /**
     * Helper method for integration tests. This creates the real graph, backend,
     * and frontend objects, then loads the europeanRail.dot data file.
     */
    private FrontendInterface createIntegrationFrontend() throws IOException {
        GraphADT<String, Double> graph = new DijkstraGraph<>();
        BackendInterface backend = new Backend(graph);
        backend.loadGraphData("data/europeanRail.dot");
        return new Frontend(backend);
    }

    /**
     * Integration test that checks whether the frontend can use the real backend
     * to generate a shortest path response from Lyon to Paris.
     */
    @Test
    public void shortestPathIntegrationTest() throws IOException {
        // Create frontend connected to real backend and graph.
        FrontendInterface frontend = createIntegrationFrontend();

        // generate a shortest path response.
        String html = frontend.generateShortestPathResponseHTML("Lyon", "Paris");

        // Check that the response includes the expected path and total time.
        assertTrue(html.contains("Lyon"));
        assertTrue(html.contains("Paris"));
        assertTrue(html.contains("112"));
    }

    /**
     * Integration test that checks whether the frontend and backend together find
     * an indirect shortest path from Berlin to Paris.
     */
    @Test
    public void indirectShortestPathIntegrationTest() throws IOException {
        // Create frontend connected to real integrated backend.
        FrontendInterface frontend = createIntegrationFrontend();

        // Berlin to Paris should use the real graph and Dijkstra implementation.
        String html = frontend.generateShortestPathResponseHTML("Berlin", "Paris");

        // The result should mention both endpoints and a total time.
        assertTrue(html.contains("Berlin"));
        assertTrue(html.contains("Paris"));
        assertTrue(html.toLowerCase().contains("total"));
        assertTrue(html.contains("361"));
    }

    /**
     * Integration test that checks whether the closest-from-all feature works
     * with the real backend, graph, and hashtable implementations.
     */
    @Test
    public void closestFromAllIntegrationTest() throws IOException {
        // Create frontend using real integrated project components.
        FrontendInterface frontend = createIntegrationFrontend();

        // Ask for the closest location from three real graph locations.
        String html = frontend.generateClosestLocationsFromAllResponseHTML("Lyon, Paris, Berlin");

        // Based on the integrated graph data, Paris should be the result.
        assertTrue(html.contains("Lyon"));
        assertTrue(html.contains("Paris"));
        assertTrue(html.contains("Berlin"));
        assertTrue(html.contains("Closest"));
        assertTrue(html.contains("473"));
    }

    /**
     * Integration test that checks whether the frontend and backend handle an
     * invalid shortest path request using the real integrated classes.
     */
    @Test
    public void invalidShortestPathIntegrationTest() throws IOException {
        // Create frontend connected to the real backend, graph, and hashtable.
        FrontendInterface frontend = createIntegrationFrontend();

        // Ask for a path from a location that does not exist in the graph.
        String html = frontend.generateShortestPathResponseHTML("NotARealCity", "Paris");

        // The response should not crash and should show some kind of error/no-path message.
        assertTrue(html.toLowerCase().contains("error"));
    }
}
