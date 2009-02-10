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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.io.RandomAccessFile;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.config.Dataset.State;
import uk.ac.rdg.resc.ncwms.config.Variable;
import uk.ac.rdg.resc.ncwms.controller.MetadataController;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.datareader.NcwmsCredentialsProvider;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Class that handles the periodic reloading of metadata (manages calls to
 * DataReader.getAllLayers()).  Initialized by the Spring framework.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class MetadataLoader
{
    private static final Logger logger = LoggerFactory.getLogger(MetadataLoader.class);
    
    private Timer timer = new Timer("Dataset reloader", true);
    
    private Config config; // Will be injected by Spring
    private MetadataStore metadataStore; // Ditto
    private NcwmsCredentialsProvider credentialsProvider; // Ditto
    
    // Controls the number of datasets that can be reloaded at any one time
    private ExecutorService datasetReloader = Executors.newFixedThreadPool(4);
    
    /**
     * Called by the Spring framework to initialize this object
     */
    public void init()
    {
        // Initialize the cache of NetcdfDatasets.  Hold between 50 and 500
        // datasets, refresh cache every 5 minutes (Are these sensible values)?
        // The length of the refresh period affects the metadata that will be
        // loaded: a long refresh period might lead to a lag in updates to
        // metadata).
        NetcdfDataset.initNetcdfFileCache(50, 500, 500, 5 * 60);
        logger.debug("NetcdfDatasetCache initialized");
        if (logger.isDebugEnabled())
        {
            // Allows us to see how many RAFs are in the NetcdfFileCache at
            // any one time
            RandomAccessFile.setDebugLeaks(true);
        }
        
        /**
         * Task that runs periodically, refreshing the metadata catalogue.
         * Each dataset is loaded in a new thread
         * @todo Use a thread pool to prevent server overload?
         */
        this.timer.schedule(new TimerTask() {
            @Override
            public void run()
            {
                for (Dataset ds : config.getDatasets().values())
                {
                    boolean needsRefresh = false;
                    synchronized(ds)
                    {
                        if (ds.needsRefresh())
                        {
                            // Schedule this dataset for refreshing
                            needsRefresh = true;
                        }
                    }
                    if (needsRefresh) scheduleMetadataReload(ds);
                }
            }
        }, 0, 60 * 1000); // Check every minute after no delay
        
        logger.debug("Started periodic reloading of datasets");
    }
    
    /**
     * Called by both the periodic reloader and the
     * {@link uk.ac.rdg.resc.ncwms.controller.AdminController AdminController}
     * (to force a reload of the given dataset, without waiting for the periodic reloader).
     */
    public void scheduleMetadataReload(final Dataset ds)
    {
        logger.debug("Scheduling reload of dataset {}", ds.getId());
        synchronized(ds)
        {
            ds.setState(State.SCHEDULED);
        }
        
        this.datasetReloader.execute(new Runnable() {
            public void run() {
                doReloadMetadata(ds);
            }}
        );
    }
        
    private void doReloadMetadata(final Dataset ds)
    {
        // Include the id of the dataset in the thread for debugging purposes
        // Comment this out to use the default thread names (e.g. "pool-2-thread-1")
        Thread.currentThread().setName("load-metadata-" + ds.getId());
        logger.debug("Loading metadata for {}", ds.getId());
        synchronized(ds)
        {
            ds.setState(ds.getState() == State.READY ? State.UPDATING : State.LOADING);
        }
        try
        {
            // Get a DataReader object of the correct type
            logger.debug("Getting data reader of type {}", ds.getDataReaderClass());
            DataReader dr = DataReader.getDataReader(ds.getDataReaderClass(), ds.getLocation());
            // Look for OPeNDAP datasets and update the credentials provider accordingly
            this.updateCredentialsProvider(ds);
            // Read the metadata
            Map<String, LayerImpl> layers = dr.getAllLayers(ds);
            for (LayerImpl layer : layers.values())
            {
                layer.setDataset(ds);
            }
            logger.debug("loaded layers");
            // Search for vector quantities (e.g. northward/eastward_sea_water_velocity)
            findVectorQuantities(ds, layers);
            logger.debug("found vector quantities");
            // Look for overriding attributes in the configuration
            readLayerConfig(ds, layers);
            logger.debug("attributes overridden");
            // Update the metadata store
            this.metadataStore.setLayersInDataset(ds.getId(), layers);
            ds.setState(State.READY);
            // TODO: set this when reading from database too.
            this.config.setLastUpdateTime(new Date());
            // Save the config information to the file
            this.config.save();
            logger.debug("Loaded metadata for {}, num RAFs open = {}", ds.getId(),
                RandomAccessFile.getOpenFiles().size());
        }
        catch(Exception e)
        {
            ds.setState(State.ERROR);
            // Reduce logging volume by only logging the error if it's a new
            // type of exception.
            if (ds.getException() == null || ds.getException().getClass() != e.getClass())
            {
                logger.error(e.getClass().getName() + " loading metadata for dataset "  + ds.getId(), e);
            }
            ds.setException(e);
            logger.debug("{} loading metadata for {}, num RAFs open = {}",
                new Object[]{e.getClass().getName(), ds.getId(),
                RandomAccessFile.getOpenFiles().size()});
        }
    }
    
    /**
     * Searches through the collection of Layer objects, looking for
     * pairs of quantities that represent the components of a vector, e.g.
     * northward/eastward_sea_water_velocity.  Modifies the given Hashtable
     * in-place.
     * @todo Only works for northward/eastward and x/y components so far
     */
    private static void findVectorQuantities(Dataset ds, Map<String, LayerImpl> layers)
    {
        // This hashtable will store pairs of components in eastward-northward
        // order, keyed by the standard name for the vector quantity
        Map<String, LayerImpl[]> components = new HashMap<String, LayerImpl[]>();
        for (LayerImpl layer : layers.values())
        {
            if (layer.getTitle().contains("eastward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("eastward_", "");
                // Look to see if we've already found the northward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the northward component yet
                    components.put(vectorKey, new LayerImpl[2]);
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
                    components.put(vectorKey, new LayerImpl[2]);
                }
                components.get(vectorKey)[1] = layer;
            }
            // We can only calculate vectors if the components are eastward
            // and northward (otherwise we need to know the directions of the
            // grid lines at each point - which isn't impossible but is hard).
            /*else if (layer.getTitle().contains("_x_"))
            {
                String vectorKey = layer.getTitle().replaceFirst("_x_", "_");
                // Look to see if we've already found the y component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the y component yet
                    components.put(vectorKey, new LayerImpl[2]);
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
                    components.put(vectorKey, new LayerImpl[2]);
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
                    components.put(vectorKey, new LayerImpl[2]);
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
                    components.put(vectorKey, new LayerImpl[2]);
                }
                components.get(vectorKey)[1] = layer;
            }*/
        }
        
        // Now add the vector quantities to the collection of Layer objects
        for (String key : components.keySet())
        {
            LayerImpl[] comps = components.get(key);
            if (comps[0] != null && comps[1] != null)
            {
                comps[0].setDataset(ds);
                comps[1].setDataset(ds);
                // We've found both components.  Create a new Layer object
                LayerImpl vec = new VectorLayerImpl(key, comps[0], comps[1]);
                // Use the title as the unique ID for this variable
                vec.setId(key);
                layers.put(key, vec);
            }
        }
    }

    /**
     * Read the configuration information from individual layers from the
     * config file.
     */
    private static void readLayerConfig(Dataset dataset, Map<String, LayerImpl> layers)
    {
        for (LayerImpl layer : layers.values())
        {
            // Load the Variable object from the config file or create a new
            // one if it doesn't exist.
            Variable var = dataset.getVariables().get(layer.getId());
            if (var == null)
            {
                var = new Variable();
                var.setId(layer.getId());
                dataset.addVariable(var);
            }

            // If there is no title set for this layer in the config file, we
            // use the title that was read by the DataReader.
            if (var.getTitle() == null) var.setTitle(layer.getTitle());

            // Set the colour scale range.  If this isn't specified in the
            // config information, load an "educated guess" at the scale range
            // from the source data.
            if (var.getColorScaleRange() == null)
            {
                float[] minMax;
                try
                {
                    // Set the scale range for each variable by reading a 100x100
                    // chunk of data and finding the min and max values of this chunk.
                    HorizontalGrid grid = new HorizontalGrid("CRS:84", 100, 100, layer.getBbox());
                    // Read from the first t and z indices
                    int tIndex = layer.isTaxisPresent() ? 0 : -1;
                    int zIndex = layer.isZaxisPresent() ? 0 : -1;
                    minMax = MetadataController.findMinMax(layer, tIndex,
                        zIndex, grid, null);
                    if (Float.isNaN(minMax[0]) || Float.isNaN(minMax[1]))
                    {
                        // Just guess at a scale
                        minMax = new float[]{-50.0f, 50.0f};
                    }
                    else if (minMax[0] == minMax[1])
                    {
                        // This happens occasionally if the above algorithm happens
                        // to hit an area of uniform data.  We make sure that
                        // the max is greater than the min.
                        minMax[1] = minMax[0] + 1.0f;
                    }
                    else
                    {
                        // Set the scale range of the layer, factoring in a 10% expansion
                        // to deal with the fact that the sample data we read might
                        // not be representative
                        float diff = minMax[1] - minMax[0];
                        minMax = new float[]{minMax[0] - 0.05f * diff,
                            minMax[1] + 0.05f * diff};
                    }
                }
                catch(Exception e)
                {
                    logger.error("Error reading min-max from layer " + layer.getId()
                        + " in dataset " + dataset.getId(), e);
                    minMax = new float[]{-50.0f, 50.0f};
                }
                var.setColorScaleRange(minMax);
            }
        }
    }
    
    /**
     * If the given dataset is an OPeNDAP location, this looks for
     * a username and password and, if it finds one, updates the 
     * credentials provider
     */
    private void updateCredentialsProvider(Dataset ds)
    {
        logger.debug("Called updateCredentialsProvider, {}", ds.getLocation());
        if (WmsUtils.isOpendapLocation(ds.getLocation()))
        {
            // Make sure the URL starts with "http://" or the 
            // URL parsing might not work
            // (TODO: register dods:// as a valid protocol?)
            String newLoc = "http" + ds.getLocation().substring(4);
            try
            {
                URL url = new URL(newLoc);
                String userInfo = url.getUserInfo();
                logger.debug("user info = {}", userInfo);
                if (userInfo != null)
                {
                    this.credentialsProvider.addCredentials(
                        url.getHost(),
                        url.getPort() >= 0 ? url.getPort() : url.getDefaultPort(),
                        userInfo);
                }
                // Change the location to "dods://..." so that the Java NetCDF
                // library knows to use the OPeNDAP protocol rather than plain
                // http
                ds.setLocation("dods" + newLoc.substring(4));
            }
            catch(MalformedURLException mue)
            {
                logger.warn(newLoc + " is not a valid url");
            }
        }
    }
    
    /**
     * Called by the Spring framework to clean up this object
     */
    public void close()
    {
        this.timer.cancel();
        this.datasetReloader.shutdown();
        this.config = null;
        NetcdfDataset.shutdown();
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

    public void setCredentialsProvider(NcwmsCredentialsProvider credentialsProvider)
    {
        this.credentialsProvider = credentialsProvider;
    }
    
}
