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

package uk.ac.rdg.resc.ncwms.config;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import simple.xml.Element;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.load.Commit;
import simple.xml.load.PersistenceException;
import simple.xml.load.Validate;
import uk.ac.rdg.resc.ncwms.config.thredds.ThreddsConfig;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;

/**
 * Configuration of the server.  We use Simple XML Serialization
 * (http://simple.sourceforge.net/) to convert to and from XML.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="config")
public class Config
{
    private static final Logger logger = Logger.getLogger(Config.class);
    
    @Element(name="contact", required=false)
    private Contact contact = new Contact();
    
    @Element(name="server")
    private Server server = new Server();
    
    // We don't do "private List<Dataset> datasetList..." here because if we do,
    // the config file will contain "<datasets class="java.util.ArrayList>",
    // presumably because the definition doesn't clarify what sort of List should
    // be used.
    // This is a temporary store of datasets that are read from the config file.
    // The real set of all datasets is in the datasets Map.
    @ElementList(name="datasets", type=Dataset.class)
    private ArrayList<Dataset> datasetList = new ArrayList<Dataset>();
    
    @Element(name="threddsCatalog", required=false)
    private String threddsCatalogLocation = null;    //location of the Thredds Catalog.xml (if there is one...)
    // This will store the datasets we have read from the thredds catalog
    private List<Dataset> threddsDatasets = new ArrayList<Dataset>();
    
    private Date lastUpdateTime = new Date(); // Time of the last update to this configuration
                                              // or any of the contained metadata
    private MetadataStore metadataStore; // Gives access to metadata
    
    /**
     * This contains the map of dataset IDs to Dataset objects
     */
    private Map<String, Dataset> datasets = new HashMap<String, Dataset>();
    
    /**
     * Package-private constructor: only the NcwmsContext object is allowed to
     * create new Config objects.  This prevents other classes being tempted to
     * create new Config objects instead of accessing through NcwmsContext.
     */
    Config() {}
    
    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate dataset IDs.
     */
    @Validate
    public void checkDuplicateDatasetIds() throws PersistenceException
    {
        List<String> dsIds = new ArrayList<String>();
        for (Dataset ds : datasetList)
        {
            String dsId = ds.getId();
            if (dsIds.contains(dsId))
            {
                throw new PersistenceException("Duplicate dataset id %s", dsId);
            }
            dsIds.add(dsId);
        }
        for (Dataset ds : threddsDatasets)
        {
            String dsId = ds.getId();
            if (dsIds.contains(dsId))
            {
                throw new PersistenceException("Duplicate dataset id %s", dsId);
            }
            dsIds.add(dsId);
        }
    }
    
    /**
     * Called when we have checked that the configuration is valid.  Populates
     * the datasets hashmap and loads the datasets from the THREDDS catalog.
     */
    @Commit
    public void build()
    {
        for (Dataset ds : this.datasetList)
        {
            ds.setConfig(this);
            this.datasets.put(ds.getId(), ds);
        }
        this.loadThreddsCatalog();
    }
    
    public synchronized void setLastUpdateTime(Date date)
    {
        if (date.after(this.lastUpdateTime))
        {
            this.lastUpdateTime = date;
        }
    }
    
    public Date getLastUpdateTime()
    {
        return this.lastUpdateTime;
    }
    
    /**
     * @return the time of the last change to the configuration or metadata,
     * in milliseconds since the epoch
     */
    public long getLastUpdateTimeMilliseconds()
    {
        return this.lastUpdateTime.getTime();
    }

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    public Contact getContact()
    {
        return contact;
    }

    public void setContact(Contact contact)
    {
        this.contact = contact;
    }
    
    public Map<String, Dataset> getDatasets()
    {
        return this.datasets;
    }
    
    public void addDataset(Dataset ds)
    {
        ds.setConfig(this);
        this.datasets.put(ds.getId(), ds);
    }
    
    public void removeDataset(Dataset ds)
    {
        this.datasets.remove(ds.getId());
    }
    
    /**
     * Used by Dataset to provide a method to get variables
     */
    MetadataStore getMetadataStore()
    {
        return this.metadataStore;
    }
    
    public void loadThreddsCatalog()
    {
        // First remove the datasets that belonged to the THREDDS catalog.
        for (Dataset ds : this.threddsDatasets)
        {
            this.removeDataset(ds);
        }
        if (this.threddsCatalogLocation != null && !this.threddsCatalogLocation.trim().equals(""))
        {
            logger.debug("Loading datasets from THREDDS catalog at " + this.threddsCatalogLocation);
            try
            {
                this.threddsDatasets = ThreddsConfig.readThreddsDatasets(this.threddsCatalogLocation);

                logger.debug("Number of thredds Datasets: " + this.threddsDatasets.size());

                for(Dataset d : this.threddsDatasets)
                {
                    logger.debug("adding thredds dataset: " + d.getTitle() + " id: " + d.getId());
                    this.addDataset(d);
                }
            }
            catch(Exception e)
            {
                logger.error("Problems loading thredds catalog at " + this.threddsCatalogLocation, e);
            }
        }
    }
    
    public String getThreddsCatalogLocation()
    {
        return this.threddsCatalogLocation;
    }
    
    public void setThreddsCatalogLocation(String threddsCatalogLocation)
    {
        this.threddsCatalogLocation = threddsCatalogLocation;
    }
    
    /**
     * Called by NcwmsContext to inject the metadata store
     */
    public void setMetadataStore(MetadataStore metadataStore)
    {
        this.metadataStore = metadataStore;
    }
    
}
