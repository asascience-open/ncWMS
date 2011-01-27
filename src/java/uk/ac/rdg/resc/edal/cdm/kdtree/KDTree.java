package uk.ac.rdg.resc.edal.cdm.kdtree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import uk.ac.rdg.resc.edal.cdm.CurvilinearGrid;

public class KDTree {

    int num_elements;
    Point[] source_data = null;
    TreeNode[] tree = null;
    float nominal_minimum_resolution;
    float expansion_factor;
    int approx_queries = 0, approx_results = 0, approx_iterations = 0;
    CurvilinearGrid curvGrid;

    public KDTree(CurvilinearGrid curvGrid) {
        super();
        this.curvGrid = curvGrid;
        num_elements = curvGrid.size();
        source_data = new Point[num_elements];

        expansion_factor = 3.5f;//2.0f;
    }

    public void buildTree()  {
        // Load data from files into source_data, and keep track of min/max
        float min_lat = Float.MAX_VALUE, min_lon = Float.MAX_VALUE;
        float max_lat = Float.MIN_VALUE, max_lon = Float.MIN_VALUE;
        int counter = 0;
        for (CurvilinearGrid.Cell cell : this.curvGrid.getCells())
        {
            float new_lat = (float)cell.getCentre().getLatitude();
            float new_lon = (float)cell.getCentre().getLongitude();
            min_lat = Math.min(min_lat, new_lat);
            max_lat = Math.max(max_lat, new_lat);
            min_lon = Math.min(min_lon, new_lon);
            max_lon = Math.max(max_lon, new_lon);
            source_data[counter] = new Point(new_lat, new_lon, counter);
            counter++;
        }

        // Compute the nominal resolution
        nominal_minimum_resolution = 0.5f;//(float) Math.sqrt(Math.min(max_lon - min_lon, max_lat - min_lat)) / 20.0f;

        // Perform an initial sort of the source data by latitude
        Comparator<Point> latitude_comp = new PointComparator(true);
        Arrays.sort(source_data, latitude_comp);

        // Calculate the number of elements needed in the tree
        int num_leaf_elements = (int) Math.pow(2.0, Math.ceil(Math.log(num_elements)
                / Math.log(2.0)));
        int num_tree_elements = 2 * num_leaf_elements - 1;
        System.out.println(num_elements + " elements, " + num_leaf_elements
                + " leaf, " + num_tree_elements + " total");
        // Create the uninitialised tree with this number of elements
        tree = new TreeNode[num_tree_elements];

        // Recursively build this into a tree
        recursiveBuildTree(0, num_elements - 1, 0, true);

        // Clear the source data - no longer needed
        source_data = null;

    }

    ;

    private void recursiveBuildTree(int source_index_first, int source_index_last,
            int tree_index_current, boolean sorted_by_latitude) {

        if (source_index_first == source_index_last) {
            // If the recursion has bottomed out and there is only one point of source data left,
            // store it in the tree at the current index
            tree[tree_index_current] = source_data[source_index_first];
            return;
        }

        // Determine whether latitude or longitude has the biggest range across
        // our current set of source data
        // A discriminator node will then be inserted that splits this data into
        // 2 (high and low)

        // Determine whether we should be sorting by latitude or longitude
        float lat_min = Float.MAX_VALUE;
        float lat_max = Float.MIN_VALUE;
        float lon_min = Float.MAX_VALUE;
        float lon_max = Float.MIN_VALUE;
        for (int current_index = source_index_first; current_index <= source_index_last; current_index++) {
            lat_min = Math.min(lat_min, source_data[current_index].latitude);
            lat_max = Math.max(lat_max, source_data[current_index].latitude);
            lon_min = Math.min(lon_min, source_data[current_index].longitude);
            lon_max = Math.max(lon_max, source_data[current_index].longitude);
        }
        boolean sort_by_latitude = Math.abs(lat_max - lat_min) >= Math.abs(lon_max - lon_min);

        // Determine if we need to sort - if sort_by_latitude and
        // sorted_by_latitude have different values
        if (sort_by_latitude ^ sorted_by_latitude) {
            // Sort by the latitude or longitude as appropriate
            Comparator<Point> comp = new PointComparator(sort_by_latitude);
            Arrays.sort(source_data, source_index_first, source_index_last,
                    comp);
        }

        // Work out the median, and the indices of the values surrounding the
        // median
        int end_left, start_right;
        float discriminator;
        if (((source_index_last - source_index_first) % 2) != 0) {
            // even number of elements
            end_left = source_index_first
                    + ((source_index_last - source_index_first - 1) / 2);
            start_right = end_left + 1;
            if (sort_by_latitude) {
                discriminator = (float) ((source_data[end_left].latitude + source_data[start_right].latitude) / 2.0);
            } else {
                discriminator = (float) ((source_data[end_left].longitude + source_data[start_right].longitude) / 2.0);
            }
        } else {
            // odd number of elements
            end_left = ((source_index_last - source_index_first) / 2)
                    + source_index_first;
            start_right = end_left + 1;
            if (sort_by_latitude) {
                discriminator = source_data[end_left].latitude;
            } else {
                discriminator = source_data[end_left].longitude;
            }
        }

        // Store this information back into the tree to create the discriminator
        // node
        tree[tree_index_current] = new NonTerminalTreeNode(discriminator,
                sort_by_latitude);

        // Recurse
        recursiveBuildTree(source_index_first, end_left,
                (2 * (tree_index_current + 1)) - 1, sort_by_latitude);
        recursiveBuildTree(start_right, source_index_last,
                2 * (tree_index_current + 1), sort_by_latitude);
    }

    /*public Point nearestNeighbourIndexed(float latitude, float longitude, int tolerance) {
    int density_index_lat = (int) ((latitude - min_lat) / density_index_vres);
    int density_index_lon = (int) ((longitude - min_lon) / density_index_hres);
    float density = density_index[density_index_lat * density_index_size + density_index_lon];
    ArrayList<Point> result = null;
    while (true) {
    result = rangeQuery(latitude - density, latitude + density, longitude - density, longitude + density);
    if (result.size() == 0) {
    density *= 2.0;
    continue;
    }
    if (result.size() > tolerance) {
    density /=
    }
    }

    }*/
    public List<Point> approxNearestNeighbour(float latitude, float longitude, float max_distance) {
        approx_queries++;
        float current_distance = nominal_minimum_resolution;
        ArrayList<Point> results;
        while (current_distance <= max_distance) {
            approx_iterations++;
            results = rangeQuery(latitude - current_distance, latitude + current_distance, longitude - current_distance, longitude + current_distance);
            if (results.size() > 0) {
                approx_results += results.size();
                return results;
            }
            current_distance *= expansion_factor;
        }
        return Collections.emptyList();
    }

    public void printApproxQueryStats() {
        System.out.println("Computed nominal resolution " + nominal_minimum_resolution);
        System.out.println("Results per query " + (float) (approx_results / approx_queries));
        System.out.println("Iterations per query " + (float) (approx_iterations / approx_queries));
    }

    public void resetApproxQueryStats() {
        approx_iterations = 0;
        approx_results = 0;
        approx_queries = 0;
    }

    public Point nearestNeighbour(float latitude, float longitude) {
        return nearestNeighbourRecurse(latitude, longitude, 0);
    }

    private final static double squaredDistance(Point p, float latitude, float longitude) {
        return Math.pow(p.latitude - latitude, 2.0)
                + Math.pow(p.longitude - longitude, 2.0);
    }

    public ArrayList<Point> rangeQuery(float min_lat, float max_lat, float min_lon, float max_lon) {
        ArrayList<Point> results = new ArrayList<Point>();
        rangeQueryRecurse(min_lat, max_lat, min_lon, max_lon, results, 0);
        return results;
    }

    private final void rangeQueryRecurse(float min_lat, float max_lat, float min_lon, float max_lon, ArrayList<Point> results, int tree_current_index) {
        if (tree[tree_current_index] instanceof Point) {
            // Terminal - return this point if it's within bounds
            Point terminal_point = (Point) tree[tree_current_index];
            if (terminal_point.latitude >= min_lat && terminal_point.latitude <= max_lat
                    && terminal_point.longitude >= min_lon && terminal_point.latitude <= max_lon) {
                results.add(terminal_point);
            }
            return;
        } else {
            // 3 cases - the discriminator in the non-terminal node can be less than search range, within it, or greater than it
            // Less than: Search right of this node
            // Within: Search left and right of this node
            // Greater than: Search left of this node
            boolean search_left, search_right;
            NonTerminalTreeNode node = (NonTerminalTreeNode) tree[tree_current_index];
            if (node.is_latitude) {
                search_left = (node.discriminator >= min_lat);
                search_right = (node.discriminator <= max_lat);
            } else {
                search_left = (node.discriminator >= min_lon);
                search_right = (node.discriminator <= max_lon);
            }

            if (search_left) {
                rangeQueryRecurse(min_lat, max_lat, min_lon, max_lon, results, (2 * (tree_current_index + 1)) - 1);
            }
            if (search_right) {
                rangeQueryRecurse(min_lat, max_lat, min_lon, max_lon, results, (2 * (tree_current_index + 1)));
            }
        }
    }

    private final Point nearestNeighbourRecurse(float latitude, float longitude, int current_index) {
        if (tree[current_index] instanceof Point) {
            // Terminal node reached - return it
            return (Point) tree[current_index];
        } else {
            // Non-terminal
            NonTerminalTreeNode node = (NonTerminalTreeNode) tree[current_index];
            double pivot_target_distance;
            if (node.is_latitude) {
                pivot_target_distance = node.discriminator - latitude;
            } else {
                pivot_target_distance = node.discriminator - longitude;
            }

            // Search the 'near' branch
            Point best;
            if (pivot_target_distance > 0) {
                best = nearestNeighbourRecurse(latitude, longitude, (2 * (current_index + 1)) - 1);
            } else {
                best = nearestNeighbourRecurse(latitude, longitude, 2 * (current_index + 1));
            }

            // Only search the 'away' branch if the squared distance between the current best and the target is greater
            // Than the squared distance between the target and the branch pivot
            if (squaredDistance(best, latitude, longitude) > Math.pow(pivot_target_distance, 2.0)) {
                Point potential_best;
                // Search the 'away' branch
                if (pivot_target_distance > 0) {
                    potential_best = nearestNeighbourRecurse(latitude, longitude, 2 * (current_index + 1));
                } else {
                    potential_best = nearestNeighbourRecurse(latitude, longitude, (2 * (current_index + 1)) - 1);
                }
                if (squaredDistance(potential_best, latitude, longitude) < squaredDistance(best, latitude, longitude)) {
                    return potential_best;
                }
            }

            return best;
        }
    }
}
