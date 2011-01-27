package uk.ac.rdg.resc.edal.cdm.kdtree;

import java.util.Comparator;

public class PointComparator implements Comparator<Point>{
	Boolean sort_by_latitude;
	
	
	public PointComparator(Boolean sortByLatitude) {
		super();
		sort_by_latitude = sortByLatitude;
	}


	public int compare(Point o1, Point o2) {
		if (sort_by_latitude) {
			return (int) (o1.latitude - o2.latitude);
		} else {
			return (int) (o1.longitude - o2.longitude);
		}
	}

}
