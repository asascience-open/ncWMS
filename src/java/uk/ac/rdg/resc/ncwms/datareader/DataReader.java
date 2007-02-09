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
import java.util.Date;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetCache;
import ucar.nc2.units.DateFormatter;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

/**
 * Abstract superclass for classes that read data and metadata from NetCDF datasets.
 * Called from nj22dataset.py.
 * @author jdb
 */
public abstract class DataReader
{
    private static final Logger logger = Logger.getLogger(DataReader.class);
    
    /**
     * Maps class names to DataReader objects.  Only one DataReader object of
     * each class will ever be created.
     */
    private static Hashtable<String, DataReader> readers;
    /**
     * Maps locations to hashtables that map variable IDs to VariableMetadata
     * objects.
     */
    private static Hashtable<String, Hashtable<String, VariableMetadata>> metadataCache;
    
    /**
     * Initialize the cache. Must be called before trying to get datasets or
     * metadata from the cache.  Does nothing if already called.
     */
    public static synchronized void init()
    {
        if (metadataCache == null)
        {
            NetcdfDatasetCache.init();
            metadataCache = new Hashtable<String, Hashtable<String, VariableMetadata>>();
            readers = new Hashtable<String, DataReader>();
            logger.debug("DatasetCache initialized");
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
     * Clears the cache of datasets and metadata.  This is called periodically
     * by a Timer (see WMS.py), to make sure we are synchronized with the disk.
     * @todo if a dataset is already open, it will not be removed from the cache
     */
    public static synchronized void clear()
    {
        NetcdfDatasetCache.clearCache(false);
        metadataCache.clear();
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
    
    /**
     * Gets a DataReader object.  <b>Only one</b> object of each class will be
     * created (hence methods have to be thread-safe).
     * @param className Name of the class to generate
     * @return a DataReader object of the given class, or {@link DefaultDataReader}
     * if <code>className</code> is null or the empty string
     * @throws a {@link WMSExceptionInJava} if the DataReader could not be created
     */
    private static DataReader getDataReader(String className)
        throws WMSExceptionInJava
    {
        String clazz = DefaultDataReader.class.getName();
        try
        {
            if (className != null && !className.trim().equals(""))
            {
                clazz = className;
            }
            if (!readers.containsKey(clazz))
            {
                // Create the DataReader object
                Object drObj = Class.forName(clazz).newInstance();
                // this will throw a ClassCastException if drObj is not a DataReader
                readers.put(clazz, (DataReader)drObj);
            }
            return readers.get(clazz);
        }
        catch(ClassNotFoundException cnfe)
        {
            logger.error("Class not found: " + clazz, cnfe);
            throw new WMSExceptionInJava("Internal error: class " + clazz +
                " not found");
        }
        catch(InstantiationException ie)
        {
            logger.error("Instantiation error for class: " + clazz, ie);
            throw new WMSExceptionInJava("Internal error: class " + clazz +
                " could not be instantiated");
        }
        catch(IllegalAccessException iae)
        {
            logger.error("Illegal access error for class: " + clazz, iae);
            throw new WMSExceptionInJava("Internal error: constructor for " + clazz +
                " could not be accessed");
        }
    }
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single time index only.
     *
     * @param location Location of the NetCDF dataset (full file path, OPeNDAP URL etc)
     * @param dataReaderClassName The name of the class that we will use to read data
     * @param varID Unique identifier for the required variable in the file
     * @param tIndex The index along the time axis as found in getmap.py
     * @param zValue The value of elevation as specified by the client
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @return array of data values
     * @throws WMSExceptionInJava if an error occurs
     */
    public static float[] read(String location, String dataReaderClassName, String varID,
        int tIndex, String zValue, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava
    {
        try
        {
            DataReader dr = getDataReader(dataReaderClassName);
            VariableMetadata vm = getAllVariableMetadata(location, dr).get(varID);
            if (vm == null)
            {
                throw new WMSExceptionInJava("Could not find variable called "
                    + vm.getId() + " in " + location);
            }
            // Find the index along the depth axis
            int zIndex = 0; // Default value of z is the first in the axis
            if (zValue != null && !zValue.equals("") && vm.getZvalues() != null)
            {
                zIndex = vm.findZIndex(zValue);
            }
            return dr.read(location, vm, tIndex, zIndex, latValues, lonValues, fillValue);
        }
        catch(Exception e)
        {
            logger.error(e.getMessage(), e);
            throw new WMSExceptionInJava(e.getMessage());
        }
    }
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single time index only.
     *
     * @param location Location of the NetCDF dataset (full file path, OPeNDAP URL etc)
     * @param vm {@link VariableMetadata} object representing the variable
     * @param tIndex The index along the time axis as found in getmap.py
     * @param zIndex The index along the vertical axis (or 0 if there is no vertical axis)
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @throws WMSExceptionInJava if an error occurs
     */
    protected abstract float[] read(String location, VariableMetadata vm,
        int tIndex, int zIndex, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava;
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location.
     * @param location Location of the NetCDF dataset (full file path, OPeNDAP URL etc)
     * @param dataReaderClassName The name of the class that we will use to read data
     * @return Hashtable of variable IDs mapped to {@link VariableMetadata} objects
     * @throws WMSExceptionInJava if there was an error reading from the data source
     */
    public static Hashtable<String, VariableMetadata> getAllVariableMetadata(String location,
        String dataReaderClassName) throws WMSExceptionInJava
    {
        try
        {
            DataReader dr = getDataReader(dataReaderClassName);
            return getAllVariableMetadata(location, dr);
        }
        catch(IOException ioe)
        {
            logger.error("IOException reading metadata: " + ioe.getMessage(), ioe);
            throw new WMSExceptionInJava("Internal IO error reading metadata " + ioe.getMessage());
        }
    }
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location.
     * @param location Location of the NetCDF dataset (full file path, OPeNDAP URL etc)
     * @param dr The DataReader that we will use to read data
     * @return Hashtable of variable IDs mapped to {@link VariableMetadata} objects
     * @throws IOException if there was an error reading from the data source
     */
    public static Hashtable<String, VariableMetadata> getAllVariableMetadata(String location,
        DataReader dr) throws IOException
    {
        if (!metadataCache.containsKey(location))
        {
            metadataCache.put(location, dr.getVariableMetadata(location));
        }
        return metadataCache.get(location);
    }
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location.
     * @param location Location of the NetCDF dataset (full file path, OPeNDAP URL etc)
     * @return Hashtable of variable IDs mapped to {@link VariableMetadata} objects
     * @throws IOException if there was an error reading from the data source
     */
    protected abstract Hashtable<String, VariableMetadata> getVariableMetadata(String location)
        throws IOException;
}
