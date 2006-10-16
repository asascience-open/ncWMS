/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.ncwms.dataprovider;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;

/**
 * Interface describing methods that must be implemented by a DataProvider.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class DataProvider
{
    
    protected String id; // Unique ID
    protected String title; // Human-readable title
    protected String location; // Location of the underlying dataset
    protected Hashtable<String, DataLayer> layers; // Map of DataLayers to 
        // their unique IDs.
    
    /**
     * Protected constructor to prevent direct creation of a DataProvider
     */
    protected DataProvider()
    {
        this.layers = new Hashtable<String, DataLayer>();
    }
    
    /**
     * Creates a new DataProvider
     * @param id The unique id for the data provider
     * @param title A human-readable title for the data provider
     * @param location Location of the underlying data
     * @return The DataProvider object
     * @throws IOException if there was an error reading the metadata of the
     * underlying dataset
     */
    public static DataProvider create(String id, String title, String location)
        throws IOException
    {
        // For now, always create a DefaultDataProvider
        DataProvider dp = new DefaultDataProvider(location);
        dp.id = id;
        dp.title = title;
        dp.location = location;
        return dp;
    }
    
    /**
     * @return a unique ID for this DataProvider
     */
    public final String getID()
    {
        return this.id;
    }
    
    /**
     * @return a human-readable title for this DataProvider
     */
    public final String getTitle()
    {
        return this.title;
    }
    
    /**
     * @return the location of the underlying data
     */
    public final String getLocation()
    {
        return this.location;
    }
    
    /**
     * @return all the {@link DataLayer}s that are contained in this
     * DataProvider
     */
    public final Collection<DataLayer> getDataLayers()
    {
        return this.layers.values();
    }
    
    /**
     * Gets the DataLayer with the given id
     * @param id The unique id of the DataLayer
     * @return the DataLayer, or null if it does not exist
     */
    public final DataLayer getDataLayer(String id)
    {
        return this.layers.get(id);
    }
    
}
