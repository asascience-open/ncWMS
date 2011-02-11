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
    double nominal_minimum_resolution;
    double expansion_factor;
    int approx_queries = 0, approx_results = 0, approx_iterations = 0;
    CurvilinearGrid curvGrid;
    private double square_root_2 = Math.sqrt(2.0);

    public KDTree(CurvilinearGrid curvGrid) {
        super();
        this.curvGrid = curvGrid;
        num_elements = curvGrid.size();
        source_data = new Point[num_elements];

        expansion_factor = 3.5f;
    }

    public void setQueryParameters(double expansionFactor, double nominalMinimumResolution) {
        expansion_factor = expansionFactor;
        nominal_minimum_resolution = nominalMinimumResolution;
    }

    public void buildTree() {
        // Load data from files into source_data, and keep track of min/max
        double min_lat = Float.POSITIVE_INFINITY, min_lon = Float.NEGATIVE_INFINITY;
        double max_lat = Float.NEGATIVE_INFINITY, max_lon = Float.POSITIVE_INFINITY;
        int source_counter = -1, destination_counter = 0;
        for (CurvilinearGrid.Cell cell : this.curvGrid.getCells()) {
            source_counter++;
            double new_lat = cell.getCentre().getLatitude();
            double new_lon = cell.getCentre().getLongitude();
            if (Double.isNaN(new_lat) || Double.isNaN(new_lon)) {
                continue;
            }
            min_lat = Math.min(min_lat, new_lat);
            max_lat = Math.max(max_lat, new_lat);
            min_lon = Math.min(min_lon, new_lon);
            max_lon = Math.max(max_lon, new_lon);
            source_data[destination_counter] = new Point(new_lat, new_lon, source_counter);
            destination_counter++;
        }
        num_elements = destination_counter; // Hack! If NaNs were filtered, take account of this

        // Compute the nominal resolution
        nominal_minimum_resolution = 0.25f;// Math.sqrt(Math.min(max_lon - min_lon, max_lat - min_lat)) / 20.0f;

        // Perform an initial sort of the source data by longitude (likely to be bigger for world data)
        Comparator<Point> longitude_comp = new PointComparator(false);
        Arrays.sort(source_data, 0, num_elements, longitude_comp);

        // Calculate the number of elements needed in the tree
        int num_leaf_elements = (int) Math.pow(2.0, Math.ceil(Math.log(num_elements)
                / Math.log(2.0)));
        int num_tree_elements = 2 * num_leaf_elements - 1;
        //System.out.println(num_elements + " elements, " + num_leaf_elements
        //        + " leaf, " + num_tree_elements + " total");
        // Create the uninitialised tree with this number of elements
        tree = new TreeNode[num_tree_elements];

        // Recursively build this into a tree
        recursiveBuildTree(0, num_elements - 1, 0, false);

        // Clear the source data array - no longer needed
        source_data = null;
    }

    public void verifyChildren(int current_index) {
        // Verify the correctness of the tree (call with current_index = 0)

        // Reached a leaf node, no more checks can be made
        if (tree[current_index] instanceof Point) return;

        int left_child_index = 2 * current_index + 1;
        int right_child_index = 2 * current_index + 2;

        NonTerminalTreeNode myself = (NonTerminalTreeNode) tree[current_index];

        // If child nodes are leaf nodes, check that the fall on the correct
        // side of the current discriminator
        // If the child nodes are non terminal tree nodes, then they can
        // only be checked if the discriminators are of the same type

        if (tree[left_child_index] instanceof Point) {
            Point left_child = (Point) tree[left_child_index];
            if (myself.is_latitude) {
                if (left_child.getLatitude() > myself.discriminator) {
                    System.out.println("Error! Left child latitude greater than self");
                }
            } else {
                if (left_child.getLongitude() > myself.discriminator) {
                    System.out.println("Error! Left child longitude greater than self");
                }
            }
        } else {
            NonTerminalTreeNode left_child = (NonTerminalTreeNode) tree[left_child_index];
            if (!(myself.is_latitude ^ left_child.is_latitude)) {
                if (left_child.discriminator > myself.discriminator) {
                    System.out.println("Error! Compatible left child discriminator greater than self");
                }
            }
        }

        if (tree[right_child_index] instanceof Point) {
            Point right_child = (Point) tree[right_child_index];
            if (myself.is_latitude) {
                if (right_child.getLatitude() < myself.discriminator) {
                    System.out.println("Error! Right child latitude lesser than self");
                }
            } else {
                if (right_child.getLongitude() < myself.discriminator) {
                    System.out.println("Error! Right child longitude lesser than self");
                }
            }
        } else {
            NonTerminalTreeNode right_child = (NonTerminalTreeNode) tree[right_child_index];
            if (!(myself.is_latitude ^ right_child.is_latitude)) {
                if (right_child.discriminator < myself.discriminator) {
                    System.out.println("Error! Compatible right child discriminator lesser than self");
                }
            }
        }

        // Recurse
        verifyChildren(left_child_index);
        verifyChildren((right_child_index));
    }

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
        double lat_min = Float.POSITIVE_INFINITY;
        double lat_max = Float.NEGATIVE_INFINITY;
        double lon_min = Float.POSITIVE_INFINITY;
        double lon_max = Float.NEGATIVE_INFINITY;
        for (int current_index = source_index_first; current_index <= source_index_last; current_index++) {
            lat_min = Math.min(lat_min, source_data[current_index].getLatitude());
            lat_max = Math.max(lat_max, source_data[current_index].getLatitude());
            lon_min = Math.min(lon_min, source_data[current_index].getLongitude());
            lon_max = Math.max(lon_max, source_data[current_index].getLongitude());
        }
        boolean discriminate_on_latitude = (Math.abs(lat_max - lat_min) >= Math.abs(lon_max - lon_min));

        // Determine if we need to sort - if discriminate_on_latitude and
        // sorted_by_latitude have different values
        if (discriminate_on_latitude ^ sorted_by_latitude) {
            // Sort by the latitude or longitude as appropriate
            Comparator<Point> comp = new PointComparator(discriminate_on_latitude);
            Arrays.sort(source_data, source_index_first, source_index_last+1,
                    comp);
        }

        // Work out the median, and the indices of the values surrounding the
        // median
        int end_left, start_right;
        double discriminator;
        if (((source_index_last - source_index_first) % 2) != 0) {
            // even number of elements
            end_left = source_index_first
                    + ((source_index_last - source_index_first - 1) / 2);
            start_right = end_left + 1;
            if (discriminate_on_latitude) {
                discriminator = (source_data[end_left].getLatitude() + source_data[start_right].getLatitude()) / 2.0;
            } else {
                discriminator = (source_data[end_left].getLongitude() + source_data[start_right].getLongitude()) / 2.0;
            }
        } else {
            // odd number of elements
            end_left = ((source_index_last - source_index_first) / 2)
                    + source_index_first;
            start_right = end_left + 1;
            if (discriminate_on_latitude) {
                discriminator = source_data[end_left].getLatitude();
            } else {
                discriminator = source_data[end_left].getLongitude();
            }
        }

        // Store this information back into the tree to create the discriminator
        // node
        tree[tree_index_current] = new NonTerminalTreeNode(discriminator,
                discriminate_on_latitude);

        // Recurse
        recursiveBuildTree(source_index_first, end_left,
                2 * tree_index_current + 1, discriminate_on_latitude);
        recursiveBuildTree(start_right, source_index_last,
                2 * tree_index_current + 2, discriminate_on_latitude);
    }

    public Point limitedNearestNeighbour(double latitude, double longitude, double max_distance) {
        Point nearest_neighbour = null;
        double best_distance = Float.POSITIVE_INFINITY;
        for (Point candidate : approxNearestNeighbour(latitude, longitude, max_distance)) {
            double current_distance_latitude = (latitude - candidate.getLatitude());
            double current_distance_longitude = (longitude - candidate.getLongitude());
            double current_distance = current_distance_latitude * current_distance_latitude + current_distance_longitude * current_distance_longitude;
            if (current_distance < best_distance) {
                nearest_neighbour = candidate;
                best_distance = current_distance;
            }
        }
        // Return best result
        return nearest_neighbour;
    }

    public List<Point> approxNearestNeighbour(double latitude, double longitude, double max_distance) {
        approx_queries++;
        double current_distance = nominal_minimum_resolution;
        ArrayList<Point> results;
        boolean break_next = false;
        while (current_distance <= max_distance) {
            approx_iterations++;
            results = rangeQuery(latitude - current_distance, latitude + current_distance, longitude - current_distance, longitude + current_distance);
            if (results.size() > 0) {
                // Need to do one more check - if the point found is at the corner of the current box,
                // there could be a closer point within that distance, so set the search distance to
                // the distance to the current point
                current_distance *= square_root_2;
                results = rangeQuery(latitude - current_distance, latitude + current_distance, longitude - current_distance, longitude + current_distance);
                approx_results += results.size();
                return results;
            }
            if (break_next) {
                break;
            }
            current_distance *= expansion_factor;
            if (current_distance > max_distance) {
                break_next = true;
                current_distance = max_distance;
            }
        }
        // Reached max distance and no points found - return empty
        return Collections.emptyList();
    }

    public void printApproxQueryStats() {
        System.out.println("Computed nominal resolution " + nominal_minimum_resolution);
        if (approx_queries > 0) {
            System.out.println("Results per query " + (double) (approx_results / approx_queries));
            System.out.println("Iterations per query " + (double) (approx_iterations / approx_queries));
        } else {
            System.out.println("No approximate queries made");
        }
    }

    public void resetApproxQueryStats() {
        approx_iterations = 0;
        approx_results = 0;
        approx_queries = 0;
    }

    private final static double squaredDistance(Point p, double latitude, double longitude) {
        return Math.pow(p.getLatitude() - latitude, 2.0)
                + Math.pow(p.getLongitude() - longitude, 2.0);
    }

    public Point nearestNeighbour(double latitude, double longitude) {
        return nearestNeighbourRecurse(latitude, longitude, 0);
    }

    private final Point nearestNeighbourRecurse(double latitude, double longitude, int current_index) {
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
                best = nearestNeighbourRecurse(latitude, longitude, 2 * current_index + 1);
            } else {
                best = nearestNeighbourRecurse(latitude, longitude, 2 * current_index + 2);
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

    public ArrayList<Point> rangeQuery(double min_lat, double max_lat, double min_lon, double max_lon) {
        ArrayList<Point> results = new ArrayList<Point>();
        rangeQueryRecurse(min_lat, max_lat, min_lon, max_lon, results, 0);
        return results;
    }

    private final void rangeQueryRecurse(double min_lat, double max_lat, double min_lon, double max_lon, ArrayList<Point> results, int tree_current_index) {
        if (tree[tree_current_index] instanceof Point) {
            // Terminal - return this point if it's within bounds
            Point terminal_point = (Point) tree[tree_current_index];
            if (terminal_point.getLatitude() >= min_lat && terminal_point.getLatitude() <= max_lat
                    && terminal_point.getLongitude() >= min_lon && terminal_point.getLongitude() <= max_lon) {
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
}
