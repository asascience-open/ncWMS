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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.cdm.CdmUtils;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.config.LayerImpl;
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

    /**
     * Reads data from a NetCDF file.  Reads data for a single timestep only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by null.
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param domain The list of real-world x-y points for which we need data.
     * In the case of a GetMap operation this will usually be a {@link HorizontalGrid}.
     * @return an array of floating-point data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws IOException if an input/output exception occurred when reading data
     */
    @Override
    public List<Float> read(String filename, Layer layer, int tIndex, int zIndex,
        Domain<HorizontalPosition> domain) throws IOException
    {
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset, using the cache for NcML aggregations
            nc = openDataset(filename);
            // Read and return the data
            return CdmUtils.readHorizontalPoints(
                nc,
                layer.getId(),           // The grid of data to read from
                layer.getHorizontalGrid(),
                tIndex,
                zIndex,
                domain
            );
        }
        finally
        {
            closeDataset(nc);
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
            // Read and return the data
            return CdmUtils.readTimeseries(
                nc,
                layer.getId(),
                layer.getHorizontalGrid(),
                tIndices,
                zIndex,
                xy
            );
        }
        finally
        {
            closeDataset(nc);
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
     * @todo Some of this code is repeated in THREDDS's ThreddsDataset, but it's
     * hard to refactor neatly in a manner that allows better reuse.
     */
    @Override
    protected void findAndUpdateLayers(String location, Map<String, LayerImpl> layers)
            throws IOException
    {
        logger.debug("Finding layers in {}", location);
        
        NetcdfDataset nc = null;
        try
        {
            // Open the dataset, using the cache for NcML aggregations
            nc = openDataset(location);
            GridDataset gd = CdmUtils.getGridDataset(nc);

            if (gd == null)           throw new NullPointerException("GridDataset can't be null");
            if (layers == null)       throw new NullPointerException("layers can't be null");

            // Search through all coordinate systems, creating appropriate metadata
            // for each.  This allows metadata objects to be shared among Layer objects,
            // saving memory.
            for (Gridset gridset : gd.getGridsets())
            {
                GridCoordSystem coordSys = gridset.getGeoCoordSystem();

                // Look for new variables in this coordinate system.
                List<GridDatatype> grids = gridset.getGrids();
                List<GridDatatype> newGrids = new ArrayList<GridDatatype>();
                for (GridDatatype grid : grids)
                {
                    if (layers.containsKey(grid.getName()))
                    {
                        logger.debug("We already have data for {}", grid.getName());
                    }
                    else
                    {
                        // We haven't seen this variable before so we must create
                        // a Layer object later
                        logger.debug("{} is a new grid", grid.getName());
                        newGrids.add(grid);
                    }
                }

                // We only create all the coordsys-related objects if we have
                // new Layers to create
                if (!newGrids.isEmpty())
                {
                    logger.debug("Creating coordinate system objects");
                    // Create an object that will map lat-lon points to nearest grid points
                    HorizontalGrid horizGrid = CdmUtils.createHorizontalGrid(coordSys);

                    CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                    List<Double> zValues = CdmUtils.getZValues(coordSys);

                    // Get the bounding box
                    GeographicBoundingBox bbox = CdmUtils.getBbox(coordSys.getLatLonBoundingBox());

                    // Now add every variable that has this coordinate system
                    for (GridDatatype grid : newGrids)
                    {
                        logger.debug("Creating new Layer object for {}", grid.getName());
                        LayerImpl layer = new LayerImpl(grid.getName());
                        layer.setTitle(CdmUtils.getVariableTitle(grid.getVariable()));
                        layer.setAbstract(grid.getDescription());
                        layer.setUnits(grid.getUnitsString());
                        layer.setHorizontalGrid(horizGrid);
                        layer.setGeographicBoundingBox(bbox);

                        if (zAxis != null)
                        {
                            layer.setElevationValues(zValues);
                            // We should have defined the meaning of "zPositive"
                            // a bit more carefully. It's not intuitive,
                            // particularly for a pressure axis
                            layer.setElevationPositive(coordSys.isZPositive());
                            layer.setElevationUnits(zAxis.getUnitsString());
                        }

                        // Add this layer to the Map
                        layers.put(layer.getId(), layer);
                    }
                }

                // Now we add the new timestep information for *all* grids
                // in this Gridset
                List<DateTime> timesteps;
                if (coordSys.hasTimeAxis1D()) {
                    timesteps = CdmUtils.getTimesteps(coordSys.getTimeAxis1D());
                } else {
                    timesteps = Collections.emptyList();
                }
                for (GridDatatype grid : grids)
                {
                    LayerImpl layer = layers.get(grid.getName());
                    for (int i = 0, len = timesteps.size(); i < len; i++)
                    {
                        layer.addTimestepInfo(timesteps.get(i), location, i);
                    }
                }
            }
        }
        finally
        {
            logger.debug("In finally clause");
            closeDataset(nc);
        }
    }

    /** Closes the given dataset, logging any exceptions at debug level */
    private static void closeDataset(NetcdfDataset nc)
    {
        if (nc == null) return;
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
     * given location.
     * @throws IOException if there was an error reading from the data source.
     */
    private static NetcdfDataset openDataset(String location) throws IOException
    {
        boolean usedCache = false;
        NetcdfDataset nc;
        long start = System.nanoTime();
        if (WmsUtils.isNcmlAggregation(location))
        {
            // We use the cache of NetcdfDatasets to read NcML aggregations
            // as they can be time-consuming to put together.  If the underlying
            // data can change we rely on the server admin setting the
            // "recheckEvery" parameter in the aggregation file.
            nc = NetcdfDataset.acquireDataset(location, null);
            usedCache = true;
        }
        else
        {
            // For local single files and OPeNDAP datasets we don't use the
            // cache, to ensure that we are always reading the most up-to-date
            // data.  There is a small possibility that the dataset cache will
            // have swallowed up all available file handles, in which case
            // the server admin will need to increase the number of available
            // handles on the server.
            nc = NetcdfDataset.openDataset(location);
        }
        long openedDS = System.nanoTime();
        String verb = usedCache ? "Acquired" : "Opened";
        logger.debug(verb + " NetcdfDataset in {} milliseconds", (openedDS - start) / 1.e6);
        return nc;
    }
    
}
