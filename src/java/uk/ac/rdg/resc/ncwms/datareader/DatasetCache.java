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
import org.apache.log4j.Logger;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordSysBuilder;
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
            CoordSysBuilder.registerConvention("NEMO", NemoCoordSysBuilder.class);
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
            try
            {
                if (location.contains("NEMO"))
                {
                    // This is very ropey logic!  Just a quick and dirty way 
                    // of handling the NEMO files, which are very far from
                    // standards-compliant!
                    createNemoMetadata(location);
                }
                else
                {
                    Hashtable<String, VariableMetadata> vars = new Hashtable<String, VariableMetadata>();
                    // We use openDataset() rather than acquiring from cache
                    // because we need to enhance the dataset
                    nc = NetcdfDataset.openDataset(location, true, null);
                    GridDataset gd = new GridDataset(nc);
                    for (Iterator it = gd.getGrids().iterator(); it.hasNext(); )
                    {
                        GeoGrid gg = (GeoGrid)it.next();
                        VariableMetadata vm = new VariableMetadata();
                        vm.setId(gg.getName());
                        vm.setTitle(getStandardName(gg.getVariable().getOriginalVariable()));
                        vm.setAbstract(gg.getDescription());
                        vm.setUnits(gg.getUnitsString());
                        GridCoordSys coordSys = gg.getCoordinateSystem();
                        vm.setXaxis(EnhancedCoordAxis.create(coordSys.getXHorizAxis()));
                        vm.setYaxis(EnhancedCoordAxis.create(coordSys.getYHorizAxis()));

                        if (coordSys.hasVerticalAxis())
                        {
                            CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
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
                            Date[] tVals = coordSys.getTimeDates();
                            double[] sse = new double[tVals.length]; // Seconds since the epoch
                            for (int i = 0; i < tVals.length; i++)
                            {
                                sse[i] = tVals[i].getTime() / 1000.0;
                            }
                            vm.setTvalues(sse);
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
                        vm.setBbox(new double[]{minLon, minLat, maxLon, maxLat});

                        vm.setValidMin(gg.getVariable().getValidMin());
                        vm.setValidMax(gg.getVariable().getValidMax());

                        vars.put(vm.getId(), vm);
                    }
                    cache.put(location, vars);
                }
            }
            finally
            {
                if (nc != null) nc.close();
            }
        }
        
        return cache.get(location);
    }
    
    /**
     * @return the value of the standard_name attribute of the variable,
     * or the unique name if it does not exist
     */
    private static String getStandardName(Variable var)
    {
        Attribute stdNameAtt = var.findAttributeIgnoreCase("standard_name");
        return stdNameAtt == null ? var.getName() : stdNameAtt.getStringValue();
    }
    
    /**
     * Creates the VariableMetadata objects for a NEMO dataset and adds them
     * to the cache
     * @throws IOException if an IO error occurred
     */
    private static void createNemoMetadata(String location) throws IOException
    {
        Hashtable<String, VariableMetadata> vars = new Hashtable<String, VariableMetadata>();
        NetcdfDataset nc = null;
        
        try
        {
            nc = NetcdfDataset.openDataset(location, false, null);        
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

            for (Object varObj : nc.getVariables())
            {
                Variable var = (Variable)varObj;
                // We ignore the coordinate axes
                if (!var.getName().equals("nav_lon") && !var.getName().equals("nav_lat")
                    && !var.getName().equals("deptht") && !var.getName().equals("time_counter"))
                {
                    VariableMetadata vm = new VariableMetadata();
                    vm.setId(var.getName());
                    //vm.setTitle(getStandardName(var));
                    //vm.setAbstract(var.getDescription());
                    vm.setTitle(var.getDescription()); // TODO: standard_names are not set: set these in NcML?
                    vm.setUnits(var.getUnitsString());
                    vm.setZpositive(false);
                    // TODO: check for the presence of a z axis in a neater way
                    if (var.getRank() == 4)
                    {
                        vm.setZvalues(zVals);
                        vm.setZunits(zUnits);
                    }
                    // TODO: should check these values exist
                    vm.setValidMin(var.findAttribute("valid_min").getNumericValue().doubleValue());
                    vm.setValidMax(var.findAttribute("valid_max").getNumericValue().doubleValue());
                    // TODO: create these axes properly
                    vm.setXaxis(new Regular1DCoordAxis(0, 360.0 / 1442, 1442, true));
                    vm.setYaxis(new Regular1DCoordAxis(-90, 180.0 / 1021, 1021, false));

                    vars.put(vm.getId(), vm);
                }
            }
            cache.put(location, vars);
        }
        finally
        {
            if (nc != null) nc.close();
        }
    }
    
}
