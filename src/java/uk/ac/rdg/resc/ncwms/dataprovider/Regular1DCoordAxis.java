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
public class Regular1DCoordAxis extends OneDCoordAxis
{
    private double start;  // The first value along the axis
    private double stride; // The stride length along the axis
    
    
    /**
     * Creates a new instance of Regular1DCoordAxis
     * @param axis1D A regular {@link CoordinateAxis1D} - we have already
     * checked that axis1d.isRegular() == true
     */
    public Regular1DCoordAxis(CoordinateAxis1D axis1D)
    {
        super(axis1D);
        this.start = axis1D.getStart();
        this.stride = axis1D.getIncrement();
    }
    
    /**
     * Gets the index of the given point
     * @param point The {@link LatLonPoint}, which will have lon in range
     * [-180,180] and lat in range [-90,90]
     * @return the index that is nearest to this point, or -1 if the point is
     * out of range for the axis
     */
    public int getIndex(LatLonPoint point)
    {
        if (this.isLongitude)
        {
            /*Longitude start = new Longitude(axis.getStart());
            double distance = start.getClockwiseDistanceTo(this);
            double exactNumSteps = distance / axis.getStride();
            int numSteps = (int)exactNumSteps;
            float fracDiff = (exactNumSteps - (float)numSteps) / (float)numSteps;
            if (fracDiff < 1.0e-6 || round == ROUND_DOWN)
            {
                return numSteps;
            }
            else
            {
                return numSteps + 1;
            }*/
        }
        else
        {
            // this is a latitude axis
        }
        return -1;
    }
}
