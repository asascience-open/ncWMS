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
import uk.ac.rdg.resc.ncwms.datareader.VariableMetadata;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;

/**
 * Interface describing a persistent store of metadata
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public interface MetadataStore
{
    /**
     * Gets a VariableMetadata object based on its unique layer name
     * @param id The layer name of the variable (e.g. "FOAM_ONE/TMP")
     * @return The VariableMetadata object corresponding with this ID, or null
     * if there is no object with this ID
     * @throws LayerNotDefinedException if the layer does not exist.
     * @throws Exception if an error occurs reading from the persistent store
     */
    public VariableMetadata getVariableByLayerName(String layerName)
        throws LayerNotDefinedException, Exception;
    
    /**
     * Gets all the variables that belong to a dataset
     * @param datasetId The unique ID of the dataset, as defined in the config
     * file
     * @return a Collection of VariableMetadata objects that belong to this dataset
     * @throws Exception if an error occurs reading from the persistent store
     */
    public Collection<VariableMetadata> getVariablesInDataset(String datasetId)
        throws Exception;
    
    /**
     * Adds or updates a VariableMetadata object
     * @param vm The VariableMetadata object to add or update.  This object must
     * have all of its fields (including its ID and the Dataset ID) set before
     * calling this method.
     * @throws Exception if an error occurs writing to the persistent store
     */
    public void addOrUpdateVariable(VariableMetadata vm)
        throws Exception;
    
}
