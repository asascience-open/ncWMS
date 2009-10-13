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

package uk.ac.rdg.resc.ncwms.coordsys;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;

/**
 * An object that provides an approximate means for mapping from longitude-latitude
 * coordinates to i and j index coordinates in a curvilinear grid.
 * @todo Some duplication of {@link HorizontalGrid}?  There's a difference in
 * how the "tick marks" along the axes are set up: see how the Regular1DCoordAxes
 * are created.
 * @author Jon
 */
final class LookUpTable
{
    // The contents of the look-up table: i.e. the i and j indices of each
    // lon-lat point in the LUT.  These are flattened from a 2D to a 1D array.
    // We store these as shorts to save disk space.  The LUT would need to be
    // extremely large before we would have to worry about overflows.
    // Each array has the size nLon * nLat
    private final short[] iIndices;
    private final short[] jIndices;

    private final int nLon;
    private final int nLat;

    // Converts from lat-lon coordinates to index space in the LUT.
    private final AffineTransform transform = new AffineTransform();

    /**
     * Creates an empty look-up table (with all indices set to -1).
     * @param curvGrid The CurvilinearGrid which this LUT will approximate
     * @param minResolution The minimum resolution of the LUT in degrees
     */
    public LookUpTable(CurvilinearGrid curvGrid, double minResolution)
    {
        GeographicBoundingBox bbox = curvGrid.getBoundingBox();
        
        double lonDiff = bbox.getEastBoundLongitude() - bbox.getWestBoundLongitude();
        double latDiff = bbox.getNorthBoundLatitude() - bbox.getSouthBoundLatitude();

        // Now calculate the number of points in the LUT along the longitude
        // and latitude directions
        this.nLon = (int)Math.ceil(lonDiff / minResolution);
        this.nLat = (int)Math.ceil(latDiff / minResolution);
        if (this.nLon <= 0 || this.nLat <= 0)
        {
            String msg = String.format("nLon (=%d) and nLat (=%d) must be positive and > 0", this.nLon, this.nLat);
            throw new IllegalStateException(msg);
        }

        // This ensures that the highest value of longitude (corresponding
        // with nLon - 1) is getLonMax()
        double lonStride = lonDiff / (this.nLon - 1);
        double latStride = latDiff / (this.nLat - 1);

        // Create the transform.  We scale by the inverse of the stride length
        this.transform.scale(1.0/lonStride, 1.0/latStride);
        // Then we translate by the minimum coordinate values
        this.transform.translate(-bbox.getWestBoundLongitude(), -bbox.getSouthBoundLatitude());

        this.iIndices = new short[this.nLon * this.nLat];
        this.jIndices = new short[this.nLon * this.nLat];
        Arrays.fill(this.iIndices, (short)-1);
        Arrays.fill(this.jIndices, (short)-1);
    }

    /**
     * Returns the nearest coordinates in the original CurvilinearGrid
     * to the given longitude-latitude
     * point, or null if the given longitude-latitude point is not in the domain
     * of this look-up table.
     * @param longitude The longitude of the point of interest
     * @param latitude The latitude of the point of interest
     * @return A newly-created integer array with two values: the first value is
     * the i coordinate in the grid, the second is the j coordinate.  Returns
     * null if the given longitude-latitude point is not in the domain of this LUT.
     */
    public int[] getGridCoordinates(double longitude, double latitude)
    {
        // Convert from longitude-latitude to index space in this LUT
        Point2D indexPoint =
            this.transform.transform(new Point2D.Double(longitude, latitude), null);
        int iLon = (int)Math.round(indexPoint.getX());
        int iLat = (int)Math.round(indexPoint.getY());

        if (iLon < 0 || iLat < 0 || iLon >= this.nLon || iLat >= this.nLat)
        {
            return null;
        }
        int index = iLon + (iLat * this.nLon);
        int iIndex = this.iIndices[index];
        int jIndex = this.jIndices[index];
        if (iIndex < 0 || jIndex < 0) return null;
        return new int[] {iIndex, jIndex};
    }

    /**
     * Sets the coordinates of the nearest source grid point to the provided
     * longitude-latitude point. The longitude-latitude point is defined by the
     * indices along the longitude and latitude axes.
     * @param lonIndex Index along the longitude axis in this look-up table.
     * @param latIndex Index along the latitude axis in this look-up table.
     * @param dataIndices Pair of i-j indices of the nearest grid point in the
     * source data, or null if the lon-lat point is not within the grid's domain
     * @throws IllegalArgumentException if {@code dataIndices} is not null and
     * is not a two-element array, or if either of the dataIndices are greater
     * than {@link Short#MAX_VALUE}.
     */
    public void setGridCoordinates(int lonIndex, int latIndex, int[] dataIndices)
    {
        if (dataIndices == null) return;
        if (dataIndices.length != 2)
        {
            throw new IllegalArgumentException("dataIndices must be a two-element array, or null");
        }
        int index = (latIndex * this.nLon) + lonIndex;
        if (dataIndices[0] > Short.MAX_VALUE ||
            dataIndices[1] > Short.MAX_VALUE)
        {
            throw new IllegalArgumentException("data indices out of range for this look-up table");
        }
        this.iIndices[index] = (short)dataIndices[0];
        this.jIndices[index] = (short)dataIndices[1];
    }

    /**
     * Gets the number of points in this look-up table along its longitude axis
     */
    public int getNumLonPoints()
    {
        return this.nLon;
    }

    /**
     * Gets the number of points in this look-up table along its latitude axis
     */
    public int getNumLatPoints()
    {
        return this.nLat;
    }

    /**
     * Gets an affine transform that converts from lat-lon coordinates to the
     * indices in the lat-lon grid used by this look-up table.
     * @return
     */
    public AffineTransform getTransform()
    {
        return this.transform;
    }

}
