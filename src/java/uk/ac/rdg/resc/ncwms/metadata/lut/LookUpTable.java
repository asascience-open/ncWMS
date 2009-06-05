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

import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.AxisType;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;

/**
 * An object that provides an approximate means for mapping from longitude-latitude
 * coordinates to i and j index coordinates in a curvilinear grid.
 * @author Jon
 */
public class LookUpTable implements Serializable
{
    private static final long serialVersionUID = 34591759431294534L;

    private static final Logger logger = LoggerFactory.getLogger(LookUpTable.class);

    // We store these as shorts to save disk space and (more importantly) disk
    // read/write time.  The LUT would need to be extremely large before we
    // have to worry about this.
    private short[] iIndices;
    private short[] jIndices;

    // These fields are used to define the axes of the LUT.  They match the
    // values from the LutCacheKey that was used as a recipe to generate this LUT
    private double lonMin;
    private double lonMax;
    private double latMin;
    private double latMax;
    private int nLon;
    private int nLat;

    // The axes of the look-up table, generated on the fly from the values above.
    // These are not serialized.
    // TODO: make sure that these are regenerated on deserialization
    private transient Regular1DCoordAxis lonAxis;
    private transient Regular1DCoordAxis latAxis;

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
        int xi = this.lonAxis.getIndex(longitude);
        int yi = this.latAxis.getIndex(latitude);
        if (xi >= 0 && yi >= 0)
        {
            return new int[]{
                this.iIndices[yi * this.nLon + xi],
                this.jIndices[yi * this.nLat + xi]
            };
        }
        else
        {
            return null;
        }
    }

    /** Creates the {@link Regular1DCoordAxis} objects (e.g. on deserialization) */
    private void createAxes()
    {
        double lonStride = (this.lonMax - this.lonMin) / (this.nLon - 1);
        double latStride = (this.latMax - this.latMin) / (this.nLat - 1);
        this.lonAxis = new Regular1DCoordAxis(this.lonMin, lonStride, this.nLon, AxisType.Lon);
        this.latAxis = new Regular1DCoordAxis(this.latMin, latStride, this.nLat, AxisType.Lat);
    }

}
