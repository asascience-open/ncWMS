/*/*
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.units.DateUnit;
import uk.ac.rdg.resc.ncwms.metadata.LUTCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;

/**
 * Description of NemoDataReader
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NemoDataReader extends DefaultDataReader
{
    private static final Logger logger = LoggerFactory.getLogger(NemoDataReader.class);
    
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
        logger.debug("Reading data from {}", filename);
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

            // Now build the picture array.  We don't need any of the enhancements
            // but we want to use the file cache if possible
            nc = NetcdfDataset.acquireDataset(null, filename, EnumSet.noneOf(Enhance.class), -1, null, null);
            Variable var = nc.findVariable(layer.getId());
            logger.debug("Is var object for layer {} null? {}", layer.getId(), var == null);
            
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
            float missingValue = var.findAttribute("missing_value").getNumericValue().floatValue();
            // TODO: should check these values exist
  
            double validMin = var.findAttribute("valid_min").getNumericValue().doubleValue();
            double validMax = var.findAttribute("valid_max").getNumericValue().doubleValue();
           
            logger.debug("Valid Min/Max :" + validMin + "::" +validMax);
            
            if (validMin > validMax)
            {
                 validMin = -1 * validMin;
                 validMax = -1 * validMax;
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
        
            // Add dummy ranges for x and y
            ranges.add(new Range(0,0));
            ranges.add(new Range(0,0));
            
            // Iterate through the scanlines, the order doesn't matter
            for (int j : pixelMap.getJIndices())
            {
                ranges.set(yAxisIndex, new Range(j, j));
                int imin = pixelMap.getMinIIndexInRow(j);
                int imax = pixelMap.getMaxIIndexInRow(j);
                Range xRange = new Range(imin, imax);
                
                ranges.set(xAxisIndex, xRange);

                // Read the scanline from the disk, from the first to the last x index
                Array data = var.read(ranges);
                Object arrObj = data.copyTo1DJavaArray();
                
                for (int i : pixelMap.getIIndices(j))
                {
                    float val;
                    if (arrObj instanceof float[])
                    {
                        val = ((float[])arrObj)[i - imin];
                    }
                    else
                    {
                        // We assume this is an array of shorts
                        val = ((short[])arrObj)[i - imin];
                    }
                    // The missing value is calculated based on the compressed,
                    // not the uncompressed, data, despite the fact that it's
                    // recorded as a float
                    if (val != missingValue)
                    {
                        float realVal = addOffset + val * scaleFactor;
                        if (realVal >= validMin && realVal <= validMax)
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
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the dataset (N.B. not an aggregation)
     * @throws IOException if there was an error reading from the data source
     */
    @Override
    protected void findAndUpdateLayers(String location, Map<String, LayerImpl> layers)
        throws IOException
    {
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset.  We use the dataset cache here for the following
            // reason: if the cache has swallowed all of our available file handles,
            // we can't bypass the cache as there will be no file handles left.
            nc = NetcdfDataset.acquireDataset(location, null);
            
            // Get the depth values and units
            Variable depth = nc.findVariable("deptht");
            float[] fzVals = (float[])depth.read().copyTo1DJavaArray();
            // Copy to an array of doubles
            double[] zVals = new double[fzVals.length];
            for (int i = 0; i < fzVals.length; i++)
            {
                zVals[i] = -fzVals[i];
            }
            String zUnits = depth.getUnitsString();
            
            // Get the time values and units
            Variable time = nc.findVariable("time_counter");
            float[] ftVals = (float[])time.read().copyTo1DJavaArray();
            DateUnit dateUnit = null;
            try
            {
                dateUnit = new DateUnit(time.getUnitsString());
            }
            catch(Exception e)
            {
                // Shouldn't happen if file is well formed
                logger.error("Malformed time units string " + time.getUnitsString());
                // IOException not ideal here but didn't want to create new exception
                // type just for this rare case
                throw new IOException("Malformed time units string " + time.getUnitsString());
            }

            for (Variable var: nc.getVariables())
            {
                // We ignore the coordinate axes
                if (!var.getName().equals("nav_lon") && !var.getName().equals("nav_lat")
                    && !var.getName().equals("deptht") && !var.getName().equals("time_counter"))
                {
                    LayerImpl layer;
                    if (layers.containsKey(var.getName()))
                    {
                        // This variable already exists in the map.  We don't need to
                        // create a new one.  We'll just update the timesteps later.
                        layer = (LayerImpl)layers.get(var.getName());
                    }
                    else
                    {
                        layer = new LayerImpl();
                        layer.setId(var.getName());
                        layer.setAbstract(var.getDescription());
                        layer.setTitle(var.getDescription()); // TODO: standard_names are not set: set these in NcML?
                        layer.setUnits(var.getUnitsString());
                        layer.setZpositive(false);
                        // TODO: check for the presence of a z axis in a neater way
                        if (var.getRank() == 4)
                        {
                            layer.setZvalues(zVals);
                            layer.setZunits(zUnits);
                        }

                        // Create the coordinate axes
                        if (nc.findGlobalAttributeIgnoreCase("resolution") == null)
                        {
                            throw new IOException("NEMO datasets must have a \"resolution\" attribute");
                        }
                        String res = nc.findGlobalAttributeIgnoreCase("resolution").getStringValue();
                        if (res.equals("one_degree"))
                        {
                            layer.setXaxis(LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/ORCA1_4x4.zip/ORCA1_ilt_4x4.dat", AxisType.GeoX));
                            layer.setYaxis(LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/ORCA1_4x4.zip/ORCA1_jlt_4x4.dat", AxisType.GeoY));
                        }
                        else
                        {
                            layer.setXaxis(LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/ORCA025_12x12.zip/ORCA025_ilt_12x12_new.dat", AxisType.GeoX));
                            layer.setYaxis(LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/ORCA025_12x12.zip/ORCA025_jlt_12x12_new.dat", AxisType.GeoY));
                        }
                        
                        // Add this new layer to the map
                        layers.put(layer.getId(), layer);
                    }
                    
                    // Set the time axis
                    for (int i = 0; i < ftVals.length; i++)
                    {
                        Date timestep = dateUnit.makeDate(ftVals[i]);
                        layer.addTimestepInfo(new TimestepInfo(new DateTime(timestep), location, i));
                    }
                }
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
    }
}

