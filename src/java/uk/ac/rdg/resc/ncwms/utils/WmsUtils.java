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

package uk.ac.rdg.resc.ncwms.utils;

import java.io.File;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import uk.ac.rdg.resc.ncwms.exceptions.WmsException;

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

    private static DateTimeFormatter ISO_DATE_TIME_FORMATTER =
        ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    private static DateTimeFormatter ISO_DATE_TIME_PARSER =
        ISODateTimeFormat.dateTimeParser().withZone(DateTimeZone.UTC);

    private static DateTimeFormatter ISO_TIME_FORMATTER =
        ISODateTimeFormat.time().withZone(DateTimeZone.UTC);
    
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
    public static DateTime iso8601ToDateTime(String isoDateTime)
    {
        return ISO_DATE_TIME_PARSER.parseDateTime(isoDateTime);
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
     * dataset.  Matches up with parseUniqueLayerName.
     */
    public static String createUniqueLayerName(String datasetId, String layerId)
    {
        return datasetId + "/" + layerId;
    }
    
    /**
     * Parses a unique layer name and returns a two-element String array containing
     * the dataset id (first element) and the layer id (second element).  Matches
     * up with getUniqueLayerName().  This method does not check for the existence
     * or otherwise of the dataset or layer.
     * @throws ParseException if the provided layer name is not in the correct
     * format.
     */
    public static String[] parseUniqueLayerName(String uniqueLayerName)
        throws ParseException
    {
        String[] els = new String[2];
        
        if(uniqueLayerName.lastIndexOf("/") > 0)
        {
            els[0] = uniqueLayerName.substring(0, uniqueLayerName.lastIndexOf("/"));
            els[1] = uniqueLayerName.substring(uniqueLayerName.lastIndexOf("/") + 1, uniqueLayerName.length());
            
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
    
}
