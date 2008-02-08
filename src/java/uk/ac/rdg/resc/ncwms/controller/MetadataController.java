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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
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
import uk.ac.rdg.resc.ncwms.exceptions.MetadataException;
import uk.ac.rdg.resc.ncwms.datareader.TargetGrid;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogger;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Controller that handles all requests for non-standard metadata by the
 * Godiva2 site.  Eventually Godiva2 will be changed to accept standard
 * metadata (i.e. fragments of GetCapabilities)... maybe.
 *
 * @todo Output exceptions in JSON format for display on web interface?
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
    private MetadataStore metadataStore;
    private UsageLogger usageLogger;
    
    public ModelAndView handleRequest(HttpServletRequest request,
        HttpServletResponse response, UsageLogEntry usageLogEntry)
        throws MetadataException
    {
        try
        {
            // Check for a "url" parameter, which means that we're delegating to
            // a third-party layer server (TODO)
            String url = request.getParameter("url");
            if (url != null && !url.trim().equals(""))
            {
                usageLogEntry.setRemoteServerUrl(url);
                proxyRequest(url, request, response);
                return null; // proxyRequest writes directly to the response object
            }
            String item = request.getParameter("item");
            usageLogEntry.setWmsOperation("GetMetadata:" + item);
            if (item == null)
            {
                throw new Exception("Must provide an ITEM parameter");
            }
            else if (item.equals("menu"))
            {
                return this.showMenu(request, usageLogEntry);
            }
            else if (item.equals("layerDetails"))
            {
                return this.showLayerDetails(request, usageLogEntry);
            }
            else if (item.equals("timesteps"))
            {
                return this.showTimesteps(request);
            }
            else if (item.equals("minmax"))
            {
                return this.showMinMax(request);
            }
            else
            {
                throw new Exception("Invalid value for ITEM parameter");
            }
        }
        catch(Exception e)
        {
            // Wrap all exceptions in a MetadataException.  These will be automatically
            // displayed via displayMetadataException.jsp, in JSON format
            throw new MetadataException(e);
        }
    }
    
    /**
     * Forwards the request to a third party.  In this case this server is acting
     * as a proxy.
     * @param url The URL to the third party server (e.g. "http://myhost.com/ncWMS/wms")
     * @param request Http request object.  All query string parameters (except "&url=")
     * will be copied from this request object to the request to the third party server.
     * @param response Http response object
     */
    static void proxyRequest(String url, HttpServletRequest request,
        HttpServletResponse response) throws Exception
    {
        // The commented-out code below will only be relevant for third-party
        // plain WMSs.
        /*ThirdPartyLayerProvider layerProvider =
            this.config.getThirdPartyLayerProviders().get(url);
        if (layerProvider == null)
        {
            throw new Exception("Layer provider at URL " + url + " not registered");
        }
        if (layerProvider.getType() != ThirdPartyLayerProvider.Type.NCWMS)
        {
            throw new Exception("Can only handle ncWMS third-party providers");
        }*/
        // Download the data from the remote URL
        // TODO: is there a proxy class we can invoke here?
        StringBuffer fullURL = new StringBuffer(url);
        boolean firstTime = true;
        for (Object urlParamNameObj : request.getParameterMap().keySet())
        {
            fullURL.append(firstTime ? "?" : "&");
            firstTime = false;
            String urlParamName = (String)urlParamNameObj;
            if (!urlParamName.equalsIgnoreCase("url"))
            {
                fullURL.append(urlParamName + "=" + request.getParameter(urlParamName));
            }
        }
        InputStream in = null;
        OutputStream out = null;
        try
        {
            // TODO: better error handling
            URLConnection conn = new URL(fullURL.toString()).openConnection();
            // Set header information (TODO: do all headers)
            response.setContentType(conn.getContentType());
            response.setContentLength(conn.getContentLength());
            in = conn.getInputStream();
            out = response.getOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) >= 0)
            {
                out.write(buf, 0, len);
            }
        }
        finally
        {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
    
    /**
     * Shows the hierarchy of layers available from this server, or a pre-set
     * hierarchy.
     */
    private ModelAndView showMenu(HttpServletRequest request, UsageLogEntry usageLogEntry)
        throws Exception
    {
        String menu = "default";
        String menuFromRequest = request.getParameter("menu");
        if (menuFromRequest != null && !menuFromRequest.trim().equals(""))
        {
            menu = menuFromRequest.toLowerCase();
        }
        usageLogEntry.setMenu(menu);
        Map<String, Object> models = new HashMap<String, Object>();
        models.put("serverTitle", this.config.getServer().getTitle());
        models.put("datasets", this.config.getDatasets());
        return new ModelAndView(menu + "Menu", models);
    }
    
    /**
     * Shows an JSON document containing the details of the given variable (units,
     * zvalues, tvalues etc).  See showLayerDetails.jsp.
     */
    private ModelAndView showLayerDetails(HttpServletRequest request,
        UsageLogEntry usageLogEntry) throws Exception
    {
        Layer layer = this.getLayer(request);
        usageLogEntry.setLayer(layer);
        String targetDateIso = request.getParameter("time");
        if (targetDateIso == null || targetDateIso.trim().equals(""))
        {
            throw new Exception("Must provide a value for the time parameter");
        }
        long targetTimeMs = WmsUtils.iso8601ToDate(targetDateIso).getTime();
        
        Map<Integer, Map<Integer, List<Integer>>> datesWithData =
            new HashMap<Integer, Map<Integer, List<Integer>>>();
        long nearestTimeMs = layer.isTaxisPresent() && layer.getTvalues().length > 0 
            ? layer.getTvalues()[0] : 0;
        
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
            Calendar cal = getGMTCalendar(ms);
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
        return new ModelAndView("showLayerDetails", models);
    }
    
    /**
     * @return the Layer that the user is requesting, throwing an
     * Exception if it doesn't exist or if there was a problem reading from the
     * data store.
     */
    private Layer getLayer(HttpServletRequest request) throws Exception
    {
        String layerName = request.getParameter("layerName");
        if (layerName == null)
        {
            throw new Exception("Must provide a value for the layerName parameter");
        }
        Layer layer = this.metadataStore.getLayerByUniqueName(layerName);
        if (layer == null)
        {
            throw new Exception("There is no layer with the name " + layerName);
        }
        return layer;
    }
    
    /**
     * @return a new Calendar object, set to the given time (in milliseconds
     * since the epoch), using the GMT timezone.
     */
    private static Calendar getGMTCalendar(long millisecondsSinceEpoch)
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
    private ModelAndView showTimesteps(HttpServletRequest request)
        throws Exception
    {
        Layer layer = getLayer(request);
        String dayStr = request.getParameter("day");
        if (dayStr == null)
        {
            throw new Exception("Must provide a value for the day parameter");
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
        Calendar cal1 = getGMTCalendar(s1);
        Calendar cal2 = getGMTCalendar(s2);
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
    private ModelAndView showMinMax(HttpServletRequest request)
        throws Exception
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
        TargetGrid grid = new TargetGrid(dataRequest);
        
        // Get the index along the z axis
        int zIndex = WmsController.getZIndex(dataRequest.getElevationString(), layer); // -1 if no z axis present
        
        // Get the information about the requested timestep (taking the first only)
        int tIndex = WmsController.getTIndices(dataRequest.getTimeString(), layer).get(0);
        
        // Now read the data and calculate the minimum and maximum values
        float[] minMax = findMinMax(layer, tIndex, zIndex, grid);
        
        return new ModelAndView("showMinMax", "minMax", minMax);
    }
    
    /**
     * Finds the minimum and maximum values of data in the given arrays.
     * @param layer the Layer from which to read data
     * @param tIndex the time index, or -1 if there is no time axis
     * @param zIndex the z index, or -1 if there is to vertical axis
     * @param grid The grid onto which the data is to be read
     * @return Array of two floats: [min, max]
     * @throws Exception if there was an error reading the data
     */
    public static float[] findMinMax(Layer layer, int tIndex, int zIndex, TargetGrid grid)
        throws Exception
    {
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
        return new float[]{min, max};
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
    
    /**
     * Called by Spring to inject the usage logger
     */
    public void setUsageLogger(UsageLogger usageLogger)
    {
        this.usageLogger = usageLogger;
    }
    
}
