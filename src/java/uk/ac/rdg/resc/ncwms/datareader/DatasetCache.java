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
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

/**
 * Contains caches of datasets and metadata for those datasets.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class DatasetCache
{
    private static final Logger logger = Logger.getLogger(DatasetCache.class);
    // Maps locations to hashtables that map variable IDs to VariableMetadata
    // objects
    private static Hashtable<String, Hashtable<String, VariableMetadata>> cache;
        
    
    /**
     * Initialize the cache. Must be called before trying to get datasets or
     * metadata from the cache.  Does nothing if already called.
     */
    public static synchronized void init()
    {
        if (cache == null)
        {
            NetcdfDatasetCache.init();
            cache = new Hashtable<String, Hashtable<String, VariableMetadata>>();
        }
        logger.debug("DatasetCache initialized");
    }
    
    
    /**
     * Gets the {@link NetcdfDataset} at the given location from the cache.
     * @throws IOException if there was an error opening the dataset
     */
    public static synchronized NetcdfDataset getDataset(String location)
        throws IOException
    {
        NetcdfDataset nc = NetcdfDatasetCache.acquire(location, null, DatasetFactory.get());
        logger.debug("Returning NetcdfDataset at {} from cache", location);
        return nc;
    }
    
    /**
     * Gets the metadata for the variable with the given ID from the file at the
     * given location
     * @param location Location of the NetcdfDataset containing the variable
     * @param varID the unique ID of the variable
     * @throws IOException if there was an error reading the metadata from disk
     */
    public static synchronized VariableMetadata getVariableMetadata(String location, String varID)
        throws IOException
    {
        return getVariableMetadata(location).get(varID);
    }
    
    /**
     * Clears the cache of datasets and metadata.  This is called periodically
     * by a Timer (see WMS.py), to make sure we are synchronized with the disk.
     * @todo if a dataset is already open, it will not be removed from the cache
     */
    public static synchronized void clear()
    {
        NetcdfDatasetCache.clearCache(false);
        cache.clear();
        logger.debug("DatasetCache cleared");
    }
    
    /**
     * Cleans up the cache.
     */
    public static synchronized void exit()
    {
        NetcdfDatasetCache.exit();
        logger.debug("Cleaned up DatasetCache");
    }
    
    /** Private constructor so this class can't be instantiated */
    private DatasetCache()
    {
    }
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location.  Puts the metadata in the cache.
     * @throws IOException if there was an error reading from the data source
     */
    public static Hashtable<String, VariableMetadata>
        getVariableMetadata(String location) throws IOException
    {
        if (cache.containsKey(location))
        {
            logger.debug("Metadata for {} already in cache", location);
        }
        else
        {
            logger.debug("Reading metadata for {}", location);
            NetcdfDataset nc = null;
            Hashtable<String, VariableMetadata> vars = new Hashtable<String, VariableMetadata>();
            try
            {
                // We use openDataset() rather than acquiring from cache
                // because we need to enhance the dataset
                nc = NetcdfDataset.openDataset(location);
                GridDataset gd = new GridDataset(nc);
                for (Iterator it = gd.getGrids().iterator(); it.hasNext(); )
                {
                    GeoGrid gg = (GeoGrid)it.next();
                    VariableMetadata vm = new VariableMetadata();
                    vm.setId(gg.getName());
                    vm.setTitle(getStandardName(gg));
                    vm.setAbstract(gg.getDescription());
                    vm.setUnits(gg.getUnitsString());
                    GridCoordSys coordSys = gg.getCoordinateSystem();
                    vm.setXaxis(EnhancedCoordAxis.create(coordSys.getXHorizAxis()));
                    vm.setYaxis(EnhancedCoordAxis.create(coordSys.getYHorizAxis()));

                    List dims = gg.getDimensions();
                    /*int xPos = findDimensionPos(dims, coordSys.getXHorizAxis().getDimension(0));
                    int yPos = findDimensionPos(dims, coordSys.getYHorizAxis().getDimension(0));
                    int zPos = -1;
                    int tPos = -1;*/

                    if (coordSys.hasVerticalAxis())
                    {
                        CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                        //zPos = findDimensionPos(dims, zAxis.getDimension(0));
                        vm.setZunits(zAxis.getUnitsString());
                        double[] zVals = zAxis.getCoordValues();
                        vm.setZpositive(coordSys.isZPositive());
                        if (coordSys.isZPositive())
                        {
                            vm.setZvalues(zVals);
                        }
                        else
                        {
                            double[] zVals2 = new double[zVals.length];
                            for (int i = 0; i < zVals.length; i++)
                            {
                                zVals2[i] = 0.0 - zVals[i];
                            }
                            vm.setZvalues(zVals2);
                        }
                    }

                    if (coordSys.isDate())
                    {
                        //tPos = findDimensionPos(dims, coordSys.getTimeAxis().getDimension(0));
                        Date[] tVals = coordSys.getTimeDates();
                        double[] sse = new double[tVals.length]; // Seconds since the epoch
                        for (int i = 0; i < tVals.length; i++)
                        {
                            sse[i] = tVals[i].getTime() / 1000.0;
                        }
                        vm.setTvalues(sse);
                    }

                    //vm.setAxisPositions(tPos, zPos, yPos, xPos);

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
                    vm.setBbox(new double[]{minLon, minLat, maxLon, maxLat});

                    vm.setValidMin(gg.getVariable().getValidMin());
                    vm.setValidMax(gg.getVariable().getValidMax());

                    vars.put(vm.getId(), vm);
                    cache.put(location, vars);
                }
            }
            finally
            {
                if (nc != null)
                {
                    nc.close();
                }
            }
        }
        
        return cache.get(location);
    }
    
    private static int findDimensionPos(List dims, Dimension want)
    {
        for (int i = 0; i < dims.size(); i++)
        {
            Dimension dim = (Dimension)dims.get(i);
            if (dim.equals(want))
            {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * @return the value of the standard_name attribute of the geogrid,
     * or the unique name if it does not exist
     */
    private static String getStandardName(GeoGrid geogrid)
    {
        Attribute stdNameAtt = geogrid.findAttributeIgnoreCase("standard_name");
        return stdNameAtt == null ? geogrid.getName() : stdNameAtt.getStringValue();
    }
    
}
