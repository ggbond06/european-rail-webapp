import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

public class Frontend implements FrontendInterface {

  private BackendInterface backend;
  
  public Frontend(BackendInterface backend) {
    this.backend = backend;
  }
  /**
     * Returns an HTML fragment that can be embedded within the body of a
     * larger HTML page. This HTML output should include:
     *     - a text input field with the id="start", for the start location
     *     - a text input field with the id="end", for the end location
     *     - a button labelled "Find Shortest Path" to request this computation
     * Ensure these text fields are clearly labelled, so the user can understand
     * how to use them.
     * @return an HTML string containing input controls the user can use to 
     *         request a shortest path computation
     */
  @Override
  public String generateShortestPathPromptHTML() {
    String html = "<label for=\"start\">Start:</label>" +
                  "<input id=\"start\" type=\"text\">" +
                  "<label for=\"end\">End:</label>" +
                  "<input id=\"end\" type=\"text\">" +
                  "<button>Find Shortest Path</button>";
    
    return html;
  }

  /**
     * Returns an HTML fragment that can be embedded within the body of a
     * larger HTML page.  This HTML output should include:
     *     - a paragraph tag for the path's start and end locations
     *     - an ordered list tag for locations along that shortest path
     *     - a paragraph tag that includes the total time along this path
     * Or, if there is no such path, the HTML returned should instead indicate 
     * the kind of problem encountered.
     * @param start is the starting location to find a shortest path from
     * @param end is the end location that this shortest path should end at
     * @return an HTML string for the shortest path between these two locations
     */
  @Override
  public String generateShortestPathResponseHTML(String start, String end) {

    if (start == null || end == null || start.trim().equals("") || end.trim().equals("")) {
      return "<p>Error: start and end locations must both be provided.</p>";
    }

    try {
      List<String> locations = backend.findLocationsOnShortestPath(start.trim(), end.trim());
      List<Double> times = backend.findTimesOnShortestPath(start.trim(), end.trim());

      if (locations == null || locations.isEmpty()) {
        return "<p>Error: No path found from " + start + " to " + end + ".</p>";
      }

      String html = "<p>Shortest path from " + start + " to " + end + ":</p>\n";
      html += "<ol>\n";
      for (String loc : locations) {
        html += "  <li>" + loc + "</li>\n";
      }
      html += "</ol>\n";

      double totalTime = 0.0;
      for (Double t : times) {
        totalTime += t;
      }

      html += "<p>Total time: " + totalTime + " minutes</p>\n";
      return html;
    } catch (NoSuchElementException | IllegalArgumentException e) {
      return "<p>Error: no path could be found from " + start + " to " + end + ".</p>";
    }
  }

  /**
     * Returns an HTML fragment that can be embedded within the body of a larger
     * HTML page. This HTML output should include:
     *     - a text input field with the id="from", for the start locations
     *     - a button labelled "Closest From All" to submit this request
     * Ensure this text field is clearly labelled, so the user can understand
     * they should enter a comma separated list of as many locations as they 
     * would like into this field.
     * @return an HTML string containing input controls the user can use to
     *         request a calculation for the ten closest locations
     */
  @Override
  public String generateClosestLocationsFromAllPromptHTML() {
    String html = "<label for=\"from\">From (comma separated):</label>" +
                  "<input id=\"from\" type=\"text\">" +
                  "<button>Closest From All</button>";

    return html;
  }

  /**
     * Returns an HTML fragment that can be embedded within the body of a larger
     * HTML page. This HTML output should include:
     *     - an unordered list tag for the start Locations
     *     - a paragraph tag describing the location that is closest to all the
     *         start locations
     *     - a paragraph that displays the total time to all start locations
     * Or, if no such locations can be found, the HTML returned should 
     * instead indicate the kind of problem encountered.
     * @param starts is the comma separated list of starting locations to search
     *         search from
     * @return an HTML string for the closest locations from the specified start
     */ 
  @Override
  public String generateClosestLocationsFromAllResponseHTML(String starts) {
    if (starts == null || starts.trim().equals("")) {
      return "<p>Error: at least one start location must be provided.</p>";
    }

    String[] parts = starts.split(",");
    List<String> startLocations = new ArrayList<>();
    for (String part : parts) {
      String trimmed = part.trim();
      if (!trimmed.equals("")) {
        startLocations.add(trimmed);
      }
    }

    if (startLocations.isEmpty()) {
      return "<p>Error: at least one start location must be provided.</p>";
    }

    try {
      String closest = backend.getClosestLocationFromAll(startLocations);
      String html = "<p>Start locations:</p>\n";
      html += "<ul>\n";
      for (String loc : startLocations) {
        html += "  <li>" + loc + "</li>\n";
      }
      html += "</ul>\n";
      html += "<p>Closest location from all: " + closest + "</p>\n";

      double totalTime = 0.0;
      for (String loc : startLocations) {
        List<Double> times = backend.findTimesOnShortestPath(loc, closest);
        for (Double t : times) {
          totalTime += t;
        }
      }

      html += "<p>Total time from all start locations to " + closest + ": " + totalTime + " minutes</p>\n";
      return html;

    } catch (Exception e) {
      return "<p>Error: no location could be reached from all provided start locations.</p>";
    }
  }
}
