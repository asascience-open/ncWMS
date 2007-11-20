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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.metadata.EnhancedCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.LUTCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;

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
    private static final Logger logger = Logger.getLogger(USGSDataReader.class);
    
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
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @throws Exception if an error occurs
     */
    public float[] read(String filename, Layer layer,
        int tIndex, int zIndex, double[] latValues, double[] lonValues)
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
            float[] picData = new float[lonValues.length * latValues.length];
            Arrays.fill(picData, Float.NaN);
            
            // Maps x and y indices to pixel indices
            PixelMap pixelMap = new PixelMap(layer, latValues, lonValues);
            if (pixelMap.isEmpty()) return picData;
            start = System.currentTimeMillis();
            
            // Now build the picture
            nc = getDataset(filename);
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
            double validMin = var.findAttribute("valid_min").getNumericValue().doubleValue();
            double validMax = var.findAttribute("valid_max").getNumericValue().doubleValue();
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
            
            // Add dummy ranges for x and y
            ranges.add(new Range(0,0));
            ranges.add(new Range(0,0));
            
            // Iterate through the y indices, the order doesn't matter
            for (int yIndex : pixelMap.getYIndices())
            {
                // Set the Ranges to read all the data between x_min and x_max
                // in this row
                ranges.set(yAxisIndex, new Range(yIndex, yIndex));
                int xmin = pixelMap.getMinXIndexInRow(yIndex);
                int xmax = pixelMap.getMaxXIndexInRow(yIndex);
                Range xRange = new Range(xmin, xmax);
                ranges.set(xAxisIndex, xRange);
                
                // Read the scanline from the disk, from the first to the last x index
                Array data = var.read(ranges);
                Object arrObj = data.copyTo1DJavaArray();
                
                for (int xIndex : pixelMap.getXIndices(yIndex))
                {
                    for (int p : pixelMap.getPixelIndices(xIndex, yIndex))
                    {
                        float val;
                        if (arrObj instanceof float[])
                        {
                            val = ((float[])arrObj)[xIndex - xmin];
                        }
                        else
                        {
                            // We assume this is an array of shorts
                            val = ((short[])arrObj)[xIndex - xmin];
                        }
                        // The missing value is calculated based on the compressed,
                        // not the uncompressed, data, despite the fact that it's
                        // recorded as a float
                        if (val != missingValue)
                        {
                            float realVal = addOffset + val * scaleFactor;
                            if (realVal >= validMin && realVal <= validMax)
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
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param filename Full path to the dataset (N.B. not an aggregation)
     * @return List of {@link Layer} objects
     * @throws IOException if there was an error reading from the data source
     */
    protected List<Layer> getLayers(String filename) throws IOException
    {
        logger.debug("Reading metadata for dataset {}", filename);
        List<Layer> layers = new ArrayList<Layer>();
        NetcdfDataset nc = null;
        try
        {
            // We use openDataset() rather than acquiring from cache
            // because we need to enhance the dataset
            nc = NetcdfDataset.openDataset(filename, true, null);
            GridDataset gd = new GridDataset(nc);
            for (Iterator it = gd.getGrids().iterator(); it.hasNext(); )
            {
                GeoGrid gg = (GeoGrid)it.next();
                if (!gg.getName().equals("temp") && !gg.getName().equals("shflux") &&
                    !gg.getName().equals("ssflux") && !gg.getName().equals("latent") &&
                    !gg.getName().equals("sensible") && !gg.getName().equals("lwrad") &&
                    !gg.getName().equals("swrad") && !gg.getName().equals("zeta"))
                {
                    // Only display temperature data for the moment
                    continue;
                }
                GridCoordSys coordSys = gg.getCoordinateSystem();
                logger.debug("Creating new Layer object for {}", gg.getName());
                LayerImpl layer = new LayerImpl();
                layer.setId(gg.getName());
                layer.setTitle(getStandardName(gg.getVariable().getOriginalVariable()));
                layer.setAbstract(gg.getDescription());
                layer.setUnits(gg.getUnitsString());
                layer.setXaxis(LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/LUT_USGS_501_351.zip/LUT_USGS_i_501_351.dat"));
                layer.setYaxis(LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/LUT_USGS_501_351.zip/LUT_USGS_j_501_351.dat"));
                
                if (coordSys.hasVerticalAxis())
                {
                    CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                    layer.setZunits(zAxis.getUnitsString());
                    double[] zVals = zAxis.getCoordValues();
                    layer.setZpositive(false);
                    layer.setZvalues(zVals);
                }
                
                // Set the bounding box
                // TODO: should take into account the cell bounds
                LatLonRect latLonRect = coordSys.getLatLonBoundingBox();
                LatLonPoint lowerLeft = latLonRect.getLowerLeftPoint();
                LatLonPoint upperRight = latLonRect.getUpperRightPoint();
                double minLon = lowerLeft.getLongitude();
                double maxLon = upperRight.getLongitude();
                double minLat = lowerLeft.getLatitude();
                double maxLat = upperRight.getLatitude();
                if (latLonRect.crossDateline())
                {
                    minLon = -180.0;
                    maxLon = 180.0;
                }
                layer.setBbox(new double[]{minLon, minLat, maxLon, maxLat});
                
                // Now add the timestep information to the VM object
                Date[] tVals = this.getTimesteps(nc, gg);
                for (int i = 0; i < tVals.length; i++)
                {
                    TimestepInfo tInfo = new TimestepInfo(tVals[i], filename, i);
                    layer.addTimestepInfo(tInfo);
                }
                // Add this to the Hashtable
                layers.add(layer);
            }
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
        return layers;
    }
}
    
class Scanline
{
    // Maps x indices to a collection of pixel indices
    //             x         pixels
    private Map<Integer, List<Integer>> xIndices;

    public Scanline()
    {
        this.xIndices = new HashMap<Integer, List<Integer>>();
    }

    /**
     * @param xIndex The x index of the point in the source data
     * @param pixelIndex The index of the corresponding point in the picture
     */
    public void put(int xIndex, int pixelIndex)
    {
        List<Integer> pixelIndices = this.xIndices.get(xIndex);
        if (pixelIndices == null)
        {
            pixelIndices = new ArrayList<Integer>();
            this.xIndices.put(xIndex, pixelIndices);
        }
        pixelIndices.add(pixelIndex);
    }

    /**
     * @return a Vector of all the x indices in this scanline, sorted in
     * ascending order
     */
    public List<Integer> getSortedXIndices()
    {
        List<Integer> v = new ArrayList<Integer>(this.xIndices.keySet());
        Collections.sort(v);
        return v;
    }

    /**
     * @return a Vector of all the pixel indices that correspond to the
     * given x index, or null if the x index does not exist in the scanline
     */
    public List<Integer> getPixelIndices(int xIndex)
    {
        return this.xIndices.get(xIndex);
    }
}
