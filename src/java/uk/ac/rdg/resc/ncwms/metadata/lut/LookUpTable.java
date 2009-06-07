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
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dt.GridCoordSystem;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;

/**
 * An object that provides an approximate means for mapping from longitude-latitude
 * coordinates to i and j index coordinates in a curvilinear grid.
 * @author Jon
 */
public final class LookUpTable implements Serializable
{
    private static final long serialVersionUID = 34591759431294534L;

    private static final Logger logger = LoggerFactory.getLogger(LookUpTable.class);

    // The contents of the look-up table: i.e. the i and j indices of each
    // lon-lat point in the LUT.  These are flattened from a 2D to a 1D array.
    // We store these as shorts to save disk space and (more importantly) disk
    // read/write time.  The LUT would need to be extremely large before we
    // have to worry about this.
    // Each array has the size nLon * nLat
    private short[] iIndices;
    private short[] jIndices;

    // These fields are used to define the axes of the LUT.
    // They are package-private to make them accessible to the LUT generator.
    double lonMin;
    double lonStride;
    int nLon;
    double latMin;
    double latStride;
    int nLat;

    // The axes of the look-up table, generated on the fly from the values above.
    // These are not serialized: upon deserialization they are regenerated in the
    // readObject() method.
    private transient Regular1DCoordAxis lonAxis;
    private transient Regular1DCoordAxis latAxis;
    
    /**
     * Temporary cache of LUTs
     */
    private static final Map<LutCacheKey, LookUpTable> LUTS = 
        new HashMap<LutCacheKey, LookUpTable>();

    /**
     * Temporary method for creating a LUT from a GridCoordSystem.  LUTs are
     * cached in memory.
     * @param coordSys
     * @return a LookUpTable for the given coordinate system
     * @throws Exception if there was an error generating the LUT
     */
    public static LookUpTable fromCoordSys(GridCoordSystem coordSys)
        throws Exception
    {
        LutCacheKey key = LutCacheKey.fromCoordSys(coordSys, 3); // resolution multiplier is 3
        LookUpTable lut = LUTS.get(key);
        if (lut == null)
        {
            logger.debug("Need to calculate look up table for key {}", key);
            lut = RtreeLutGenerator.INSTANCE.generateLut(key);
            LUTS.put(key, lut);
        }
        else
        {
            logger.debug("Look up table found in cache");
        }
        return lut;
    }

    /**
     * Creates an empty look-up table (with all indices set to zero) from the
     * given key.
     * @param key The {@link LutCacheKey} that contains the "recipe" for generating
     * this look-up table. This key is assumed to contain self-consistent information
     * so its fields are not checked here.
     */
    public LookUpTable(LutCacheKey key)
    {
        this.lonMin = key.lonMin;
        this.nLon = key.nLon;
        this.latMin = key.latMin;
        this.nLat = key.nLat;
        this.lonStride = (key.lonMax - key.lonMin) / (key.nLon - 1);
        this.latStride = (key.latMax - key.latMin) / (key.nLat - 1);
        this.iIndices = new short[nLon * nLat];
        this.jIndices = new short[nLon * nLat];
    }

    /**
     * Returns the nearest grid coordinates to the given longitude-latitude
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
        int iLon = this.lonAxis.getIndex(longitude);
        int iLat = this.latAxis.getIndex(latitude);
        if (iLon >= 0 && iLat >= 0)
        {
            int index = iLon + (iLat * this.nLon);
            return new int[]{
                this.iIndices[index],
                this.jIndices[index]
            };
        }
        else
        {
            return null;
        }
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
    void setGridCoordinates(int lonIndex, int latIndex, int[] dataIndices)
    {
        if (dataIndices != null && dataIndices.length != 2)
        {
            throw new IllegalArgumentException("dataIndices must be a two-element array, or null");
        }
        int index = (latIndex * this.nLon) + lonIndex;
        if (dataIndices == null)
        {
            this.iIndices[index] = -1;
            this.jIndices[index] = -1;
        }
        else
        {
            if (dataIndices[0] > Short.MAX_VALUE ||
                dataIndices[1] > Short.MAX_VALUE)
            {
                throw new IllegalArgumentException("data indices out of range for this look-up table");
            }
            this.iIndices[index] = (short)dataIndices[0];
            this.jIndices[index] = (short)dataIndices[1];
        }
    }

    /** Ensures that the transient fields are populated upon deserialization */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        this.createAxes();
    }

    /** Creates the {@link Regular1DCoordAxis} objects (e.g. on deserialization) */
    private void createAxes()
    {
        this.lonAxis = new Regular1DCoordAxis(this.lonMin, this.lonStride, this.nLon, AxisType.Lon);
        this.latAxis = new Regular1DCoordAxis(this.latMin, this.latStride, this.nLat, AxisType.Lat);
    }
    
    @Override public int hashCode()
    {
        int result = 17;
        result = 31 * result + Arrays.hashCode(this.iIndices);
        result = 31 * result + Arrays.hashCode(this.jIndices);
        result = 31 * result + new Double(this.lonMin).hashCode();
        result = 31 * result + new Double(this.lonStride).hashCode();
        result = 31 * result + this.nLon;
        result = 31 * result + new Double(this.latMin).hashCode();
        result = 31 * result + new Double(this.latStride).hashCode();
        result = 31 * result + this.nLat;
        return result;
    }

    @Override public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof LookUpTable)) return false;
        LookUpTable other = (LookUpTable)obj;

        // We do the cheap comparisons first
        return this.nLon == other.nLon &&
               this.nLat == other.nLat &&
               this.lonMin == other.lonMin &&
               this.lonStride == other.lonStride &&
               this.latMin == other.latMin &&
               this.latStride == other.latStride &&
               Arrays.equals(this.iIndices, other.iIndices) &&
               Arrays.equals(this.jIndices, other.jIndices);
    }

}
