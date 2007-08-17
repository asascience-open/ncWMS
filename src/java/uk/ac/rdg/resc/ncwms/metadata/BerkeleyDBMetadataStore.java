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

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.SecondaryIndex;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;
import uk.ac.rdg.resc.ncwms.datareader.VariableMetadata;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Stores metadata in a Berkeley database.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class BerkeleyDBMetadataStore implements MetadataStore
{
    private static final Logger logger = Logger.getLogger(BerkeleyDBMetadataStore.class);
    
    /**
     * The name of the directory in which we will create the database
     */
    private static final String DATABASE_DIR_NAME = "metadataDB";
    /**
     * The name of the database in which we will store VariableMetadata objects
     */
    private static final String STORE_NAME = "variables";
    
    private Environment env;
    private EntityStore store; // This is where we keep VariableMetadata objects
    
    private PrimaryIndex<String, VariableMetadata> variablesByLayerName;
    private SecondaryIndex<String, String, VariableMetadata> variablesByDatasetId;
    
    // Injected by Spring: gives the path to the working directory
    private NcwmsContext ncwmsContext;
    
    /**
     * This is called by the Spring framework to initialize this object
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
        
        // Set up the indices that we will use to access VariableMetadata objects
        this.variablesByLayerName = this.store.getPrimaryIndex(String.class,
            VariableMetadata.class);
        // The string "datasetId" matches the name of the dataset id field in
        // VariableMetadata
        this.variablesByDatasetId = this.store.getSecondaryIndex(this.variablesByLayerName,
            String.class, "datasetId");
        
        logger.debug("Database for VariableMetadata objects created in " + dbPath.getPath());
    }

    /**
     * Gets all the variables that belong to a dataset
     * 
     * @param datasetId The unique ID of the dataset, as defined in the config
     * file
     * @return a Collection of VariableMetadata objects that belong to this dataset
     * @throws Exception if an error occurs reading from the persistent store
     */
    public Collection<VariableMetadata> getVariablesInDataset(String datasetId)
        throws Exception
    {
        // EntityCursors are not thread-safe so this method must be synchronized
        EntityCursor<VariableMetadata> cursor = null;
        List<VariableMetadata> vars = new ArrayList<VariableMetadata>();
        try
        {
            cursor = this.variablesByDatasetId.subIndex(datasetId).entities();
            for (VariableMetadata var : cursor)
            {
                vars.add(var);
            }
            return vars;
        }
        finally
        {
            if (cursor != null)
            {
                cursor.close();
            }
        }
    }

    /**
     * Gets a VariableMetadata object based on its unique layer name
     * 
     * @param id The layer name of the variable (e.g. "FOAM_ONE/TMP")
     * @return The VariableMetadata object corresponding with this ID, or null
     * if there is no object with this ID
     * @throws LayerNotDefinedException if the layer does not exist.
     * @throws Exception if an error occurs reading from the persistent store
     */
    public VariableMetadata getVariableByLayerName(String layerName)
        throws LayerNotDefinedException, Exception
    {
        return this.variablesByLayerName.get(layerName);
    }

    /**
     * Adds or updates a VariableMetadata object
     * 
     * @param vm The VariableMetadata object to add or update.  This object must
     * have all of its fields (including its ID and the Dataset ID) set before
     * calling this method.
     * @throws Exception if an error occurs writing to the persistent store
     */
    public void addOrUpdateVariable(VariableMetadata vm) throws Exception
    {
        this.variablesByLayerName.put(vm);
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
        logger.debug("Database of VariableMetadata objects closed");
    }
    
    /**
     * Called by Spring to inject the context containing the path to the ncWMS
     * working directory.
     */
    public void setNcwmsContext(NcwmsContext ncwmsContext)
    {
        this.ncwmsContext = ncwmsContext;
    }
    
}
