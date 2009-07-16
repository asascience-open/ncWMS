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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.rdg.resc.ncwms.config.Dataset;
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
     * This class can only be instantiated through forDataset()
     */
    protected DataReader(){}
    
    /**
     * Gets a DataReader for the given dataset.  Note that DataReader objects
     * may be shared among datasets.
     * @param dataset The dataset for which the DataReader is to be retrieved
     * @return a DataReader that can read data for the given layer
     * @throws Exception if there was an error retrieving the layer
     */
    public static DataReader forDataset(Dataset ds) throws Exception
    {
        String className = ds.getDataReaderClass();
        String location = ds.getLocation();
        String clazz = DefaultDataReader.class.getName();
        if (className != null && !className.trim().equals(""))
        {
            clazz = className;
        }
        // TODO make this thread safe.  Can this be done without explicit locking?
        // See Bloch, Effective Java.
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
     * Reads data from a NetCDF file.  Reads data for a single timestep only.
     * This method knows
     * nothing about aggregation: it simply reads data from the given file.
     * Missing values (e.g. land pixels in oceanography data) will be represented
     * by Float.NaN.
     *
     * @param filename Location of the file, NcML aggregation or OPeNDAP URL
     * @param layer {@link Layer} object representing the variable
     * @param tIndex The index along the time axis (or -1 if there is no time axis)
     * @param zIndex The index along the vertical axis (or -1 if there is no vertical axis)
     * @param pointList The list of real-world x-y points for which we need data
     * @return an array of floating-point data values, one for each point in
     * the {@code pointList}, in the same order.
     * @throws Exception if an error occurs
     */
    public abstract float[] read(String filename, Layer layer,
        int tIndex, int zIndex, PointList pointList)
        throws Exception;
    
    /**
     * Reads and returns the metadata for all the layers (i.e. variables) in the
     * given {@link Dataset}.
     * @param location Location of the dataset's files, as set in the admin
     * application
     * @return Map of layer IDs mapped to {@link LayerImpl} objects
     * @throws Exception if there was an error reading from the data source
     */
    public Map<String, LayerImpl> getAllLayers(final Dataset dataset)
        throws Exception
    {
        String location = dataset.getLocation();
        // A list of names of files resulting from glob expansion
        List<String> filenames = new ArrayList<String>();
        if (WmsUtils.isOpendapLocation(location))
        {
            // We don't do the glob expansion
            filenames.add(location);
        }
        else
        {
            // Add all the files in the dataset
            for (String filename : dataset.getFiles())
            {
                filenames.add(filename);
            }
        }
        if (filenames.size() == 0)
        {
            throw new Exception(location + " does not match any files");
        }
        // Create a ProgressMonitor that will update the "loading progress"
        // of the dataset
        ProgressMonitor progressMonitor = new ProgressMonitor() {
            public void updateProgress(String progress) {
                // TODO: a bit ugly
                dataset.setLoadingProgress(dataset.getLoadingProgress() + "\n" + progress);
            }
        };
        // Now extract the data for each individual file
        // LinkedHashMaps preserve the order of insertion
        Map<String, LayerImpl> layers = new LinkedHashMap<String, LayerImpl>();
        for (String filename : filenames)
        {
            // Read the metadata from the file and update the Map.
            // TODO: only do this if the file's last modified date has changed?
            // This would require us to keep the previous metadata...
            progressMonitor.updateProgress("Loading metadata from " + filename);
            this.findAndUpdateLayers(filename, layers, progressMonitor);
        }
        progressMonitor.updateProgress("Metadata loading complete");
        return layers;
    }
    
    /**
     * Reads the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param location Full path to the dataset
     * @param layers Map of Layer Ids to Layer objects to populate or update
     * @param progressMonitor A {@link ProgressMonitor} that can be updated
     * with updates on the progress with loading the metadata.  Can be null.
     * @throws Exception if there was an error reading from the data source
     */
    protected abstract void findAndUpdateLayers(String location, Map<String, LayerImpl> layers,
        ProgressMonitor progressMonitor) throws Exception;

    /**
     * Simple interface describing a class that can be used to monitor progress
     * of a long-running job such as the extraction of metadata from a dataset
     */
    public static interface ProgressMonitor
    {
        /** Updates the monitor with the latest progress information */
        public void updateProgress(String progress);
    }
}
