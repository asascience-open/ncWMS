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
        // TODO: handle this iteration properly: for now we only allow 1 layer anyway.
        for (String layer : layers)
        {
            // Get the handle to the dataset and variable
            String[] dsAndVar = layer.split("/");
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
            try
            {
                //resp.setContentType("text/plain");
                long start = System.currentTimeMillis();
                
                // Maps y indices to scanlines
                Hashtable<Integer, Scanline> scanlines = new Hashtable<Integer, Scanline>();
                // Cycle through each pixel in the picture and work out which
                // x and y index in the source data it corresponds to
                int pixelIndex = 0;
                for (Iterator<LatLonPoint> it = crs.getLatLonPointIterator(); it.hasNext(); )
                {
                    LatLonPoint latLon = it.next();
                    // Translate lat-lon to projection coordinates
                    XYPoint coords = dl.getXYCoordElement(latLon);                    
                    if (coords != null)
                    {
                        // Get the scanline for this y index
                        Scanline scanline = scanlines.get(coords.getY());
                        if (scanline == null)
                        {
                            scanline = new Scanline();
                            scanlines.put(coords.getY(), scanline);
                        }
                        scanline.put(coords.getX(), pixelIndex);
                    }
                    
                    pixelIndex++;
                }
                //resp.getWriter().write("Produced scanlines: " +
                //    (System.currentTimeMillis() - start) + " ms\n");
                start = System.currentTimeMillis();
                
                // Create the picture array - initially filled with zeroes,
                // which represent missing data (transparent pixels)
                float[] picArray = new float[crs.getPictureWidth() *
                    crs.getPictureHeight()];
                
                // Open the underlying dataset of the layer.
                dl.open();
                
                // Now build the picture: iterate through the scanlines,
                // the order doesn't matter
                for (int yIndex : scanlines.keySet())
                {
                    Scanline scanline = scanlines.get(yIndex);
                    Vector<Integer> xIndices = scanline.getSortedXIndices();
                    
                    // Read the scanline from the disk, from the first to the
                    // last x index
                    // TODO: deal with t and z indices
                    // TODO: make more efficient by subsampling?
                    float[] arr = dl.getScanline(0, 0, yIndex,
                        xIndices.firstElement(), xIndices.lastElement());

                    for (int xIndex : xIndices)
                    {
                        for (int p : scanline.getPixelIndices(xIndex))
                        {
                            picArray[p] = arr[xIndex - xIndices.firstElement()];
                        }
                    }
                }
                //resp.getWriter().write("Produced picture: " +
                //    (System.currentTimeMillis() - start) + " ms\n");
                
                // TODO cache the picture array (arr)
                
                // Now make the actual image
                resp.setContentType("image/png");
                PicMaker picMaker = new SimplePicMaker(picArray,
                    crs.getPictureWidth(), crs.getPictureHeight());
                picMaker.createAndOutputPicture(resp.getOutputStream());
                resp.getOutputStream().close();
            }
            catch(IOException ioe)
            {
                throw new WMSInternalError("Error reading from data layer "
                    + dl + ": " + ioe.getMessage(), ioe);
            }
            finally
            {
                try
                {
                    // This will do nothing if the underlying dataset is not open
                    dl.close();
                }
                catch(IOException ioe)
                {
                    // Do nothing, perhaps log the error.
                }
            }
        }
    }
    
    private static class Scanline
    {
        // Maps x indices to a collection of pixel indices
        //                  x          pixels
        private Hashtable<Integer, Vector<Integer>> xIndices;
        
        public Scanline()
        {
            this.xIndices = new Hashtable<Integer, Vector<Integer>>();
        }
        
        /**
         * @param xIndex The x index of the point in the source data
         * @param pixelIndex The index of the corresponding point in the picture
         */
        public void put(int xIndex, int pixelIndex)
        {
            Vector<Integer> pixelIndices = this.xIndices.get(xIndex);
            if (pixelIndices == null)
            {
                pixelIndices = new Vector<Integer>();
                this.xIndices.put(xIndex, pixelIndices);
            }
            pixelIndices.add(pixelIndex);
        }
        
        /**
         * @return a Vector of all the x indices in this scanline, sorted in
         * ascending order
         */
        public Vector<Integer> getSortedXIndices()
        {
            Vector<Integer> v = new Vector<Integer>(this.xIndices.keySet());
            Collections.sort(v);
            return v;
        }
        
        /**
         * @return a Vector of all the pixel indices that correspond to the
         * given x index, or null if the x index does not exist in the scanline
         */
        public Vector<Integer> getPixelIndices(int xIndex)
        {
            return this.xIndices.get(xIndex);
        }
    }
    
}
