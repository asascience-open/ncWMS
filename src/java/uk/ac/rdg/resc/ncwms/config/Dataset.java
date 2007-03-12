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

import java.util.Hashtable;
import simple.xml.Attribute;
import simple.xml.Root;
import uk.ac.rdg.resc.ncwms.datareader.DataReader;
import uk.ac.rdg.resc.ncwms.datareader.VariableMetadata;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

/**
 * A dataset Java bean: contains a number of VariableMetadata objects.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="dataset")
public class Dataset
{
    /**
     * The state of a Dataset
     */
    public static enum State { TO_BE_LOADED, LOADING, READY, ERROR };
    /**
     * The datasets currently in memory: maps ids to Dataset objects
     */
    private static Hashtable<String, Dataset> datasets = new Hashtable<String, Dataset>();
    
    @Attribute(name="id")
    private String id; // Unique ID for this dataset
    @Attribute(name="location")
    private String location; // Location of this dataset (NcML file, OPeNDAP location etc)
    @Attribute(name="queryable", required=false)
    private boolean queryable; // True if we want GetFeatureInfo enabled for this dataset
    @Attribute(name="dataReaderClass", required=false)
    private String dataReaderClass;
    @Attribute(name="title")
    private String title;
    
    // Variables contained in this dataset, keyed by their unique IDs
    private Hashtable<String, VariableMetadata> vars; 
    private State state; // State of this dataset
    private Exception err; // Set if there is an error loading the dataset
    private DataReader dataReader; // Object used to read data and metadata
    
    
    public Dataset()
    {
        this.vars = new Hashtable<String, VariableMetadata>();
        this.state = State.TO_BE_LOADED;
        this.queryable = true;
        // We'll use a default data reader unless this is overridden in the config file
        this.dataReaderClass = "";
    }

    public String getId()
    {
        return this.id;
    }
    
    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
        // Mark for reload: TODO: reload immediately?
        this.state = State.TO_BE_LOADED;
    }

    public Hashtable<String, VariableMetadata> getVariables()
    {
        return vars;
    }
    
    /**
     * @return true if this dataset is ready for use
     */
    public boolean isReady()
    {
        return this.state == State.READY;
    }
    
    /**
     * @return true if this dataset can be queried using GetFeatureInfo
     */
    public boolean isQueryable()
    {
        return this.queryable;
    }
    
    /**
     * @return the human-readable Title of this dataset
     */
    public String getTitle()
    {
        return this.title;
    }
    
    /**
     * @return true if this dataset needs to be reloaded
     */
    public boolean needsRefresh()
    {
        if (this.state == State.LOADING)
        {
            return false;
        }
        else if (this.state == State.ERROR || this.state == State.TO_BE_LOADED)
        {
            return true;
        }
        else
        {
            // State = READY.  TODO: check the age of the metadata
            return true;
        }
    }
    
    /**
     * (Re)loads the metadata for this Dataset.  Clients must call needsRefresh()
     * before calling this method to check that the metadata needs reloading.
     * This is called from the {@link GlobalFilter.DatasetReloader} timer task.
     */
    public void loadMetadata()
    {
        this.state = State.LOADING;
        try
        {
            // Clear the store of variables
            this.vars = null;
            // Destroy any previous DataReader object (close files etc)
            if (this.dataReader != null) this.dataReader.close();
            // Create a new DataReader object of the correct type
            this.dataReader = DataReader.createDataReader(this.getDataReaderClass(),                 this.location);
            // Read the metadata
            this.vars = this.dataReader.getVariableMetadata();
            for (VariableMetadata vm : this.vars.values())
            {
                vm.setDataset(this);
            }
            this.state = State.READY;
        }
        catch(Exception e)
        {
            this.err = e;
            this.state = State.ERROR;
        }
    }
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single time index only.  Delegates to 
     * the DataReader.
     *
     * @param vm {@link VariableMetadata} object representing the variable
     * @param tIndex The index along the time axis as found in getmap.py
     * @param zValue The value of elevation as specified by the client
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @return array of data values
     * @throws WMSExceptionInJava if an error occurs
     */
    public float[] read(VariableMetadata vm,
        int tIndex, String zValue, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava
    {
        return this.dataReader.read(vm, tIndex, zValue, latValues, lonValues, fillValue);
    }
    
    /**
     * If this Dataset has not been loaded correctly, this returns the Exception
     * that was thrown.  If the dataset has no errors, this returns null.
     */
    public Exception getException()
    {
        return this.state == State.ERROR ? this.err : null;
    }
    
    public State getState()
    {
        return this.state;
    }
    
    public String toString()
    {
        return "id: " + this.id + ", location: " + this.location;
    }

    public String getDataReaderClass()
    {
        return dataReaderClass;
    }

    public void setDataReaderClass(String dataReaderClass)
    {
        this.dataReaderClass = dataReaderClass;
    }
}
