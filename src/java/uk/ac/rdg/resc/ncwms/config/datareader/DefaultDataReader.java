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

package uk.ac.rdg.resc.ncwms.config.datareader;

import uk.ac.rdg.resc.ncwms.coords.PointList;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.ncwms.cdm.AbstractScalarLayerBuilder;
import uk.ac.rdg.resc.ncwms.cdm.CdmUtils;
import uk.ac.rdg.resc.ncwms.config.LayerImpl;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * Default data reading class for CF-compliant NetCDF datasets.
 *
 * @author Jon Blower
 */
public class DefaultDataReader extends DataReader
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultDataReader.class);
    // We'll use this logger to output performance information
    private static final Logger benchmarkLogger = LoggerFactory.getLogger("ncwms.benchmark");

    /**
     * Enumeration of enhancements we want to perform when opening NetcdfDatasets
     * Read the coordinate systems but don't automatically process
     * scale/missing/offset when reading data, for efficiency reasons.
     */
    private static final Set<Enhance> DATASET_ENHANCEMENTS =
        EnumSet.of(Enhance.ScaleMissingDefer, Enhance.CoordSystems);

    /**
     * Reads data from a NetCDF file.  Reads data for a single timestep only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by null.
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
     * @throws IOException if an input/output exception occurred when reading data
     */
    @Override
    public List<Float> read(String filename, Layer layer, int tIndex, int zIndex,
        PointList pointList) throws IOException
    {
        NetcdfDataset nc = null;
        try
        {
            long start = System.currentTimeMillis();
            
            // Open the dataset, using the cache for NcML aggregations
            nc = openDataset(filename);
            long openedDS = System.currentTimeMillis();
            logger.debug("Opened NetcdfDataset in {} milliseconds", (openedDS - start));

            // Get a GridDataset object, since we know this is a grid
            GridDataset gd = CdmUtils.getGridDataset(nc);
            
            logger.debug("Getting GridDatatype with id {}", layer.getId());
            GridDatatype gridData = gd.findGridDatatype(layer.getId());
            logger.debug("filename = {}, gg = {}", filename, gridData.toString());

            return CdmUtils.readPointList(
                gridData,           // The grid of data to read from
                layer.getHorizontalCoordSys(),
                tIndex,
                zIndex,
                pointList,
                CdmUtils.getOptimumDataReadingStrategy(nc),
                CdmUtils.isScaleMissingDeferred(nc)
            );

            // Write to the benchmark logger (if enabled in log4j.properties)
            // Headings are written in NcwmsContext.init()
            /*if (pixelMap.getNumUniqueIJPairs() > 1)
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
            }*/
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
     * <p>Reads a timeseries of data from a file from a single xyz point.  This
     * method knows nothing about aggregation: it simply reads data from the
     * given file.  Missing values (e.g. land pixels in oceanography data) will
     * be represented by null.</p>
     * <p>If the provided Layer doesn't have a time axis then {@code tIndices}
     * must be a single-element list with value -1.  In this case the returned
     * "timeseries" of data will be a single data value. (TODO: make this more
     * sensible.)</p>
     * <p>This implementation reads all data with a single I/O operation
     * (as opposed to the {@link DataReader#readTimeseries(java.lang.String,
     * uk.ac.rdg.resc.ncwms.metadata.Layer, java.util.List, int,
     * uk.ac.rdg.resc.ncwms.coordsys.LonLatPosition) superclass implementation},
     * which uses an I/O operation for each individual point).  This method is
     * therefore expected to be more efficient, particularly when reading from
     * OPeNDAP servers.</p>
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndices the indices along the time axis within this file
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param xy the horizontal position of the point
     * @return an array of floating-point data values, one for each point in
     * {@code tIndices}, in the same order.
     * @throws IOException if an input/output exception occurred when reading data
     * @todo Validity checking on tIndices and layer.hasTAxis()?
     */
    @Override
    public List<Float> readTimeseries(String filename, Layer layer,
        List<Integer> tIndices, int zIndex, HorizontalPosition xy)
        throws IOException
    {
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset, using the cache for NcML aggregations
            nc = openDataset(filename);
            GridDataset gd = CdmUtils.getGridDataset(nc);
            GridDatatype grid = gd.findGridDatatype(layer.getId());
            
            // Read and return the data
            return CdmUtils.readTimeseries(
                grid,
                layer.getHorizontalCoordSys(),
                tIndices,
                zIndex,
                xy,
                CdmUtils.isScaleMissingDeferred(nc)
            );
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
     * @throws IOException if there was an error reading from the data source
     */
    @Override
    protected void findAndUpdateLayers(String location,
        Map<String, LayerImpl> layers) throws IOException
    {
        logger.debug("Finding layers in {}", location);
        
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset, using the cache for NcML aggregations
            nc = openDataset(location);
            GridDataset gd = CdmUtils.getGridDataset(nc);

            LayerImplBuilder layerBuilder = new LayerImplBuilder(location);
            CdmUtils.findAndUpdateLayers(gd, layerBuilder, layers);
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

    private static final class LayerImplBuilder extends AbstractScalarLayerBuilder<LayerImpl>
    {
        private final String location;

        public LayerImplBuilder(String location) {
            this.location = location;
        }

        @Override
        public LayerImpl newLayer(String id) {
            return new LayerImpl(id);
        }

        @Override
        public void setTimeValues(LayerImpl layer, List<DateTime> times) {
            for (int i = 0; i < times.size(); i++) {
                layer.addTimestepInfo(times.get(i), this.location, i);
            }
        }

        @Override
        public void setGridDatatype(LayerImpl layer, GridDatatype gd) {
            // Do nothing: we don't hold on to the GridDatatype object
        }
    }

    /**
     * Opens the NetCDF dataset at the given location, using the dataset
     * cache if {@code location} represents an NcML aggregation.  We cannot
     * use the cache for OPeNDAP or single NetCDF files because the underlying
     * data may have changed and the NetcdfDataset cache may cache a dataset
     * forever.  In the case of NcML we rely on the fact that server administrators
     * ought to have set a "recheckEvery" parameter for NcML aggregations that
     * may change with time.  It is desirable to use the dataset cache for NcML
     * aggregations because they can be time-consuming to assemble and we don't
     * want to do this every time a map is drawn.
     * @param location The location of the data: a local NetCDF file, an NcML
     * aggregation file or an OPeNDAP location, {@literal i.e.} anything that can be
     * passed to NetcdfDataset.openDataset(location).
     * @return a {@link NetcdfDataset} object for accessing the data at the
     * given location.  The coordinate systems will have been read, but
     * the application of scale-offset-missing is deferred.
     * @throws IOException if there was an error reading from the data source.
     */
    private static NetcdfDataset openDataset(String location) throws IOException
    {
        if (WmsUtils.isNcmlAggregation(location))
        {
            // We use the cache of NetcdfDatasets to read NcML aggregations
            // as they can be time-consuming to put together.  If the underlying
            // data can change we rely on the server admin setting the
            // "recheckEvery" parameter in the aggregation file.
            return NetcdfDataset.acquireDataset(
                null, // Use the default factory
                location,
                DATASET_ENHANCEMENTS,
                -1, // use default buffer size
                null, // no CancelTask
                null // no iospMessage
            );
        }
        else
        {
            // For local single files and OPeNDAP datasets we don't use the
            // cache, to ensure that we are always reading the most up-to-date
            // data.  There is a small possibility that the dataset cache will
            // have swallowed up all available file handles, in which case
            // the server admin will need to increase the number of available
            // handles on the server.
            return NetcdfDataset.openDataset(
                location,
                DATASET_ENHANCEMENTS,
                -1, // use default buffer size
                null, // no CancelTask
                null // no iospMessage
            );
        }
    }
    
}
