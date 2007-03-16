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

import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import org.apache.log4j.Logger;
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
    private static final Logger logger = Logger.getLogger(Dataset.class);
    
    /**
     * The state of a Dataset.
     * TO_BE_LOADED: Dataset is new or has changed and needs to be loaded
     * LOADING: In the process of loading
     * READY: Ready for use
     * UPDATING: A previously-ready dataset is synchronizing with the disk
     * ERROR: An error occurred when loading the dataset.
     */
    public static enum State { TO_BE_LOADED, LOADING, READY, UPDATING, ERROR };
    
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
    @Attribute(name="updateInterval", required=false)
    private int updateInterval; // The update interval in minutes
    
    // Variables contained in this dataset, keyed by their unique IDs
    private Hashtable<String, VariableMetadata> vars;
    
    private State state; // State of this dataset
    private Exception err; // Set if there is an error loading the dataset
    private DataReader dataReader; // Object used to read data and metadata
    private Date lastUpdate; // Time at which the dataset was last updated
    
    public Dataset()
    {
        this.vars = new Hashtable<String, VariableMetadata>();
        this.state = State.TO_BE_LOADED;
        this.queryable = true;
        // We'll use a default data reader unless this is overridden in the config file
        this.dataReaderClass = "";
        this.updateInterval = -1; // Means "never update"
        this.lastUpdate = null;
    }

    public String getId()
    {
        return this.id;
    }
    
    public void setId(String id)
    {
        this.id = id.trim();
    }
    
    public String getLocation()
    {
        return location;
    }

    public synchronized void setLocation(String location)
    {
        // Mark for reload only if the location has changed
        if (this.location != null && !this.location.trim().equals(location.trim()))
        {
            // TODO: actually reload the dataset.
            this.state = State.TO_BE_LOADED;
        }
        this.location = location.trim();
    }

    public Hashtable<String, VariableMetadata> getVariables()
    {
        return vars;
    }
    
    /**
     * @return true if this dataset is ready for use
     */
    public synchronized boolean isReady()
    {
        return this.state == State.READY || this.state == State.UPDATING;
    }
    
    /**
     * @return true if this dataset can be queried using GetFeatureInfo
     */
    public boolean isQueryable()
    {
        return this.queryable;
    }
    
    public void setQueryable(boolean queryable)
    {
        this.queryable = queryable;
    }
    
    /**
     * @return the human-readable Title of this dataset
     */
    public String getTitle()
    {
        return this.title;
    }
    
    public void setTitle(String title)
    {
        this.title = title;
    }
    
    /**
     * @return true if this dataset needs to be reloaded
     */
    public synchronized boolean needsRefresh()
    {
        if (this.state == State.LOADING || this.state == State.UPDATING)
        {
            return false;
        }
        else if (this.state == State.ERROR || this.state == State.TO_BE_LOADED
            || this.lastUpdate == null)
        {
            return true;
        }
        else if (this.updateInterval < 0)
        {
            return false; // We never update this dataset
        }
        else
        {
            // State = READY.  Check the age of the metadata
            Calendar cal = Calendar.getInstance();
            cal.setTime(this.lastUpdate);
            cal.add(Calendar.MINUTE, this.updateInterval);
            // Return true if we are after the next scheduled update
            return new Date().after(cal.getTime());
        }
    }
    
    /**
     * (Re)loads the metadata for this Dataset.  Clients must call needsRefresh()
     * before calling this method to check that the metadata needs reloading.
     * This is called from the {@link WMSFilter.DatasetReloader} timer task.
     */
    public void loadMetadata()
    {
        this.state = this.state == State.READY ? State.UPDATING : State.LOADING;
        this.err = null;
        try
        {
            // Destroy any previous DataReader object (close files etc)
            if (this.dataReader != null) this.dataReader.close();
            logger.debug("Closed datareader (if it existed)");
            // Create a new DataReader object of the correct type
            if (this.dataReader == null ||
                !this.dataReader.getClass().getName().equals(this.dataReaderClass))
            {
                logger.debug("Creating new data reader of type {}", this.dataReaderClass);
                this.dataReader = DataReader.createDataReader(this.dataReaderClass,
                    this.location);
            }
            // Read the metadata
            Hashtable<String, VariableMetadata> vars = this.dataReader.getVariableMetadata();
            logger.debug("loaded VariableMetadata");
            for (VariableMetadata vm : vars.values())
            {
                vm.setDataset(this);
            }
            this.vars = vars;
            this.state = State.READY;
            this.lastUpdate = new Date();
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
     * @return true if there is an error with this dataset
     */
    public boolean isError()
    {
        return this.state == State.ERROR;
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

    /**
     * @return the update interval for this dataset in minutes
     */
    public int getUpdateInterval()
    {
        return updateInterval;
    }

    /**
     * Sets the update interval for this dataset in minutes
     */
    public void setUpdateInterval(int updateInterval)
    {
        this.updateInterval = updateInterval;
    }
}
