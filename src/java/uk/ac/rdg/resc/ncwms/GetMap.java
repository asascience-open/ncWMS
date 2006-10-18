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

package uk.ac.rdg.resc.ncwms;

import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import javax.servlet.http.HttpServletResponse;
import ucar.unidata.geoloc.LatLonPoint;
import uk.ac.rdg.resc.ncwms.config.NcWMSConfig;
import uk.ac.rdg.resc.ncwms.dataprovider.DataLayer;
import uk.ac.rdg.resc.ncwms.dataprovider.DataProvider;
import uk.ac.rdg.resc.ncwms.dataprovider.XYPoint;
import uk.ac.rdg.resc.ncwms.exceptions.WMSException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSInternalError;
import uk.ac.rdg.resc.ncwms.graphics.PicMaker;
import uk.ac.rdg.resc.ncwms.graphics.SimplePicMaker;
import uk.ac.rdg.resc.ncwms.proj.RequestCRS;
import uk.ac.rdg.resc.ncwms.proj.RequestCRSFactory;

/**
 * Implements the GetMap operation
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GetMap
{
    
    /**
     * The GetMap operation
     * @param reqParser The RequestParser object that was created from the URL
     * arguments
     * @param config the configuration object for this WMS
     * @param resp The HttpServletResponse object to which we will write the image
     * @throws WMSException if the client's request was invalid
     * @throws WMSInternalError if there was an internal problem (e.g.
     * could not access underlying data, could not dynamically create a RequestCRS
     * object, etc)
     */
    public static void getMap(RequestParser reqParser, NcWMSConfig config,
        HttpServletResponse resp) throws WMSException, WMSInternalError
    {
        String version = reqParser.getParameterValue("VERSION");
        if (!version.equals(WMS.VERSION))
        {
            throw new WMSException("VERSION must be " + WMS.VERSION);
        }
        if (reqParser.getParameterValue("LAYERS").trim().equals(""))
        {
            throw new WMSException("Must provide a value for the LAYERS argument");
        }
        String[] layers = reqParser.getParameterValue("LAYERS").split(",");
        if (layers.length > config.getLayerLimit())
        {
            throw new WMSException("You may only request a maximum of " +
                config.getLayerLimit() +
                " layer(s) from this server with a single request");
        }
        String[] styles = reqParser.getParameterValue("STYLES").split(",");
        if (styles.length != layers.length && !styles.equals(new String[]{""}))
        {
            throw new WMSException("You must request exactly one STYLE per layer,"
                + "or use the default style for each layer with STYLES=");
        }
        
        // Get an object representing the CRS of the request
        RequestCRS crs =
            RequestCRSFactory.getRequestCRS(reqParser.getParameterValue("CRS"));
        
        // Get the bounding box
        crs.setBoundingBox(reqParser.getParameterValue("BBOX"));
        
        // Get the picture dimensions
        int width = parseImageDimension(reqParser, "WIDTH", config.getMaxImageWidth());
        int height = parseImageDimension(reqParser, "HEIGHT", config.getMaxImageHeight());
        crs.setPictureDimension(width, height);
        
        // Get the required image format
        String format = reqParser.getParameterValue("FORMAT");
        if (!format.equals("image/png")) // TODO get supported formats from somewhere
        {
            throw new InvalidFormatException(format);
        }
        
        processRequest(resp, config, layers, styles, crs, format);
    }
    
    /**
     * Parses the image dimensions
     * @param dimName The name of the dimension (WIDTH or HEIGHT)
     * @param maxValue The maximum value for the dimension
     * @throws WMSException if the image dimension could not be parsed or was
     * greater than maxValue or less than 1
     */
    private static int parseImageDimension(RequestParser reqParser,
        String dimName, int maxValue) throws WMSException
    {
        try
        {
            int dim = Integer.parseInt(reqParser.getParameterValue(dimName));
            if (dim > maxValue || dim < 1)
            {
                throw new WMSException(dimName + " must be between 1 and "
                    + maxValue + " inclusive");
            }
            return dim;
        }
        catch(NumberFormatException nfe)
        {
            throw new WMSException("Invalid integer for " + dimName + " parameter");
        }
    }
    
    /**
     * Perform the operation
     */
    private static void processRequest(HttpServletResponse resp, NcWMSConfig config,
        String[] layers, String[] styles, RequestCRS crs, String format)
        throws WMSException, WMSInternalError
    {
        // TODO: handle more layers
        if (layers.length != 1)
        {
            // Shouldn't get here: should have been caught above.  We will only
            // get here if the server config file is incorrect
            throw new WMSInternalError("Can only handle one level at a time");
        }
        // Get the handle to the dataset and variable
        String[] dsAndVar = layers[0].split("/");
        if (dsAndVar.length != 2)
        {
            throw new LayerNotDefinedException("Invalid format for layer " +
                "(must be <dataset>/<variable>)");
        }
        // Look for the DataProvider
        DataProvider dp = config.getDataProvider(dsAndVar[0].trim());
        if (dp == null)
        {
            throw new LayerNotDefinedException("Dataset with id " +
                dsAndVar[0] + " not found");
        }
        // Look for the DataLayer
        DataLayer dl = dp.getDataLayer(dsAndVar[1].trim());
        if (dl == null)
        {
            throw new LayerNotDefinedException("Variable with id " +
                dsAndVar[1] + " not found");
        }

        // Now extract the data and build the picture
        // We create a new MapBuilder with every request for thread safety.
        float[] mapData = new MapBuilder(crs, dl).buildMapData();

        // TODO cache the picture array (arr)

        // Now make the actual image
        try
        {
            resp.setContentType("image/png");
            PicMaker picMaker = new SimplePicMaker(mapData,
                crs.getPictureWidth(), crs.getPictureHeight());
            picMaker.createAndOutputPicture(resp.getOutputStream());
            resp.getOutputStream().close();
        }
        catch(IOException ioe)
        {
            throw new WMSInternalError("IOException when writing image",
                ioe);
        }
    }
    
}
