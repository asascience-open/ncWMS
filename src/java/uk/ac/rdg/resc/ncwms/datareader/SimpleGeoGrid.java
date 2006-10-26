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

import java.util.Arrays;
import java.util.List;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import uk.ac.rdg.resc.ncwms.dataprovider.DataChunk;

/**
 * Wraps a GeoGrid to provide a means for reading "raw" data
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class SimpleGeoGrid
{
    private GeoGrid gg;
    private int xDimOrgIndex = -1, yDimOrgIndex = -1, zDimOrgIndex = -1, tDimOrgIndex = -1;
    
    /**
     * Creates a new instance of SimpleGeoGrid
     */
    public SimpleGeoGrid(GeoGrid gg)
    {
        this.gg = gg;
        GridCoordSys gcs = gg.getCoordinateSystem();
        if (gcs.isProductSet())
        {
            xDimOrgIndex = findDimension( gcs.getXHorizAxis().getDimension(0));
            yDimOrgIndex = findDimension( gcs.getYHorizAxis().getDimension(0));
            
        }
        else
        { // 2D case
            yDimOrgIndex = findDimension( gcs.getXHorizAxis().getDimension(0));
            xDimOrgIndex = findDimension( gcs.getXHorizAxis().getDimension(1));
        }
        
        if (gcs.getVerticalAxis() != null) zDimOrgIndex = findDimension( gcs.getVerticalAxis().getDimension(0));
        if (gcs.getTimeAxis() != null) tDimOrgIndex = findDimension( gcs.getTimeAxis().getDimension(0));
        
    }
    
    private int findDimension( Dimension want)
    {
        java.util.List dims = this.gg.getVariable().getDimensions();
        for (int i=0; i < dims.size();i ++)
        {
            Dimension d = (Dimension) dims.get(i);
            if (d.equals( want))
                return i;
        }
        return -1;
    }
    
    
    
    /**
     * Efficiently reads a raw Data chunk from the file - we don't use
     * GeoGrid.subset() because it performs conversions and missing data
     * comparisons for every single data point, whereas we only want to do this
     * for the data points that we will plot.
     */
    public DataChunk readDataChunk(Range tRange, Range zRange, Range yRange,
        Range xRange) throws Exception
    {
        // Create List of Range objects (code pinched from GeoGrid.java)
        int rank = this.gg.getRank();
        Range[] ranges = new Range[rank];
        if (null != this.gg.getXDimension())
          ranges[xDimOrgIndex] = xRange;
        if (null != this.gg.getYDimension())
          ranges[yDimOrgIndex] = yRange;
        if (null != this.gg.getZDimension())
          ranges[zDimOrgIndex] = zRange;
        if (null != this.gg.getTimeDimension())
          ranges[tDimOrgIndex] = tRange;
        List rangesList = Arrays.asList(ranges);
        // Get a handle to the non-enhanced Variable:
        Variable var = this.gg.getVariable().getOriginalVariable();
        Array arr = var.read(rangesList);
        return new DataChunk(arr.reduce());
    }
    
}
