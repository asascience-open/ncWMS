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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.grids.AbstractGrid;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Controller that handles all requests for non-standard metadata by the
 * Godiva2 site.  Eventually Godiva2 will be changed to accept standard
 * metadata (i.e. fragments of GetCapabilities)... maybe.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class MetadataController
{
    
    // These objects will be injected by Spring
    private Config config;
    private Factory<AbstractGrid> gridFactory;
    private MetadataStore metadataStore;
    
    public ModelAndView handleRequest(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        String item = request.getParameter("item");
        if (item == null)
        {
            throw new WmsException("Must provide an ITEM parameter");
        }
        else if (item.equals("datasets"))
        {
            return this.showDatasets(request, response);
        }
        else if (item.equals("variables"))
        {
            return this.showVariables(request, response);
        }
        else if (item.equals("variableDetails"))
        {
            return this.showVariableDetails(request, response);
        }
        else if (item.equals("timesteps"))
        {
            return this.showTimesteps(request, response);
        }
        else if (item.equals("minmax"))
        {
            return this.showMinMax(request, response);
        }
        else
        {
            throw new WmsException("Invalid value for ITEM parameter");
        }
    }
    
    /**
     * Shows the datasets available from this server, optionally filtered.
     * Filtering is currently done by matching the first part of the dataset
     * id (e.g. "MERSEA_BALTIC").
     */
    public ModelAndView showDatasets(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // There could be more than one filter
        String[] filters = request.getParameter("filter").split(",");
        // Find the list of displayable datasets that match any of the
        // provided filters
        List<Dataset> displayables = new ArrayList<Dataset>();
        for (Dataset ds : this.config.getDatasets().values())
        {
            if (ds.isReady()) // Check that the dataset is loaded properly
            {
                if (filters == null)
                {
                    displayables.add(ds);
                }
                else
                {
                    for (String filter : filters)
                    {
                        if (ds.getId().startsWith(filter))
                        {
                            displayables.add(ds);
                            break;
                        }
                    }
                }
            }
        }
        return new ModelAndView("showDatasets", "datasets", displayables);
    }
    
    /**
     * Shows a JSON document containing the set of variables for the given dataset.
     */
    public ModelAndView showVariables(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // Find the dataset that the user is interested in
        Dataset ds = this.getDataset(request);
        return new ModelAndView("showVariables", "dataset", ds);
    }
    
    /**
     * @return the Dataset that the user is requesting, throwing an exception if
     * it doesn't exist
     */
    private Dataset getDataset(HttpServletRequest request) throws WmsException
    {
        String dsId = request.getParameter("dataset");
        if (dsId == null)
        {
            throw new WmsException("Must provide a value for the dataset parameter");
        }
        Dataset ds = this.config.getDatasets().get(dsId);
        if (ds == null)
        {
            throw new WmsException("There is no dataset with id " + dsId);
        }
        return ds;
    }
    
    /**
     * Shows an JSON document containing the details of the given variable (units,
     * zvalues, tvalues etc).  See showVariableDetails.jsp.
     */
    public ModelAndView showVariableDetails(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        Layer layer = this.getLayer(request);
        String targetDateIso = request.getParameter("time");
        if (targetDateIso == null)
        {
            throw new WmsException("Must provide a value for the time parameter");
        }
        long targetTimeMs = WmsUtils.iso8601ToDate(targetDateIso).getTime();
        
        Map<Integer, Map<Integer, List<Integer>>> datesWithData =
            new HashMap<Integer, Map<Integer, List<Integer>>>();
        long nearestTimeMs = 0;
        
        // Takes an array of time values for a layer and turns it into a Map of
        // year numbers to month numbers to day numbers, for use in
        // showVariableDetails.jsp.  This is used to provide a list of days for
        // which we have data.  Also calculates the nearest value on the time axis
        // to the time we're currently displaying on the web interface.
        for (long ms : layer.getTvalues())
        {
            if (Math.abs(ms - targetTimeMs) < Math.abs(nearestTimeMs - targetTimeMs))
            {
                nearestTimeMs = ms;
            }
            Calendar cal = getCalendar(ms);
            int year = cal.get(Calendar.YEAR);
            Map<Integer, List<Integer>> months = datesWithData.get(year);
            if (months == null)
            {
                months = new HashMap<Integer, List<Integer>>();
                datesWithData.put(year, months);
            }
            int month = cal.get(Calendar.MONTH); // zero-based
            List<Integer> days = months.get(month);
            if (days == null)
            {
                days = new ArrayList<Integer>();
                months.put(month, days);
            }
            int day = cal.get(Calendar.DAY_OF_MONTH); // one-based
            if (!days.contains(day))
            {
                days.add(day);
            }
        }
        
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("layer", layer);
        models.put("datesWithData", datesWithData);
        models.put("nearestTimeIso", WmsUtils.millisecondsToISO8601(nearestTimeMs));
        return new ModelAndView("showVariableDetails", models);
    }
    
    /**
     * @return the Layer that the user is requesting, throwing an
     * Exception if it doesn't exist or if there was a problem reading from the
     * data store.
     */
    private Layer getLayer(HttpServletRequest request) throws Exception
    {
        Dataset ds = this.getDataset(request);
        String varId = request.getParameter("variable");
        if (varId == null)
        {
            throw new Exception("Must provide a value for the variable parameter");
        }
        Layer layer = this.metadataStore.getLayer(ds.getId(), varId);
        if (layer == null)
        {
            throw new Exception("There is no variable with id " + varId
                + " in the dataset " + ds.getId());
        }
        return layer;
    }
    
    /**
     * @return a new Calendar object, set to the given time (in milliseconds
     * since the epoch).
     */
    public static Calendar getCalendar(long millisecondsSinceEpoch)
    {
        Date date = new Date(millisecondsSinceEpoch);
        Calendar cal = Calendar.getInstance();
        // Must set the time zone to avoid problems with daylight saving
        cal.setTimeZone(WmsUtils.GMT);
        cal.setTime(date);
        return cal;
    }
    
    /**
     * Finds all the timesteps that occur on the given date, which will be provided
     * in the form "2007-10-18".
     */
    public ModelAndView showTimesteps(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        Layer layer = getLayer(request);
        String dayStr = request.getParameter("day");
        if (dayStr == null)
        {
            throw new WmsException("Must provide a value for the day parameter");
        }
        Date date = WmsUtils.iso8601ToDate(dayStr);
        if (!layer.isTaxisPresent()) return null; // return no data if no time axis present
        
        // List of times (in milliseconds since the epoch) that fall on this day
        List<Long> timesteps = new ArrayList<Long>();
        // Search exhaustively through the time values
        // TODO: inefficient: should stop once last day has been found.
        for (long tVal : layer.getTvalues())
        {
            if (onSameDay(tVal, date.getTime()))
            {
                timesteps.add(tVal);
            }
        }
        
        return new ModelAndView("showTimesteps", "timesteps", timesteps);
    }
    
    /**
     * @return true if the two given dates (in milliseconds since the epoch) fall on
     * the same day
     */
    private static boolean onSameDay(long s1, long s2)
    {
        Calendar cal1 = getCalendar(s1);
        Calendar cal2 = getCalendar(s2);
        // Set hours, minutes, seconds and milliseconds to zero for both
        // calendars
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);
        cal2.set(Calendar.HOUR_OF_DAY, 0);
        cal2.set(Calendar.MINUTE, 0);
        cal2.set(Calendar.SECOND, 0);
        cal2.set(Calendar.MILLISECOND, 0);
        // Now we know that any differences are due to the day, month or year
        return cal1.compareTo(cal2) == 0;
    }
    
    /**
     * Shows an XML document containing the minimum and maximum values for the
     * tile given in the parameters.
     */
    public ModelAndView showMinMax(HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        RequestParams params = new RequestParams(request.getParameterMap());
        // We only need the bit of the GetMap request that pertains to data extraction
        // TODO: the hard-coded "1.3.0" is ugly: it basically means that the
        // GetMapDataRequest object will look for "CRS" instead of "SRS"
        GetMapDataRequest dataRequest = new GetMapDataRequest(params, "1.3.0");
        
        // TODO: some of the code below is repetitive of WmsController: refactor?
        
        // Get the variable we're interested in
        Layer layer = this.metadataStore.getLayerByUniqueName(dataRequest.getLayers()[0]);
        
        // Get the grid onto which the data is being projected
        AbstractGrid grid = WmsController.getGrid(dataRequest, this.gridFactory);
        
        // Get the index along the z axis
        int zIndex = WmsController.getZIndex(dataRequest.getElevationString(), layer); // -1 if no z axis present
        
        // Get the information about the requested timestep (taking the first only)
        int tIndex = WmsController.getTIndices(dataRequest.getTimeString(), layer).get(0);
        
        // Now read the data
        List<float[]> picData = WmsController.readData(layer, tIndex, zIndex, grid);

        // Now find the minimum and maximum values: for a vector this is the magnitude
        boolean allFillValue = true;
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int i = 0; i < picData.get(0).length; i++)
        {
            float val = picData.get(0)[i];
            if (!Float.isNaN(val))
            {
                allFillValue = false;
                if (picData.size() == 2)
                {
                    // This is a vector quantity: calculate the magnitude
                    val = (float)Math.sqrt(val * val + picData.get(1)[i] * picData.get(1)[i]);
                }
                if (val < min) min = val;
                if (val > max) max = val;
            }
        }
        return new ModelAndView("showMinMax", "minMax", new float[]{min, max});
    }
    
    /**
     * Called by Spring to inject the gridFactory object
     */
    public void setGridFactory(Factory<AbstractGrid> gridFactory)
    {
        this.gridFactory = gridFactory;
    }

    /**
     * Called by the Spring framework to inject the config object
     */
    public void setConfig(Config config)
    {
        this.config = config;
    }

    /**
     * Called by Spring to inject the metadata store
     */
    public void setMetadataStore(MetadataStore metadataStore)
    {
        this.metadataStore = metadataStore;
    }
    
}
