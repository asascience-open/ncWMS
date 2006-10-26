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
import ucar.ma2.Range;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import uk.ac.rdg.resc.ncwms.dataprovider.DataChunk;
import uk.ac.rdg.resc.ncwms.dataprovider.EnhancedCoordAxis;

/**
 * Provides static methods for reading data and returning as float arrays.
 * Called from nj22dataset.py.  Implemented in Java for efficiency.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class DataReader
{
    
    /**
     * Read an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.
     * @param location Location of the NetCDF file (full file path, OPeNDAP URL etc)
     * @param varID Unique identifier for the required variable in the file
     * @param fillValue Value to use for missing data
     * @param lonValues Array of longitude values
     * @param latValues Array of latitude values
     * @throws Exception if the variable is not in a lat-lon coordinate system,
     * or if some other error occurs (file not found etc)
     */
    public static float[] read(String location, String varID,
        float fillValue, float[] lonValues, float[] latValues)
        throws Exception
    {
        NetcdfDataset nc = null;
        try
        {
            nc = NetcdfDataset.openDataset(location);
            GridDataset gd = new GridDataset(nc);
            GeoGrid geogrid = gd.findGridByName(varID);
            if (geogrid == null)
            {
                return null;
            }
            GridCoordSys coordSys = geogrid.getCoordinateSystem();
            if (!coordSys.isLatLon())
            {
                throw new Exception("Can only read data from lat-lon coordinate systems");
            }

            // EnhancedCoordAxis gives us a fast method for reading index values
            EnhancedCoordAxis xAxis = EnhancedCoordAxis.create(coordSys.getXHorizAxis());
            EnhancedCoordAxis yAxis = EnhancedCoordAxis.create(coordSys.getYHorizAxis());
            
            // TODO: handle t and z properly
            Range tRange = new Range(0, 0);
            Range zRange = new Range(0, 0);
            // Find the range of x indices
            int minX = Integer.MAX_VALUE;
            int maxX = -Integer.MAX_VALUE;
            int[] xIndices = new int[lonValues.length];
            for (int i = 0; i < lonValues.length; i++)
            {
                xIndices[i] = xAxis.getIndex(new LatLonPointImpl(0.0, lonValues[i]));
                if (xIndices[i] >= 0)
                {
                    if (xIndices[i] < minX) minX = xIndices[i];
                    if (xIndices[i] > maxX) maxX = xIndices[i];
                }
            }
            Range xRange = new Range(minX, maxX);
            // Create an array to hold the data
            float[] picData = new float[lonValues.length * latValues.length];
            Arrays.fill(picData, fillValue);
            // Cycle through the latitude values, extracting a scanline of
            // data each time from minX to maxX
            SimpleGeoGrid sgg = new SimpleGeoGrid(geogrid);
            for (int j = 0; j < latValues.length; j++)
            {
                int yIndex = yAxis.getIndex(new LatLonPointImpl(latValues[j], 0.0));
                if (yIndex >= 0)
                {
                    Range yRange = new Range(yIndex, yIndex);
                    DataChunk dataChunk = sgg.readDataChunk(tRange, zRange,
                        yRange, xRange);
                    // Now copy the scanline's data to the picture array
                    for (int i = 0; i < xIndices.length; i++)
                    {
                        if (xIndices[i] >= 0)
                        {
                            int picIndex = j * lonValues.length + i;
                            picData[picIndex] = dataChunk.getValue(xIndices[i] - minX);
                        }
                    }
                }
            }

            return picData;
        }
        finally
        {
            if (nc != null)
            {
                nc.close();
            }
        }
    }
    
    public static void main(String[] args) throws Exception
    {
        String location = "C:\\data\\20061017-UKMO-L4UHfnd-GLOB-v01.nc";
        String varID = "sst_foundation";
        float fillValue = Float.NaN;
        float[] lonValues = new float[256];
        float[] latValues = new float[256];
        for (int i = 0; i < 256; i++)
        {
            lonValues[i] = i * 90.0f / 256 - 90;
            latValues[i] = i * 90.0f / 256;
        }
        float[] data = read(location, varID, Float.NaN, lonValues, latValues);
    }
}
