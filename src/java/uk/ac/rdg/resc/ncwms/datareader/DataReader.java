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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.oro.io.GlobFilenameFilter;

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
    private static Map<String, DataReader> readers = new HashMap<String, DataReader>();
    
    /**
     * This class can only be instantiated through getDataReader()
     */
    protected DataReader()
    {
    }
    
    /**
     * Gets a DataReader object.  <b>Only one</b> object of each class will be
     * created (hence methods have to be thread-safe).
     * 
     * @param className Name of the class to generate
     * @param the location of the dataset: used to detect OPeNDAP URLs
     * @return a DataReader object of the given class, or {@link DefaultDataReader}
     * or {@link OpendapDataReader} (depending on whether the location starts with
     * "http://" or "dods://") if <code>className</code> is null or the empty string
     * @throws an Exception if the DataReader could not be created
     */
    public static DataReader getDataReader(String className, String location)
        throws Exception
    {
        String clazz = DefaultDataReader.class.getName();
        if (isOpendapLocation(location))
        {
            clazz = OpendapDataReader.class.getName();
        }
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
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single timestep only.  This method knows
     * nothing about aggregation: it simply reads data from the given file. 
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by Float.NaN.
     * 
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param vm {@link VariableMetadata} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @throws Exception if an error occurs
     */
    public abstract float[] read(String filename, VariableMetadata vm,
        int tIndex, int zIndex, float[] latValues, float[] lonValues)
        throws Exception;
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location, which may be a glob aggregation (e.g. "/path/to/*.nc").
     * @param location Full path to the dataset, may be a glob aggregation
     * @return Map of variable IDs mapped to {@link VariableMetadata} objects
     * @throws IOException if there was an error reading from the data source
     */
    public Map<String, VariableMetadata> getAllVariableMetadata(String location)
        throws IOException
    {
        // A list of names of files resulting from glob expansion
        List<String> filenames = new ArrayList<String>();
        if (isOpendapLocation(location))
        {
            // We don't do the glob expansion
            filenames.add(location);
        }
        else
        {
            // The location might be a glob expression, in which case the last part
            // of the location path will be the filter expression
            File locFile = new File(location);
            FilenameFilter filter = new GlobFilenameFilter(locFile.getName());
            File parentFile = locFile.getParentFile();
            if (parentFile == null)
            {
                throw new IOException(locFile.getPath() + " is not a valid path");
            }
            // Find the files that match the glob pattern
            File[] files = parentFile.listFiles(filter);
            if (files == null)
            {
                throw new IOException(parentFile.getPath() + " is not a valid directory");
            }
            if (files.length == 0)
            {
                throw new IOException(location + " does not match any files");
            }
            // Add all the matching filenamse
            for (File f : files)
            {
                filenames.add(f.getPath());
            }
        }
        // Now extract the data for each individual file
        Map<String, VariableMetadata> aggVars = new HashMap<String, VariableMetadata>();
        for (String filename : filenames)
        {
            // Read the metadata from the file and add them to the aggregation
            for (VariableMetadata newVar : this.getVariableMetadata(filename))
            {
                // Look to see if this variable is already present in the aggregation
                VariableMetadata existingVar = aggVars.get(newVar.getId());
                if (existingVar == null)
                {
                    // We haven't seen this variable before: just add it to the aggregation
                    aggVars.put(newVar.getId(), newVar);
                }
                else
                {
                    // We've already seen this variable: just update the timesteps
                    // TODO: check that the rest of the metadata matches
                    for (VariableMetadata.TimestepInfo tInfo : newVar.getTimesteps())
                    {
                        existingVar.addTimestepInfo(tInfo);
                    }
                }
            }
        }
        return aggVars;
    }
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param filename Full path to the dataset (N.B. not an aggregation)
     * @return List of {@link VariableMetadata} objects
     * @throws IOException if there was an error reading from the data source
     */
    protected abstract List<VariableMetadata> getVariableMetadata(String filename)
        throws IOException;
    
    private static boolean isOpendapLocation(String location)
    {
        return location.startsWith("http://") || location.startsWith("dods://");
    }

}
