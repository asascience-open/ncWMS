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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

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
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataReader.class);
    // We'll use this logger to output performance information
    private static final Logger benchmarkLogger = LoggerFactory.getLogger("ncwms.benchmark");

    /**
     * Reads data from a NetCDF file.  Reads data for a single timestep only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by Float.NaN.
     *
     * <p>The actual reading of data is performed in {@link #populatePixelArray
     * populatePixelArray()}</p>
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param pointList The list of real-world x-y points for which we need data.
     * In the case of a GetMap operation this will usually be a {@link HorizontalGrid}.
     * @return an array of floating-point data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws Exception if an error occurs
     */
    public float[] read(String filename, Layer layer, int tIndex, int zIndex,
        PointList pointList) throws Exception
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
            float[] picData = new float[pointList.size()];
            // Use NaNs to represent missing data
            Arrays.fill(picData, Float.NaN);
            
            PixelMap pixelMap = new PixelMap(layer.getHorizontalCoordSys(), pointList);
            if (pixelMap.isEmpty()) return picData;
            
            long readMetadata = System.currentTimeMillis();
            logger.debug("Read metadata in {} milliseconds", (readMetadata - start));
            
            // Get the dataset, from the cache if possible
            nc = NetcdfDataset.acquireDataset(
                null, // Use the default factory
                filename,
                // Read the coordinate systems but don't automatically process
                // scale/missing/offset when reading data, for efficiency reasons
                EnumSet.of(Enhance.ScaleMissingDefer, Enhance.CoordSystems),
                -1, // use default buffer size
                null, // no CancelTask
                null // no iospMessage
            );
            long openedDS = System.currentTimeMillis();
            logger.debug("Opened NetcdfDataset in {} milliseconds", (openedDS - readMetadata));
            // Get a GridDataset object, since we know this is a grid
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(FeatureType.GRID, nc, null, null);
            
            logger.debug("Getting GeoGrid with id {}", layer.getId());
            GridDatatype gridData = gd.findGridDatatype(layer.getId());
            logger.debug("filename = {}, gg = {}", filename, gridData.toString());
            
            // Read the data from the dataset
            long before = System.currentTimeMillis();
            // Decide on which strategy to use for reading data from the source
            String fileType = nc.getFileTypeId();
            // If data are local and uncompressed then it's relatively cheap to
            // make many small reads of data to save memory.  If data are remote
            // or compressed, it's generally more efficient to read data in a
            // single operation, even if the memory footprint is larger.
            DataReadingStrategy drStrategy = fileType.equals("netCDF") || fileType.equals("HDF4")
                ? DataReadingStrategy.SCANLINE
                : DataReadingStrategy.BOUNDING_BOX;
            drStrategy.populatePixelArray(picData, tRange, zRange, pixelMap, gridData);
            long after = System.currentTimeMillis();

            // Write to the benchmark logger (if enabled in log4j.properties)
            // Headings are written in NcwmsContext.init()
            if (pixelMap.getNumUniqueIJPairs() > 1)
            {
                // Don't log single-pixel (GetFeatureInfo) requests
                benchmarkLogger.info
                (
                    layer.getDataset().getId() + "," +
                    layer.getId() + "," +
                    this.getClass().getSimpleName() + "," +
                    pointList.size() + "," +
                    pixelMap.getNumUniqueIJPairs() + "," +
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
     * Reads the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the dataset. This will be passed to 
     * {@link NetcdfDataset#openDataset}.
     * @param layers Map of Layer Ids to LayerImpl objects to populate or update
     * @param progressMonitor A {@link ProgressMonitor} that can be updated
     * with updates on the progress with loading the metadata.  Can be null.
     * @throws Exception if there was an error reading from the data source
     */
    protected void findAndUpdateLayers(String location,
        Map<String, LayerImpl> layers) throws Exception
    {
        logger.debug("Finding layers in {}", location);
        
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset, using the cache if appropriate
            nc = WmsUtils.openDataset(location);
            GridDataset gd = (GridDataset)TypedDatasetFactory.open(FeatureType.GRID,
                nc, null, null);
            
            // Search through all coordinate systems, creating appropriate metadata
            // for each.  This allows metadata objects to be shared among Layer objects,
            // saving memory.
            for (Gridset gridset : gd.getGridsets())
            {
                GridCoordSystem coordSys = gridset.getGeoCoordSystem();
                
                // Compute TimestepInfo objects for this file
                logger.debug("Computing TimestepInfo objects");
                List<TimestepInfo> timesteps = getTimesteps(location, coordSys);
                
                // Look for new variables in this coordinate system.
                List<GridDatatype> grids = gridset.getGrids();
                List<GridDatatype> newGrids = new ArrayList<GridDatatype>();
                for (GridDatatype grid : grids)
                {
                    if (!layers.containsKey(grid.getName()) &&
                        this.includeGrid(grid))
                    {
                        // We haven't seen this variable before so we must create
                        // a Layer object later
                        logger.debug("{} is a new grid", grid.getName());
                        newGrids.add(grid);
                    }
                    else
                    {
                        logger.debug("We already have data for {}", grid.getName());
                    }
                }
                
                // We only create all the coordsys-related objects if we have
                // new Layers to create
                if (newGrids.size() > 0)
                {
                    logger.debug("Creating coordinate system objects");
                    // Create an object that will map lat-lon points to nearest grid points
                    HorizontalCoordSys horizCoordSys = HorizontalCoordSys.fromCoordSys(coordSys);
                    
                    boolean zPositive = this.isZPositive(coordSys);
                    CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                    double[] zValues = this.getZValues(zAxis, zPositive);

                    // Set the bounding box
                    // TODO: should take into account the cell bounds
                    LatLonRect latLonRect = coordSys.getLatLonBoundingBox();
                    LatLonPoint lowerLeft = latLonRect.getLowerLeftPoint();
                    LatLonPoint upperRight = latLonRect.getUpperRightPoint();
                    double minLon = lowerLeft.getLongitude();
                    double maxLon = upperRight.getLongitude();
                    double minLat = lowerLeft.getLatitude();
                    double maxLat = upperRight.getLatitude();
                    // Correct the bounding box in case of mistakes or in case it 
                    // crosses the date line
                    if (latLonRect.crossDateline() || minLon >= maxLon)
                    {
                        minLon = -180.0;
                        maxLon = 180.0;
                    }
                    if (minLat >= maxLat)
                    {
                        minLat = -90.0;
                        maxLat = 90.0;
                    }
                    double[] bbox = new double[]{minLon, minLat, maxLon, maxLat};

                    // Now add every variable that has this coordinate system
                    for (GridDatatype grid : newGrids)
                    {
                        logger.debug("Creating new Layer object for {}", grid.getName());
                        LayerImpl layer = new LayerImpl();
                        layer.setId(grid.getName());
                        layer.setTitle(getLayerTitle(grid.getVariable()));
                        layer.setAbstract(grid.getDescription());
                        layer.setUnits(grid.getUnitsString());
                        layer.setHorizontalCoordSys(horizCoordSys);
                        layer.setBbox(bbox);

                        if (zAxis != null)
                        {
                            layer.setZunits(zAxis.getUnitsString());
                            layer.setZpositive(zPositive);
                            layer.setZvalues(zValues);
                        }

                        // Add this layer to the Map
                        layers.put(layer.getId(), layer);
                    }
                }
                // Now we add the new timestep information for all grids
                // in this Gridset
                for (GridDatatype grid : grids)
                {
                    if (this.includeGrid(grid))
                    {
                        logger.debug("Adding timestep info to {}", grid.getName());
                        LayerImpl layer = layers.get(grid.getName());
                        for (TimestepInfo timestep : timesteps)
                        {
                            layer.addTimestepInfo(timestep);
                        }
                    }
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
                    nc.close();
                    logger.debug("NetCDF file closed");
                }
                catch (IOException ex)
                {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
    
    /**
     * @return true if the given variable is to be exposed through the WMS.
     * This default implementation always returns true, but allows subclasses
     * to filter out variables if required.
     */
    protected boolean includeGrid(GridDatatype grid)
    {
        return true;
    }
    
    /**
     * @return the values on the z axis, with sign reversed if zPositive == false.
     * Returns null if zAxis is null.
     */
    protected double[] getZValues(CoordinateAxis1D zAxis, boolean zPositive)
    {
        double[] zValues = null;
        if (zAxis != null)
        {
            zValues = zAxis.getCoordValues();
            if (!zPositive)
            {
                double[] zVals = new double[zValues.length];
                for (int i = 0; i < zVals.length; i++)
                {
                    zVals[i] = 0.0 - zValues[i];
                }
                zValues = zVals;
            }
        }
        return zValues;
    }
    
    /**
     * @return true if the zAxis is positive
     */
    protected boolean isZPositive(GridCoordSystem coordSys)
    {
        return coordSys.isZPositive();
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
                DateTime dt = new DateTime(dates[i]);
                TimestepInfo tInfo = new TimestepInfo(dt, filename, i);
                timesteps.add(tInfo);
            }
        }
        return timesteps;
    }
    
    /**
     * @return the value of the standard_name attribute of the variable,
     * or the long_name if it does not exist, or the unique id if neither of
     * these attributes exist.
     */
    protected static String getLayerTitle(VariableEnhanced var)
    {
        Attribute stdNameAtt = var.findAttributeIgnoreCase("standard_name");
        if (stdNameAtt == null || stdNameAtt.getStringValue().trim().equals(""))
        {
            Attribute longNameAtt = var.findAttributeIgnoreCase("long_name");
            if (longNameAtt == null || longNameAtt.getStringValue().trim().equals(""))
            {
                return var.getName();
            }
            else
            {
                return longNameAtt.getStringValue();
            }
        }
        else
        {
            return stdNameAtt.getStringValue();
        }
    }
    
}
