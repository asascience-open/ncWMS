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

package uk.ac.rdg.resc.ncwms.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;
import uk.ac.rdg.resc.ncwms.cache.TileCache;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Contact;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.config.Server;
import uk.ac.rdg.resc.ncwms.config.Variable;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.MetadataLoader;
import uk.ac.rdg.resc.ncwms.styles.ColorPalette;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogger;
import uk.ac.rdg.resc.ncwms.usagelog.h2.H2UsageLogger;

/**
 * Displays the administrative pages of the ncWMS application (i.e. /admin/*)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class AdminController extends MultiActionController
{
    // These will be injected by Spring
    private MetadataLoader metadataLoader;
    private Config config;
    private TileCache tileCache;
    private UsageLogger usageLogger;
    
    /**
     * Displays the administrative web page
     */
    public ModelAndView displayAdminPage(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        return new ModelAndView("admin", "config", this.config);
    }
    
    /**
     * Displays the errors associated with a particular dataset
     */
    public ModelAndView displayErrorPage(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        return new ModelAndView("admin_error", "dataset", this.getDataset(request));
    }

    /**
     * Displays information about the progress of loading a particular dataset
     */
    public ModelAndView displayLoadingPage(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        return new ModelAndView("admin_loading", "dataset", this.getDataset(request));
    }

    /**
     * Gets the Dataset that is specified in the given request
     * @param request Request object, must contain a "dataset" parameter
     * @return The requested {@link Dataset}
     * @throws Exception if the request does not contain a dataset id, or if
     * the provided dataset id does not exist
     */
    private Dataset getDataset(HttpServletRequest request) throws Exception
    {
        // Get the dataset id
        String datasetId = request.getParameter("dataset");
        if (datasetId == null)
        {
            throw new Exception("Must provide a dataset id");
        }
        Dataset dataset = this.config.getDatasets().get(datasetId);
        if (dataset == null)
        {
            throw new Exception("There is no dataset with id " + datasetId);
        }
        return dataset;
    }
    
    /**
     * Displays the page showing usage statistics
     */
    public ModelAndView displayUsagePage(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        return new ModelAndView("admin_usage", "usageLogger", this.usageLogger);
    }
    
    /**
     * Converts the usage log into CSV format and sends it to the client
     * for opening in Excel
     */
    public void downloadUsageLog(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        if (!(this.usageLogger instanceof H2UsageLogger))
        {
            throw new Exception("Cannot download usage log: operation is not supported by the usage logger");
        }
        H2UsageLogger h2logger = (H2UsageLogger)this.usageLogger;
        response.setContentType("application/excel");
        response.setHeader("Content-Disposition", "inline; filename=usageLog.csv");
        h2logger.writeCsv(response.getOutputStream());
    }
    
    /**
     * Handles the submission of new configuration information from admin_index.jsp
     */
    public ModelAndView updateConfig(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        Contact contact = this.config.getContact();
        Server server = this.config.getServer();

        if (request.getParameter("contact.name") != null)
        {
            contact.setName(request.getParameter("contact.name"));
            contact.setOrg(request.getParameter("contact.org"));
            contact.setTel(request.getParameter("contact.tel"));
            contact.setEmail(request.getParameter("contact.email"));

            // Process the server details
            server.setTitle(request.getParameter("server.title"));
            server.setAbstract(request.getParameter("server.abstract"));
            server.setKeywords(request.getParameter("server.keywords"));
            server.setUrl(request.getParameter("server.url"));
            server.setMaxImageWidth(Integer.parseInt(request.getParameter("server.maximagewidth")));
            server.setMaxImageHeight(Integer.parseInt(request.getParameter("server.maximageheight")));
            server.setAllowFeatureInfo(request.getParameter("server.allowfeatureinfo") != null);
            server.setAllowGlobalCapabilities(request.getParameter("server.allowglobalcapabilities") != null);

            // Save the dataset information, checking for removals
            // First look through the existing datasets for edits.
            List<Dataset> datasetsToRemove = new ArrayList<Dataset>(); 
            // Keeps track of dataset IDs that have been changed
            Map<String, String> changedIds = new HashMap<String, String>();
            for (Dataset ds : this.config.getDatasets().values())
            {
                boolean refreshDataset = false;
                if (request.getParameter("dataset." + ds.getId() + ".remove") != null)
                {
                    datasetsToRemove.add(ds);
                }
                else
                {
                    ds.setTitle(request.getParameter("dataset." + ds.getId() + ".title"));
                    String newLocation = request.getParameter("dataset." + ds.getId() + ".location");
                    if (!newLocation.trim().equals(ds.getLocation().trim()))
                    {
                        refreshDataset = true;
                    }
                    ds.setLocation(newLocation);
                    String newDataReaderClass = request.getParameter("dataset." + ds.getId() + ".reader");
                    if (!newDataReaderClass.trim().equals(ds.getDataReaderClass().trim()))
                    {
                        refreshDataset = true;
                    }
                    ds.setDataReaderClass(newDataReaderClass);
                    boolean disabled = request.getParameter("dataset." + ds.getId() + ".disabled") != null;
                    if (disabled == false && ds.isDisabled())
                    {
                        // We've re-enabled the dataset so need to reload it
                        refreshDataset = true;
                    }
                    ds.setDisabled(disabled);
                    ds.setQueryable(request.getParameter("dataset." + ds.getId() + ".queryable") != null);
                    ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset." + ds.getId() + ".updateinterval")));
                    ds.setMoreInfo(request.getParameter("dataset." + ds.getId() + ".moreinfo"));
                    ds.setCopyrightStatement(request.getParameter("dataset." + ds.getId() + ".copyright"));
                    
                    if (request.getParameter("dataset." + ds.getId() + ".refresh") != null)
                    {
                        refreshDataset = true;
                    }
                    
                    // Check to see if we have updated the ID
                    String newId = request.getParameter("dataset." + ds.getId() + ".id").trim();
                    if (!newId.equals(ds.getId()))
                    {
                        changedIds.put(ds.getId(), newId);
                        // The ID will be changed later
                    }
                }
                if (refreshDataset)
                {
                    this.metadataLoader.scheduleMetadataReload(ds);
                }
            }
            // Now we can remove the datasets
            for (Dataset ds : datasetsToRemove)
            {
                config.removeDataset(ds);
            }
            // Now we change the ids of the relevant datasets
            for (String oldId : changedIds.keySet())
            {
                config.changeDatasetId(oldId, changedIds.get(oldId));
            }
            
            // Now look for the new datasets. The logic below means that we don't have
            // to know in advance how many new datasets the user has created (or
            // how many spaces were available in admin_index.jsp)
            int i = 0;
            while (request.getParameter("dataset.new" + i + ".id") != null)
            {
                // Look for non-blank ID fields
                if (!request.getParameter("dataset.new" + i + ".id").trim().equals(""))
                {
                    Dataset ds = new Dataset();
                    ds.setId(request.getParameter("dataset.new" + i + ".id"));
                    ds.setTitle(request.getParameter("dataset.new" + i + ".title"));
                    ds.setLocation(request.getParameter("dataset.new" + i + ".location"));
                    ds.setDataReaderClass(request.getParameter("dataset.new" + i + ".reader"));
                    ds.setDisabled(request.getParameter("dataset.new" + i + ".disabled") != null);
                    ds.setQueryable(request.getParameter("dataset.new" + i + ".queryable") != null);
                    ds.setUpdateInterval(Integer.parseInt(request.getParameter("dataset.new" + i + ".updateinterval")));
                    ds.setMoreInfo(request.getParameter("dataset.new" + i + ".moreinfo"));
                    ds.setCopyrightStatement(request.getParameter("dataset.new" + i + ".copyright"));
                    config.addDataset(ds);
                    this.metadataLoader.scheduleMetadataReload(ds);
                }
                i++;
            }
            
            // Set the properties of the cache
            config.getCache().setEnabled(request.getParameter("cache.enable") != null);
            config.getCache().setElementLifetimeMinutes(Integer.parseInt(request.getParameter("cache.elementLifetime")));
            config.getCache().setMaxNumItemsInMemory(Integer.parseInt(request.getParameter("cache.maxNumItemsInMemory")));
            config.getCache().setEnableDiskStore(request.getParameter("cache.enableDiskStore") != null);
            config.getCache().setMaxNumItemsOnDisk(Integer.parseInt(request.getParameter("cache.maxNumItemsOnDisk")));
            
            // Set the location of the THREDDS catalog if it has changed
            String newThreddsCatalogLocation = request.getParameter("thredds.catalog.location");
            if (!config.getThreddsCatalogLocation().trim().equals(newThreddsCatalogLocation))
            {
                config.setThreddsCatalogLocation(newThreddsCatalogLocation);
                // Reload Thredds datasets in a new thread TODO
            }

            // Save the updated config information to disk
            this.config.save();
        }
        
        // This causes a client-side redirect, meaning that the user can safely
        // press refresh in their browser without resubmitting the new config information.
        // TODO: ... although it probably doesn't really matter if they do.  Does it?
        return new ModelAndView("postConfigUpdate");
    }

    /**
     * Displays a page allowing the administrator to edit the attributes of the
     * variables in a certain dataset.
     * @return
     */
    public ModelAndView displayEditVariablesPage(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // First we check that there is a dataset with the given ID
        String datasetID = request.getParameter("dataset");
        if (datasetID == null)
        {
            throw new Exception("Must specify a dataset id");
        }
        Dataset ds = this.config.getDatasets().get(datasetID);
        if (ds == null)
        {
            throw new Exception("Must specify a valid dataset id");
        }
        if (!ds.isReady())
        {
            throw new Exception("Dataset must be ready before its variables can be edited");
        }
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("dataset", ds);
        models.put("paletteNames", ColorPalette.getAvailablePaletteNames());
        return new ModelAndView("editVariables", models);
    }

    /**
     * Called when the user presses "Save" or "Cancel" on the edit variables page
     */
    public ModelAndView updateVariables(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // We only take action if the user pressed "save"
        if (request.getParameter("save") != null)
        {
            Dataset ds = this.config.getDatasets().get(request.getParameter("dataset.id"));
            for (Layer layer : ds.getLayers())
            {
                String newTitle = request.getParameter(layer.getId() + ".title").trim();
                // Find the min and max colour scale range for this variable
                // TODO: nicer error handling
                float min = Float.parseFloat(request.getParameter(layer.getId() + ".scaleMin").trim());
                float max = Float.parseFloat(request.getParameter(layer.getId() + ".scaleMax").trim());
                float[] colorScaleRange = new float[]{min, max};
                // Get the variable config info. This should not be null,
                // as we will have created it in MetadataLoader.checkAttributeOverrides()
                // if it wasn't in the config file itself.
                Variable var = ds.getVariables().get(layer.getId());
                var.setTitle(newTitle);
                var.setColorScaleRange(colorScaleRange);
                var.setPaletteName(request.getParameter(layer.getId() + ".palette"));
                var.setScaling(request.getParameter(layer.getId() + ".scaling"));
            }
            // Saves the new configuration information to disk
            this.config.save();
        }
        // This causes a client-side redirect, meaning that the user can safely
        // press refresh in their browser without resubmitting the new config information.
        // TODO: ... although it probably doesn't really matter if they do.  Does it?
        return new ModelAndView("postConfigUpdate");
    }
    
    /**
     * Called by Spring to inject the metadata loading object
     */
    public void setMetadataLoader(MetadataLoader metadataLoader)
    {
        this.metadataLoader = metadataLoader;
    }
    
    /**
     * Called by Spring to inject the context containing method to save the
     * configuration information
     */
    public void setConfig(Config config)
    {
        this.config = config;
    }
    
    /**
     * Called by Spring to inject the usage logger
     */
    public void setUsageLogger(UsageLogger usageLogger)
    {
        this.usageLogger = usageLogger;
    }
    
    /**
     * Called by Spring to inject the tile cache
     */
    public void setTileCache(TileCache tileCache)
    {
        this.tileCache = tileCache;
    }
    
}
