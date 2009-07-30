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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.oro.io.GlobFilenameFilter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.load.Commit;
import org.simpleframework.xml.load.PersistenceException;
import org.simpleframework.xml.load.Validate;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * A dataset Java bean: contains a number of Layer objects.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="dataset")
public class Dataset
{
    private static final Logger logger = LoggerFactory.getLogger(Dataset.class);
    
    /**
     * The state of a Dataset.
     * NEEDS_REFRESH: Dataset is new or has changed and needs to be loaded
     * SCHEDULED: Needs to be loaded and is in the queue
     * LOADING: In the process of loading
     * READY: Ready for use
     * UPDATING: A previously-ready dataset is synchronizing with the disk
     * ERROR: An error occurred when loading the dataset.
     */
    public static enum State { NEEDS_REFRESH, SCHEDULED, LOADING, READY, UPDATING, ERROR  };
    
    @Attribute(name="id")
    private String id; // Unique ID for this dataset
    
    @Attribute(name="location")
    private String location; // Location of this dataset (NcML file, OPeNDAP location etc)
    
    @Attribute(name="queryable", required=false)
    private boolean queryable = true; // True if we want GetFeatureInfo enabled for this dataset
    
    @Attribute(name="dataReaderClass", required=false)
    private String dataReaderClass = ""; // We'll use a default data reader
                                         // unless this is overridden in the config file
    
    @Attribute(name="copyrightStatement", required=false)
    private String copyrightStatement = "";

    @Attribute(name="moreInfo", required=false)
    private String moreInfo = "";
    
    @Attribute(name="disabled", required=false)
    private boolean disabled = false; // Set true to disable the dataset without removing it completely
    
    @Attribute(name="title")
    private String title;
    
    @Attribute(name="updateInterval", required=false)
    private int updateInterval = -1; // The update interval in minutes. -1 means "never update automatically"

    // We don't do "private List<Variable> variable..." here because if we do,
    // the config file will contain "<variable class="java.util.ArrayList>",
    // presumably because the definition doesn't clarify what sort of List should
    // be used.
    // This allows the admin to override certain auto-detected parameters of
    // the variables within the dataset (e.g. title, min and max values)
    // This is a temporary store of variables that are read from the config file.
    // The real set of all variables is in the variables Map.
    @ElementList(name="variables", type=Variable.class, required=false)
    private ArrayList<Variable> variableList = new ArrayList<Variable>();
    
    private State state;     // State of this dataset.  Will be set in Config.readConfig()
    
    private Exception err;   // Set if there is an error loading the dataset
    private StringBuffer loadingProgress = new StringBuffer(); // Used to express progress with loading
                                         // the metadata for this dataset
    private Config config;   // The Config object to which this belongs

    /**
     * This contains the map of dataset IDs to Dataset objects.  We use a
     * LinkedHashMap so that the order of datasets in the Map is preserved.
     */
    private Map<String, Variable> variables = new LinkedHashMap<String, Variable>();

    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate variable IDs.
     */
    @Validate
    public void validate() throws PersistenceException
    {
        List<String> varIds = new ArrayList<String>();
        for (Variable var : this.variableList)
        {
            String varId = var.getId();
            if (varIds.contains(varId))
            {
                throw new PersistenceException("Duplicate variable id %s", varId);
            }
            varIds.add(varId);
        }
    }

    /**
     * Called when we have checked that the configuration is valid.  Populates
     * the variables hashmap.
     */
    @Commit
    public void build()
    {
        // We already know from validate() that there are no duplicate variable
        // IDs
        for (Variable var : this.variableList)
        {
            var.setDataset(this);
            this.variables.put(var.getId(), var);
        }
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

    public void setLocation(String location)
    {
        this.location = location.trim();
    }
    
    /**
     * @return true if this dataset is ready for use
     */
    public synchronized boolean isReady()
    {
        return this.disabled == false &&
            (this.state == State.READY || this.state == State.UPDATING);
    }

    /**
     * @return true if this dataset is in the process of being loaded
     */
    public synchronized boolean isLoading()
    {
        return !this.isDisabled() &&
               (this.state == State.NEEDS_REFRESH ||
                this.state == State.SCHEDULED ||
                this.state == State.LOADING ||
                this.state == State.UPDATING);
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
     * @return true if the metadata from this dataset needs to be reloaded
     * automatically via the periodic reloader in MetadataLoader.  Note that this
     * does something more sophisticated than simply checking that
     * this.state == NEEDS_REFRESH!
     */
    public boolean needsRefresh()
    {
        DateTime lastUpdate = this.getLastUpdate();
        logger.debug("Last update time for dataset {} is {}", this.id, lastUpdate);
        logger.debug("State of dataset {} is {}", this.id, this.state);
        logger.debug("Disabled = {}", this.disabled);
        if (this.disabled || this.state == State.SCHEDULED ||
            this.state == State.LOADING || this.state == State.UPDATING)
        {
            return false;
        }
        else if (this.state == State.ERROR || this.state == State.NEEDS_REFRESH
            || lastUpdate == null)
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
            // Return true if we are after the next scheduled update
            return new DateTime().isAfter(lastUpdate.plusMinutes(this.updateInterval));
        }
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
    
    /**
     * Called by the MetadataReloader to set the error associated with this
     * dataset
     */
    public void setException(Exception e)
    {
        this.err = e;
    }
    
    public State getState()
    {
        return this.state;
    }
    
    public void setState(State state)
    {
        this.state = state;
    }
    
    @Override
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

    public void setConfig(Config config)
    {
        this.config = config;
    }
    
    /**
     * @return a Date object representing the time at which this dataset was
     * last updated, or null if this dataset has never been updated.  Delegates
     * to {@link uk.ac.rdg.resc.ncwms.metadata.MetadataStore#getLastUpdateTime}
     * (because the last update time is 
     * stored with the metadata - which may or may not be persistent across
     * server reboots, depending on the type of MetadataStore).
     */
    public DateTime getLastUpdate()
    {
        return this.config.getMetadataStore().getLastUpdateTime(this.id);
    }
    
    /**
     * @return a Collection of all the layers in this dataset.  A convenience
     * method that reads from the metadata store.
     * @throws Exception if there was an error reading from the store.
     */
    public Collection<? extends Layer> getLayers() throws Exception
    {
        return this.config.getMetadataStore().getLayersInDataset(this.id);
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    public String getCopyrightStatement()
    {
        return copyrightStatement;
    }

    public void setCopyrightStatement(String copyrightStatement)
    {
        this.copyrightStatement = copyrightStatement;
    }

    public String getMoreInfo()
    {
        return moreInfo;
    }

    public void setMoreInfo(String moreInfo)
    {
        this.moreInfo = moreInfo;
    }

    /**
     * Gets an explanation of the current progress with loading this dataset.
     * Will be displayed in the admin application when isLoading() == true.
     */
    public String getLoadingProgress()
    {
        return this.loadingProgress.toString();
    }

    /**
     * Sets an explanation of the current progress with loading this dataset.
     * Will be displayed in the admin application when isLoading() == true.
     * Also logs the progress string to the debug stream.
     */
    public void setLoadingProgress(String progress)
    {
        this.loadingProgress = new StringBuffer(progress);
    }

    /**
     * Adds a newline to the end of the {@link #getLoadingProgress() current
     * loading progress} and appends the given string.
     */
    public void appendLoadingProgress(String progressUpdate)
    {
        this.loadingProgress.append("\n" + progressUpdate);
    }

    /**
     * Gets the configuration information for all the {@link Variable}s in this
     * dataset.  This information allows the system administrator to manually
     * set certain properties that would otherwise be auto-detected.
     * @return A {@link Map} of variable IDs to {@link Variable} objects.  The
     * variable ID is unique within a dataset and corresponds with the {@link Layer#getId()}.
     * @see Variable
     */
    public Map<String, Variable> getVariables()
    {
        return variables;
    }

    public void addVariable(Variable var)
    {
        var.setDataset(this);
        this.variableList.add(var);
        this.variables.put(var.getId(), var);
    }

    /**
     * Gets a List of the files that comprise this dataset; if this dataset's
     * location is a glob expression, this will be expanded.  This method
     * recursively searches directories, allowing for glob expressions like
     * {@code "c:\\data\\200[6-7]\\*\\1*\\A*.nc"}.  If this dataset's location
     * is not a glob expression, this method will return a single-element list
     * containing the dataset's {@link #getLocation() location}.
     * @return a list of the full paths to files that comprise this dataset,
     * or a single-element list containing the dataset's location.
     * @throws Exception if the glob expression does not represent an absolute
     * path
     * @author Mike Grant, Plymouth Marine Labs; Jon Blower
     */
    public List<String> getFiles() throws Exception
    {
        List<String> filePaths = new ArrayList<String>();
        if (WmsUtils.isOpendapLocation(location))
        {
            filePaths.add(this.location);
            return filePaths;
        }
        // Check that the glob expression is an absolute path.  Relative paths
        // would cause unpredictable and platform-dependent behaviour so
        // we disallow them.
        // If ds.getLocation() is a glob expression this test will still work
        // because we are not attempting to resolve the string to a real path.
        File globFile = new File(this.location);
        if (!globFile.isAbsolute())
        {
            throw new Exception("Dataset location must be an absolute path");
        }

        // Break glob pattern into path components.  To do this in a reliable
        // and platform-independent way we use methods of the File class, rather
        // than String.split().
        List<String> pathComponents = new ArrayList<String>();
        while (globFile != null)
        {
            // We "pop off" the last component of the glob pattern and place
            // it in the first component of the pathComponents List.  We therefore
            // ensure that the pathComponents end up in the right order.
            File parent = globFile.getParentFile();
            // For a top-level directory, getName() returns an empty string,
            // hence we use getPath() in this case
            String pathComponent = parent == null ? globFile.getPath() : globFile.getName();
            pathComponents.add(0, pathComponent);
            globFile = parent;
        }

        // We must have at least two path components: one directory and one
        // filename or glob expression
        List<File> searchPaths = new ArrayList<File>();
        searchPaths.add(new File(pathComponents.get(0)));
        int i = 1; // Index of the glob path component

        while(i < pathComponents.size())
        {
            FilenameFilter globFilter = new GlobFilenameFilter(pathComponents.get(i));
            List<File> newSearchPaths = new ArrayList<File>();
            // Look for matches in all the current search paths
            for (File dir : searchPaths)
            {
                if (dir.isDirectory())
                {
                    // Workaround for automounters that don't make filesystems
                    // appear unless they're poked
                    // do a listing on searchpath/pathcomponent whether or not
                    // it exists, then discard the results
                    new File(dir, pathComponents.get(i)).list();

                    for (File match : dir.listFiles(globFilter))
                    {
                        newSearchPaths.add(match);
                    }
                }
            }
            // Next time we'll search based on these new matches and will use
            // the next globComponent
            searchPaths = newSearchPaths;
            i++;
        }

        // Now we've done all our searching, we'll only retain the files from
        // the list of search paths
        for (File path : searchPaths)
        {
            if (path.isFile()) filePaths.add(path.getPath());
        }

        return filePaths;
    }
}
