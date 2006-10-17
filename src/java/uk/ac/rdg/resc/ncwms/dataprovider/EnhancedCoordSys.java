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

import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.exceptions.NcWMSConfigException;

/**
 * An "enhanced" coordinate system that automatically calculates a function
 * to map a point in lat-lon space to the x-y indices of the relevant point
 * in the source data.
 * @see DefaultDataLayer
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class EnhancedCoordSys
{
    private GridCoordSys coordSys;
    private LatLonRect bbox;
    
    /**
     * Creates a new instance of EnhancedCoordSys
     * @param coordSys The {@link GridCoordSys} of the variable in question
     * @throws NcWMSConfigException if the relevant metadata could not be 
     * read or was not valid
     */
    public EnhancedCoordSys(GridCoordSys coordSys)
        throws NcWMSConfigException
    {
        this.coordSys = coordSys;
        this.calcLatLonBBox();
        
        if (coordSys.getXHorizAxis() instanceof CoordinateAxis1D)
        {
            CoordinateAxis1D xAxis = (CoordinateAxis1D)coordSys.getXHorizAxis();
        }
        else
        {
            throw new NcWMSConfigException("x axis is not 1D");
        }
        
        if (coordSys.getYHorizAxis() instanceof CoordinateAxis1D)
        {
            
        }
        else
        {
            throw new NcWMSConfigException("y axis is not 1D");
        }
    }
    
    /**
     * Calculates the latitude-longitude bounding box of the coordinate
     * system, in which longitude is in range[-180:180]
     */
    private void calcLatLonBBox()
    {
        this.bbox = new LatLonRect(); // TODO: do properly
    }
    
    /**
     * @return the bounding box for this layer in lat-lon space.  Longitudes
     * are in the range [-180:180] as per the WMS spec.
     */
    public LatLonRect getLatLonBoundingBox()
    {
        return this.bbox;
    }
    
    /**
     * @return the x-y coordinates of the given point in latitude-longitude
     * space.  Returns a {@link XYPoint} of two integers if the point is
     * within range or null otherwise.
     * @todo Allow different interpolation methods
     */
    public XYPoint getXYCoordElement(LatLonPoint point)
    {
        // Check that this point is within the box
        if (!this.bbox.contains(point))
        {
            return null;
        }
        return null;
    }
    
}
