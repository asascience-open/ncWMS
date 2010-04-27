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

package uk.ac.rdg.resc.ncwms.util;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import uk.ac.rdg.resc.ncwms.coords.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.coords.chrono.ThreeSixtyDayChronology;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;
import uk.ac.rdg.resc.ncwms.wms.SimpleVectorLayer;
import uk.ac.rdg.resc.ncwms.wms.VectorLayer;

/**
 * <p>Collection of static utility methods that are useful in the WMS application.</p>
 *
 * <p>Through the taglib definition /WEB-INF/taglib/wmsUtils.tld, these functions
 * are also available as JSP2.0 functions. For example:</p>
 * <code>
 * <%@taglib uri="/WEB-INF/taglib/wmsUtils" prefix="utils"%>
 * The epoch: ${utils:secondsToISO8601(0)}
 * </code>
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class WmsUtils
{
    /**
     * The versions of the WMS standard that this server supports
     */
    public static final Set<String> SUPPORTED_VERSIONS = new HashSet<String>();

    private static final DateTimeFormatter ISO_DATE_TIME_FORMATTER =
        ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private static final DateTimeFormatter ISO_DATE_TIME_PARSER =
        ISODateTimeFormat.dateTimeParser().withZone(DateTimeZone.UTC);

    private static final DateTimeFormatter ISO_TIME_FORMATTER =
        ISODateTimeFormat.time().withZone(DateTimeZone.UTC);

    /**
     * <p>A {@link Comparator} that compares {@link DateTime} objects based only
     * on their millisecond instant values.  This can be used for
     * {@link Collections#sort(java.util.List, java.util.Comparator) sorting} or
     * {@link Collections#binarySearch(java.util.List, java.lang.Object,
     * java.util.Comparator) searching} {@link List}s of {@link DateTime} objects.</p>
     * <p>The ordering defined by this Comparator is <i>inconsistent with equals</i>
     * because it ignores the Chronology of the DateTime instants.</p>
     * <p><i>(Note: The DateTime object inherits from Comparable, not
     * Comparable&lt;DateTime&gt;, so we can't use the methods in Collections
     * directly.  However we can reuse the {@link DateTime#compareTo(java.lang.Object)}
     * method.)</i></p>
     */
    public static final Comparator<DateTime> DATE_TIME_COMPARATOR =
        new Comparator<DateTime>()
    {
        @Override
        public int compare(DateTime dt1, DateTime dt2) {
            return dt1.compareTo(dt2);
        }
    };
    
    static
    {
        SUPPORTED_VERSIONS.add("1.1.1");
        SUPPORTED_VERSIONS.add("1.3.0");
    }

    /** Private constructor to prevent direct instantiation */
    private WmsUtils() { throw new AssertionError(); }

    /**
     * Converts a {@link DateTime} object into an ISO8601-formatted String.
     */
    public static String dateTimeToISO8601(DateTime dateTime)
    {
        return ISO_DATE_TIME_FORMATTER.print(dateTime);
    }

    /**
     * Converts an ISO8601-formatted String into a {@link DateTime} object
     * @throws IllegalArgumentException if the string is not a valid ISO date-time
     */
    public static DateTime iso8601ToDateTime(String isoDateTime, Chronology chronology)
    {
        return ISO_DATE_TIME_PARSER.withChronology(chronology).parseDateTime(isoDateTime);
    }
    
    /**
     * Formats a DateTime as the time only
     * in the format "HH:mm:ss", e.g. "14:53:03".  Time zone offset is zero (UTC).
     */
    public static String formatUTCTimeOnly(DateTime dateTime)
    {
        return ISO_TIME_FORMATTER.print(dateTime);
    }

    /**
     * Searches the given list of timesteps for the specified date-time using the binary
     * search algorithm.  Matches are found based only upon the millisecond
     * instant of the target DateTime, not its Chronology.
     * @param  target The timestep to search for.
     * @return the index of the search key, if it is contained in the list;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the list: the index of the first
     *	       element greater than the key, or <tt>list.size()</tt> if all
     *	       elements in the list are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the key is found.  If this Layer does not have a time
     *         axis this method will return -1.
     */
    public static int findTimeIndex(List<DateTime> dtList, DateTime target)
    {
        return Collections.binarySearch(dtList, target, DATE_TIME_COMPARATOR);
    }
    
    /**
     * Creates a directory, throwing an Exception if it could not be created and
     * it does not already exist.
     */
    public static void createDirectory(File dir) throws Exception
    {
        if (dir.exists())
        {
            if (dir.isDirectory())
            {
                return;
            }
            else
            {
                throw new Exception(dir.getPath() + 
                    " already exists but it is a regular file");
            }
        }
        else
        {
            boolean created = dir.mkdirs();
            if (!created)
            {
                throw new Exception("Could not create directory "
                    + dir.getPath());
            }
        }
    }
    
    /**
     * Creates a unique name for a Layer (for display in the Capabilities
     * document) based on a dataset ID and a Layer ID that is unique within a
     * dataset.  Matches up with {@link #parseUniqueLayerName(java.lang.String)}.
     */
    public static String createUniqueLayerName(String datasetId, String layerId)
    {
        return datasetId + "/" + layerId;
    }
    
    /**
     * Parses a unique layer name and returns a two-element String array containing
     * the dataset id (first element) and the layer id (second element).  Matches
     * up with {@link #createUniqueLayerName(java.lang.String, java.lang.String)}.
     * This method does not check for the existence or otherwise of the dataset
     * or layer.
     * @throws ParseException if the provided layer name is not in the correct
     * format.
     */
    public static String[] parseUniqueLayerName(String uniqueLayerName)
        throws ParseException
    {
        String[] els = new String[2];
        
        int slashIndex = uniqueLayerName.lastIndexOf("/");
        if(slashIndex > 0)
        {
            els[0] = uniqueLayerName.substring(0, slashIndex);
            els[1] = uniqueLayerName.substring(slashIndex + 1);
            return els;
        }
        else
        {
            // We don't bother looking for the position in the string where the
            // parse error occurs
            throw new ParseException(uniqueLayerName + " is not in the correct format", -1);
        }
    }
    
    /**
     * Converts a string of the form "x1,y1,x2,y2" into a bounding box of four
     * doubles.
     * @throws WmsException if the format of the bounding box is invalid
     */
    public static double[] parseBbox(String bboxStr) throws WmsException
    {
        String[] bboxEls = bboxStr.split(",");
        // Check the validity of the bounding box
        if (bboxEls.length != 4)
        {
            throw new WmsException("Invalid bounding box format: need four elements");
        }
        double[] bbox = new double[4];
        try
        {
            for (int i = 0; i < bbox.length; i++)
            {
                bbox[i] = Double.parseDouble(bboxEls[i]);
            }
        }
        catch(NumberFormatException nfe)
        {
            throw new WmsException("Invalid bounding box format: all elements must be numeric");
        }
        if (bbox[0] >= bbox[2] || bbox[1] >= bbox[3])
        {
            throw new WmsException("Invalid bounding box format");
        }
        return bbox;
    }

    /**
     * Calculates the magnitude of the vector components given in the provided
     * Lists.  The two lists must be of the same length.  For any element in the
     * component lists, if either east or north is null, the magnitude will also
     * be null.
     * @return a List of the magnitudes calculated from the components.
     */
    public static List<Float> getMagnitudes(List<Float> eastData, List<Float> northData)
    {
        if (eastData == null || northData == null) throw new NullPointerException();
        if (eastData.size() != northData.size())
        {
            throw new IllegalArgumentException("east and north data components must be the same length");
        }
        List<Float> mag = new ArrayList<Float>(eastData.size());
        for (int i = 0; i < eastData.size(); i++)
        {
            Float east = eastData.get(i);
            Float north = northData.get(i);
            Float val = null;
            if (east != null && north != null)
            {
                val = (float)Math.sqrt(east * east + north * north);
            }
            mag.add(val);
        }
        if (mag.size() != eastData.size()) throw new AssertionError();
        return mag;
    }
    
    /**
     * @return true if the given location represents an OPeNDAP dataset.
     * This method simply checks to see if the location string starts with "http://",
     * "https://" or "dods://".
     */
    public static boolean isOpendapLocation(String location)
    {
        return location.startsWith("http://") || location.startsWith("dods://")
            || location.startsWith("https://");
    }
    
    /**
     * @return true if the given location represents an NcML aggregation. dataset.
     * This method simply checks to see if the location string ends with ".xml"
     * or ".ncml", following the same procedure as the Java NetCDF library.
     */
    public static boolean isNcmlAggregation(String location)
    {
        return location.endsWith(".xml") || location.endsWith(".ncml");
    }

    /**
     * Estimate the range of values in this layer by reading a sample of data
     * from the default time and elevation.  Works for both Scalar and Vector
     * layers.
     * @return
     * @throws IOException if there was an error reading from the source data
     */
    public static Range<Float> estimateValueRange(Layer layer) throws IOException
    {
        if (layer instanceof ScalarLayer)
        {
            List<Float> dataSample = readDataSample((ScalarLayer)layer);
            return Ranges.findMinMax(dataSample);
        }
        else if (layer instanceof VectorLayer)
        {
            VectorLayer vecLayer = (VectorLayer)layer;
            List<Float> eastDataSample = readDataSample(vecLayer.getEastwardComponent());
            List<Float> northDataSample = readDataSample(vecLayer.getEastwardComponent());
            List<Float> magnitudes = WmsUtils.getMagnitudes(eastDataSample, northDataSample);
            return Ranges.findMinMax(magnitudes);
        }
        else
        {
            throw new IllegalStateException("Unrecognized layer type");
        }
    }

    private static List<Float> readDataSample(ScalarLayer layer) throws IOException
    {
        try {
            // Read a low-resolution grid of data covering the entire spatial extent
            return layer.readPointList(
                layer.getDefaultTimeValue(),
                layer.getDefaultElevationValue(),
                new HorizontalGrid(100, 100, layer.getGeographicBoundingBox())
            );
        } catch (InvalidDimensionValueException idve) {
            // This would only happen due to a programming error in getDefaultXValue()
            throw new IllegalStateException(idve);
        }
    }

    /**
     * Finds the VectorLayers that can be derived from the given collection of
     * ScalarLayers, by examining the layer Titles (usually CF standard names)
     * and looking for "eastward_X"/"northward_X" pairs.
     */
    public static List<VectorLayer> findVectorLayers(Collection<? extends ScalarLayer> scalarLayers)
    {
        // This hashtable will store pairs of components in eastward-northward
        // order, keyed by the standard name for the vector quantity
        Map<String, ScalarLayer[]> components = new LinkedHashMap<String, ScalarLayer[]>();
        for (ScalarLayer layer : scalarLayers)
        {
            if (layer.getTitle().contains("eastward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("eastward_", "");
                // Look to see if we've already found the northward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the northward component yet
                    components.put(vectorKey, new ScalarLayer[2]);
                }
                components.get(vectorKey)[0] = layer;
            }
            else if (layer.getTitle().contains("northward"))
            {
                String vectorKey = layer.getTitle().replaceFirst("northward_", "");
                // Look to see if we've already found the eastward component
                if (!components.containsKey(vectorKey))
                {
                    // We haven't found the eastward component yet
                    components.put(vectorKey, new ScalarLayer[2]);
                }
                components.get(vectorKey)[1] = layer;
            }
        }

        // Now add the vector quantities to the collection of Layer objects
        List<VectorLayer> vectorLayers = new ArrayList<VectorLayer>();
        for (String key : components.keySet())
        {
            ScalarLayer[] comps = components.get(key);
            if (comps[0] != null && comps[1] != null)
            {
                // We've found both components.  Create a new Layer object
                VectorLayer vec = new SimpleVectorLayer(key, comps[0], comps[1]);
                vectorLayers.add(vec);
            }
        }

        return vectorLayers;
    }

    /**
     * Returns true if the given layer is a VectorLayer.  This is used in the
     * wmsUtils.tld taglib, since an "instanceof" function is not available in
     * JSTL.
     */
    public static boolean isVectorLayer(Layer layer)
    {
        return layer instanceof VectorLayer;
    }

    /**
     * <p>Returns the string to be used to display units for the TIME dimension
     * in Capabilities documents.  For standard (ISO) chronologies, this will
     * return "ISO8601".  For 360-day chronologies this will return "360_day".
     * For other chronologies this will return "unknown".</p>
     */
    public static String getTimeAxisUnits(Chronology chronology)
    {
        if (chronology instanceof ISOChronology) return "ISO8601";
        if (chronology instanceof ThreeSixtyDayChronology) return "360_day";
        return "unknown";
    }
    
}
