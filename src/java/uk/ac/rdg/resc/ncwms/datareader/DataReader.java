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
import org.apache.oro.io.GlobFilenameFilter;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Abstract superclass for classes that read data and metadata from datasets.
 * Only one instance of each DataReader class will be created, hence subclasses
 * must be thread-safe (no instance variables etc).
 * @see DefaultDataReader
 * @author jdb
 */
public abstract class DataReader
{

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
     * @param location the location of the dataset: used to detect OPeNDAP URLs
     * @return a DataReader object of the given class, or {@link DefaultDataReader}
     * or {@link BoundingBoxDataReader} (depending on whether the location starts with
     * "http://" or "dods://") if <code>className</code> is null or the empty string
     * @throws an Exception if the DataReader could not be created
     */
    public static DataReader getDataReader(String className, String location)
        throws Exception
    {
        String clazz = DefaultDataReader.class.getName();
        if (WmsUtils.isOpendapLocation(location))
        {
            clazz = BoundingBoxDataReader.class.getName();
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
     * Reads an array of data from a NetCDF file and projects onto the given
     * {@link HorizontalGrid}.  Reads data for a single timestep only.  This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by Float.NaN.
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param grid The grid onto which the data are to be read
     * @throws Exception if an error occurs
     */
    public abstract float[] read(String filename, Layer layer,
        int tIndex, int zIndex, HorizontalGrid grid)
        throws Exception;
    
    /**
     * Reads and returns the metadata for all the layers (i.e. variables) in the
     * given {@link Dataset}.
     * @param location Location of the dataset's files, as set in the admin
     * application
     * @return Map of layer IDs mapped to {@link LayerImpl} objects
     * @throws Exception if there was an error reading from the data source
     */
    public Map<String, LayerImpl> getAllLayers(String location)
        throws Exception
    {
        // A list of names of files resulting from glob expansion
        List<String> filenames = new ArrayList<String>();
        if (WmsUtils.isOpendapLocation(location))
        {
            // We don't do the glob expansion
            filenames.add(location);
        }
        else
        {
            // Add all the matching filenames
            for (File f : globFiles(location))
            {
                filenames.add(f.getPath());
            }
        }
        if (filenames.size() == 0)
        {
            throw new Exception(location + " does not match any files");
        }
        // Now extract the data for each individual file
        Map<String, LayerImpl> layers = new HashMap<String, LayerImpl>();
        for (String filename : filenames)
        {
            // Read the metadata from the file and update the Map.
            // TODO: only do this if the file's last modified date has changed?
            // This would require us to keep the previous metadata...
            this.findAndUpdateLayers(filename, layers);
        }
        return layers;
    }
    
    /**
     * Reads the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the dataset
     * @param layers Map of Layer Ids to Layer objects to populate or update
     * @throws IOException if there was an error reading from the data source
     */
    protected abstract void findAndUpdateLayers(String location, Map<String, LayerImpl> layers)
        throws IOException;


    /**
     * Finds all files (and, optionally, directories, etc) matching
     * a glob pattern.  This method recursively searches directories, allowing
     * for glob expressions like {@code "c:\\data\\200[6-7]\\*\\1*\\A*.nc"}.
     * @param globExpression The glob expression
     * @return List of File objects matching the glob pattern.  This will never
     * be null but might be empty
     * @throws Exception if the glob expression does not represent an absolute
     * path
     * @author Mike Grant, Plymouth Marine Labs; Jon Blower
     */
    private static List<File> globFiles(String globExpression) throws Exception
    {
        // Check that the glob expression is an absolute path.  Relative paths
        // would cause unpredictable and platform-dependent behaviour so
        // we disallow them.
        // If ds.getLocation() is a glob expression this test will still work
        // because we are not attempting to resolve the string to a real path.
        File globFile = new File(globExpression);
        if (!globFile.isAbsolute())
        {
            throw new Exception("Dataset location must be an absolute path");
        }
        
        // Break glob pattern into path components.  To do this in a reliable
        // and platform-independent way we use methods of the File class, rather
        // than String.split().
        List<String> pathComponents = new ArrayList<String>();
        while (globFile != null)
        {
            // We "pop off" the last component of the glob pattern and place
            // it in the first component of the pathComponents List.  We therefore
            // ensure that the pathComponents end up in the right order.
            File parent = globFile.getParentFile();
            // For a top-level directory, getName() returns an empty string,
            // hence we use getPath() in this case
            String pathComponent = parent == null ? globFile.getPath() : globFile.getName();
            pathComponents.add(0, pathComponent);
            globFile = parent;
        }
        
        // We must have at least two path components: one directory and one
        // filename or glob expression
        List<File> searchPaths = new ArrayList<File>();
        searchPaths.add(new File(pathComponents.get(0)));
        int i = 1; // Index of the glob path component
        
        while(i < pathComponents.size())
        {
            FilenameFilter globFilter = new GlobFilenameFilter(pathComponents.get(i));
            List<File> newSearchPaths = new ArrayList<File>();
            // Look for matches in all the current search paths
            for (File dir : searchPaths)
            {
                if (dir.isDirectory())
                {
                    // Workaround for automounters that don't make filesystems
                    // appear unless they're poked
                    // do a listing on searchpath/pathcomponent whether or not
                    // it exists, then discard the results
                    new File(dir, pathComponents.get(i)).list();
                    
                    for (File match : dir.listFiles(globFilter))
                    {
                        newSearchPaths.add(match);
                    }
                }
            }
            // Next time we'll search based on these new matches and will use
            // the next globComponent
            searchPaths = newSearchPaths;
            i++;
        }
        
        // Now we've done all our searching, we'll only retain the files from
        // the list of search paths
        List<File> filesToReturn = new ArrayList<File>();
        for (File path : searchPaths)
        {
            if (path.isFile()) filesToReturn.add(path);
        }
        
        return filesToReturn;
    }
}
