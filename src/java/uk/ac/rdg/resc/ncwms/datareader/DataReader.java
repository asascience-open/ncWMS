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
import java.util.Date;
import java.util.Hashtable;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonPointImpl;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.MissingDimensionValueException;
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
    private static DateFormatter dateFormatter = new DateFormatter();
    
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
            DatasetCache ds = DatasetCache.acquire(location);
            Hashtable<String, VariableMetadata> vars = ds.getVariableMetadata();
            if (!vars.containsKey(varID))
            {
                throw new WMSExceptionInJava("Could not find variable called "
                    + varID + " in " + location);
            }
            // TODO: check for a lat-lon coordinate system
            VariableMetadata vm = vars.get(varID);
            
            // Find the index along the time axis
            int tIndex = 0;
            if (vm.getTvalues() != null)
            {
                if (tValue == null || tValue.trim().equals(""))
                {
                    throw new MissingDimensionValueException("time");
                }
                tIndex = findTIndex(vm.getTvalues(), tValue);
            }
            Range tRange = new Range(tIndex, tIndex);
            
            // Find the index along the depth axis
            int zIndex = 0; // Default value of z is the first in the axis
            if (zValue != null && !zValue.equals("") && vm.getZvalues() != null)
            {
                zIndex = findZIndex(vm.getZvalues(), zValue);
            }
            Range zRange = new Range(zIndex, zIndex);
            
            // Find the range of x indices
            int minX = Integer.MAX_VALUE;
            int maxX = -Integer.MAX_VALUE;
            int[] xIndices = new int[lonValues.length];
            for (int i = 0; i < lonValues.length; i++)
            {
                xIndices[i] = vm.getXaxis().getIndex(new LatLonPointImpl(0.0, lonValues[i]));
                if (xIndices[i] >= 0)
                {
                    if (xIndices[i] < minX) minX = xIndices[i];
                    if (xIndices[i] > maxX) maxX = xIndices[i];
                }
            }
            // TODO: subsample if we are going to read very many more points
            // than we actually need
            Range xRange = new Range(minX, maxX);
            // Create an array to hold the data
            float[] picData = new float[lonValues.length * latValues.length];
            Arrays.fill(picData, fillValue);
            
            // Open the data file
            nc = NetcdfDataset.openDataset(location, false, null);
            Variable var = nc.findVariable(varID);
            if (var == null)
            {
                throw new WMSExceptionInJava("Could not find variable called "
                    + varID + " in " + location);
            }
            // Get an enhanced version of the variable for fast reading of data
            EnhanceScaleMissingImpl enhanced = new EnhanceScaleMissingImpl((VariableDS)var);
            
            DataChunk dataChunk;
            // Cycle through the latitude values, extracting a scanline of
            // data each time from minX to maxX
            for (int j = 0; j < latValues.length; j++)
            {
                int yIndex = vm.getYaxis().getIndex(new LatLonPointImpl(latValues[j], 0.0));
                if (yIndex >= 0)
                {
                    Range yRange = new Range(yIndex, yIndex);
                    // Read a chunk of data - values will not be unpacked or
                    // checked for missing values yet
                    Array arr = var.read(vm.getRangesList(tRange, zRange, yRange, xRange)).reduce();
                    dataChunk = new DataChunk(arr);
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
     * Finds the index of a certain t value by binary search (the axis may be
     * very long, so a brute-force search is inappropriate)
     * @param tValues Array of floats representing the t axis values in
     * seconds since the epoch
     * @param tValue Date to search for as an ISO8601-formatted String
     * @return the t index corresponding with the given targetVal
     * @throws InvalidDimensionValueException if targetVal could not be found
     * within tValues
     * @todo almost repeats code in {@link Irregular1DCoordAxis} - refactor?
     */
    private static int findTIndex(float[] tValues, String tValue)
        throws InvalidDimensionValueException
    {
        Date d = dateFormatter.getISODate(tValue);
        if (d == null)
        {
            throw new InvalidDimensionValueException("time", tValue);
        }
        float target = d.getTime() / 1000.0f;
        
        // Check that the point is within range
        if (target < tValues[0] || target > tValues[tValues.length - 1])
        {
            throw new InvalidDimensionValueException("time", tValue);
        }
        
        // do a binary search to find the nearest index
        int low = 0;
        int high = tValues.length - 1;
        while (low <= high)
        {
            int mid = (low + high) >> 1;
            float midVal = tValues[mid];
            if (midVal == target)
            {
                return mid;
            }
            else if (midVal < target)
            {
                low = mid + 1;
            }
            else if (midVal > target)
            {
                high = mid - 1;
            }
        }
        
        // If we've got this far we have to decide between values[low]
        // and values[high]
        if (tValues[low] == target)
        {
            return low;
        }
        else if (tValues[high] == target)
        {
            return high;
        }
        throw new InvalidDimensionValueException("time", tValue);
    }
        
    /**
     * Finds the index of a certain z value by brute-force search.  We can afford
     * to be inefficient here because z axes are not likely to be large.
     * @param zValues Array of values of the z coordinate
     * @param targetVal Value to search for
     * @return the z index corresponding with the given targetVal
     * @throws InvalidDimensionValueException if targetVal could not be found
     * within zValues
     */
    private static int findZIndex(double[] zValues, String targetVal)
        throws InvalidDimensionValueException
    {
        try
        {
            float zVal = Float.parseFloat(targetVal);
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
