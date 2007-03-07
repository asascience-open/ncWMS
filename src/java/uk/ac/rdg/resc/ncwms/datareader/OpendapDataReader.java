/*
 * Copyright (c) 2007 The University of Reading
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
import org.apache.log4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.EnhanceScaleMissingImpl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

/**
 * DataReader for OPeNDAP datasets.  This is very similar to the DefaultDataReader
 * except that it's designed to make as few requests to the OPeNDAP server as
 * possible: it downloads a large chunk of data in a single request, then does
 * the subsetting locally.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class OpendapDataReader extends DefaultDataReader
{
    private static final Logger logger = Logger.getLogger(OpendapDataReader.class);
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single time index only.
     *
     * TODO: refactor this: repeats a lot of code in DefaultDataReader
     *
     * @param vm {@link VariableMetadata} object representing the variable
     * @param tIndex The index along the time axis as found in getmap.py
     * @param zIndex The index along the vertical axis (or 0 if there is no vertical axis)
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @throws WMSExceptionInJava if an error occurs
     */
    public float[] read(VariableMetadata vm,
        int tIndex, int zIndex, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava
    {
        try
        {
            // Get the metadata from the cache
            long start = System.currentTimeMillis();
            
            Range tRange = new Range(tIndex, tIndex);
            Range zRange = new Range(zIndex, zIndex);
            
            EnhancedCoordAxis xAxis = vm.getXaxis();
            EnhancedCoordAxis yAxis = vm.getYaxis();
            
            // Create an array to hold the data
            float[] picData = new float[lonValues.length * latValues.length];
            Arrays.fill(picData, fillValue);
            
            // Find the range of x indices
            int minX = -1;
            int maxX = -1;
            int[] xIndices = new int[lonValues.length];
            for (int i = 0; i < lonValues.length; i++)
            {
                xIndices[i] = xAxis.getIndex(new LatLonPointImpl(0.0, lonValues[i]));
                if (xIndices[i] >= 0)
                {
                    if (minX < 0 || xIndices[i] < minX) minX = xIndices[i];
                    if (maxX < 0 || xIndices[i] > maxX) maxX = xIndices[i];
                }
            }
            // TODO: subsample if we are going to read very many more points
            // than we actually need
            if (minX < 0 || maxX < 0)
            {
                // We haven't found any valid data
                return picData;
            }
            logger.debug("xRange = ({},{})", minX, maxX);
            Range xRange = new Range(minX, maxX);
            
            // Find the range of y indices
            int minY = -1;
            int maxY = -1;
            int[] yIndices = new int[latValues.length];
            for (int i = 0; i < latValues.length; i++)
            {
                if (latValues[i] >= -90.0 && latValues[i] <= 90.0)
                {
                    yIndices[i] = yAxis.getIndex(new LatLonPointImpl(latValues[i], 0));
                }
                else
                {
                    yIndices[i] = -1;
                }
                if (yIndices[i] >= 0)
                {
                    if (minY < 0 || yIndices[i] < minY) minY = yIndices[i];
                    if (maxY < 0 || yIndices[i] > maxY) maxY = yIndices[i];
                }
            }
            if (minY < 0 || maxY < 0)
            {
                // We haven't found any valid data
                return picData;
            }
            logger.debug("yRange = ({},{})", minY, maxY);
            Range yRange = new Range(minY, maxY);
            
            long readMetadata = System.currentTimeMillis();
            logger.debug("Read metadata in {} milliseconds", (readMetadata - start));
            
            // Get the dataset from the cache, without enhancing it. We hold
            // the dataset in memory until this DataReader is closed.
            if (this.nc == null)
            {
                this.nc = NetcdfDataset.openDataset(this.location, false, null);
                CoordSysBuilder.addCoordinateSystems(nc, null);
            }
            long openedDS = System.currentTimeMillis();
            logger.debug("Opened NetcdfDataset in {} milliseconds", (openedDS - readMetadata));            
            GridDataset gd = new GridDataset(this.nc);
            GeoGrid gg = gd.findGridByName(vm.getId());
            // Get an enhanced version of the variable for fast reading of data
            EnhanceScaleMissingImpl enhanced = getEnhanced(gg);
            
            // Read the whole chunk of x-y data
            GeoGrid subset = gg.subset(tRange, zRange, yRange, xRange);
            Array arr = subset.readYXData(0,0);
            logger.debug("Rank of arr = {}", arr.getRank());
            for (int i : arr.getShape())
            {
                logger.debug("Dimension size = {}", i);
            }
            DataChunk dataChunk = new DataChunk(arr);
            long readData = System.currentTimeMillis();
            logger.debug("Read data over OPeNDAP in {} milliseconds", (readData - openedDS));
            
            // Now create the picture from the data array
            for (int j = 0; j < yIndices.length; j++)
            {
                if (yIndices[j] >= 0)
                {
                    for (int i = 0; i < xIndices.length; i++)
                    {
                        if (xIndices[i] >= 0)
                        {
                            try
                            {
                                float val = dataChunk.getValue(yIndices[j] - minY, xIndices[i] - minX);
                                int picIndex = j * lonValues.length + i;
                                // We unpack and check for missing values just for
                                // the points we need to display.
                                float pixel = (float)enhanced.convertScaleOffsetMissing(val);
                                picData[picIndex] = Float.isNaN(pixel) ? fillValue : pixel;
                            }
                            catch(ArrayIndexOutOfBoundsException aioobe)
                            {
                                logger.error("Array index ({},{}) out of bounds",
                                    (yIndices[j] - minY), (xIndices[i] - minX));
                                throw aioobe;
                            }
                        }
                    }
                }
            }
            
            long builtPic = System.currentTimeMillis();
            logger.debug("Built picture in {} milliseconds", (builtPic - readData));
            logger.info("Whole read() operation took {} milliseconds", (builtPic - start));
            
            return picData;
        }
        catch(IOException e)
        {
            logger.error("IOException reading from " + nc.getLocation(), e);
            throw new WMSExceptionInJava("IOException: " + e.getMessage());
        }
        catch(InvalidRangeException ire)
        {
            logger.error("InvalidRangeException reading from " + nc.getLocation(), ire);
            throw new WMSExceptionInJava("InvalidRangeException: " + ire.getMessage());
        }
        // We don't close the dataset just yet: we wait till this.close() is called
    }
    
}
