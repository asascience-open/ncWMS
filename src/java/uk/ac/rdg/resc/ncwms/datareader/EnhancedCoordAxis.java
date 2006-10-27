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

import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Enhances a {@link CoordinateAxis} by providing an efficient means of finding
 * the index for a given value and by providing a correct method for calculating
 * the horizontal bounding box in lat-lon space.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class EnhancedCoordAxis
{
    
    /**
     * Method for creating an EnhancedCoordAxis.
     * @param axis The {@link CoordinateAxis} to wrap, which must be a 
     * latitude or longitude axis
     * @return an EnhancedCoordAxis
     * @throws IllegalArgumentException if the provided axis cannot be turned into
     * an EnhancedCoordAxis
     */
    public static EnhancedCoordAxis create(CoordinateAxis axis)
    {
        if (axis instanceof CoordinateAxis1D)
        {
            CoordinateAxis1D axis1D = (CoordinateAxis1D)axis;
            if (axis1D.isRegular())
            {
                return new Regular1DCoordAxis(axis1D);
            }
            else
            {
                return new Irregular1DCoordAxis(axis1D);
            }
        }
        else
        {
            throw new IllegalArgumentException("Cannot yet deal with coordinate" +
                " axes of >1 dimension");
        }
    }
    
    /**
     * Gets the index of the given point
     * @param point The {@link LatLonPoint}, which will have lon in range
     * [-180,180] and lat in range [-90,90]
     * @return the index that is nearest to this point, or -1 if the point is
     * out of range for the axis
     */
    public abstract int getIndex(LatLonPoint point);
    
    /**
     * Gets the range of values covered by this axis, as will appear in the
     * DataLayer.getLatLonBoundingBox().  Note that this will
     * not simply include the minimum and maximum values along the axis: we 
     * also take into account the "cell size" of each axis point.
     * @return array of two doubles [minVal,maxVal]
     */
    public abstract double[] getBboxRange();
    
    
}
