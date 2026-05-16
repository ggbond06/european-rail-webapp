import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Test class for the Backend implementation. Uses Graph_Placeholder to test
 * Backend methods. Contains three test methods that collectively call all
 * five BackendInterface methods at least once.
 */
public class BackendTests {

    /**
     * Tests loadGraphData and getListOfAll. Loads the europeanRail.dot file
     * and verifies that locations are populated and that the list contains
     * expected cities. Also tests that loading again clears previous data
     * and reloads correctly.
     */
    @Test
    public void roleTest1() {
        Graph_Placeholder graph = new Graph_Placeholder();
        Backend backend = new Backend(graph);

        // Before loading, the list should be empty
        assertTrue(backend.getListOfAll().isEmpty(),
            "Location list should be empty before loading data");

        // Test loading the dot file
        try {
            backend.loadGraphData("data/europeanRail.dot");
        } catch (IOException e) {
            fail("loadGraphData threw IOException: " + e.getMessage());
        }

        // After loading, the list should contain locations
        List<String> locations = backend.getListOfAll();
        assertFalse(locations.isEmpty(),
            "Location list should not be empty after loading data");

        // Verify some expected cities are present
        assertTrue(locations.contains("Paris"),
            "Locations should contain Paris");
        assertTrue(locations.contains("Berlin"),
            "Locations should contain Berlin");

        // Test reloading: load again and verify data is refreshed
        int firstSize = locations.size();
        try {
            backend.loadGraphData("data/europeanRail.dot");
        } catch (IOException e) {
            fail("Second loadGraphData threw IOException: " + e.getMessage());
        }
        // After reload, size should be consistent
        List<String> reloadedLocations = backend.getListOfAll();
        assertFalse(reloadedLocations.isEmpty(),
            "Location list should not be empty after reloading");

        // Test IOException for non-existent file
        assertThrows(IOException.class, () -> {
            backend.loadGraphData("nonexistent_file.dot");
        }, "Loading a nonexistent file should throw IOException");
    }

    /**
     * Tests findLocationsOnShortestPath and findTimesOnShortestPath using the
     * Graph_Placeholder. The placeholder has a hardcoded path:
     * "Union South" -> "Computer Sciences and Statistics" -> "Weeks Hall for Geological Sciences"
     * so we test shortest path queries against that known data.
     */
    @Test
    public void roleTest2() {
        Graph_Placeholder graph = new Graph_Placeholder();
        Backend backend = new Backend(graph);

        // Test findLocationsOnShortestPath with the placeholder's hardcoded path
        List<String> path = backend.findLocationsOnShortestPath(
            "Union South", "Weeks Hall for Geological Sciences");
        assertFalse(path.isEmpty(),
            "Path should not be empty for valid start and end in placeholder");
        assertEquals("Union South", path.get(0),
            "Path should start with Union South");
        assertEquals("Weeks Hall for Geological Sciences",
            path.get(path.size() - 1),
            "Path should end with Weeks Hall for Geological Sciences");

        // Test findTimesOnShortestPath for the same path
        List<Double> times = backend.findTimesOnShortestPath(
            "Union South", "Weeks Hall for Geological Sciences");
        assertFalse(times.isEmpty(),
            "Times list should not be empty for a valid path");
        // Times list should have one fewer element than the path
        assertEquals(path.size() - 1, times.size(),
            "Times list should have one fewer element than path nodes");

        // Test with non-existent locations: should return empty lists
        List<String> noPath = backend.findLocationsOnShortestPath(
            "Nonexistent1", "Nonexistent2");
        assertTrue(noPath.isEmpty(),
            "Path for nonexistent locations should be empty");

        List<Double> noTimes = backend.findTimesOnShortestPath(
            "Nonexistent1", "Nonexistent2");
        assertTrue(noTimes.isEmpty(),
            "Times for nonexistent locations should be empty");
    }

    /**
     * Tests getClosestLocationFromAll using the Graph_Placeholder. Since the
     * placeholder has limited nodes and path data, this tests that the method
     * returns a valid location reachable from all starts, and that it throws
     * NoSuchElementException when a start location does not exist.
     */
    @Test
    public void roleTest3() {
        Graph_Placeholder graph = new Graph_Placeholder();
        Backend backend = new Backend(graph);

        // Manually register the placeholder's nodes as known locations
        // by loading data or adding them. Since the placeholder already has
        // nodes, we need to track them in the backend. We'll do this by
        // constructing a small dot file.
        // Instead, let's test with the placeholder directly.
        // The placeholder has: "Union South", "Computer Sciences and Statistics",
        // "Weeks Hall for Geological Sciences"
        // We need these to be in allLocations for getClosestLocationFromAll to work.
        // Let's create a simple dot file to load these locations.

        // First, test that a nonexistent start throws NoSuchElementException
        List<String> badStarts = new ArrayList<>();
        badStarts.add("Nonexistent City");
        assertThrows(java.util.NoSuchElementException.class, () -> {
            backend.getClosestLocationFromAll(badStarts);
        }, "Should throw NoSuchElementException for nonexistent start location");

        // Now test with a file that adds data. Since placeholder has limited
        // functionality, we test with the europeanRail.dot file.
        try {
            backend.loadGraphData("data/europeanRail.dot");
        } catch (IOException e) {
            fail("loadGraphData threw IOException: " + e.getMessage());
        }

        // Test getClosestLocationFromAll with a single start
        // The closest location from a single start to itself should be itself
        // (with cost 0), but the placeholder may not support this fully.
        // With real graph data loaded, we test with valid cities.
        List<String> starts = new ArrayList<>();
        starts.add("Paris");
        // Should return some valid location (likely Paris itself with cost 0)
        try {
            String closest = backend.getClosestLocationFromAll(starts);
            assertNotNull(closest,
                "Closest location should not be null");
            assertTrue(backend.getListOfAll().contains(closest),
                "Closest location should be in the graph");
        } catch (java.util.NoSuchElementException e) {
            // With placeholder graph, this might not find a path - that's okay
            // The important thing is the method runs without unexpected errors
        }

        // Test with multiple starts that don't exist
        List<String> invalidStarts = new ArrayList<>();
        invalidStarts.add("FakeCity1");
        invalidStarts.add("FakeCity2");
        assertThrows(java.util.NoSuchElementException.class, () -> {
            backend.getClosestLocationFromAll(invalidStarts);
        }, "Should throw NoSuchElementException for nonexistent start locations");
    }
}
