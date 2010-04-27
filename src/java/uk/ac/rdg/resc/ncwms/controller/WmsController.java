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

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import uk.ac.rdg.resc.ncwms.coords.CrsHelper;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.LonLatPosition;
import uk.ac.rdg.resc.ncwms.coords.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.coords.LineString;
import uk.ac.rdg.resc.ncwms.coords.PixelMap;
import uk.ac.rdg.resc.ncwms.coords.PointList;
import uk.ac.rdg.resc.ncwms.exceptions.CurrentUpdateSequence;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidUpdateSequence;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotQueryableException;
import uk.ac.rdg.resc.ncwms.exceptions.OperationNotSupportedException;
import uk.ac.rdg.resc.ncwms.exceptions.Wms1_1_1Exception;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.graphics.ImageFormat;
import uk.ac.rdg.resc.ncwms.graphics.KmzFormat;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogger;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.util.Range;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;

/**
 * <p>This Controller is the entry point for all standard WMS operations
 * (GetMap, GetCapabilities, GetFeatureInfo).  Only one WmsController object 
 * is created.  Spring manages the creation of this object and the injection 
 * of the objects that it needs (i.e. its dependencies), such as the
 * {@linkplain ServerConfig configuration object}.
 * The Spring configuration file <tt>web/WEB-INF/WMS-servlet.xml</tt>
 * defines all this information and also defines that this Controller will handle
 * all requests to the URI pattern <tt>/wms</tt>.  (See the SimpleUrlHandlerMapping
 * in <tt>web/WEB-INF/WMS-servlet.xml</tt>).</p>
 *
 * <p>See the {@link #handleRequestInternal handleRequestInternal()}
 * method for more information.</p>
 *
 * <p><i>(Note that we cannot use a CommandController here
 * because there is no (apparent) way in Spring to use case-insensitive parameter
 * names to bind request parameters to an object.)</i></p>
 *
 * @author Jon Blower
 */
public class WmsController extends AbstractController {

    private static final Logger log = LoggerFactory.getLogger(WmsController.class);
    /**
     * The maximum number of layers that can be requested in a single GetMap
     * operation
     */
    private static final int LAYER_LIMIT = 1;
    private static final String FEATURE_INFO_XML_FORMAT = "text/xml";
    private static final String FEATURE_INFO_PNG_FORMAT = "image/png";

    // This object handles requests for non-standard metadata
    private MetadataController metadataController;

    // These objects will be injected by Spring
    private ServerConfig serverConfig;
    private UsageLogger usageLogger;

    /**
     * Called automatically by Spring after all the dependencies have been
     * injected.
     */
    public void init() {
        // Create a MetadataController for handling non-standard metadata request
        this.metadataController = new MetadataController(this.serverConfig);

        // We initialize the ColorPalettes.  We need to do this from here
        // because we need a way to find out the real path of the 
        // directory containing the palettes.  Therefore we need a way of 
        // getting at the ServletContext object, which isn't available from
        // the ColorPalette class.
        String paletteLocation = this.getWebApplicationContext()
            .getServletContext().getRealPath("/WEB-INF/conf/palettes");
        File paletteLocationDir = new File(paletteLocation);
        if (paletteLocationDir.exists() && paletteLocationDir.isDirectory()) {
            ColorPalette.loadPalettes(paletteLocationDir);
        } else {
            log.info("Directory of palette files does not exist or is not a directory");
        }
    }

    /**
     * <p>Entry point for all requests to the WMS.  This method first 
     * creates a <tt>RequestParams</tt> object from the URL query string.  This
     * object provides methods for retrieving parameter values, based on the fact that
     * WMS parameter <i>names</i> are case-insensitive.</p>
     * 
     * <p>Based on the value of the
     * REQUEST parameter this method then delegates to
     * {@link #getCapabilities getCapabilities()}, {@link #getMap getMap()}
     * or {@link #getFeatureInfo getFeatureInfo()}.
     * If the information returned from
     * this method is to be presented as an XML/JSON/HTML document, the method returns
     * a ModelAndView object containing the name of a JSP page and the data that the JSP
     * needs to render.  If the information is to be presented as an image, the method
     * writes the image to the servlet's output stream, then returns null.</p>
     *
     * <p>Any Exceptions that are thrown by this method or its delegates are 
     * automatically handled by Spring and converted to XML to be presented to the
     * user.  See the <a href="../exceptions/package-summary.html">Exceptions package</a>
     * for more details.</p>
     */
    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws Exception {
        UsageLogEntry usageLogEntry = new UsageLogEntry(httpServletRequest);
        boolean logUsage = true;

        // Create an object that allows request parameters to be retrieved in
        // a way that is not sensitive to the case of the parameter NAMES
        // (but is sensitive to the case of the parameter VALUES).
        RequestParams params = new RequestParams(httpServletRequest.getParameterMap());

        try {
            // Check the REQUEST parameter to see if we're producing a capabilities
            // document, a map or a FeatureInfo
            String request = params.getMandatoryString("request");
            usageLogEntry.setWmsOperation(request);
            if (request.equals("GetCapabilities")) {
                return getCapabilities(params, httpServletRequest, usageLogEntry);
            } else if (request.equals("GetMap")) {
                return getMap(params, httpServletResponse, usageLogEntry);
            } else if (request.equals("GetFeatureInfo")) {
                return getFeatureInfo(params, httpServletRequest, httpServletResponse,
                        usageLogEntry);
            }
            // The REQUESTs below are non-standard and could be refactored into
            // a different servlet endpoint
            else if (request.equals("GetMetadata")) {
                // This is a request for non-standard metadata.  (This will one
                // day be replaced by queries to Capabilities fragments, if possible.)
                // Delegate to the MetadataController
                return this.metadataController.handleRequest(httpServletRequest,
                        httpServletResponse, usageLogEntry);
            } else if (request.equals("GetLegendGraphic")) {
                // This is a request for an image that contains the colour scale
                // and range for a given layer
                return getLegendGraphic(params, httpServletResponse);
            /*} else if (request.equals("GetKML")) {
                // This is a request for a KML document that allows the selected
                // layer(s) to be displayed in Google Earth in a manner that 
                // supports region-based overlays.  Note that this is distinct
                // from simply setting "KMZ" as the output format of a GetMap
                // request: GetKML will give generally better results, but relies
                // on callbacks to this server.  Requesting KMZ files from GetMap
                // returns a standalone KMZ file.
                return getKML(params, httpServletRequest);
            } else if (request.equals("GetKMLRegion")) {
                // This is a request for a particular sub-region from Google Earth.
                logUsage = false; // We don't log usage for this operation
                return getKMLRegion(params, httpServletRequest); */
            } else if (request.equals("GetTransect")) {
                return getTransect(params, httpServletResponse, usageLogEntry);
            } else {
                throw new OperationNotSupportedException(request);
            }
        } catch (WmsException wmse) {
            // We don't log these errors
            usageLogEntry.setException(wmse);
            String wmsVersion = params.getWmsVersion();
            if (wmsVersion != null && wmsVersion.equals("1.1.1")) {
                // We create a new exception type to ensure that the correct
                // JSP is used to render it.  This class also translates any
                // exception codes that are different in 1.1.1 (i.e. InvalidCRS/SRS)
                throw new Wms1_1_1Exception(wmse);
            }
            throw wmse;
        } catch (SocketException se) {
            // SocketExceptions usually happen when the client has aborted the
            // connection, so there's nothing we can do here
            return null;
        } catch (IOException ioe) {
            // Filter out Tomcat ClientAbortExceptions, which for some reason
            // don't inherit from SocketException.
            // We check the class name to avoid a compile-time dependency on the
            // Tomcat libraries
            if (ioe.getClass().getName().equals("org.apache.catalina.connector.ClientAbortException")) {
                return null;
            }
            // Other types of IOException are potentially interesting and
            // must be rethrown to avoid hiding errors (maybe they
            // represent internal errors when reading data for instance).
            throw ioe;
        } catch (Exception e) {
            // An unexpected (internal) error has occurred
            usageLogEntry.setException(e);
            throw e;
        } finally {
            if (logUsage && this.usageLogger != null) {
                // Log this request to the usage log
                this.usageLogger.logUsage(usageLogEntry);
            }
        }
    }

    /**
     * Executes the GetCapabilities operation, returning a ModelAndView for
     * display of the information as an XML document.  If the user has
     * requested VERSION=1.1.1 the information will be rendered using
     * <tt>web/WEB-INF/jsp/capabilities_xml_1_1_1.jsp</tt>.  If the user
     * specifies VERSION=1.3.0 (or does not specify a version) the information
     * will be rendered using <tt>web/WEB-INF/jsp/capabilities_xml.jsp</tt>.
     * @throws IOException if there was an i/o error getting the dataset(s) from
     * the underlying data store
     */
    protected ModelAndView getCapabilities(RequestParams params,
            HttpServletRequest httpServletRequest, UsageLogEntry usageLogEntry)
            throws WmsException, IOException {
        // Check the SERVICE parameter
        String service = params.getMandatoryString("service");
        if (!service.equals("WMS")) {
            throw new WmsException("The value of the SERVICE parameter must be \"WMS\"");
        }

        // Check the VERSION parameter (not compulsory for GetCapabilities)
        String versionStr = params.getWmsVersion();
        usageLogEntry.setWmsVersion(versionStr);

        // Check the FORMAT parameter
        String format = params.getString("format");
        usageLogEntry.setOutputFormat(format);
        // The WMS 1.3.0 spec says that we can respond with the default text/xml
        // format if the client has requested an unknown format.  Hence we do
        // nothing here.

        // The DATASET parameter is an optional parameter that allows a 
        // Capabilities document to be generated for a single dataset only
        String datasetId = params.getString("dataset");
        Collection<? extends Dataset> datasets;
        DateTime lastUpdate;
        if (datasetId == null || datasetId.trim().equals("")) {
            // No specific dataset has been chosen so we create a Capabilities
            // document including every dataset.
            // First we check to see that the system admin has allowed us to
            // create a global Capabilities doc (this can be VERY large)
            Map<String, ? extends Dataset> allDatasets = this.serverConfig.getAllDatasets();
            if (this.serverConfig.getAllowsGlobalCapabilities() && allDatasets != null) {
                datasets = allDatasets.values();
            } else {
                throw new WmsException("Cannot create a Capabilities document "
                    + "that includes all datasets on this server. "
                    + "You must specify a dataset identifier with &amp;DATASET=");
            }
            // The last update time for the Capabilities doc is the last time
            // any of the datasets were updated
            lastUpdate = this.serverConfig.getLastUpdateTime();
        } else {
            // Look for this dataset
            Dataset ds = this.serverConfig.getDatasetById(datasetId);
            if (ds == null) {
                throw new WmsException("There is no dataset with ID " + datasetId);
            } else if (!ds.isReady()) {
                throw new WmsException("The dataset with ID " + datasetId +
                    " is not ready for use");
            }
            datasets = Arrays.asList(ds);
            // The last update time for the Capabilities doc is the last time
            // this particular dataset was updated
            lastUpdate = ds.getLastUpdateTime();
        }

        // Do UPDATESEQUENCE negotiation according to WMS 1.3.0 spec (sec 7.2.3.5)
        String updateSeqStr = params.getString("updatesequence");
        if (updateSeqStr != null) {
            DateTime updateSequence;
            try {
                updateSequence = WmsUtils.iso8601ToDateTime(updateSeqStr, ISOChronology.getInstanceUTC());
            } catch (IllegalArgumentException iae) {
                throw new InvalidUpdateSequence(updateSeqStr +
                        " is not a valid ISO date-time");
            }
            // We use isEqual(), which compares dates based on millisecond values
            // only, because we know that the calendar system will be
            // the same in each case (ISO).  Comparisons using equals() may return false
            // because updateSequence is read using UTC, whereas lastUpdate is
            // created in the server's time zone, meaning that the Chronologies
            // are different.
            if (updateSequence.isEqual(lastUpdate)) {
                throw new CurrentUpdateSequence(updateSeqStr);
            } else if (updateSequence.isAfter(lastUpdate)) {
                throw new InvalidUpdateSequence(updateSeqStr +
                        " is later than the current server updatesequence value");
            }
        }

        Map<String, Object> models = new HashMap<String, Object>();
        models.put("config", this.serverConfig);
        models.put("datasets", datasets);
        models.put("lastUpdate", lastUpdate);
        models.put("wmsBaseUrl", httpServletRequest.getRequestURL().toString());
        // Show only a subset of the CRS codes that we are likely to use.
        // Otherwise Capabilities doc gets very large indeed.
        // TODO: make configurable in admin app
        String[] supportedCrsCodes = new String[]{
            "EPSG:4326", "CRS:84", // Plate Carree
            "EPSG:41001", // Mercator (~ Google Maps)  TODO replace with real Google Maps code
            "EPSG:27700", // British National Grid
            // See http://nsidc.org/data/atlas/ogc_services.html for useful
            // stuff about polar stereographic projections
            "EPSG:3408", // NSIDC EASE-Grid North
            "EPSG:3409", // NSIDC EASE-Grid South
            "EPSG:32661", // North Polar stereographic
            "EPSG:32761" // South Polar stereographic
        };
        models.put("supportedCrsCodes", supportedCrsCodes); //*/HorizontalGrid.SUPPORTED_CRS_CODES);
        models.put("supportedImageFormats", ImageFormat.getSupportedMimeTypes());
        models.put("layerLimit", LAYER_LIMIT);
        models.put("featureInfoFormats", new String[]{FEATURE_INFO_PNG_FORMAT,
                    FEATURE_INFO_XML_FORMAT});
        models.put("legendWidth", ColorPalette.LEGEND_WIDTH);
        models.put("legendHeight", ColorPalette.LEGEND_HEIGHT);
        models.put("paletteNames", ColorPalette.getAvailablePaletteNames());

        // Do WMS version negotiation.  From the WMS 1.3.0 spec:
        // * If a version unknown to the server and higher than the lowest
        //   supported version is requested, the server shall send the highest
        //   version it supports that is less than the requested version.
        // * If a version lower than any of those known to the server is requested,
        //   then the server shall send the lowest version it supports.
        // We take the version to be 1.3.0 if not specified
        WmsVersion wmsVersion = versionStr == null
                ? WmsVersion.VERSION_1_3_0
                : new WmsVersion(versionStr);
        if (wmsVersion.compareTo(WmsVersion.VERSION_1_3_0) >= 0) {
            // version is >= 1.3.0. Send 1.3.0 Capabilities
            return new ModelAndView("capabilities_xml", models);
        } else {
            // version is < 1.3.0. Send 1.1.1 Capabilities
            return new ModelAndView("capabilities_xml_1_1_1", models);
        }
    }

    /**
     * Executes the GetMap operation.  This methods performs the following steps:
     * <ol>
     * <li>Creates a {@link GetMapRequest} object from the given {@link RequestParams}.
     * This parses the parameters and checks their validity.</li>
     * <li>Finds the relevant {@link Layer} object from the config system.</li>
     * <li>Creates a {@link HorizontalGrid} object that represents the grid on
     * which the final image will sit (based on the requested CRS and image
     * width/height).</li>
     * <li>Looks for TIME and ELEVATION parameters (TIME may be expressed as a
     * start/end range, in which case we will produce an animation).</li>
     * <li>Extracts the data using
     * {@link uk.ac.rdg.resc.ncwms.datareader.DataReader#read DataReader.read()}.
     * This returns an array of floats, representing
     * the data values at each pixel in the final image.</li>
     * <li>Uses an {@link ImageProducer} object to turn the array of data into
     * a {@link java.awt.image.BufferedImage} (or, in the case of an animation, several
     * {@link java.awt.image.BufferedImage}s).</li>
     * <li>Uses a {@link ImageFormat} object to write the image to the servlet's
     * output stream in the requested format.</li>
     * </ol>
     * @throws WmsException if the user has provided invalid parameters
     * @throws Exception if an internal error occurs
     * @see uk.ac.rdg.resc.ncwms.datareader.DataReader#read DataReader.read()
     * @see uk.ac.rdg.resc.ncwms.datareader.DefaultDataReader#read DefaultDataReader.read()
     * @todo Separate Model and View code more cleanly
     */
    protected ModelAndView getMap(RequestParams params,
            HttpServletResponse httpServletResponse, UsageLogEntry usageLogEntry)
            throws WmsException, Exception {
        // Parse the URL parameters
        GetMapRequest getMapRequest = new GetMapRequest(params);
        GetMapStyleRequest styleRequest = getMapRequest.getStyleRequest();
        usageLogEntry.setGetMapRequest(getMapRequest);

        // Get the ImageFormat object corresponding with the requested MIME type
        String mimeType = getMapRequest.getStyleRequest().getImageFormat();
        // This throws an InvalidFormatException if the MIME type is not supported
        ImageFormat imageFormat = ImageFormat.get(mimeType);

        GetMapDataRequest dr = getMapRequest.getDataRequest();
        String[] layers = dr.getLayers();
        if (layers.length > LAYER_LIMIT) {
            throw new WmsException("You may only request a maximum of " +
                WmsController.LAYER_LIMIT + " layer(s) simultaneously from this server");
        }
        // TODO: support more than one layer (superimposition, difference, mask)
        Layer layer = this.serverConfig.getLayerByUniqueName(layers[0]);
        usageLogEntry.setLayer(layer);

        // Check the dimensions of the image
        if (dr.getHeight() > this.serverConfig.getMaxImageHeight() ||
            dr.getWidth()  > this.serverConfig.getMaxImageWidth()) {
            throw new WmsException("Requested image size exceeds the maximum of "
                + this.serverConfig.getMaxImageWidth() + "x"
                + this.serverConfig.getMaxImageHeight());
        }

        // Get the grid onto which the data will be projected
        HorizontalGrid grid = new HorizontalGrid(dr.getCrsCode(), dr.getWidth(),
                dr.getHeight(), dr.getBbox());

        // Create an object that will turn data into BufferedImages
        String[] styles = styleRequest.getStyles();
        ImageProducer imageProducer = new ImageProducer.Builder()
            .layer(layer)
            .width(dr.getWidth())
            .height(dr.getHeight())
            .style(styles.length == 0 ? null : styles[0]) // Use null to trigger default style
            .colourScaleRange(styleRequest.getColorScaleRange())
            .backgroundColour(styleRequest.getBackgroundColour())
            .transparent(styleRequest.isTransparent())
            .logarithmic(styleRequest.isScaleLogarithmic())
            .opacity(styleRequest.getOpacity())
            .numColourBands(styleRequest.getNumColourBands())
            .build();
        // Need to make sure that the images will be compatible with the
        // requested image format
        if (imageProducer.isTransparent() && !imageFormat.supportsFullyTransparentPixels()) {
            throw new WmsException("The image format " + mimeType +
                    " does not support fully-transparent pixels");
        }
        if (imageProducer.getOpacity() < 100 && !imageFormat.supportsPartiallyTransparentPixels()) {
            throw new WmsException("The image format " + mimeType +
                    " does not support partially-transparent pixels");
        }

        double zValue = getElevationValue(dr.getElevationString(), layer);

        // Cycle through all the provided timesteps, extracting data for each step
        List<String> tValueStrings = new ArrayList<String>();
        List<DateTime> timeValues = getTimeValues(dr.getTimeString(), layer);
        if (timeValues.size() > 1 && !imageFormat.supportsMultipleFrames()) {
            throw new WmsException("The image format " + mimeType +
                    " does not support multiple frames");
        }
        usageLogEntry.setNumTimeSteps(timeValues.size());
        long beforeExtractData = System.currentTimeMillis();
        for (DateTime timeValue : timeValues) {
            // A List<Float> for each component of a vector quantity, although
            // we only use the first component for scalars.
            List<List<Float>> picData = new ArrayList<List<Float>>(2);
            if (layer instanceof ScalarLayer) {
                // Note that if the layer doesn't have a time axis, timeValue==null but this
                // will be ignored by readPointList()
                picData.add(this.serverConfig.readDataGrid((ScalarLayer)layer, timeValue, zValue, grid, usageLogEntry));
            } else if (layer instanceof VectorLayer) {
                VectorLayer vecLayer = (VectorLayer)layer;
                picData.add(this.serverConfig.readDataGrid(vecLayer.getEastwardComponent(),  timeValue, zValue, grid, usageLogEntry));
                picData.add(this.serverConfig.readDataGrid(vecLayer.getNorthwardComponent(), timeValue, zValue, grid, usageLogEntry));
            } else {
                throw new IllegalStateException("Unrecognized layer type");
            }

            // Only add a label if this is part of an animation
            String tValueStr = "";
            if (timeValues.size() > 1 && timeValue != null) {
                tValueStr = WmsUtils.dateTimeToISO8601(timeValue);
            }
            tValueStrings.add(tValueStr);
            imageProducer.addFrame(picData, tValueStr); // the tValue is the label for the image
        }
        long timeToExtractData = System.currentTimeMillis() - beforeExtractData;
        usageLogEntry.setTimeToExtractDataMs(timeToExtractData);

        // We only create a legend object if the image format requires it
        BufferedImage legend = imageFormat.requiresLegend() ? imageProducer.getLegend() : null;

        // Write the image to the client.
        // First we set the HTTP headers
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        httpServletResponse.setContentType(mimeType);
        // If this is a KMZ file give it a sensible filename
        if (imageFormat instanceof KmzFormat) {
            httpServletResponse.setHeader("Content-Disposition", "inline; filename=" +
                    layer.getDataset().getId() + "_" + layer.getId() + ".kmz");
        }
        // Render the images and write to the output stream
        imageFormat.writeImage(imageProducer.getRenderedFrames(),
                httpServletResponse.getOutputStream(), layer, tValueStrings,
                dr.getElevationString(), grid.getBbox(), legend);

        return null;
    }

    /**
     * Executes the GetFeatureInfo operation
     * @throws WmsException if the user has provided invalid parameters
     * @throws Exception if an internal error occurs
     * @todo Separate Model and View code more cleanly
     */
    protected ModelAndView getFeatureInfo(RequestParams params,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse,
            UsageLogEntry usageLogEntry)
            throws WmsException, Exception {
        // Look to see if we're requesting data from a remote server
        String url = params.getString("url");
        if (url != null && !url.trim().equals("")) {
            usageLogEntry.setRemoteServerUrl(url);
            MetadataController.proxyRequest(url, httpServletRequest, httpServletResponse);
            return null;
        }

        GetFeatureInfoRequest request = new GetFeatureInfoRequest(params);
        usageLogEntry.setGetFeatureInfoRequest(request);
        GetFeatureInfoDataRequest dr = request.getDataRequest();

        // Check the feature count
        if (dr.getFeatureCount() != 1) {
            throw new WmsException("Can only provide feature info for one layer at a time");
        }

        // Check the output format
        if (!request.getOutputFormat().equals(FEATURE_INFO_XML_FORMAT) &&
                !request.getOutputFormat().equals(FEATURE_INFO_PNG_FORMAT)) {
            throw new InvalidFormatException("The output format " +
                    request.getOutputFormat() + " is not valid for GetFeatureInfo");
        }

        // Get the layer we're interested in
        String layerName = dr.getLayers()[0];
        Layer layer = this.serverConfig.getLayerByUniqueName(layerName);
        usageLogEntry.setLayer(layer);
        if (!layer.isQueryable()) {
            throw new LayerNotQueryableException(layerName);
        }

        // Get the grid onto which the data is being projected
        HorizontalGrid grid = new HorizontalGrid(dr.getCrsCode(), dr.getWidth(),
                dr.getHeight(), dr.getBbox());
        // Get the x and y values of the point of interest
        double x = grid.getXAxisValues()[dr.getPixelColumn()];
        double y = grid.getYAxisValues()[dr.getPixelRow()];
        LonLatPosition lonLat = grid.getCrsHelper().crsToLonLat(x, y);
        usageLogEntry.setFeatureInfoLocation(lonLat.getLongitude(), lonLat.getLatitude());

        // Find out the i,j coordinates of this point in the source grid (could be null)
        int[] gridCoords = layer.getHorizontalCoordSys().lonLatToGrid(lonLat);
        // *** TODO: what if gridCoords is null? ***
        // Get the location of the centre of the grid cell
        LonLatPosition gridCellCentre = layer.getHorizontalCoordSys().gridToLonLat(gridCoords);

        // Get the elevation value requested
        double zValue = getElevationValue(dr.getElevationString(), layer);

        // Get the requested timesteps.  If the layer doesn't have
        // a time axis then this will return a single-element List with value null.
        List<DateTime> tValues = getTimeValues(dr.getTimeString(), layer);
        usageLogEntry.setNumTimeSteps(tValues.size());

        // First we read the timeseries data.  If the layer doesn't have a time
        // axis this will return a List of one-element arrays.
        List<Float> tsData;
        if (layer instanceof ScalarLayer) {
            tsData = ((ScalarLayer)layer).readTimeseries(tValues, zValue, lonLat);
        } else if (layer instanceof VectorLayer) {
            VectorLayer vecLayer = (VectorLayer)layer;
            List<Float> tsDataEast  = vecLayer.getEastwardComponent() .readTimeseries(tValues, zValue, lonLat);
            List<Float> tsDataNorth = vecLayer.getNorthwardComponent().readTimeseries(tValues, zValue, lonLat);
            tsData = WmsUtils.getMagnitudes(tsDataEast, tsDataNorth);
        } else {
            throw new IllegalStateException("Unrecognized layer type");
        }

        // Internal consistency check: arrays should be the same length
        if (tValues.size() != tsData.size()) {
            throw new IllegalStateException("Internal error: timeseries length inconsistency");
        }

        // Now we map date-times to data values
        // The map is sorted in order of ascending time
        SortedMap<DateTime, Float> featureData = new TreeMap<DateTime, Float>();
        for (int i = 0; i < tValues.size(); i++) {
            featureData.put(tValues.get(i), tsData.get(i));
        }

        if (request.getOutputFormat().equals(FEATURE_INFO_XML_FORMAT)) {
            Map<String, Object> models = new HashMap<String, Object>();
            models.put("longitude", lonLat.getLongitude());
            models.put("latitude", lonLat.getLatitude());
            models.put("gridCoords", gridCoords);
            models.put("gridCentre", gridCellCentre);
            models.put("data", featureData);
            return new ModelAndView("showFeatureInfo_xml", models);
        } else {
            // Must be PNG format: prepare and output the JFreeChart
            // TODO: this is nasty: we're mixing presentation code in the controller
            TimeSeries ts = new TimeSeries("Data", Millisecond.class);
            for (DateTime dateTime : featureData.keySet()) {
                ts.add(new Millisecond(dateTime.toDate()), featureData.get(dateTime));
            }
            TimeSeriesCollection xydataset = new TimeSeriesCollection();
            xydataset.addSeries(ts);

            // Create a chart with no legend, tooltips or URLs
            String title = "Lon: " + lonLat.getLongitude() + ", Lat: " +
                    lonLat.getLatitude();
            String yLabel = layer.getTitle() + " (" + layer.getUnits() + ")";
            JFreeChart chart = ChartFactory.createTimeSeriesChart(title,
                    "Date / time", yLabel, xydataset, false, false, false);
            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
            renderer.setSeriesShape(0, new Ellipse2D.Double(-1.0, -1.0, 2.0, 2.0));
            renderer.setSeriesShapesVisible(0, true);
            chart.getXYPlot().setRenderer(renderer);
            chart.getXYPlot().setNoDataMessage("There is no data for your choice");
            chart.getXYPlot().setNoDataMessageFont(new Font("sansserif", Font.BOLD, 32));
            httpServletResponse.setContentType("image/png");

            ChartUtilities.writeChartAsPNG(httpServletResponse.getOutputStream(),
                    chart, 400, 300);
            return null;
        }
    }

    /**
     * Reads timeseries data from the given variable from a single point,
     * returning a List of data arrays.
     * This List will have a single element if the variable is scalar, or two
     * elements if the variable is a vector.  Each element in the list is
     * an array of floats, representing the timeseries data.  The length of each
     * array will equal tIndices.size().
     */
    /*private static List<float[]> readTimeseriesData(Layer layer,
            LonLatPosition lonLat, List<Integer> tIndices, int zIndex)
            throws Exception {
        List<float[]> tsData = new ArrayList<float[]>();
        if (layer instanceof VectorLayer) {
            VectorLayer vecLayer = (VectorLayer) layer;
            tsData.add(readTimeseriesDataArray(vecLayer.getEastwardComponent(),
                    lonLat, tIndices, zIndex));
            tsData.add(readTimeseriesDataArray(vecLayer.getNorthwardComponent(),
                    lonLat, tIndices, zIndex));
        } else {
            tsData.add(readTimeseriesDataArray(layer, lonLat, tIndices, zIndex));
        }
        return tsData;
    }*/

    /**
     * Reads a timeseries of data from a Layer that is <b>not</b> a VectorLayer.
     */
    /*private static float[] readTimeseriesDataArray(Layer layer,
            LonLatPosition lonLat, List<Integer> tIndices, int zIndex)
            throws Exception {

        if (tIndices == null) throw new NullPointerException("tIndices");

        // We need to group the tIndices by their containing file.  That way, we
        // can read all the time data from the same file in the same operation.
        // This maps filenames to lists of t indices within the file.  We must
        // preserve the insertion order so we use a LinkedHashMap.
        Map<String, List<Integer>> files = new LinkedHashMap<String, List<Integer>>();
        for (int tIndexInLayer : tIndices) {
            FilenameAndTindex ft = getFilenameAndTindex(layer, tIndexInLayer);
            List<Integer> tIndicesInFile = files.get(ft.filename);
            if (tIndicesInFile == null) {
                tIndicesInFile = new ArrayList<Integer>();
                files.put(ft.filename, tIndicesInFile);
            }
            tIndicesInFile.add(ft.tIndexInFile);
        }

        // Get a DataReader object for reading the data
        Dataset ds = layer.getDataset();
        DataReader dr = DataReader.forDataset(ds);
        log.debug("Got data reader of type {}", dr.getClass().getName());

        // Now we read the data from each file and add it to the timeseries
        List<Float> data = new ArrayList<Float>();
        for (String filename : files.keySet()) {
            List<Integer> tIndicesInFile = files.get(filename);
            float[] arr = dr.readTimeseries(filename, layer, tIndicesInFile, zIndex, lonLat);
            for (float val : arr) {
                data.add(val);
            }
        }

        // Check that we have the right number of data points
        if (data.size() != tIndices.size()) {
            throw new AssertionError("Timeseries length inconsistency");
        }

        // Copy the data to an array of primitives and return
        float[] arr = new float[data.size()];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = data.get(i);
        }

        return arr;
    }*/

    /**
     * Creates and returns a PNG image with the colour scale and range for 
     * a given Layer
     */
    private ModelAndView getLegendGraphic(RequestParams params,
            HttpServletResponse httpServletResponse) throws Exception {
        BufferedImage legend;

        // numColourBands defaults to 254 (the maximum) if not set
        int numColourBands = GetMapStyleRequest.getNumColourBands(params);

        String paletteName = params.getString("palette");

        // Find out if we just want the colour bar with no supporting text
        String colorBarOnly = params.getString("colorbaronly", "false");
        if (colorBarOnly.equalsIgnoreCase("true")) {
            // We're only creating the colour bar so we need to know a width
            // and height
            int width = params.getPositiveInt("width", 50);
            int height = params.getPositiveInt("height", 200);
            // Find the requested colour palette, or use the default if not set
            ColorPalette palette = ColorPalette.get(paletteName);
            legend = palette.createColorBar(width, height, numColourBands);
        } else {
            // We're creating a legend with supporting text so we need to know
            // the colour scale range and the layer in question
            String layerName = params.getMandatoryString("layer");
            Layer layer = this.serverConfig.getLayerByUniqueName(layerName);

            // We default to the layer's default palette if none is specified
            ColorPalette palette = paletteName == null
                ? layer.getDefaultColorPalette()
                : ColorPalette.get(paletteName);

            // See if the client has specified a logarithmic scaling, defaulting
            // to the layer's default
            Boolean isLogScale = GetMapStyleRequest.isLogScale(params);
            boolean logarithmic = isLogScale == null ? layer.isLogScaling() : isLogScale.booleanValue();

            // Now get the colour scale range
            Range<Float> colorScaleRange = GetMapStyleRequest.getColorScaleRange(params);
            if (colorScaleRange == null) {
                // Use the layer's default range if none is specified
                colorScaleRange = layer.getApproxValueRange();
            } else if (colorScaleRange.isEmpty()) {
                throw new WmsException("Cannot automatically create a colour scale "
                    + "for a legend graphic.  Use COLORSCALERANGE=default or specify "
                    + "the scale extremes explicitly.");
            }

            // Now create the legend image
            legend = palette.createLegend(numColourBands, layer, logarithmic, colorScaleRange);
        }
        httpServletResponse.setContentType("image/png");
        ImageIO.write(legend, "png", httpServletResponse.getOutputStream());

        return null;
    }

    // This doesn't really work well so we're commenting it out for now.
    /*private ModelAndView getKML(RequestParams params,
            HttpServletRequest httpServletRequest) throws Exception {
        // Get the Layer objects that we are to include in the KML.  The layer
        // objects are bundled with information about the top-level tiles that
        // need to be created in the top-level KML
        List<TiledLayer> tiledLayers = new ArrayList<TiledLayer>();
        for (String layerName : params.getMandatoryString("layers").split(",")) {
            Layer layer = this.serverConfig.getLayerByUniqueName(layerName);
            // The data will be displayed on Google Earth using tiles.  To take
            // best advantage of the tile cache, we want to make sure that the
            // tiles match those that will be generated by the Godiva2 site.

            // The widest relevant zoom level (which we shall call level zero)
            // in Godiva2 covers the earth with two
            // tiles, one for the eastern and one for the western hemisphere, i.e.
            // a side length of 180 degrees.  Each zoom level halves the side length.
            // l = 180 / 2^z (l = side length, z = zoom level)

            // We need to calculate the widest zoom level that is relevant for this
            // layer.  This is the zoom level that will allow the layer to be
            // displayed with a maximum of four tiles.  Then the sub-regions will
            // be calculated automatically in getKMLRegion().

            // First we need to calculate the length of the longest side of the
            // bounding box in degrees.  (We know that layer.getBbox() returns
            // values in lat/lon degrees.)
            double[] bbox = layer.getBbox();
            log.debug("Layer bbox = {},{},{},{}", new Object[]{
                        bbox[0], bbox[1], bbox[2], bbox[3]});
            double longestBboxSideLength = Math.max(
                    bbox[2] - bbox[0],
                    bbox[3] - bbox[1]);
            log.debug("Longest side length = {}", longestBboxSideLength);
            // z = ln(180/l) / ln(2)
            double zoom = Math.log(180.0 / longestBboxSideLength) / Math.log(2.0);
            // Need to take the floor of this number to ensure that the tiles
            // are big enough (remember the lower the z value, the larger the tiles)
            int z = (int) Math.floor(zoom);
            if (z < 0) {
                z = 0; // Don't want to zoom out any further
            }
            log.debug("Zoom level = {}", z);
            // Calculate the side length at this zoom level
            double sideLength = 180.0 / Math.pow(2, z);
            log.debug("Side length at this zoom level = {}", sideLength);
            assert (sideLength >= longestBboxSideLength);
            // Calculate the index of the tile in the horizontal direction
            // that contains the left-hand edge of the bounding box of the layer
            // The first tile (with index 0) is the one whose LH edge is at -180 degrees
            int leftIndex = (int) ((bbox[0] + 180.0) / sideLength);
            int rightIndex = (int) ((bbox[2] + 180.0) / sideLength);
            // Similarly for the bottom and top edges of the layer's bbox
            int bottomIndex = (int) ((bbox[1] + 90.0) / sideLength);
            int topIndex = (int) ((bbox[3] + 90.0) / sideLength);
            log.debug("Indices: L={}, R={}, B={}, T={}", new Object[]{
                        leftIndex, rightIndex, bottomIndex, topIndex});
            // Create bounding boxes for the tiles that cover the layer
            List<double[]> tiles = new ArrayList<double[]>();
            for (int j = bottomIndex; j <= topIndex; j++) {
                double bottom = sideLength * j - 90.0;
                double top = bottom + sideLength;
                for (int i = leftIndex; i <= rightIndex; i++) {
                    double left = sideLength * i - 180.0;
                    double right = left + sideLength;
                    log.debug("Adding new tile({},{},{},{})", new Object[]{
                                left, bottom, right, top});
                    tiles.add(new double[]{left, bottom, right, top});
                }
            }
            assert (tiles.size() <= 4);
            log.debug("Created {} tiles for layer {}", tiles.size(), layer.getLayerName());
            tiledLayers.add(new TiledLayer(layer, tiles));
        }

        Map<String, Object> models = new HashMap<String, Object>();
        models.put("tiledLayers", tiledLayers);
        models.put("title", this.serverConfig.getTitle());
        models.put("description", this.serverConfig.getAbstract());
        models.put("wmsBaseUrl", httpServletRequest.getRequestURL().toString());
        return new ModelAndView("topLevelKML", models);
    }

    private ModelAndView getKMLRegion(RequestParams params,
            HttpServletRequest httpServletRequest) throws Exception {
        Layer layer = this.serverConfig.getLayerByUniqueName(params.getMandatoryString("layer"));
        double[] dbox = WmsUtils.parseBbox(params.getMandatoryString("dbox"));
        // Calculate the bounding boxes of all the four sub-regions
        double[][] regionDBoxes = new double[4][4];
        double halfLon = (dbox[2] - dbox[0]) / 2.0;
        double halfLat = (dbox[3] - dbox[1]) / 2.0;

        regionDBoxes[0][0] = dbox[0];
        regionDBoxes[0][1] = dbox[1];
        regionDBoxes[0][2] = dbox[0] + halfLon;
        regionDBoxes[0][3] = dbox[1] + halfLat;

        regionDBoxes[1][0] = dbox[0] + halfLon;
        regionDBoxes[1][1] = dbox[1];
        regionDBoxes[1][2] = dbox[2];
        regionDBoxes[1][3] = dbox[1] + halfLat;

        regionDBoxes[2][0] = dbox[0];
        regionDBoxes[2][1] = dbox[1] + halfLat;
        regionDBoxes[2][2] = dbox[0] + halfLon;
        regionDBoxes[2][3] = dbox[3];

        regionDBoxes[3][0] = dbox[0] + halfLon;
        regionDBoxes[3][1] = dbox[1] + halfLat;
        regionDBoxes[3][2] = dbox[2];
        regionDBoxes[3][3] = dbox[3];

        Map<String, Object> models = new HashMap<String, Object>();
        models.put("layer", layer);
        models.put("elevation", params.getString("elevation"));
        models.put("time", params.getString("time"));
        models.put("dbox", dbox);
        models.put("size", 256);
        models.put("regionDBoxes", regionDBoxes);
        models.put("wmsBaseUrl", httpServletRequest.getRequestURL().toString());
        return new ModelAndView("regionBasedOverlay", models);
    }*/

    /**
     * Outputs a transect (data value versus distance along a path) in PNG or
     * XML format.
     * @todo this method is too long, refactor!
     */
    private ModelAndView getTransect(RequestParams params, HttpServletResponse response,
            UsageLogEntry usageLogEntry) throws Exception {

        // Parse the request parameters
        String layerStr = params.getMandatoryString("layer");
        Layer layer = this.serverConfig.getLayerByUniqueName(layerStr);
        String crsCode = params.getMandatoryString("crs");
        String lineString = params.getMandatoryString("linestring");
        String outputFormat = params.getMandatoryString("format");
        DateTime tValue = getTimeValues(params.getString("time"), layer).get(0); // null if no t axis
        double zValue = getElevationValue(params.getString("elevation"), layer);

        if (!outputFormat.equals(FEATURE_INFO_PNG_FORMAT) &&
                !outputFormat.equals(FEATURE_INFO_XML_FORMAT)) {
            throw new InvalidFormatException(outputFormat);
        }

        usageLogEntry.setLayer(layer);
        usageLogEntry.setOutputFormat(outputFormat);

        final CrsHelper crsHelper = CrsHelper.fromCrsCode(crsCode);

        // Parse the line string, which is in the form "x1 y1, x2 y2, x3 y3"
        final LineString transect = new LineString(lineString, crsHelper);
        log.debug("Got {} control points", transect.getControlPoints().size());

        // Find the optimal number of points to sample the layer's source grid
        PointList pointList = getOptimalTransectPointList(layer, transect);
        log.debug("Using transect consisting of {} points", pointList.size());

        // Read the data from the data source, without using the tile cache
        List<Float> transectData;
        if (layer instanceof ScalarLayer) {
            transectData = ((ScalarLayer)layer).readPointList(tValue, zValue, pointList);
        } else if (layer instanceof VectorLayer) {
            VectorLayer vecLayer = (VectorLayer)layer;
            List<Float> tsDataEast  = vecLayer.getEastwardComponent() .readPointList(tValue, zValue, pointList);
            List<Float> tsDataNorth = vecLayer.getNorthwardComponent().readPointList(tValue, zValue, pointList);
            transectData = WmsUtils.getMagnitudes(tsDataEast, tsDataNorth);
        } else {
            throw new IllegalStateException("Unrecognized layer type");
        }
        log.debug("Transect: Got {} dataValues", transectData.size());

        // Now output the data in the selected format
        response.setContentType(outputFormat);
        if (outputFormat.equals(FEATURE_INFO_PNG_FORMAT)) {
            XYSeries series = new XYSeries("data", true); // TODO: more meaningful title
            for (int i = 0; i < transectData.size(); i++) {
                series.add(i, transectData.get(i));
            }

            XYSeriesCollection xySeriesColl = new XYSeriesCollection();
            xySeriesColl.addSeries(series);

            JFreeChart chart = ChartFactory.createXYLineChart(
                    "Transect for " + layer.getTitle(), // title
                    "distance along transect (arbitrary units)", // TODO more meaningful x axis label
                    layer.getTitle() + " (" + layer.getUnits() + ")",
                    xySeriesColl,
                    PlotOrientation.VERTICAL,
                    false, // show legend
                    false, // show tooltips (?)
                    false // urls (?)
                    );

            XYPlot plot = chart.getXYPlot();
            plot.getRenderer().setSeriesPaint(0, Color.RED);
            if (layer.getDataset().getCopyrightStatement() != null) {
                final TextTitle textTitle = new TextTitle(layer.getDataset().getCopyrightStatement());
                textTitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
                textTitle.setPosition(RectangleEdge.BOTTOM);
                textTitle.setHorizontalAlignment(HorizontalAlignment.RIGHT);
                chart.addSubtitle(textTitle);
            }
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

            rangeAxis.setAutoRangeIncludesZero(false);
            plot.setNoDataMessage("There is no data for what you have chosen.");

            //Iterate through control points to show segments of transect
            Double prevCtrlPointDistance = null;
            for (int i = 0; i < transect.getControlPoints().size(); i++) {
                double ctrlPointDistance = transect.getFractionalControlPointDistance(i);
                if (prevCtrlPointDistance != null) {

                    log.debug("ctrl point [" + i + "].");
                    log.debug("prevCtrlPointDistance " + prevCtrlPointDistance);
                    log.debug("ctrlPointDistance " + ctrlPointDistance);
                    //determine start end end value for marker based on index of ctrl point
                    IntervalMarker target = new IntervalMarker(transectData.size() * prevCtrlPointDistance, transectData.size() * ctrlPointDistance);
                    // TODO: printing to two d.p. not always appropriate
                    target.setLabel("[" + printTwoDecimals(transect.getControlPoints().get(i - 1).getY()) + "," + printTwoDecimals(transect.getControlPoints().get(i - 1).getX()) + "]");
                    target.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
                    //alter color of segment and position of label based on odd/even index
                    if (i % 2 == 0) {
                        target.setPaint(new Color(222, 222, 255, 128));
                        target.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                        target.setLabelTextAnchor(TextAnchor.TOP_LEFT);
                    } else {
                        target.setPaint(new Color(233, 225, 146, 128));
                        target.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
                        target.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
                    }
                    //add marker to plot
                    plot.addDomainMarker(target);

                }
                prevCtrlPointDistance = transect.getFractionalControlPointDistance(i);

            }           
            ChartUtilities.writeChartAsPNG(response.getOutputStream(), chart, 400, 300);
        } else if (outputFormat.equals(FEATURE_INFO_XML_FORMAT)) {
            // Output data as XML using a template
            // First create an ordered map of ProjectionPoints to data values
            Map<HorizontalPosition, Float> dataPoints = new LinkedHashMap<HorizontalPosition, Float>();
            List<HorizontalPosition> points = pointList.asList();
            for (int i = 0; i < points.size(); i++) {
                dataPoints.put(points.get(i), transectData.get(i));
            }

            Map<String, Object> models = new HashMap<String, Object>();
            models.put("crs", crsCode);
            models.put("layer", layer);
            models.put("linestring", lineString);
            models.put("data", dataPoints);
            return new ModelAndView("showTransect_xml", models);
        }
        return null;
    }

    /**
     * Prints a double-precision number to 2 decimal places
     * @param d the double
     * @return rounded value to 2 places, as a String
     */
    private static String printTwoDecimals(double d)
    {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        // We need to set the Locale properly, otherwise the DecimalFormat doesn't
        // work in locales that use commas instead of points.
        // Thanks to Justino Martinez for this fix!
        DecimalFormatSymbols decSym = DecimalFormatSymbols.getInstance(new Locale("us", "US"));
        twoDForm.setDecimalFormatSymbols(decSym);
        return twoDForm.format(d);
    }

    /**
     * Gets a PointList that contains (near) the minimum necessary number of
     * points to sample a layer's source grid of data.  That is to say,
     * creating a PointList at higher resolution would not result in sampling
     * significantly more points in the layer's source grid.
     * @param layer The layer for which the transect will be generated
     * @param transect The transect as specified in the request
     * @return a PointList that contains (near) the minimum necessary number of
     * points to sample a layer's source grid of data.
     */
    private static PointList getOptimalTransectPointList(Layer layer,
            LineString transect) throws Exception {
        // We need to work out how many points we need to include in order to
        // completely sample the data grid (i.e. we need the resolution of the
        // points to be higher than that of the data grid).  It's hard to work
        // this out neatly (data grids can be irregular) but we can estimate
        // this by creating transects at progressively higher resolution, and
        // working out how many grid points will be sampled.
        int numTransectPoints = 500; // a bit more than the final image width
        int lastNumGridPointsSampled = -1;
        PointList pointList = null;
        while (true) {
            // Create a transect with the required number of points, interpolating
            // between the control points in the line string
            List<HorizontalPosition> points = transect.getPointsOnPath(numTransectPoints);
            // Create a PointList from the interpolated points
            PointList testPointList = PointList.fromList(points, transect.getCrsHelper());
            // Work out how many grid points will be sampled by this transect
            int numGridPointsSampled =
                new PixelMap(layer.getHorizontalCoordSys(), testPointList).getNumUniqueIJPairs();
            log.debug("With {} transect points, we'll sample {} grid points",
                    numTransectPoints, numGridPointsSampled);
            // If this increase in resolution results in at least 10% more points
            // being sampled we'll go around the loop again
            if (numGridPointsSampled > lastNumGridPointsSampled * 1.1) {
                // We need to increase the transect resolution and try again
                lastNumGridPointsSampled = numGridPointsSampled;
                numTransectPoints += 500;
                pointList = testPointList;
            } else {
                // We've gained little advantage by the last resolution increase
                return pointList;
            }
        }
    }

    /**
     * Gets the elevation value requested by the client.
     * @param zValue the value of the ELEVATION string from the request
     * @return the elevation value requested by the client.  Returns
     * {@link Layer#getDefaultElevationValue() layer.getDefaultElevationValue()}
     * if zValue is null and the layer supports a default elevation value.
     * Returns {@link Double#NaN} if the layer does not have an elevation axis.
     * @throws InvalidDimensionValueException if the provided z value is not
     * a valid number, or if zValue is null and the layer does not support
     * a default elevation value
     */
    static double getElevationValue(String zValue, Layer layer) throws InvalidDimensionValueException
    {
        if (layer.getElevationValues().isEmpty()) return Double.NaN;
        if (zValue == null)
        {
            double defaultVal = layer.getDefaultElevationValue();
            if (Double.isNaN(defaultVal))
            {
                throw new InvalidDimensionValueException("elevation", "null");
            }
            return defaultVal;
        }

        // Check to see if this is a single value (the
        // user hasn't requested anything of the form z1,z2 or start/stop/step)
        if (zValue.contains(",") || zValue.contains("/")) {
            throw new InvalidDimensionValueException("elevation", zValue);
        }

        try {
            return Double.parseDouble(zValue);
        } catch (NumberFormatException nfe) {
            throw new InvalidDimensionValueException("elevation", zValue);
        }
    }

    /**
     * Gets the list of time values requested by the client.  If the layer does
     * not have a time axis the timeString will be ignored and a List containing
     * a single null value will be returned.
     * @param timeString the string provided for the TIME parameter, or null
     * if there was no TIME parameter in the client's request
     * @return the list of time values requested by the client or a List containing
     * a single null element if the layer does not have a time axis.
     * @throws InvalidDimensionValueException if the time string cannot be parsed,
     * or if any of the requested times are not valid times for the layer
     */
    static List<DateTime> getTimeValues(String timeString, Layer layer)
            throws InvalidDimensionValueException {

        // If the layer does not have a time axis return a List containing
        // a single null value
        if (layer.getTimeValues().isEmpty()) return Arrays.asList((DateTime)null);
        
        // Use the default time if none is specified
        if (timeString == null) {
            DateTime defaultDateTime = layer.getDefaultTimeValue();
            if (defaultDateTime == null) {
                // Must specify a TIME: this layer does not support a default time value
                throw new InvalidDimensionValueException("time", timeString);
            }
            return Arrays.asList(defaultDateTime);
        }

        // Interpret the time specification
        List<DateTime> tValues = new ArrayList<DateTime>();
        for (String t : timeString.split(",")) {
            String[] startStop = t.split("/");
            if (startStop.length == 1) {
                // This is a single time value
                tValues.add(findTValue(startStop[0], layer));
            } else if (startStop.length == 2) {
                // Use all time values from start to stop inclusive
                tValues.addAll(findTValues(startStop[0], startStop[1], layer));
            } else {
                throw new InvalidDimensionValueException("time", t);
            }
        }
        return tValues;
    }
    
    /**
     * Gets the index of the DateTime corresponding with the given ISO string,
     * checking that the time is valid for the given layer.
     * @throws InvalidDimensionValueException if the layer does not contain
     * the given time, or if the given ISO8601 string is not valid.
     */
    static int findTIndex(String isoDateTime, Layer layer)
        throws InvalidDimensionValueException
    {
        DateTime target = isoDateTime.equals("current")
            ? layer.getCurrentTimeValue()
            : WmsUtils.iso8601ToDateTime(isoDateTime, layer.getChronology());

        // Find the equivalent DateTime in the Layer.  Note that we can't simply
        // use the contains() method of the List, since this is based on equals().
        // We want to find the DateTime with the same millisecond instant.
        int index = WmsUtils.findTimeIndex(layer.getTimeValues(), target);
        if (index < 0)
        {
            throw new InvalidDimensionValueException("time", isoDateTime);
        }
        return index;
    }

    /**
     * Gets the DateTime corresponding with the given ISO string, checking
     * that the time is valid for the given layer.
     * @throws InvalidDimensionValueException if the layer does not contain
     * the given time, or if the given ISO8601 string is not valid.
     */
    private static DateTime findTValue(String isoDateTime, Layer layer)
        throws InvalidDimensionValueException
    {
        return layer.getTimeValues().get(findTIndex(isoDateTime, layer));
    }

    /**
     * Gets a List of integers representing indices along the time axis
     * starting from isoDateTimeStart and ending at isoDateTimeEnd, inclusive.
     * @param isoDateTimeStart ISO8601-formatted String representing the start time
     * @param isoDateTimeEnd ISO8601-formatted String representing the start time
     * @return List of Integer indices
     * @throws InvalidDimensionValueException if either of the start or end
     * values were not found in the axis, or if they are not valid ISO8601 times.
     */
    private static List<DateTime> findTValues(String isoDateTimeStart,
        String isoDateTimeEnd, Layer layer) throws InvalidDimensionValueException
    {
        int startIndex = findTIndex(isoDateTimeStart, layer);
        int endIndex = findTIndex(isoDateTimeEnd, layer);
        if (startIndex > endIndex)
        {
            throw new InvalidDimensionValueException("time",
                isoDateTimeStart + "/" + isoDateTimeEnd);
        }
        List<DateTime> layerTValues = layer.getTimeValues();
        List<DateTime> tValues = new ArrayList<DateTime>();
        for (int i = startIndex; i <= endIndex; i++)
        {
            tValues.add(layerTValues.get(i));
        }
        return tValues;
    }

    /**
     * Called by the Spring framework to inject the object that represents the
     * server's configuration.
     */
    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * Called by Spring to inject the usage logger
     */
    public void setUsageLogger(UsageLogger usageLogger) {
        this.usageLogger = usageLogger;
    }

    /**
     * Represents a WMS version number.
     */
    private static final class WmsVersion implements Comparable<WmsVersion> {

        private Integer value; // Numerical value of the version number,
        // used for comparisons
        private String str;
        private int hashCode;
        public static final WmsVersion VERSION_1_1_1 = new WmsVersion("1.1.1");
        public static final WmsVersion VERSION_1_3_0 = new WmsVersion("1.3.0");

        /**
         * Creates a new WmsVersion object based on the given String
         * (e.g. "1.3.0")
         * @throws IllegalArgumentException if the given String does not represent
         * a valid WMS version number
         */
        public WmsVersion(String versionStr) {
            String[] els = versionStr.split("\\.");  // regex: split on full stops
            if (els.length != 3) {
                throw new IllegalArgumentException(versionStr +
                        " is not a valid WMS version number");
            }
            int x, y, z;
            try {
                x = Integer.parseInt(els[0]);
                y = Integer.parseInt(els[1]);
                z = Integer.parseInt(els[2]);
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(versionStr +
                        " is not a valid WMS version number");
            }
            if (y > 99 || z > 99) {
                throw new IllegalArgumentException(versionStr +
                        " is not a valid WMS version number");
            }
            // We can calculate all these values up-front as this object is
            // immutable
            this.str = x + "." + y + "." + z;
            this.value = (100 * 100 * x) + (100 * y) + z;
            this.hashCode = 7 + 79 * this.value.hashCode();
        }

        /**
         * Compares this WmsVersion with the specified Version for order.  Returns a
         * negative integer, zero, or a positive integer as this Version is less
         * than, equal to, or greater than the specified Version.
         */
        @Override
        public int compareTo(WmsVersion otherVersion) {
            return this.value.compareTo(otherVersion.value);
        }

        /**
         * @return String representation of this version, e.g. "1.3.0"
         */
        @Override
        public String toString() {
            return this.str;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj instanceof WmsVersion) {
                final WmsVersion other = (WmsVersion) obj;
                return this.value.equals(other.value);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
