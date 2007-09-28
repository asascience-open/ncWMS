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

package uk.ac.rdg.resc.ncwms.metadata;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.log4j.Logger;
import ucar.nc2.dataset.NetcdfDatasetCache;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.config.Dataset.State;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;

/**
 * Class that handles the periodic reloading of metadata (manages calls to
 * Dataset.loadMetadata()).  Initialized by the Spring framework.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class MetadataLoader
{
    private static final Logger logger = Logger.getLogger(MetadataLoader.class);
    
    private Timer timer;
    
    private Config config; // Will be injected by Spring
    private MetadataStore metadataStore; // Ditto
    
    /**
     * Called by the Spring framework to initialize this object
     */
    public void init()
    {
        // Initialize the cache of NetcdfDatasets
        NetcdfDatasetCache.init();
        logger.debug("NetcdfDatasetCache initialized");
        
        // Now start the regular TimerTask that periodically checks to see if
        // the datasets need reloading
        this.timer = new Timer("Dataset reloader", true);
        // TODO: read this interval from an init-param
        int intervalMs = 60 * 1000; // Check every minute
        this.timer.schedule(new DatasetReloader(), 0, intervalMs);
        logger.debug("Started periodic reloading of datasets");
    }
    
    /**
     * Task that runs periodically, refreshing the metadata catalogue
     */
    private class DatasetReloader extends TimerTask
    {
        public void run()
        {
            for (Dataset ds : config.getDatasets().values())
            {
                reloadMetadata(ds);
            }
        }
    }
    
    /**
     * Called by the AdminController to force a reload of the given dataset
     * in a new thread, without waiting for the periodic reloader.
     */
    public void forceReloadMetadata(final Dataset ds)
    {
        ds.setState(State.TO_BE_LOADED); // causes needsRefresh() to return true
        reloadMetadata(ds);
    }
    
    /**
     * Reloads the metadata for a given dataset in a new thread
     */
    private void reloadMetadata(final Dataset ds)
    {
        new Thread()
        {
            public void run()
            {
                logger.debug("Loading metadata for {}", ds.getLocation());
                boolean loaded = doReloadMetadata(ds);
                String message = loaded ? "Loaded metadata for {}" :
                    "Did not load metadata for {}";
                logger.debug(message, ds.getLocation());
            }
        }.start();
    }
    
    /**
     * (Re)loads the metadata for the given dataset.
     * @return true if the metadata were (re)loaded, false if no reload was
     * necessary, or if there was an error loading the metadata.
     */
    private boolean doReloadMetadata(Dataset ds)
    {
        // We must make this part of the method thread-safe because more than
        // one thread might be trying to update the metadata.
        // TODO: re-examine this strategy
        synchronized(ds)
        {
            // needsRefresh() examines the last update time of the dataset from
            // the MetadataStore
            if (ds.needsRefresh())
            {
                ds.setState(ds.getState() == State.READY ? State.UPDATING : State.LOADING);
            }
            else
            {
                return false;
            }
        }
        try
        {
            // Get a DataReader object of the correct type
            logger.debug("Getting data reader of type {}", ds.getDataReaderClass());
            DataReader dr = DataReader.getDataReader(ds.getDataReaderClass(), ds.getLocation());
            // Read the metadata
            Map<String, Layer> layers = dr.getAllLayers(ds.getLocation());
            logger.debug("loaded layers");
            // Search for vector quantities (e.g. northward/eastward_sea_water_velocity)
            findVectorQuantities(layers);
            // Set the Dataset property for all layers
            for (Layer layer : layers.values())
            {
                // Must convert to mutable layer before altering
                ((LayerImpl)layer).setDataset(ds);
            }
            // Update the metadata store
            this.metadataStore.setLayersInDataset(ds.getId(), layers);
            ds.setState(State.READY);
            Date lastUpdate = new Date();
            // TODO: set this when reading from database too.
            this.config.setLastUpdateTime(lastUpdate);
            return true;
        }
        catch(Exception e)
        {
            ds.setState(State.ERROR);
            // Reduce logging volume by only logging the error if it's a new
            // type of exception.
            if (ds.getException() == null || ds.getException().getClass() != e.getClass())
            {
                logger.error("Error loading metadata for dataset " + ds.getId(), e);
            }
            ds.setException(e);
            return false;
        }
    }
    
    /**
     * Searches through the collection of Layer objects, looking for
     * pairs of quantities that represent the components of a vector, e.g.
     * northward/eastward_sea_water_velocity.  Modifies the given Hashtable
     * in-place.
     * @todo Only works for northward/eastward and x/y components so far
     */
    private static void findVectorQuantities(Map<String, Layer> layers)
    {
        // This hashtable will store pairs of components in eastward-northward
        // order, keyed by the standard name for the vector quantity
        Map<String, Layer[]> components = new HashMap<String, Layer[]>();
        for (Layer layer : layers.values())
        {
            if (layer.getTitle().contains("eastward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("eastward_", "");
                // Look to see if we've already found the northward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the northward component yet
                    components.put(vectorKey, new Layer[2]);
                }
                components.get(vectorKey)[0] = layer;
            }
            else if (layer.getTitle().contains("northward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("northward_", "");
                // Look to see if we've already found the eastward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the eastward component yet
                    components.put(vectorKey, new Layer[2]);
                }
                components.get(vectorKey)[1] = layer;
            }
            else if (layer.getTitle().contains("_x_"))
            {
                String vectorKey = layer.getTitle().replaceFirst("_x_", "_");
                // Look to see if we've already found the y component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the y component yet
                    components.put(vectorKey, new Layer[2]);
                }
                components.get(vectorKey)[0] = layer;
            }
            else if (layer.getTitle().contains("_y_"))
            {
                String vectorKey = layer.getTitle().replaceFirst("_y_", "_");
                // Look to see if we've already found the x component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the x component yet
                    components.put(vectorKey, new Layer[2]);
                }
                components.get(vectorKey)[1] = layer;
            }
            else if (layer.getTitle().startsWith("x_"))
            {
                String vectorKey = layer.getTitle().replaceFirst("x_", "");
                // Look to see if we've already found the y component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the y component yet
                    components.put(vectorKey, new Layer[2]);
                }
                components.get(vectorKey)[0] = layer;
            }
            else if (layer.getTitle().startsWith("y_"))
            {
                String vectorKey = layer.getTitle().replaceFirst("y_", "");
                // Look to see if we've already found the x component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the x component yet
                    components.put(vectorKey, new Layer[2]);
                }
                components.get(vectorKey)[1] = layer;
            }
        }
        
        // Now add the vector quantities to the collection of Layer objects
        for (String key : components.keySet())
        {
            Layer[] comps = components.get(key);
            if (comps[0] != null && comps[1] != null)
            {
                // We've found both components.  Create a new Layer object
                LayerImpl vec = new VectorLayerImpl(key, comps[0], comps[1]);
                // Use the title as the unique ID for this variable
                vec.setId(key);
                layers.put(key, vec);
            }
        }
    }
    
    /**
     * Called by the Spring framework to clean up this object
     */
    public void close()
    {
        if (this.timer != null) this.timer.cancel();
        this.config = null;
        NetcdfDatasetCache.exit();
        logger.info("Cleaned up MetadataLoader");
    }

    /**
     * Called by the Spring framework to inject the config object
     */
    public void setConfig(Config config)
    {
        this.config = config;
    }

    /**
     * Called by Spring to inject the metadata store
     */
    public void setMetadataStore(MetadataStore metadataStore)
    {
        this.metadataStore = metadataStore;
    }
    
}
