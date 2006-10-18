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

package uk.ac.rdg.resc.ncwms.dataprovider;

import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;

/**
 * A one-dimensional coordinate axis
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class OneDCoordAxis extends EnhancedCoordAxis
{
    
    protected int count; // The number of points along the axis
    
    protected double minRange; // The minimum and maximum values covered
    protected double maxRange; // by this axis
    
    protected boolean isLongitude; // True if this is a longitude axis
    
    /**
     * Creates a new instance of OneDCoordAxis
     * @param axis1D A {@link CoordinateAxis1D}
     */
    protected OneDCoordAxis(CoordinateAxis1D axis1D)
    {
        this.count = (int)axis1D.getSize();
        this.isLongitude = (axis1D.getAxisType() == AxisType.Lon);
        
        // The range is from the "left" edge of the first coordinate to the
        // "right" edge of the last coordinate
        this.minRange = axis1D.getCoordEdges(0)[0];
        this.maxRange = axis1D.getCoordEdges(this.count - 1)[1];
    }
    
    /**
     * Gets the range of values covered by this axis.  Note that this will
     * not simply include the minimum and maximum values along the axis: we 
     * also take into account the "cell size" of each axis point.
     * @return array of two doubles [minVal,maxVal]
     */
    public double[] getBboxRange()
    {
        if (this.isLongitude)
        {
            Longitude startLon = new Longitude(this.minRange);
            Longitude endLon = new Longitude(this.maxRange);
            double lonRange = startLon.getClockwiseDistanceTo(endLon);
            double distTo180 = startLon.getClockwiseDistanceTo(180.0);
            if (startLon.equals(endLon) || distTo180 < lonRange)
            {
                // The data cover the whole globe, or the +/-180 degrees line
                // comes in the middle of the data range.  The data therefore
                // span the range -180 to 180, even though there may be a gap
                // in the data in this range
                return new double[]{-180.0, 180.0};
            }
        }
        return new double[]{this.minRange, this.maxRange};
    }
    
}
