/*
 * Copyright (c) 2009 The University of Reading
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.metadata.lut;

import com.infomatiq.jsi.IntProcedure;
import com.infomatiq.jsi.Point;
import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import ucar.unidata.geoloc.LatLonPoint;
import uk.ac.rdg.resc.ncwms.metadata.lut.CurvilinearGrid.Cell;

/**
 * Wraps an {@link RTree} implementation, providing convenience methods
 * for inserting and retrieving values.  Coordinates in the RTree can be
 * stored in a specified coordinate reference system.
 */
final class RtreeWrapper
{
    // The latitude range for points in this RTree
    private final double minLat;
    private final double maxLat;
    // The Rtree that will hold the longitude-latitude points
    private final RTree rTree = new RTree();
    // Maps indices in the Rtree to cells in the source data grid
    private final List<Cell> cells = new ArrayList<Cell>();
    // The coordinate reference system used by coordinates in this RTree
    private final CoordinateReferenceSystem crs;
    // Transformation from longitude-latitude to the CRS used by this Rtree
    private final MathTransform lonLatToCrs;
    // Callback object for retrieving nearest-neighbour indices
    // This RtreeWrapper will only be used within a single thread so it's
    // OK to share this callback object
    private final Callback callback = new Callback();
    private float cachedEstimatedFurthestDistance = Float.NaN;
    // The radius of the largest Cell in this RTree, calculated by
    // getBoundingRadius.  Used as the furthestDistance parameter in RTree searches
    private double largestCellRadius = 0.0;

    /**
     * Simple callback object for retrieving the index of a single "hit" from
     * searching an {@link RTree}.
     */
    private static final class Callback implements IntProcedure
    {
        private int nearest;
        public boolean execute(int id)
        {
            this.nearest = id;
            return true;
        }
    }

    /**
     * Creates a new RtreeWrapper, covering a given latitude range and recording
     * coordinates in a particular coordinate reference system.
     * @param minLat The minimum latitude of the centres of grid cells in this
     * RTree.
     * @param maxLat The maximum latitude of the centres of grid cells in this
     * RTree.
     * @param crsCode Code identifying the {@link CoordinateReferenceSystem} to
     * be used by this RTree.  If this is null, WGS84 (longitude, latitude) is
     * used.
     * @throws FactoryException if the given {@code crsCode} could not be
     * interpreted.
     */
    public RtreeWrapper(double minLat, double maxLat, String crsCode)
        throws FactoryException
    {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.rTree.init(new Properties()); // If we don't initialize the RTree it doesn't work
        if (crsCode == null)
        {
            this.crs = null;
            this.lonLatToCrs = null;
        }
        else
        {
            // "true" means "force longitude-first"
            this.crs = CRS.decode(crsCode, true);
            // "true" means "ignore datum shifts"
            this.lonLatToCrs = CRS.findMathTransform(DefaultGeographicCRS.WGS84, crs, true);
        }
    }

    /**
     * Adds a cell from the source grid to this Rtree.  Does nothing if the
     * cell is outside the latitude range.
     * @param cell the cell from the source grid.
     * @throws TransformException if the coordinates of the cell could not
     * be transformed to the CRS of this Rtree
     */
    public void addCell(Cell cell) throws TransformException
    {
        double longitude = cell.getCentrePoint().getLongitude();
        double latitude = cell.getCentrePoint().getLatitude();
        if (latitude < this.minLat || latitude > this.maxLat) return;
        // Find the coordinates in the CRS of this RTree
        double[] coords = this.lonLatToCrs(longitude, latitude);
        // Add this point to the RTree
        Rectangle rect = new Rectangle((float)coords[0], (float)coords[1], (float)coords[0], (float)coords[1]);
        // Add to the Rtree, using the current size of the rTree as the
        // index.
        this.rTree.add(rect, this.rTree.size());
        // Add the cell to the list so we can find it later.  The index
        // of the cell within this list will match the index of the rectangle
        // in the RTree
        this.cells.add(cell);
        // See if this cell has the largest radius
        this.largestCellRadius = Math.max(this.largestCellRadius, this.getBoundingRadius(cell));

        // Record that the furthestDistance parameter must be re-estimated
        this.cachedEstimatedFurthestDistance = Float.NaN;
    }

    /**
     * Calculates the radius of the smallest circle, centred at the cell's centre,
     * that encloses all the circle's corners, in this Rtree's coordinate
     * reference system.
     * @throws TransformException if the coordinates of the cell cannot be
     * converted to the CRS of this RTree
     */
    private double getBoundingRadius(Cell cell) throws TransformException
    {
        double[] centre = this.lonLatToCrs(cell.getCentrePoint());
        List<LatLonPoint> corners = cell.getCorners();
        double[] corner1 = this.lonLatToCrs(corners.get(0));
        double d2 = getDistanceSquared(centre, corner1);
        for (int i = 1; i < 4; i++)
        {
            double[] corner = this.lonLatToCrs(corners.get(1));
            d2 = Math.max(d2, getDistanceSquared(centre, corner));
        }
        return Math.sqrt(d2);
    }

    /** Returns the square of the distance between the two points */
    private static double getDistanceSquared(double[] point1, double[] point2)
    {
        double dx = point1[0] - point2[0];
        double dy = point1[1] - point2[1];
        return dx*dx + dy*dy;
    }

    /**
     * Gets a rectangle that bounds the given cell in this RTree's CRS.
     * @param cell
     * @return
     */
    private Rectangle getBoundingRectangle(Cell cell) throws TransformException
    {
        List<LatLonPoint> corners = cell.getCorners();
        // Get the coordinates of the first corner
        double[] coords = this.lonLatToCrs(corners.get(0));
        double minX = coords[0];
        double minY = coords[1];
        double maxX = coords[0];
        double maxY = coords[1];
        // Now look at the other corners
        for (int i = 1; i < 4; i++)
        {
            coords = this.lonLatToCrs(corners.get(i));
            minX = Math.min(minX, coords[0]);
            minY = Math.min(minY, coords[1]);
            maxX = Math.max(maxX, coords[0]);
            maxY = Math.max(maxY, coords[1]);
        }
        return new Rectangle((float)minX, (float)minY, (float)maxX, (float)maxY);
    }

    /**
     * Finds the grid cell that contains the given longitude-latitude point,
     * or null if no cell contains the point.
     * @param longitude The longitude of the point to find
     * @param latitude The latitude of the point to find
     * @return the grid cell that contains the given longitude-latitude point,
     * or null if no cell contains the point.
     */
    public Cell findContainingCell(double longitude, double latitude)
        throws TransformException
    {
        if (latitude < this.minLat || latitude > this.maxLat) return null;
        // Convert the longitude-latitude point to this CRS
        double[] coords = this.lonLatToCrs(longitude, latitude);
        // Create a Point object for querying the RTree
        Point point = new Point((float)coords[0], (float)coords[1]);
        // Find the nearest-neighbour index in this RTree
        this.callback.nearest = -1;
        this.rTree.nearest(point, callback, (float)this.largestCellRadius);
        // See if we got a hit
        if (this.callback.nearest >= 0)
        {
            // Check that this point is actually within the grid cell
            Cell cell = this.cells.get(this.callback.nearest);
            if (cell.containsPoint(longitude, latitude))
            {
                return cell;
            }
            else
            {
                // Check the neigbouring grid points in case the "nearest"
                // grid cell doesn't actually contain the point (this happens
                // quite frequently)
                for (Cell neighbour : cell.getEdgeNeighbours())
                {
                    if (neighbour.containsPoint(longitude, latitude))
                    {
                        return neighbour;
                    }
                }
            }
        }
        // We didn't get a hit
        return null;
    }

    private double[] lonLatToCrs(double longitude, double latitude)
        throws TransformException
    {
        // Constrain longitude to the range [-180:180] so it is legal for
        // the WGS84 CRS
        while (longitude >  180.0) longitude -= 360.0;
        while (longitude < -180.0) longitude += 360.0;
        double[] point = new double[]{longitude, latitude};
        if (this.lonLatToCrs != null)
        {
            // Transform to lat-lon in-place
            this.lonLatToCrs.transform(point, 0, point, 0, 1);
        }
        return point;
    }

    /** Calculates the coordinates of the give lat-lon point in this CRS */
    private double[] lonLatToCrs(LatLonPoint llp) throws TransformException
    {
        return this.lonLatToCrs(llp.getLongitude(), llp.getLatitude());
    }

    /**
     * Calculates and returns an estimate of an appropriate "furthest distance"
     * parameter for this RTree.
     * When querying an {@link RTree}, we need to provide a "furthest distance"
     * parameter that defines the search radius around the point of interest.
     * This value must be carefully chosen: too small a value will result
     * in points being missed, whereas too large a value will lead to very
     * slow search times.
     * @return An esimate of an appropriate value of the "furthest distance"
     * parameter for this RTree, based upon the geographic extent of the points
     * in the RTree and the number of points therein.  The accuracy of this
     * estimate is contingent upon the points in the domain being
     * reasonably evenly spaced.
     */
    private float estimateFurthestDistance()
    {
        if (Float.isNaN(this.cachedEstimatedFurthestDistance))
        {
            // Must recalculate the furthestDistanceParameter
            // First calculate the area covered by the points in the RTree in
            // the RTree's coordinate system
            float area = this.rTree.getBounds().area();
            // Calculate the average area covered by each point in the RTree
            // TODO: watch for division by zero
            float averagePointSize = area / this.rTree.size();
            // The square root of this point size gives an underestimate of the
            // required "furthest distance": we double this value to be safe.
            // TODO: might need to triple or quadruple the value.
            this.cachedEstimatedFurthestDistance = (float)(2.0 * Math.sqrt(averagePointSize));
        }
        return this.cachedEstimatedFurthestDistance;
    }
}
