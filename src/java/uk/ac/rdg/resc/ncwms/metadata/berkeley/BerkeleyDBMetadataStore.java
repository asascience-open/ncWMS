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

package uk.ac.rdg.resc.ncwms.metadata.berkeley;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Stores metadata in a Berkeley database.
 * @deprecated Not used in current code.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class BerkeleyDBMetadataStore extends MetadataStore
{
    private static final Logger logger = Logger.getLogger(BerkeleyDBMetadataStore.class);
    
    /**
     * The name of the directory in which we will create the database
     */
    private static final String DATABASE_DIR_NAME = "metadataDB";
    /**
     * The name of the database in which we will store Dataset objects
     */
    private static final String STORE_NAME = "datasets";
    
    private Environment env;
    private EntityStore store; // This is where we keep Layer objects
    
    private PrimaryIndex<String, BerkeleyDBDataset> datasetsById;
    
    /**
     * Initializes this store.
     * @throws Exception if there was an error initializing the database
     * @todo can the environment be shared with the cache of image data?
     */
    public void init() throws Exception
    {
        // Set up the database environment
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(true);
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setTransactional(true);
        
        // Create the database environment
        File dbPath = new File(this.ncwmsContext.getWorkingDirectory(), DATABASE_DIR_NAME);
        WmsUtils.createDirectory(dbPath);
        this.env = new Environment(dbPath, envConfig);
        this.store = new EntityStore(this.env, STORE_NAME, storeConfig);
        
        // Set up the index that will be used to store simple Dataset objects
        this.datasetsById = this.store.getPrimaryIndex(String.class, BerkeleyDBDataset.class);
        
        logger.debug("Database for Dataset objects created in " + dbPath.getPath());
    }

    /**
     * Gets a Layer object from a dataset
     * 
     * @param datasetId The ID of the dataset to which the layer belongs
     * @param layerId The unique ID of the layer within the dataset
     * @return The corresponding Layer, or null if there is no corresponding
     * layer in the store.
     * @throws Exception if an error occurs reading from the persistent store
     */
    public Layer getLayer(String datasetId, String layerId) throws Exception
    {
        logger.debug("Retrieving layer {} from dataset {} from Berkeley DB...",
            layerId, datasetId);
        BerkeleyDBDataset bds = this.datasetsById.get(datasetId);
        if (bds != null)
        {
            Layer layer = bds.getLayers().get(layerId);
            if (layer != null)
            {
                Dataset ds = this.config.getDatasets().get(datasetId);
                this.addDatasetProperty(layer, ds);
            }
            logger.debug("... found.");
            return layer;
        }
        logger.debug("... not found.");
        return null;
    }

    /**
     * Gets all the Layers that belong to a dataset
     * @param datasetId The unique ID of the dataset, as defined in the config
     * file
     * @return a Collection of Layer objects that belong to this dataset, or null
     * if there is no dataset with the given ID.
     * @throws Exception if an error occurs reading from the persistent store
     */
    public Collection<Layer> getLayersInDataset(String datasetId)
        throws Exception
    {
        logger.debug("Retrieving all layers from dataset {} from Berkeley DB...",
            datasetId);
        BerkeleyDBDataset bds = this.datasetsById.get(datasetId);
        if (bds != null)
        {
            Dataset ds = this.config.getDatasets().get(datasetId);
            Collection<Layer> layers = bds.getLayers().values();
            for (Layer layer : layers)
            {
                addDatasetProperty(layer, ds);
            }
            logger.debug("... found");
            return layers;
        }
        logger.debug("... not found");
        return null;
    }

    /**
     * @return the time of the last update of the dataset with the given id,
     * or null if the dataset has not yet been loaded into this store.  Returns
     * null (and logs the error) if an error occurs reading the Date from the
     * persistent store.
     */
    public Date getLastUpdateTime(String datasetId)
    {
        try
        {
            logger.debug("Retrieving last update time for dataset {} from Berkeley DB...",
                datasetId);
            BerkeleyDBDataset ds = this.datasetsById.get(datasetId);
            logger.debug("... {}found", ds == null ? "not " : "");
            return ds == null ? null : ds.getLastUpdate();
        }
        catch (DatabaseException dbe)
        {
            logger.error("Error reading last update time for dataset " + datasetId, dbe);
            return null;
        }
    }

    /**
     * Sets the Layers that belong to the dataset with the given id, overwriting
     * all previous layers in the dataset.  This method should also update
     * the lastUpdateTime for the dataset (to harmonize with this.getLastUpdateTime()).
     * 
     * @param datasetId The ID of the dataset.
     * @param layers The Layers that belong to the dataset.  Maps layer IDs
     * (unique within a dataset) to Layer objects.
     * @throws Exception if an error occurs writing to the persistent store
     */
    public void setLayersInDataset(String datasetId, Map<String, Layer> layers) throws Exception
    {
        try
        {
            logger.debug("Setting new layers for dataset {}...", datasetId);
            // TODO: should we do this in a Transaction?  There's only one write
            // operation so I assume we're OK.
            BerkeleyDBDataset ds = this.datasetsById.get(datasetId);
            if (ds == null)
            {
                ds = new BerkeleyDBDataset();
                ds.setId(datasetId);
            }
            ds.setLayers(layers);
            ds.setLastUpdate(new Date());
            this.datasetsById.put(ds);
            logger.debug("... done");
        }
        catch(Exception e)
        {
            logger.error("Error setting new layers for dataset " + datasetId, e);
            throw e;
        }
    }
    
    /**
     * Closes the database.  This will be called automatically by the Spring
     * framework.
     * @throws DatabaseException if an error occurred
     */
    public void close() throws DatabaseException
    {
        if (this.store != null) this.store.close();
        if (this.env != null)
        {
            this.env.cleanLog();
            this.env.close();
        }
        logger.debug("Berkeley database of Layer objects closed");
    }
    
    /**
     * Runs some speed test to see how long it takes to retrieve a particular
     * dataset
     */
    public static void main(String[] args) throws Exception
    {
        MetadataStore ms = new BerkeleyDBMetadataStore();
        NcwmsContext context = new NcwmsContext();
        ms.setNcwmsContext(context);
        ms.init();
        Config config = Config.readConfig(context, ms);
        ms.setConfig(config);
        
        System.out.println("Starting test");
        long start = System.currentTimeMillis();
        int n = 10;
        for (int i = 0; i < n; i++)
        {
            ms.getLayersInDataset("NCOF_AMM");
        }
        long finish = System.currentTimeMillis();
        System.out.println("Finished in " + (finish - start) + " ms");
    }
    
}
