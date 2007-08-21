/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.ncwms.datareader;

import com.sleepycat.persist.model.Persistent;
import org.apache.log4j.Logger;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * A regular, one-dimensional coordinate axis, whose values obey the rule
 * val(i) = start + stride * i, i.e. i = (val - start) / stride;
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent
public class Regular1DCoordAxis extends OneDCoordAxis
{
    private static final Logger logger = Logger.getLogger(Regular1DCoordAxis.class);
    
    private double start;  // The first value along the axis
    private double stride; // The stride length along the axis
    private double maxValue; // The maximum value along the axis
    private boolean wraps; // True if this is a longitude axis that wraps the globe
    
    /**
     * Creates a new instance of Regular1DCoordAxis
     * @param axis1D A regular {@link CoordinateAxis1D} - we have already
     * checked that axis1d.isRegular() == true
     */
    public Regular1DCoordAxis(CoordinateAxis1D axis1D)
    {
        super(axis1D);
        this.init(axis1D.getStart(), axis1D.getIncrement());
    }
    
    /**
     * Creates a Regular1DCoordAxis "from scratch"
     */
    public Regular1DCoordAxis(double start, double stride, int count, boolean isLongitude)
    {
        super(count, isLongitude);
        this.init(start, stride);
    }
    
    /**
     * Default constructor (used by Berkeley DB).  This can still be private
     * and apparently the Berkeley DB will get around this (we don't need public
     * setters for the fields for the same reason).
     */
    private Regular1DCoordAxis() {}
    
    private void init(double start, double stride)
    {
        this.start = start;
        this.stride = stride;
        this.maxValue = this.start + this.stride * (this.getCount() - 1);
        this.wraps = false;
        if (this.isLongitude)
        {
            Longitude st = new Longitude(this.start);
            Longitude mid = new Longitude(this.start + this.stride * this.getCount() / 2);
            // Find the longitude of the point that is just off the end of the axis
            Longitude end = new Longitude(this.start + this.stride * this.getCount());
            logger.debug("Longitudes: st = {}, mid = {}, end = {}",
                new Object[]{st.getValue(), mid.getValue(), end.getValue()});
            // In some cases the end point might be past the original start point
            if (st.equals(end) || st.getClockwiseDistanceTo(mid) > st.getClockwiseDistanceTo(end))
            {
                this.wraps = true;
            }
        }
        logger.debug("Created regular {} axis, wraps = {}",
            (this.isLongitude ? "longitude" : "latitude"), this.wraps);
    }
    
    /**
     * Gets the index of the given point. Uses index = (value - start) / stride,
     * hence this is fast.
     * @param point The {@link LatLonPoint}, which will have lon in range
     * [-180,180] and lat in range [-90,90]
     * @return the index that is nearest to this point, or -1 if the point is
     * out of range for the axis
     */
    public int getIndex(LatLonPoint point)
    {
        if (this.isLongitude)
        {
            logger.debug("Finding value for longitude {}", point.getLongitude());
            Longitude lon = new Longitude(point.getLongitude());
            if (this.wraps || lon.isBetween(this.start, this.maxValue))
            {
                Longitude startLon = new Longitude(this.start);
                double distance = startLon.getClockwiseDistanceTo(lon);
                double exactNumSteps = distance / this.stride;
                // This axis might wrap, so we make sure that the returned index
                // is within range
                int index = ((int)Math.round(exactNumSteps)) % this.getCount(); 
                logger.debug("returning {}", index);
                return index;              
            }
            else
            {
                logger.debug("out of range: returning -1");
                return -1;
            }
        }
        else
        {
            logger.debug("Finding value for latitude {}", point.getLatitude());
            // this is a latitude axis
            double distance = point.getLatitude() - this.start;
            double exactNumSteps = distance / this.stride;
            int index = (int)Math.round(exactNumSteps);
            logger.debug("index = {}, count = {}", index, this.getCount());
            if (index < 0 || index >= this.getCount())
            {
                logger.debug("returning -1");
                return -1;
            }
            logger.debug("returning {}", index);
            return index;
        }
        
    }
}
