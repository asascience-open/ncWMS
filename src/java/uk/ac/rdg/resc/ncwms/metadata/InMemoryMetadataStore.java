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
public class InMemoryMetadataStore implements MetadataStore
{
    /**
     * Maps dataset IDs to maps of variable IDs to Layer objects
     */
    private Map<String, Map<String, Layer>> layers =
        new HashMap<String, Map<String, Layer>>();
    
    /**
     * Gets a Layer object based on its unique id
     * @param id The layer name of the variable (e.g. "FOAM_ONE/TMP")
     * @return The Layer object corresponding with this ID, or null
     * if there is no object with this ID
     * @throws LayerNotDefinedException if the layer does not exist.
     * @throws Exception if an error occurs reading from the persistent store
     */
    public synchronized Layer getLayerById(String layerId)
        throws LayerNotDefinedException, Exception
    {
        // NOTE!! The logic of this method must match up with
        // Layer.getLayerName()!
        String[] dsAndVarIds = layerId.split("/");
        if (dsAndVarIds.length != 2)
        {
            throw new LayerNotDefinedException(layerId);
        }
        Map<String, Layer> layersInDataset = this.layers.get(dsAndVarIds[0]);
        if (layersInDataset == null)
        {
            throw new LayerNotDefinedException(layerId);
        }
        Layer layer = layersInDataset.get(dsAndVarIds[1]);
        if (layer == null)
        {
            throw new LayerNotDefinedException(layerId);
        }
        return layer;
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
     * Adds or updates a Layer object
     * @param Layer The Layer object to add or update.  This object must
     * have all of its fields (including its ID and the Dataset ID) set before
     * calling this method.
     * @throws Exception if an error occurs writing to the persistent store
     */
    public synchronized void addOrUpdateLayer(Layer layer) throws Exception
    {
        String datasetId = layer.getDataset().getId();
        Map<String, Layer> layersInDataset = this.layers.get(datasetId);
        if (layersInDataset == null)
        {
            layersInDataset = new HashMap<String, Layer>();
            this.layers.put(datasetId, layersInDataset);
        }
        layersInDataset.put(layer.getId(), layer);
    }
    
}
