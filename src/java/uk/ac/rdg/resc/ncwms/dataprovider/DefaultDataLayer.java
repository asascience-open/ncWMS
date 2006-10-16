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

import java.util.Date;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * {@link DataLayer} that belongs to a {@link DefaultDataProvider}.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class DefaultDataLayer implements DataLayer
{
    private String id;
    private String title;
    private Date[] tValues;
    private double[] zValues;
    private String zAxisUnits;
    
    /**
     * Creates a new instance of DefaultDataLayer from a {@link GeoGrid}
     * object
     */
    public DefaultDataLayer(GeoGrid gg)
    {
        this.id = gg.getName();
        this.title = gg.getDescription();
        
        GridCoordSys coordSys = gg.getCoordinateSystem();
        
        // Set the vertical dimension
        if (coordSys.hasVerticalAxis())
        {
            CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
            this.zAxisUnits = zAxis.getUnitsString();
            double[] zvals = zAxis.getCoordValues();
            if (coordSys.isZPositive())
            {
                this.zValues = zvals;
            }
            else
            {
                this.zValues = new double[zvals.length];
                for (int i = 0; i < zvals.length; i++)
                {
                    this.zValues[i] = 0 - zvals[i];
                }
            }
        }
        else
        {
            this.zValues = null;
            this.zAxisUnits = null;
        }
        
        // Set the time dimension
        this.tValues = coordSys.isDate() ? coordSys.getTimeDates() : null;
        
        // Set the conversion between lat-lon and x-y coordinates
    }
    
    /**
     * @return a unique ID for this DataLayer
     */
    public String getID()
    {
        return this.id;
    }
    
    /**
     * @return a human-readable title for this DataLayer
     */
    public String getTitle()
    {
        return this.title;
    }
    
    /**
     * @return values along the time axis, or null if there is no time dimension
     * in this layer
     */
    public Date[] getTValues()
    {
        return this.tValues;
    }
    
    /**
     * @return a String describing the units of the vertical axis, or null if
     * there is no vertical axis
     */
    public String getZAxisUnits()
    {
        return this.zAxisUnits;
    }
    
    /**
     * @return values along the z axis (depth, elevation, sigma etc) or null
     * if there is no depth dimension in this layer
     */
    public double[] getZValues()
    {
        return this.zValues;
    }
    
    /**
     * @return the x-y coordinates (in this {@link  DataProvider}'s coordinate
     * system of the given point in latitude-longitude space.  Returns a
     * array of two integers if the point is within range or null otherwise.
     * First integer is the x coordinate, the second is the y coordinate.
     * @todo Allow different interpolation methods
     */
    public int[] getXYCoordElement(LatLonPoint point)
    {
        return null;
    }
}
