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

import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Interface describing a persistent store of metadata
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class MetadataStore
{
    
    // This is set by Spring and is needed so that subclasses know where to
    // store metadata
    protected NcwmsContext ncwmsContext;
    
    // This is set by Config.readConfig() once the config information has been
    // read.  It is needed to allow the methods that return Layers to set 
    // the Dataset objects to which Layers belong.
    protected Config config;
    
    /**
     * Subclasses can override this method to provide initialization code, which
     * is called after injection of all fields by Spring (i.e. the context object
     * will be set before calling this method).
     * This default method does nothing.
     */
    public void init() throws Exception {}
    
    /**
     * Gets a Layer object from the metadata store.  This is a convenience
     * method that wraps this.getLayer(), checking for valid input and throwing a
     * LayerNotDefinedException if the layer is not present in the store.
     * @param uniqueLayerName The unique Id of the layer, as returned by
     * WmsUtils.createUniqueLayerName().
     */
    public final Layer getLayerByUniqueName(String uniqueLayerName)
        throws LayerNotDefinedException, Exception
    {
        try
        {
            String[] els = WmsUtils.parseUniqueLayerName(uniqueLayerName);
            Layer layer = this.getLayer(els[0], els[1]);
            if (layer == null)
            {
                throw new LayerNotDefinedException(uniqueLayerName);
            }
            return layer;
        }
        catch(ParseException pe)
        {
            throw new LayerNotDefinedException(uniqueLayerName);
        }
    }
    
    /**
     * Gets a Layer object from a dataset
     * @param datasetId The ID of the dataset to which the layer belongs
     * @param layerId The unique ID of the layer within the dataset
     * @return The corresponding Layer, or null if there is no corresponding
     * layer in the store.
     * @throws Exception if an error occurs reading from the persistent store
     */
    public abstract Layer getLayer(String datasetId, String layerId)
        throws Exception;
    
    /**
     * Gets all the Layers that belong to a dataset
     * @param datasetId The unique ID of the dataset, as defined in the config
     * file
     * @return a Collection of Layer objects that belong to this dataset
     * @throws Exception if an error occurs reading from the persistent store
     */
    public abstract Collection<Layer> getLayersInDataset(String datasetId)
        throws Exception;
    
    /**
     * Sets the Layers that belong to the dataset with the given id, overwriting
     * all previous layers in the dataset.  This method should also update
     * the lastUpdateTime for the dataset (to harmonize with this.getLastUpdateTime()).
     * @param datasetId The ID of the dataset.
     * @param layers The Layers that belong to the dataset.  Maps layer IDs
     * (unique within a dataset) to Layer objects.
     * @throws Exception if an error occurs writing to the persistent store
     */
    public abstract void setLayersInDataset(String datasetId, Map<String, Layer> layers)
        throws Exception;
    
    /**
     * @return the time of the last update of the dataset with the given id,
     * or null if the dataset has not yet been loaded into this store.  If an
     * error occurs loading the last update time (which should be unlikely)
     * implementing classes should log the error and return null.
     */
    public abstract Date getLastUpdateTime(String datasetId);
    
    /**
     * Sets the Dataset property on the given layer.  Checks for Vector
     * layers, setting the dataset property on the component layers too.
     */
    protected static void addDatasetProperty(Layer layer, Dataset ds)
    {
        ((LayerImpl)layer).setDataset(ds);
        if (layer instanceof VectorLayer)
        {
            VectorLayer vecLayer = (VectorLayer)layer;
            ((LayerImpl)vecLayer.getEastwardComponent()).setDataset(ds);
            ((LayerImpl)vecLayer.getNorthwardComponent()).setDataset(ds);
        }
    }
    
    /**
     * Called by Spring to clean up this store. Subclasses should override if
     * necessary.
     */
    public void close() throws Exception {}

    /**
     * Called by Config.readConfig() to set the config object containing
     * the configuration of this ncWMS server.
     */
    public void setConfig(Config config)
    {
        this.config = config;
    }

    /**
     * Called by Spring to inject the context object
     */
    public void setNcwmsContext(NcwmsContext ncwmsContext)
    {
        this.ncwmsContext = ncwmsContext;
    }
    
}
