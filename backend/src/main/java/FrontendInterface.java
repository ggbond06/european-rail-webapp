/**
 * This is the interface that a frontend developer will implement.  It will 
 * enable users to access the functionality exposed by the BackendInterface.
 *
 * Notice the organization of the methods below into pairs that generate HTML
 * strings that 1) prompt the user for input to perform a specific computation,
 * and then 2) make use of that input, with the help of the backend, to compute
 * and then display the requested results.
 *
 * A webapp will be developed later in this project to integrate these HTML
 * snippets into a webpage that is returned custom build in response to each
 * user request.
 */
public interface FrontendInterface {

    /**
     * Implementing classes should support the constructor below.
     * @param backend is used for shortest path computations
     */
    // public Frontend(BackendInterface backend);
    
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
    public String generateShortestPathPromptHTML();
    
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
    public String generateShortestPathResponseHTML(String start, String end);

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
    public String generateClosestLocationsFromAllPromptHTML();
    
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
    public String generateClosestLocationsFromAllResponseHTML(String starts);
}
