package uk.ac.rdg.resc.edal.cdm.kdtree;

import java.util.Comparator;

public class PointComparator implements Comparator<Point> {

    Boolean sort_by_latitude;

    public PointComparator(Boolean sortByLatitude) {
        sort_by_latitude = sortByLatitude;
    }

    @Override
    public int compare(Point o1, Point o2) {
        double difference;
        if (sort_by_latitude) {
            difference = (o1.getLatitude() - o2.getLatitude());
        } else {
            difference = (o1.getLongitude() - o2.getLongitude());
        }
        if (difference < 0) {
            return -1;
        } else if (difference > 0) {
            return 1;
        } else {
            return 0;
        }

    }
}
