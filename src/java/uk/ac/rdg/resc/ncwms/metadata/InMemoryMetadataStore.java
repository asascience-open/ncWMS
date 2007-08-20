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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;

/**
 * A MetadataStore that stores metadata in memory.  This is likely to be fast
 * but will use a large amount of memory for large datasets.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class InMemoryMetadataStore extends MetadataStore
{
    /**
     * Maps dataset IDs to maps of variable IDs to Layer objects
     */
    private Map<String, Map<String, Layer>> layers =
        new HashMap<String, Map<String, Layer>>();
    /**
     * Maps datasetIDs to times of last metadata updates (in ms since the epoch).
     */
    private Map<String, Long> lastUpdateTimes = new HashMap<String, Long>();
    
    /**
     * Gets a Layer object from a dataset
     * @param datasetId The ID of the dataset to which the layer belongs
     * @param layerId The unique ID of the layer within the dataset
     * @return The corresponding Layer, or null if there is no corresponding
     * layer in the store.
     * @throws Exception if an error occurs reading from the persistent store
     */
    public Layer getLayer(String datasetId, String layerId)
        throws Exception
    {
        Map<String, Layer> layersInDataset = this.layers.get(datasetId);
        return layersInDataset == null ? null : layersInDataset.get(layerId);
    }
    
    /**
     * Gets all the Layers that belong to a dataset
     * @param datasetId The unique ID of the dataset, as defined in the config
     * file
     * @return a Collection of Layer objects that belong to this dataset
     * @throws Exception if an error occurs reading from the persistent store
     */
    public synchronized Collection<Layer> getLayersInDataset(String datasetId)
        throws Exception
    {
        // TODO: handle case where datasetId is not a valid key
        return this.layers.get(datasetId).values();
    }
    
    /**
     * Sets the Layers that belong to the dataset with the given id, overwriting
     * all previous layers in the dataset.  This method also updates
     * the lastUpdateTime for the dataset (to harmonize with this.getLastUpdateTime()).
     * @param datasetId The ID of the dataset.
     * @param layers The Layers that belong to the dataset.
     * @throws Exception if an error occurs writing to the persistent store
     */
    public void setLayersInDataset(String datasetId, Map<String, Layer> layers)
    {
        this.layers.put(datasetId, layers);
        this.lastUpdateTimes.put(datasetId, new Date().getTime());
    }

    /**
     * @return the time of the last update of the dataset with the given id,
     * or null if the dataset has not yet been loaded into this store.  Returns
     * a new Date object with every invocation.
     */
    public Date getLastUpdateTime(String datasetId)
    {
        Long lastUpdateTime = this.lastUpdateTimes.get(datasetId);
        return lastUpdateTime == null ? null : new Date(lastUpdateTime);
    }
    
}
