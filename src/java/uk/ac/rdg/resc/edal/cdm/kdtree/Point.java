package uk.ac.rdg.resc.edal.cdm.kdtree;

public class Point extends TreeNode {
	public Point(float latitude, float longitude, int index) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.index = index;
	}
	public float latitude, longitude;
	public int index;
	
	public String toString() {
		return "Lat: " + latitude + " Lon: " + longitude + " Index: " + index;
	}
}
