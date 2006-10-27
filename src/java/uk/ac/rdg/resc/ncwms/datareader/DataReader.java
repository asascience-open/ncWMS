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

import java.io.IOException;
import java.util.Arrays;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

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
     * 
     * @param location Location of the NetCDF file (full file path, OPeNDAP URL etc)
     * @param varID Unique identifier for the required variable in the file
     * @param tValue The value of time as specified by the client
     * @param zValue The value of elevation as specified by the client
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @throws WMSExceptionInJava if an error occurs
     */
    public static float[] read(String location, String varID,
        String tValue, String zValue, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava
    {
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset but don't enhance it.  We do this to avoid 
            // the performance penalty of unpacking data and checking for
            // missing values for every data point we read.  We will create
            // and enhanced variable and use this for unpacking and missing-value
            // checks, just for the data points we need to display.
            nc = NetcdfDataset.openDataset(location, false, null);
            // Add the coordinate systems
            CoordSysBuilder.addCoordinateSystems(nc, null);
            GridDataset gd = new GridDataset(nc);
            GeoGrid geogrid = gd.findGridByName(varID);
            if (geogrid == null)
            {
                return null;
            }
            GridCoordSys coordSys = geogrid.getCoordinateSystem();
            coordSys.isZPositive();
            if (!coordSys.isLatLon())
            {
                throw new WMSExceptionInJava("Can only read data from lat-lon coordinate systems");
            }
            // Get an enhanced version of the variable for fast reading of data
            EnhanceScaleMissingImpl enhanced = new EnhanceScaleMissingImpl((VariableDS)geogrid.getVariable());

            // EnhancedCoordAxis gives us a fast method for reading index values
            EnhancedCoordAxis xAxis = EnhancedCoordAxis.create(coordSys.getXHorizAxis());
            EnhancedCoordAxis yAxis = EnhancedCoordAxis.create(coordSys.getYHorizAxis());
            
            // TODO: handle t properly
            Range tRange = new Range(0, 0);
            int zIndex = 0; // Default value of z is the first in the axis
            if (zValue != null && !zValue.equals("") && coordSys.hasVerticalAxis())
            {
                zIndex = findZIndex(coordSys.getVerticalAxis().getCoordValues(),
                    zValue, coordSys.isZPositive());
            }
            Range zRange = new Range(zIndex, zIndex);
            
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
            DataChunk dataChunk;
            // Cycle through the latitude values, extracting a scanline of
            // data each time from minX to maxX
            for (int j = 0; j < latValues.length; j++)
            {
                int yIndex = yAxis.getIndex(new LatLonPointImpl(latValues[j], 0.0));
                if (yIndex >= 0)
                {
                    Range yRange = new Range(yIndex, yIndex);
                    // Read a chunk of data - values will not be unpacked or
                    // checked for missing values yet
                    GeoGrid subset = geogrid.subset(tRange, zRange, yRange, xRange);
                    dataChunk = new DataChunk(subset.readYXData(0, 0).reduce());
                    // Now copy the scanline's data to the picture array
                    for (int i = 0; i < xIndices.length; i++)
                    {
                        if (xIndices[i] >= 0)
                        {
                            int picIndex = j * lonValues.length + i;
                            float val = dataChunk.getValue(xIndices[i] - minX);
                            // We unpack and check for missing values just for 
                            // the points we need to display.
                            float pixel = (float)enhanced.convertScaleOffsetMissing(val);
                            picData[picIndex] = Float.isNaN(pixel) ? fillValue : pixel;
                        }
                    }
                }
            }

            return picData;
        }
        catch(IOException e)
        {
            throw new WMSExceptionInJava("IOException: " + e.getMessage());
        }
        catch(InvalidRangeException ire)
        {
            throw new WMSExceptionInJava("InvalidRangeException: " + ire.getMessage());
        }
        finally
        {
            if (nc != null)
            {
                try
                {
                    nc.close();
                }
                catch (IOException ex)
                {
                    // Ignore this error
                }
            }
        }
    }
    
    /**
     * Finds the index of a certain z value by brute-force search
     * @param zValues Array of values of the z coordinate
     * @param targetVal Value to search for
     * @param zIsPositive True if z values increase upwards
     * @return the z index corresponding with the given targetVal, or -1 if the
     * targetVal does not match (within tolerance) a given axis point
     * @throws InvalidDimensionValueException if targetVal could not be found
     * within zValues
     */
    private static int findZIndex(double[] zValues, String targetVal,
        boolean zIsPositive) throws InvalidDimensionValueException
    {
        try
        {
            float zVal = Float.parseFloat(targetVal);
            if (!zIsPositive)
            {
                zVal = -zVal;
            }
            for (int i = 0; i < zValues.length; i++)
            {
                if (Math.abs((zValues[i] - zVal) / zVal) < 1e-5)
                {
                    return i;
                }
            }
            throw new InvalidDimensionValueException("elevation", targetVal);
        }
        catch(NumberFormatException nfe)
        {
            throw new InvalidDimensionValueException("elevation", targetVal);
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
        float[] data = read(location, varID, "tvalue", "zvalue", latValues, lonValues, Float.NaN);
    }
}
