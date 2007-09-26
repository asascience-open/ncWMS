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
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.EnhanceScaleMissingImpl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.dataset.grid.GridDataset;
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
        int tIndex, int zIndex, float[] latValues, float[] lonValues)
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
            GridDataset gd = new GridDataset(nc);
            logger.debug("Getting GeoGrid with id {}", layer.getId());
            GeoGrid gg = gd.findGridByName(layer.getId());
            logger.debug("filename = {}, gg = " + gg, filename);
            
            long before = System.currentTimeMillis();
            this.populatePixelArray(picData, tRange, zRange, pixelMap, gg);
            long after = System.currentTimeMillis();
            benchmarkLogger.info(this.getClass().getName() + "\t" + (after - before));
            
            long builtPic = System.currentTimeMillis();
            logger.debug("Built picture array in {} milliseconds", (builtPic - readMetadata));
            logger.info("Whole read() operation took {} milliseconds", (builtPic - start));
            
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
        PixelMap pixelMap, GeoGrid gg) throws Exception
    {
        // Get an enhanced version of the variable for conversion of data
        EnhanceScaleMissingImpl enhanced = getEnhanced(gg);
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
            GeoGrid subset = gg.subset(tRange, zRange, yRange, xRange);
            dataChunk = new DataChunk(subset.readYXData(0,0).reduce());

            // Now copy the scanline's data to the picture array
            for (int xIndex : pixelMap.getXIndices(yIndex))
            {
                float val = dataChunk.getValue(xIndex - xmin);
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
     * @return enhanced version of the given GeoGrid
     */
    protected static EnhanceScaleMissingImpl getEnhanced(GeoGrid gg)
    {
        return new EnhanceScaleMissingImpl((VariableDS)gg.getVariable());
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
        logger.debug("Reading metadata for file {}", filename);
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
                GridCoordSys coordSys = gg.getCoordinateSystem();
                logger.debug("Creating new Layer object for {}", gg.getName());
                LayerImpl layer = new LayerImpl();
                layer.setId(gg.getName());
                layer.setTitle(getStandardName(gg.getVariable().getOriginalVariable()));
                layer.setAbstract(gg.getDescription());
                layer.setUnits(gg.getUnitsString());
                layer.setXaxis(EnhancedCoordAxis.create(coordSys.getXHorizAxis()));
                layer.setYaxis(EnhancedCoordAxis.create(coordSys.getYHorizAxis()));

                if (coordSys.hasVerticalAxis())
                {
                    CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                    layer.setZunits(zAxis.getUnitsString());
                    double[] zVals = zAxis.getCoordValues();
                    layer.setZpositive(coordSys.isZPositive());
                    if (coordSys.isZPositive())
                    {
                        layer.setZvalues(zVals);
                    }
                    else
                    {
                        double[] zVals2 = new double[zVals.length];
                        for (int i = 0; i < zVals.length; i++)
                        {
                            zVals2[i] = 0.0 - zVals[i];
                        }
                        layer.setZvalues(zVals2);
                    }
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
                
                layer.setValidMin(gg.getVariable().getValidMin());
                layer.setValidMax(gg.getVariable().getValidMax());

                // Now add the timestep information to the VM object
                Date[] tVals = this.getTimesteps(nc, gg);
                for (int i = 0; i < tVals.length; i++)
                {
                    TimestepInfo tInfo = new TimestepInfo(tVals[i], filename, i);
                    layer.addTimestepInfo(tInfo);
                }
                // Add this layer to the List
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
    
    /**
     * Gets array of Dates representing the timesteps of the given variable.
     * @param nc The NetcdfDataset to which the variable belongs
     * @param gg the variable as a GeoGrid
     * @return Array of {@link Date}s
     * @throws IOException if there was an error reading the timesteps data
     */
    protected Date[] getTimesteps(NetcdfDataset nc, GeoGrid gg)
        throws IOException
    {
        GridCoordSys coordSys = gg.getCoordinateSystem();
        if (coordSys.isDate())
        {
            return coordSys.getTimeDates();
        }
        else
        {
            return new Date[]{};
        }
    }
    
    /**
     * @return the value of the standard_name attribute of the variable,
     * or the unique id if it does not exist
     */
    protected static String getStandardName(Variable var)
    {
        Attribute stdNameAtt = var.findAttributeIgnoreCase("standard_name");
        return (stdNameAtt == null || stdNameAtt.getStringValue().trim().equals(""))
            ? var.getName() : stdNameAtt.getStringValue();
    }
    
}
