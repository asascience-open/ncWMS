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
import java.util.Hashtable;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

/**
 * Abstract superclass for classes that read data and metadata from NetCDF datasets.
 * Called from nj22dataset.py.
 * @author jdb
 */
public abstract class DataReader
{
    private static final Logger logger = Logger.getLogger(DataReader.class);
    
    protected String location; // Location of the data that we'll be reading
                               // (could be a glob expression or OPeNDAP address)
    
    /**
     * This class can only be instantiated through createDataReader()
     */
    protected DataReader()
    {
    }
    
    /**
     * Gets a DataReader object of a certain type, for data at a certain location.
     * We need the location information because we might want to detect whether
     * a dataset is local or remote (e.g. OPeNDAP).  Creates a new DataReader object
     * with each invocation.
     * @param className Name of the DataReader class to generate
     * @param the location of the dataset: used to detect OPeNDAP URLs
     * @return a DataReader object of the given class, or {@link DefaultDataReader}
     * or {@link OpendapDataReader} (depending on whether the location starts with
     * "http://" or "dods://") if <code>className</code> is null or the empty string
     * @throws a {@link WMSExceptionInJava} if the DataReader could not be created
     */
    public static DataReader createDataReader(String className, String location)
        throws WMSExceptionInJava
    {
        String clazz = DefaultDataReader.class.getName();
        if (isOpendapLocation(location))
        {
            clazz = OpendapDataReader.class.getName();
        }
        try
        {
            if (className != null && !className.trim().equals(""))
            {
                clazz = className;
            }
            DataReader dr = (DataReader)Class.forName(clazz).newInstance();
            dr.location = location;
            return dr;
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
    
    protected static boolean isOpendapLocation(String location)
    {
        return location.startsWith("http://") || location.startsWith("dods://")
            || location.startsWith("thredds");
    }
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single time index only.
     *
     * @param vm {@link VariableMetadata} object representing the variable
     * @param tIndex The index along the time axis as found in getmap.py
     * @param zValue The value of elevation as specified by the client
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @return array of data values
     * @throws WMSExceptionInJava if an error occurs
     */
    public float[] read(VariableMetadata vm,
        int tIndex, String zValue, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava
    {
        try
        {
            // Find the index along the depth axis
            int zIndex = 0; // Default value of z is the first in the axis
            if (zValue != null && !zValue.equals("") && vm.getZvalues() != null)
            {
                zIndex = vm.findZIndex(zValue);
            }
            return this.read(vm, tIndex, zIndex, latValues, lonValues, fillValue);
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
     * @param vm {@link VariableMetadata} object representing the variable
     * @param tIndex The index along the time axis as found in getmap.py
     * @param zIndex The index along the vertical axis (or 0 if there is no vertical axis)
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @throws WMSExceptionInJava if an error occurs
     */
    protected abstract float[] read(VariableMetadata vm,
        int tIndex, int zIndex, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava;
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location.
     * @return Hashtable of variable IDs mapped to {@link VariableMetadata} objects
     * @throws IOException if there was an error reading from the data source
     */
    public abstract Hashtable<String, VariableMetadata> getVariableMetadata()
        throws IOException;
    
    /**
     * Closes the DataReader: frees up any resources.  This default implementation
     * does nothing: subclasses should override if necessary.
     */
    public void close()
    {
    }
}
