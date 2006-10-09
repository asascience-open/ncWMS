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
import java.util.Iterator;
import javax.servlet.http.HttpServletResponse;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridDataset;
import uk.ac.rdg.resc.ncwms.config.NcWMS;
import uk.ac.rdg.resc.ncwms.exceptions.WMSException;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidFormatException;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSInternalError;
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
     * @param config the NcWMS configuration object for this WMS
     * @param resp The HttpServletResponse object to which we will write the image
     * @throws WMSException if the client's request was invalid
     * @throws WMSInternalError if there was an internal problem (e.g.
     * could not access underlying data, could not dynamically create a RequestCRS
     * object, etc)
     */
    public static void getMap(RequestParser reqParser, NcWMS config,
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
        if (layers.length > config.getService().getLayerLimit().intValue())
        {
            throw new WMSException("You may only request a maximum of " +
                config.getService().getLayerLimit() +
                " layer from this server with a single request");
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
        String[] bboxEls = reqParser.getParameterValue("BBOX").split(",");
        if (bboxEls.length != 4)
        {
            throw new WMSException("Invalid bounding box format");
        }
        double[] bbox = new double[bboxEls.length];
        for (int i = 0; i < bboxEls.length; i++)
        {
            bbox[i] = Double.parseDouble(bboxEls[i]);
        }
        if (bbox[0] > bbox[2] || bbox[1] > bbox[3])
        {
            throw new WMSException("Invalid bounding box format");
        }
        crs.setBoundingBox(bbox);
        
        // Get the picture dimensions
        int width = parseImageDimension(reqParser, "WIDTH",
            config.getService().getMaxWidth().intValue());
        int height = parseImageDimension(reqParser, "HEIGHT",
            config.getService().getMaxHeight().intValue());
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
    private static void processRequest(HttpServletResponse resp, NcWMS config,
        String[] layers, String[] styles, RequestCRS crs, String format)
        throws WMSException, WMSInternalError
    {
        // TODO: handle this iteration properly: for now we only allow 1 layer anyway.
        for (String layer : layers)
        {
            // Get the handle to the dataset
            String[] dsAndVar = layer.split("/");
            if (dsAndVar.length != 2)
            {
                throw new LayerNotDefinedException("Invalid format for layer " +
                    "(must be <dataset>/<variable>)");
            }
            // Look for the dataset location
            String location = null;
            for (Iterator it = config.getDatasets().getDataset().iterator(); it.hasNext(); )
            {
                NcWMS.Datasets.Dataset ds = (NcWMS.Datasets.Dataset)it.next();
                if (ds.getId().equals(dsAndVar[0].trim()))
                {
                    location = ds.getLocation();
                    break;
                }
            }
            if (location == null)
            {
                throw new LayerNotDefinedException("Dataset with id " +
                    dsAndVar[0] + " not found");
            }
            
            // Now extract the data and build the picture
            GridDataset gd = null;
            try
            {
                // Open the dataset (TODO: get from cache?)
                NetcdfDataset nc = NetcdfDataset.openDataset(location);
                // Wrapping as a GridDataset allows us to get at georeferencing
                gd = new GridDataset(nc);

                // Get the variable object
                GeoGrid var = gd.findGridByName(dsAndVar[1]);
                if (var == null)
                {
                    throw new LayerNotDefinedException("Variable with name " +
                        dsAndVar[1] + " not found");
                }
                
                resp.setContentType("text/plain");
                resp.getWriter().write("Projection: " + 
                    var.getProjection().getClassName());
                resp.getWriter().close();
                
            }
            catch(IOException ioe)
            {
                throw new WMSInternalError("Error reading from dataset "
                    + location + ": " + ioe.getMessage(), ioe);
            }
            finally
            {
                if (gd != null)
                {
                    try
                    {
                        gd.close();
                    }
                    catch(IOException ioe)
                    {
                        // TODO: log the error
                    }
                }
            }
        }
    }
    
}
