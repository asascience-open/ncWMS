package uk.ac.rdg.resc.edal.cdm.kdtree;

public class Point extends TreeNode {

    private double latitude, longitude;
    private int index;

    public Point(double latitude, double longitude, int index) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.index = index;
    }
    
    @Override
    public String toString() {
        return "(" + latitude + "," + longitude + "@" + index + ")";
    }

    public double getLongitude() {
        return this.longitude;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public int getIndex() {
        return this.index;
    }
}
