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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis2D;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;

/**
 * Key for an {@link LutCache}.  This contains all the information needed to
 * generate a look-up table.
 * @author Jon
 */
public final class LutCacheKey implements Serializable
{
    private static final long serialVersionUID = 432948123412387L;

    private static final Logger logger = LoggerFactory.getLogger(LutCacheKey.class);

    // These define the source grid: each point on the 2D grid has a longitude
    // and latitude value. These 2D arrays are flattened into 1D arrays.
    private double[] longitudes;
    private double[] latitudes;
    // ni and nj define the dimensions of the 2D grid.  longitudes.length == ni * nj
    private int ni;
    private int nj;
    private int resolutionMultiplier;

    // The following fields are not serialized as they contain redundant information
    // These define the look-up table itself: the lat-lon bounding box and the
    // number of points in each direction.
    private transient double lonMin;
    private transient double lonMax;
    private transient double latMin;
    private transient double latMax;
    private transient int nLon;
    private transient int nLat;
    private transient CurvilinearGrid grid; // Contains a more convenient data structure for the grid

    /**
     * Creates and returns a {@link LutCacheKey} from the given coordinate system
     * from a NetCDF dataset.
     * @param lonAxis The longitude axis
     * @param latAxis The latitude axis
     * @param resolutionMultiplier Approximate resolution multiplier for the
     * look-up table.  The LUT will usually have a higher resolution
     * than the original data grid.  If this parameter has a value of 3 (a
     * sensible default) then the final look-up table will have approximately
     * 9 times the number of points in the original grid.
     * @throws IllegalArgumentException if the axes are not longitude
     * and latitude and if they are not of the same size in the i and j directions
     * @todo Do we need to take a parameter that gives the index of the correct
     * z level to use? If the z axis is "upside down" we could end up with a lot
     * of spurious missing values.
     * @todo does the variable need to be "enhanced" to read missing values
     * properly?
     */
    public static LutCacheKey fromCoordSys(CoordinateAxis2D lonAxis,
        CoordinateAxis2D latAxis, int resolutionMultiplier)
    {
        logger.debug("Creating LutCacheKey with resolution multiplier {}", resolutionMultiplier);

        // Check the types of the coordinate axes
        if (!(lonAxis.getAxisType() == AxisType.Lon && latAxis.getAxisType() == AxisType.Lat))
        {
            throw new IllegalArgumentException("X and Y axes must be longitude and latitude");
        }
        // Check that the latitude axis has the same shape
        if (!Arrays.equals(lonAxis.getShape(), latAxis.getShape()))
        {
            throw new IllegalArgumentException("Axes are not of the same shape");
        }

        // Create a new key
        LutCacheKey key = new LutCacheKey();

        key.resolutionMultiplier = resolutionMultiplier;

        // Find the number of points in each direction in the lon and lat 2D arrays
        key.nj = lonAxis.getShape(0);
        key.ni = lonAxis.getShape(1);
        if (key.ni <= 0 || key.nj <= 0)
        {
            String msg = String.format("ni (=%d) and nj (=%d) must be positive and > 0", key.ni, key.nj);
            throw new IllegalStateException(msg);
        }

        // Load the longitude and latitude values
        key.longitudes = lonAxis.getCoordValues();
        key.latitudes  = latAxis.getCoordValues();
        // Check that the arrays are of the right length
        if (key.longitudes.length != key.latitudes.length ||
            key.longitudes.length != key.ni * key.nj)
        {
            throw new IllegalStateException("Longitudes, latitudes and "
                + "ni*nj not same length");
        }

        // Now set the redundant fields (that won't be serialized)
        key.populateTransientFields();

        logger.debug("Created LutCacheKey: {}", key.toString());

        return key;
    }

    /**
     * Populate those fields in this key that are not serialized.  These contain
     * redundant information.
     */
    private void populateTransientFields()
    {
        // Set the min and max longitude and latitudes
        this.setLutBoundingBox();

        // Now calculate the number of points in the LUT along the longitude
        // and latitude directions
        double ratio = (this.lonMax - this.lonMin) / (this.latMax - this.latMin);
        this.nLat = (int) (Math.sqrt((this.resolutionMultiplier * this.resolutionMultiplier * this.ni * this.nj) / ratio));
        this.nLon = (int) (ratio * this.nLat);
        if (this.nLon <= 0 || this.nLat <= 0)
        {
            String msg = String.format("nLon (=%d) and nLat (=%d) must be positive and > 0", this.nLon, this.nLat);
            throw new IllegalStateException(msg);
        }

        this.grid = new CurvilinearGrid(this.ni, this.nj, this.longitudes, this.latitudes);
    }

    /**
     * Sets the lat-lon bounding box of the LUT that will be generated from this
     * key.
     */
    private void setLutBoundingBox()
    {
        // Set the bounding box of the
        this.lonMin = this.longitudes[0];
        this.lonMax = this.longitudes[0];
        for (int i = 1; i < this.longitudes.length; i++)
        {
            double lon = this.longitudes[i];
            if (lon < this.lonMin) this.lonMin = lon;
            if (lon > this.lonMax) this.lonMax = lon;
        }
        this.latMin = this.latitudes[0];
        this.latMax = this.latitudes[0];
        for (int i = 1; i < this.latitudes.length; i++)
        {
            double lat = this.latitudes[i];
            if (lat < this.latMin) this.latMin = lat;
            if (lat > this.latMax) this.latMax = lat;
        }
        if (this.lonMin >= this.lonMax || this.latMin >= this.latMax)
        {
            String msg = String.format("Invalid bounding box for LUT: %f, %f, %f, %f",
                this.lonMin, this.latMin, this.lonMax, this.latMax);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Returns the longitude axis for the look-up table that will be generated
     * from this key
     */
    public Regular1DCoordAxis getLutLonAxis()
    {
        double lonStride = (this.lonMax - this.lonMin) / (this.nLon - 1);
        return new Regular1DCoordAxis(this.lonMin, lonStride, this.nLon, AxisType.Lon);
    }

    /**
     * Returns the latitude axis for the look-up table that will be generated
     * from this key
     */
    public Regular1DCoordAxis getLutLatAxis()
    {
        double latStride = (this.latMax - this.latMin) / (this.nLat - 1);
        return new Regular1DCoordAxis(this.latMin, latStride, this.nLat, AxisType.Lat);
    }

    /**
     * Returns a representation of the source grid from which the LUT will be
     * generated.
     */
    public CurvilinearGrid getSourceGrid()
    {
        return this.grid;
    }

    /** Ensures that the transient fields are populated upon deserialization */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        this.populateTransientFields();
    }

    @Override public int hashCode()
    {
        int result = 17;
        result = 31 * result + Arrays.hashCode(this.longitudes);
        result = 31 * result + Arrays.hashCode(this.latitudes);
        result = 31 * result + this.ni;
        result = 31 * result + this.nj;
        result = 31 * result + this.resolutionMultiplier;
        return result;
    }

    @Override public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof LutCacheKey)) return false;
        LutCacheKey other = (LutCacheKey)obj;

        // We do the cheap comparisons first
        return this.ni == other.ni &&
               this.nj == other.nj &&
               this.resolutionMultiplier == other.resolutionMultiplier &&
               Arrays.equals(this.longitudes, other.longitudes) &&
               Arrays.equals(this.latitudes, other.latitudes);
    }

    @Override public String toString()
    {
        return String.format("Source grid size: %d, LUT size: %d, BBOX: %f, %f, %f, %f",
            ni*nj, nLon*nLat, lonMin, latMin, lonMax, latMax);
    }

}
