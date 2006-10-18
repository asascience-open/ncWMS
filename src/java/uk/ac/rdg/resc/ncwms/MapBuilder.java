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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.geoloc.LatLonPoint;
import uk.ac.rdg.resc.ncwms.dataprovider.DataLayer;
import uk.ac.rdg.resc.ncwms.dataprovider.XYPoint;
import uk.ac.rdg.resc.ncwms.exceptions.WMSInternalError;
import uk.ac.rdg.resc.ncwms.proj.RequestCRS;

/**
 * Class that builds a map image from a {@link GetMap} request.  A new
 * MapBuilder is created with every request, for thread safety reasons.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class MapBuilder
{
    private RequestCRS crs;
    private DataLayer dl;
      
    /**
     * Creates a new instance of MapBuilder
     * @param crs The {@link RequestCRS} that describes the map area and projection
     * @param dl The {@link DataLayer} from which the data will be read
     */
    public MapBuilder(RequestCRS crs, DataLayer dl)
    {
        this.crs = crs;
        this.dl = dl;
    }
    
    /**
     * Builds the array of data that will be transformed into a map image
     * @return Array of float data of size crs.getPictureWidth() * crs.getPictureHeight()
     */
    public float[] buildMapData() throws WMSInternalError
    {
        Object dataSource = null;
        try
        {
            //resp.setContentType("text/plain");
            long start = System.currentTimeMillis();

            // Maps y indices to scanlines
            Hashtable<Integer, Scanline> scanlines = new Hashtable<Integer, Scanline>();
            // Cycle through each pixel in the picture and work out which
            // x and y index in the source data it corresponds to
            int pixelIndex = 0;
            for (Iterator<LatLonPoint> it = this.crs.getLatLonPointIterator(); it.hasNext(); )
            {
                LatLonPoint latLon = it.next();
                // Translate lat-lon to projection coordinates
                XYPoint coords = this.dl.getXYCoordElement(latLon);                    
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
            float[] mapData = new float[this.crs.getPictureWidth() *
                this.crs.getPictureHeight()];

            // Open the underlying dataset of the layer.
            dataSource = this.dl.open();
            // Get a handle to the specific variable
            Object var = this.dl.getVariable(dataSource);

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
                float[] arr = this.dl.getScanline(var, 0, 0, yIndex,
                    xIndices.firstElement(), xIndices.lastElement());

                for (int xIndex : xIndices)
                {
                    for (int p : scanline.getPixelIndices(xIndex))
                    {
                        mapData[p] = arr[xIndex - xIndices.firstElement()];
                    }
                }
            }
            //resp.getWriter().write("Produced picture: " +
            //    (System.currentTimeMillis() - start) + " ms\n");
            return mapData;
        }
        catch(IOException ioe)
        {
            throw new WMSInternalError("Error reading from data layer "
                + this.dl + ": " + ioe.getMessage(), ioe);
        }
        finally
        {
            // This will do nothing if the underlying dataset is not open
            this.dl.close(dataSource);
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
