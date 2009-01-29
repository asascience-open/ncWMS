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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.ncwms.metadata.CoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.LUTCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;

/**
 * DataReader for Rich Signell's example data
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class USGSDataReader extends DefaultDataReader
{
    private static final Logger logger = LoggerFactory.getLogger(USGSDataReader.class);
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single timestep only.  This method knows
     * nothing about aggregation: it simply reads data from the given file. 
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by Float.NaN.
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param grid The grid onto which the data are to be read
     * @throws Exception if an error occurs
     */
    @Override
    public float[] read(String filename, Layer layer, int tIndex, int zIndex, HorizontalGrid grid)
        throws Exception
    {
        NetcdfDataset nc = null;
        try
        {
            // Get the metadata from the cache
            long start = System.currentTimeMillis();
            
            // Prevent InvalidRangeExceptions for ranges we're not going to use anyway
            if (tIndex < 0) tIndex = 0;
            if (zIndex < 0) zIndex = 0;
            Range tRange = new Range(tIndex, tIndex);
            Range zRange = new Range(zIndex, zIndex);
            
            // Create an array to hold the data
            float[] picData = new float[grid.getSize()];
            Arrays.fill(picData, Float.NaN);
            
            // Maps x and y indices to pixel indices
            PixelMap pixelMap = new PixelMap(layer, grid);
            if (pixelMap.isEmpty()) return picData;
            start = System.currentTimeMillis();
            
            // Now build the picture.  We don't need to enhance the dataset but
            // we do want to use the cache if possible
            nc = NetcdfDataset.acquireDataset(null, filename, EnumSet.noneOf(Enhance.class), -1, null, null);
            Variable var = nc.findVariable(layer.getId());
            
            float scaleFactor = 1.0f;
            float addOffset = 0.0f;
            if (var.findAttribute("scale_factor") != null)
            {
                scaleFactor = var.findAttribute("scale_factor").getNumericValue().floatValue();
            }
            if (var.findAttribute("add_offset") != null)
            {
                addOffset = var.findAttribute("add_offset").getNumericValue().floatValue();
            }
            float missingValue = Float.NaN;
            if (var.findAttribute("missing_value") != null)
            {
                missingValue = var.findAttribute("missing_value").getNumericValue().floatValue();
            }// TODO: should check these values exist
            double validMin = Double.NaN;
            double validMax = Double.NaN;
            if (var.findAttribute("valid_min") != null && var.findAttribute("valid_max") != null)
            {
                validMin = var.findAttribute("valid_min").getNumericValue().doubleValue();
                validMax = var.findAttribute("valid_max").getNumericValue().doubleValue();
            }
            logger.debug("Scale factor: {}, add offset: {}", scaleFactor, addOffset);
            
            int yAxisIndex = 1;
            int xAxisIndex = 2;
            List<Range> ranges = new ArrayList<Range>();
            ranges.add(tRange);
            // TODO: logic is fragile here
            if (var.getRank() == 4)
            {
                ranges.add(zRange);
                yAxisIndex = 2;
                xAxisIndex = 3;
            }
            
            // Read the whole chunk of x-y data (cf. BoundingBoxDataReader)
            ranges.add(new Range(pixelMap.getMinJIndex(), pixelMap.getMaxJIndex()));
            ranges.add(new Range(pixelMap.getMinIIndex(), pixelMap.getMaxIIndex()));
            int iSize = pixelMap.getMaxIIndex() - pixelMap.getMinIIndex() + 1;
            
            // Read the data from disk
            Array data = var.read(ranges);
            Object arrObj = data.copyTo1DJavaArray();
            
            // Copy the data to the image array
            for (int j : pixelMap.getJIndices())
            {
                int jIndex = j - pixelMap.getMinJIndex();
                for (int i : pixelMap.getIIndices(j))
                {
                    int iIndex = i - pixelMap.getMinIIndex();
                    int indexInArr = jIndex * iSize + iIndex;
                    float val;
                    if (arrObj instanceof float[])
                    {
                        val = ((float[])arrObj)[indexInArr];
                    }
                    else
                    {
                        // We assume this is an array of shorts
                        val = ((short[])arrObj)[indexInArr];
                    }
                    // The missing value is calculated based on the compressed,
                    // not the uncompressed, data, despite the fact that it's
                    // recorded as a float
                    if (val != missingValue)
                    {
                        float realVal = addOffset + val * scaleFactor;
                        if (Double.isNaN(validMin) || Double.isNaN(validMax) ||
                            (realVal >= validMin && realVal <= validMax))
                        {
                            for (int p : pixelMap.getPixelIndices(i, j))
                            {
                                picData[p] = realVal;
                            }
                        }
                    }
                }
            }
            logger.debug("Read data in {} ms", System.currentTimeMillis() - start);
            return picData;
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
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
    
    /**
     * This is used to filter out certain fields from the dataset
     */
    @Override
    protected boolean includeGrid(GridDatatype grid)
    {
        return grid.getName().equals("temp") || grid.getName().equals("shflux") ||
               grid.getName().equals("ssflux") || grid.getName().equals("latent") ||
               grid.getName().equals("sensible") || grid.getName().equals("lwrad") ||
               grid.getName().equals("swrad") || grid.getName().equals("zeta");
    }
    
    /**
     * @return a {@link LUTCoordAxis}, since the axes in this dataset
     * are curvilinear.
     */
    @Override
    protected CoordAxis getXAxis(GridCoordSystem coordSys) throws IOException
    {
        return LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/LUT_USGS_501_351.zip/LUT_USGS_i_501_351.dat", AxisType.GeoX);
    }
    
    /**
     * @return a {@link LUTCoordAxis}, since the axes in this dataset
     * are curvilinear.
     */
    @Override
    protected CoordAxis getYAxis(GridCoordSystem coordSys) throws IOException
    {
        return LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/LUT_USGS_501_351.zip/LUT_USGS_j_501_351.dat", AxisType.GeoY);
    }
    
    /**
     * @return false: the z axis is never positive for this dataset
     */
    @Override
    protected boolean isZPositive(GridCoordSystem coordSys)
    {
        return false;
    }
    
    /**
     * @return the values on the z axis, obtained directly from 
     * {@code zAxis.getCoordValues()}.
     */
    @Override
    protected double[] getZValues(CoordinateAxis1D zAxis, boolean zPositive)
    {
        return zAxis == null ? null : zAxis.getCoordValues();
    }
}