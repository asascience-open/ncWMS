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
import uk.ac.rdg.resc.ncwms.datareader.VariableMetadata;

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
    
    // These will be injected by Spring
    private Config config;
    private MetadataStore metadataStore;
    
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
            logger.debug("Reloading metadata...");
            for (Dataset ds : config.getDatasets().values())
            {
                reloadMetadata(ds);
            }
            logger.debug("... Metadata reloaded");
        }
    }
    
    /**
     * Called by the AdminController to force a reload of the given dataset
     * in a new thread, without waiting for the periodic reloader.
     */
    public void forceReloadMetadata(final Dataset ds)
    {
        new Thread()
        {
            public void run()
            {
                logger.debug("Loading metadata for {}", ds.getLocation());
                boolean loaded = reloadMetadata(ds);
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
    private boolean reloadMetadata(Dataset ds)
    {
        // We must make this part of the method thread-safe because more than
        // one thread might be trying to update the metadata.
        // TODO: re-examine this strategy
        synchronized(ds)
        {
            if (ds.needsRefresh())
            {
                ds.setState(ds.getState() == State.READY ? State.UPDATING : State.LOADING);
                ds.setException(null);
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
            Map<String, VariableMetadata> vars = dr.getAllVariableMetadata(ds.getLocation());
            logger.debug("loaded VariableMetadata");
            // Search for vector quantities (e.g. northward/eastward_sea_water_velocity)
            findVectorQuantities(vars);
            for (VariableMetadata vm : vars.values())
            {
                vm.setDataset(ds);
                metadataStore.addOrUpdateVariable(vm);
            }
            ds.setState(State.READY);
            Date lastUpdate = new Date();
            ds.setLastUpdate(lastUpdate);
            config.setLastUpdateTime(lastUpdate);
            return true;
        }
        catch(Exception e)
        {
            logger.error("Error loading metadata for dataset " + ds.getId(), e);
            ds.setException(e);
            ds.setState(State.ERROR);
            return false;
        }
    }
    
    /**
     * Searches through the collection of VariableMetadata objects, looking for
     * pairs of quantities that represent the components of a vector, e.g.
     * northward/eastward_sea_water_velocity.  Modifies the given Hashtable
     * in-place.
     * @todo Only works for northward/eastward and x/y components so far
     */
    private static void findVectorQuantities(Map<String, VariableMetadata> vars)
    {
        // This hashtable will store pairs of components in eastward-northward
        // order, keyed by the standard name for the vector quantity
        Map<String, VariableMetadata[]> components = new HashMap<String, VariableMetadata[]>();
        for (VariableMetadata vm : vars.values())
        {
            if (vm.getTitle().contains("eastward"))
            {
                String vectorKey = vm.getTitle().replaceFirst("eastward_", "");
                // Look to see if we've already found the northward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the northward component yet
                    components.put(vectorKey, new VariableMetadata[2]);
                }
                components.get(vectorKey)[0] = vm;
            }
            else if (vm.getTitle().contains("northward"))
            {
                String vectorKey = vm.getTitle().replaceFirst("northward_", "");
                // Look to see if we've already found the eastward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the eastward component yet
                    components.put(vectorKey, new VariableMetadata[2]);
                }
                components.get(vectorKey)[1] = vm;
            }
            else if (vm.getTitle().contains("_x_"))
            {
                String vectorKey = vm.getTitle().replaceFirst("_x_", "_");
                // Look to see if we've already found the y component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the y component yet
                    components.put(vectorKey, new VariableMetadata[2]);
                }
                components.get(vectorKey)[0] = vm;
            }
            else if (vm.getTitle().contains("_y_"))
            {
                String vectorKey = vm.getTitle().replaceFirst("_y_", "_");
                // Look to see if we've already found the x component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the x component yet
                    components.put(vectorKey, new VariableMetadata[2]);
                }
                components.get(vectorKey)[1] = vm;
            }
            else if (vm.getTitle().startsWith("x_"))
            {
                String vectorKey = vm.getTitle().replaceFirst("x_", "");
                // Look to see if we've already found the y component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the y component yet
                    components.put(vectorKey, new VariableMetadata[2]);
                }
                components.get(vectorKey)[0] = vm;
            }
            else if (vm.getTitle().startsWith("y_"))
            {
                String vectorKey = vm.getTitle().replaceFirst("y_", "");
                // Look to see if we've already found the x component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the x component yet
                    components.put(vectorKey, new VariableMetadata[2]);
                }
                components.get(vectorKey)[1] = vm;
            }
        }
        
        // Now add the vector quantities to the collection of VariableMetadata objects
        for (String key : components.keySet())
        {
            VariableMetadata[] comps = components.get(key);
            if (comps[0] != null && comps[1] != null)
            {
                // We've found both components.  Create a new VariableMetadata object
                VariableMetadata vec = new VariableMetadata(key, comps[0], comps[1]);
                // Use the title as the unique ID for this variable
                vec.setId(key);
                vars.put(key, vec);
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
        logger.debug("Cleaned up MetadataLoader");
    }

    /**
     * Called by the Spring framework to inject the configuration object
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
