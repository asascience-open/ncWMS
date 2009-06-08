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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.lut.CurvilinearGrid.Cell;

/**
 * A look-up table generator that uses an Rtree spatial data index to find
 * the nearest grid point to a given longitude-latitude point.  This object is
 * stateless so only one {@link RtreeLutGenerator#INSTANCE instance} is ever created.
 * This instance is immutable and thread-safe.
 * @author Adit Santokhee
 * @author Jon Blower
 */
public final class RtreeLutGenerator implements LutGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(RtreeLutGenerator.class);

    private static final String NORTH_POLAR_STEREOGRAPHIC_CODE = "EPSG:32661";
    private static final String SOUTH_POLAR_STEREOGRAPHIC_CODE = "EPSG:32761";

    /** Singleton instance */
    public static final RtreeLutGenerator INSTANCE = new RtreeLutGenerator();

    /** Private constructor to prevent direct instantiation */
    private RtreeLutGenerator() {}

    public LookUpTable generateLut(LutCacheKey key) throws Exception
    {
        logger.debug("Generating LUT for key {}", key);

        // Create three RTrees, one for the high latitudes, one for the low
        // latitudes and one for the mid-latitudes.  We allow some overlap
        // to ensure that points don't fall between the gaps.
        List<RtreeWrapper> rTrees = new ArrayList<RtreeWrapper>();
        rTrees.add(new RtreeWrapper(-90.0, -65.0, SOUTH_POLAR_STEREOGRAPHIC_CODE));
        rTrees.add(new RtreeWrapper(-70.0,  70.0, null)); // Will use WGS84 lon-lat coordinates
        rTrees.add(new RtreeWrapper( 65.0,  90.0, NORTH_POLAR_STEREOGRAPHIC_CODE));

        // Add the points from the source coordinate system to the Rtrees.
        long nanoTime = System.nanoTime();
        logger.debug("Building rTrees...");
        for (Cell cell : key.getSourceGrid())
        {
            // Add this point to each of the RTrees: the rTrees will ignore
            // any points that are outside of their latitude ranges
            for (RtreeWrapper rTree : rTrees)
            {
                rTree.addCell(cell);
            }
        }
        logger.debug("Built rTrees in {} seconds", (System.nanoTime() - nanoTime) / 1000000000.0);
        
        // Now we build up the look-up table.
        // Create a LUT based upon the key
        LookUpTable lut = new LookUpTable(key);
        nanoTime = System.nanoTime();
        Regular1DCoordAxis lonAxis = key.getLutLonAxis();
        Regular1DCoordAxis latAxis = key.getLutLatAxis();
        for (int latIndex = 0; latIndex < latAxis.getSize(); latIndex++)
        {
            double lat = latAxis.getCoordValue(latIndex);
            for (int lonIndex = 0; lonIndex < lonAxis.getSize(); lonIndex++)
            {
                double lon = lonAxis.getCoordValue(lonIndex);
                // Look through each of the RTrees till we find a match
                for (RtreeWrapper rTree : rTrees)
                {
                    Cell containingCell = rTree.findContainingCell(lon, lat);
                    if (containingCell != null)
                    {
                        // TODO: check that the lat-lon point falls in the grid cell
                        // Add these coordinates to the look-up table.
                        lut.setGridCoordinates(lonIndex, latIndex, containingCell.getGridCoords());
                        break;
                    }
                }
            }
        }
        logger.debug("Built look-up table in {} seconds", (System.nanoTime() - nanoTime) / 1000000000.0);

        return lut;
    }

    /**
     * Simple callback object for retrieving nearest-neighbour indices from
     * RTrees.
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
     * Wraps an {@link RTree} implementation, providing convenience methods
     * for inserting and retrieving values.  Coordinates in the RTree can be
     * stored in a specified coordinate reference system.
     */
    private static final class RtreeWrapper
    {
        // The latitude range for points in this RTree
        private double minLat;
        private double maxLat;
        // The Rtree that will hold the longitude-latitude points
        private RTree rTree;
        // Maps indices in the Rtree to cells in the source data grid
        private List<Cell> cells;
        // Transformation from longitude-latitude to the CRS used by this Rtree
        private MathTransform lonLatToCrs;
        // Callback object for retrieving nearest-neighbour indices
        // This RtreeWrapper will only be used within a single thread so it's
        // OK to share this callback object
        private Callback callback = new Callback();

        // We cache our estimate of the furthestDistance parameter in this field
        // between calls to estimateFurthestDistance().  When we add a new element
        // to this Rtree, we set this field back to Float.NaN in order to ensure
        // it is recalculated when estimateFurthestDistance() is next called.
        private float cachedEstimatedFurthestDistance = Float.NaN;

        public RtreeWrapper(double minLat, double maxLat, String crsCode)
            throws FactoryException
        {
            this.minLat = minLat;
            this.maxLat = maxLat;
            this.rTree = new RTree();
            this.rTree.init(new Properties()); // If we don't initialize the RTree it doesn't work
            this.cells = new ArrayList<Cell>();
            if (crsCode == null)
            {
                this.lonLatToCrs = null;
            }
            else
            {
                // "true" means "force longitude-first"
                CoordinateReferenceSystem crs = CRS.decode(crsCode, true);
                // "true" means "ignore datum shifts"
                this.lonLatToCrs = CRS.findMathTransform(DefaultGeographicCRS.WGS84, crs, true);
            }
        }

        /**
         * Adds a cell from the source grid to this Rtree.  Does nothing if the
         * cell is outside the latitude range.
         * @param longitude The longitude of the point
         * @param latitude The latitude of the point
         * @param i The i index of the point in the source data
         * @param j The j index of the point in the source data
         */
        public void addCell(Cell cell)
            throws TransformException
        {
            double longitude = cell.getCentrePoint().getLongitude();
            double latitude = cell.getCentrePoint().getLatitude();
            if (latitude < this.minLat || latitude > this.maxLat) return;
            // Convert the longitude-latitude point to this CRS
            double[] coords = this.lonLatToCrs(longitude, latitude);
            // Create a rectangle representing the lon-lat point
            Rectangle rect = new Rectangle((float)coords[0], (float)coords[1],
                                           (float)coords[0], (float)coords[1]);
            // Add to the Rtree, using the current size of the rTree as the
            // index.
            this.rTree.add(rect, this.rTree.size());
            // Add the cell to the list so we can find it later
            this.cells.add(cell);
            
            // Record that the furthestDistance parameter must be re-estimated
            this.cachedEstimatedFurthestDistance = Float.NaN;
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
            this.rTree.nearest(point, callback, this.estimateFurthestDistance());
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

}
