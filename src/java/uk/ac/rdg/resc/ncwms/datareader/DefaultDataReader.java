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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.log4j.Logger;
import thredds.catalog.DataType;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.metadata.EnhancedCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;

/**
 * Default data reading class for CF-compliant NetCDF datasets.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class DefaultDataReader extends DataReader
{
    private static final Logger logger = Logger.getLogger(DefaultDataReader.class);
    // We'll use this logger to output performance information
    private static final Logger benchmarkLogger = Logger.getLogger("ncwms.benchmark");
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single timestep only.  This method knows
     * nothing about aggregation: it simply reads data from the given file. 
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by Float.NaN.
     * 
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable to read
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
            long start = System.currentTimeMillis();
            
            logger.debug("filename = {}, tIndex = {}, zIndex = {}",
                new Object[]{filename, tIndex, zIndex});
            // Prevent InvalidRangeExceptions for ranges we're not going to use anyway
            if (tIndex < 0) tIndex = 0;
            if (zIndex < 0) zIndex = 0;
            Range tRange = new Range(tIndex, tIndex);
            Range zRange = new Range(zIndex, zIndex);
            
            // Create an array to hold the data
            float[] picData = new float[lonValues.length * latValues.length];
            // Use NaNs to represent missing data
            Arrays.fill(picData, Float.NaN);
            
            PixelMap pixelMap = new PixelMap(layer, latValues, lonValues);
            if (pixelMap.isEmpty()) return picData;
            
            long readMetadata = System.currentTimeMillis();
            logger.debug("Read metadata in {} milliseconds", (readMetadata - start));
            
            // Get the dataset from the cache, without enhancing it
            nc = getDataset(filename);
            long openedDS = System.currentTimeMillis();
            logger.debug("Opened NetcdfDataset in {} milliseconds", (openedDS - readMetadata));
            // Get a GridDataset object, since we know this is a grid
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(DataType.GRID, nc, null, null);
            
            logger.debug("Getting GeoGrid with id {}", layer.getId());
            GridDatatype grid = gd.findGridDatatype(layer.getId());
            logger.debug("filename = {}, gg = {}", filename, grid.toString());
            
            // Read the data from the dataset
            long before = System.currentTimeMillis();
            // Get an enhanced variable for doing the conversion of missing
            // values
            VariableDS enhanced = new VariableDS(null, nc.findVariable(layer.getId()), true);
            this.populatePixelArray(picData, tRange, zRange, pixelMap, grid, enhanced);
            long after = System.currentTimeMillis();
            // Headings are written in NcwmsContext.init()
            if (pixelMap.getNumUniqueXYPairs() > 1)
            {
                // Don't log single-pixel (GetFeatureInfo) requests
                benchmarkLogger.info
                (
                    layer.getDataset().getId() + "," +
                    layer.getId() + "," +
                    this.getClass().getSimpleName() + "," +
                    (latValues.length * lonValues.length) + "," +
                    pixelMap.getNumUniqueXYPairs() + "," +
                    pixelMap.getSumRowLengths() + "," +
                    pixelMap.getBoundingBoxSize() + "," +
                    (after - before)
                );
            }
            
            long builtPic = System.currentTimeMillis();
            logger.debug("Built picture array in {} milliseconds", (builtPic - readMetadata));
            logger.debug("Whole read() operation took {} milliseconds", (builtPic - start));
            
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
     * Reads data from the given GeoGrid and populates the given pixel array.
     * This uses a scanline-based algorithm: subclasses can override this to
     * use alternative strategies, e.g. point-by-point or bounding box
     */
    protected void populatePixelArray(float[] picData, Range tRange, Range zRange,
        PixelMap pixelMap, GridDatatype grid, VariableEnhanced enhanced) throws Exception
    {
        DataChunk dataChunk = null;
        // Cycle through the latitude values, extracting a scanline of
        // data each time from minX to maxX
        for (int yIndex : pixelMap.getYIndices())
        {
            Range yRange = new Range(yIndex, yIndex);
            // Read a row of data from the source
            int xmin = pixelMap.getMinXIndexInRow(yIndex);
            int xmax = pixelMap.getMaxXIndexInRow(yIndex);
            Range xRange = new Range(xmin, xmax);
            // Read a chunk of data - values will not be unpacked or
            // checked for missing values yet
            GridDatatype subset = grid.makeSubset(null, null, tRange, zRange, yRange, xRange);
            // Read all of the x-y data in this subset
            dataChunk = new DataChunk(subset.readDataSlice(0, 0, -1, -1).reduce());
            
            // Now copy the scanline's data to the picture array
            for (int xIndex : pixelMap.getXIndices(yIndex))
            {
                float val = dataChunk.getValue(xIndex - xmin);
                if (picData.length == 100) logger.debug("val = {}", val);
                // We unpack and check for missing values just for
                // the points we need to display.
                val = (float)enhanced.convertScaleOffsetMissing(val);
                // Now we set the value of all the image pixels associated with
                // this data point.
                for (int p : pixelMap.getPixelIndices(xIndex, yIndex))
                {
                    picData[p] = val;
                }
            }
        }
    }
    
    /**
     * Gets the {@link NetcdfDataset} at the given location from the cache.
     * @throws IOException if there was an error opening the dataset
     */
    protected static synchronized NetcdfDataset getDataset(String location)
        throws IOException
    {
        NetcdfDataset nc = NetcdfDatasetCache.acquire(location, null, DatasetFactory.get());
        logger.debug("Returning NetcdfDataset at {} from cache", location);
        return nc;
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
        logger.debug("Getting layers in file {}", filename);
        List<Layer> layers = new ArrayList<Layer>();
        
        NetcdfDataset nc = null;
        try
        {
            // We use openDataset() rather than acquiring from cache
            // because we need to enhance the dataset
            nc = NetcdfDataset.openDataset(filename, true, null);
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(DataType.GRID, nc, null, null);
            
            // Search through all coordinate systems, creating appropriate metadata
            // for each.  This allows metadata objects to be shared among Layer objects,
            // saving memory.
            for (Gridset gridset : gd.getGridsets())
            {
                GridCoordSystem coordSys = gridset.getGeoCoordSystem();
                
                EnhancedCoordAxis xAxis = EnhancedCoordAxis.create(coordSys.getXHorizAxis());
                EnhancedCoordAxis yAxis = EnhancedCoordAxis.create(coordSys.getYHorizAxis());
                
                CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                double[] zValues = null;
                if (zAxis != null)
                {
                    zValues = zAxis.getCoordValues();
                    if (!coordSys.isZPositive())
                    {
                        double[] zVals = new double[zValues.length];
                        for (int i = 0; i < zVals.length; i++)
                        {
                            zVals[i] = 0.0 - zValues[i];
                        }
                        zValues = zVals;
                    }
                }
                
                // Now compute TimestepInfo objects for this file
                List<TimestepInfo> timesteps = getTimesteps(filename, coordSys);
                
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
                double[] bbox = new double[]{minLon, minLat, maxLon, maxLat};
                
                // Now add every variable that has this coordinate system
                for (GridDatatype grid : gridset.getGrids())
                {
                    logger.debug("Creating new Layer object for {}", grid.getName());
                    LayerImpl layer = new LayerImpl();
                    layer.setId(grid.getName());
                    layer.setTitle(getStandardName(grid.getVariable()));
                    layer.setAbstract(grid.getDescription());
                    layer.setUnits(grid.getUnitsString());
                    layer.setXaxis(xAxis);
                    layer.setYaxis(yAxis);
                    layer.setBbox(bbox);
                    
                    if (zAxis != null)
                    {
                        layer.setZunits(zAxis.getUnitsString());
                        layer.setZpositive(coordSys.isZPositive());
                        layer.setZvalues(zValues);
                    }
                    
                    for (TimestepInfo timestep : timesteps)
                    {
                        layer.addTimestepInfo(timestep);
                    }
                    
                    // Add this layer to the List
                    layers.add(layer);
                }
            }
        }
        finally
        {
            logger.debug("In finally clause");
            if (nc != null)
            {
                try
                {
                    logger.debug("Closing NetCDF file");
                    nc.close();
                    logger.debug("NetCDF file closed");
                }
                catch (IOException ex)
                {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
        logger.debug("Found {} layers in {}", layers.size(), filename);
        return layers;
    }
    
    /**
     * Gets array of Dates representing the timesteps of the given coordinate system.
     * @param filename The name of the file/dataset to which the coord sys belongs
     * @param coordSys The coordinate system containing the time information
     * @return List of TimestepInfo objects
     * @throws IOException if there was an error reading the timesteps data
     */
    protected static List<TimestepInfo> getTimesteps(String filename, GridCoordSystem coordSys)
        throws IOException
    {
        List<TimestepInfo> timesteps = new ArrayList<TimestepInfo>();
        if (coordSys.hasTimeAxis1D())
        {
            Date[] dates = coordSys.getTimeAxis1D().getTimeDates();
            for (int i = 0; i < dates.length; i++)
            {
                TimestepInfo tInfo = new TimestepInfo(dates[i], filename, i);
                timesteps.add(tInfo);
            }
        }
        return timesteps;
    }
    
    /**
     * @return the value of the standard_name attribute of the variable,
     * or the unique id if it does not exist
     */
    protected static String getStandardName(VariableEnhanced var)
    {
        Attribute stdNameAtt = var.findAttributeIgnoreCase("standard_name");
        return (stdNameAtt == null || stdNameAtt.getStringValue().trim().equals(""))
            ? var.getName() : stdNameAtt.getStringValue();
    }
    
}
