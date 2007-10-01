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

package uk.ac.rdg.resc.ncwms.datareader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import uk.ac.rdg.resc.ncwms.metadata.EnhancedCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.OneDCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;

/**
 * Class that maps x and y indices in source data arrays to pixel indices in
 * the image that is to be created.  
 * 
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class PixelMap
{
    private static final Logger logger = Logger.getLogger(PixelMap.class);
    
    // These define the bounding box of the data to extract from the source files
    private int minXIndex = Integer.MAX_VALUE;
    private int minYIndex = Integer.MAX_VALUE;
    private int maxXIndex = -1;
    private int maxYIndex = -1;
    
    // Maps Y indices to row information
    private Map<Integer, Row> pixelMap = new HashMap<Integer, Row>();
    
    // Number of unique x-y pairs
    private int numUniqueXYPairs = 0;
    
    /**
     * Generates a PixelMap for the given Layer for the given arrays of latitude
     * and longitude values in the destination picture
     */
    public PixelMap(Layer layer, float[] latValues, float[] lonValues)
    {
        long start = System.currentTimeMillis();
        
        EnhancedCoordAxis xAxis = layer.getXaxis();
        EnhancedCoordAxis yAxis = layer.getYaxis();
        
        // Cycle through each pixel in the picture and work out which
        // x and y index in the source data it corresponds to
        int pixelIndex = 0;
        
        // We can gain efficiency if both coordinate axes are 1D by minimizing
        // the number of calls to axis.getIndex().
        if (xAxis instanceof OneDCoordAxis && yAxis instanceof OneDCoordAxis)
        {
            logger.debug("Using optimized method for 1-D axes");
            // Calculate the indices along the x axis.
            int[] xIndices = new int[lonValues.length];
            for (int i = 0; i < xIndices.length; i++)
            {
                xIndices[i] = xAxis.getIndex(new LatLonPointImpl(0.0, lonValues[i]));
            }
            for (float lat : latValues)
            {
                if (lat >= -90.0f && lat <= 90.0f)
                {
                    int yIndex = yAxis.getIndex(new LatLonPointImpl(lat, 0.0));
                    for (int xIndex : xIndices)
                    {
                        this.put(xIndex, yIndex, pixelIndex);
                        pixelIndex++;
                    }
                }
            }
        }
        else
        {
            logger.debug("Using generic method for complex axes");
            // We use a generic, but slower, algorithm
            for (float lat : latValues)
            {
                if (lat >= -90.0f && lat <= 90.0f)
                {
                    for (float lon : lonValues)
                    {
                        LatLonPoint latLon = new LatLonPointImpl(lat, lon);
                        // Translate lat-lon to projection coordinates
                        int x = xAxis.getIndex(latLon);
                        int y = yAxis.getIndex(latLon);
                        //logger.debug("Lon: {}, Lat: {}, x: {}, y: {}", new Object[]{lon, lat, xCoord, yCoord});
                        this.put(x, y, pixelIndex); // Ignores negative indices
                        pixelIndex++;
                    }
                }
            }
        }
        logger.debug("Built pixel map in {} ms", System.currentTimeMillis() - start);
    }
    
    /**
     * Adds a new pixel index to this map.  Does nothing if either x or y is
     * negative.
     * @param x The x index of the point in the source data
     * @param y The y index of the point in the source data
     * @param pixel The index of the corresponding point in the picture
     */
    private void put(int x, int y, int pixel)
    {
        // If either of the indices are negative there is no data for this
        // pixel index
        if (x < 0 || y < 0) return;
        
        // Modify the bounding box if necessary
        if (x < this.minXIndex) this.minXIndex = x;
        if (x > this.maxXIndex) this.maxXIndex = x;
        if (y < this.minYIndex) this.minYIndex = y;
        if (y > this.maxYIndex) this.maxYIndex = y;
        
        // Get the information for this row (i.e. this y index),
        // creating a new row if necessary
        Row row = this.pixelMap.get(y);
        if (row == null)
        {
            row = new Row();
            this.pixelMap.put(y, row);
        }
        
        // Add the pixel to this row
        row.put(x, pixel);
    }
    
    /**
     * @return true if this PixelMap does not contain any data: this will happen
     * if there is no intersection between the requested data and the data on disk
     */
    public boolean isEmpty()
    {
        return this.pixelMap.size() == 0;
    }
    
    /**
     * @return the Set of all y indices in this pixel map
     */
    public Set<Integer> getYIndices()
    {
        return this.pixelMap.keySet();
    }
    
    /**
     * @return the Set of all x indices in the given row
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    public Set<Integer> getXIndices(int y)
    {
        return this.getRow(y).getXIndices().keySet();
    }
    
    /**
     * @return a List of all pixel indices that correspond with the given x and
     * y index
     * @throws IllegalArgumentException if there is no row with the given y index
     * or if the given x index is not found in the row
     */
    public List<Integer> getPixelIndices(int x, int y)
    {
        Map<Integer, List<Integer>> row = this.getRow(y).getXIndices();
        if (!row.containsKey(x))
        {
            throw new IllegalArgumentException("The x index " + x +
                " was not found in the row with y index " + y);
        }
        return row.get(x);
    }
    
    /**
     * @return the minimum x index in the row with the given y index
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    public int getMinXIndexInRow(int y)
    {
        return this.getRow(y).getMinXIndex();
    }
    
    /**
     * @return the maximum x index in the row with the given y index
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    public int getMaxXIndexInRow(int y)
    {
        return this.getRow(y).getMaxXIndex();
    }
    
    /**
     * @return the row with the given y index
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    private Row getRow(int y)
    {
        if (!this.pixelMap.containsKey(y))
        {
            throw new IllegalArgumentException("There is no row with y index " + y);
        }
        return this.pixelMap.get(y);
    }

    /**
     * @return the minimum x index in the whole pixel map
     */
    public int getMinXIndex()
    {
        return minXIndex;
    }

    /**
     * @return the minimum y index in the whole pixel map
     */
    public int getMinYIndex()
    {
        return minYIndex;
    }

    /**
     * @return the maximum x index in the whole pixel map
     */
    public int getMaxXIndex()
    {
        return maxXIndex;
    }

    /**
     * @return the maximum y index in the whole pixel map
     */
    public int getMaxYIndex()
    {
        return maxYIndex;
    }
    
    /**
     * Contains information about a particular row in the data
     */
    private class Row
    {
        // Maps x Indices to a list of pixel indices
        //             x        pixels
        private Map<Integer, List<Integer>> xIndices =
            new HashMap<Integer, List<Integer>>();
        // Min and max x Indices in this row
        private int minXIndex = Integer.MAX_VALUE;
        private int maxXIndex = -1;
        
        /**
         * Adds a mapping of an x index to a pixel index
         */
        public void put(int x, int pixel)
        {
            if (x < this.minXIndex) this.minXIndex = x;
            if (x > this.maxXIndex) this.maxXIndex = x;
            
            List<Integer> pixelIndices = this.xIndices.get(x);
            if (pixelIndices == null)
            {
                pixelIndices = new ArrayList<Integer>();
                this.xIndices.put(x, pixelIndices);
                // We have a new unique x-y pair
                numUniqueXYPairs++;
            }
            // Add the pixel index to the set
            pixelIndices.add(pixel);
        }

        public Map<Integer, List<Integer>> getXIndices()
        {
            return xIndices;
        }

        public int getMinXIndex()
        {
            return minXIndex;
        }

        public int getMaxXIndex()
        {
            return maxXIndex;
        }
    }

    /**
     * @return the number of unique x-y pairs in this pixel map.  When combined
     * with the size of the resulting image we can quantify the under- or
     * oversampling.  This is the number of data points that will be extracted
     * by the PixelByPixelDataReader.
     */
    public int getNumUniqueXYPairs()
    {
        return numUniqueXYPairs;
    }
    
    /**
     * @return the sum of the lengths of each row of data points,
     * i.e. sum(xmax - xmin + 1).  This is the number of data points that will
     * be extracted by the DefaultDataReader.
     */
    public int getSumRowLengths()
    {
        int sumRowLengths = 0;
        for (Row row : this.pixelMap.values())
        {
            sumRowLengths += (row.getMaxXIndex() - row.getMinXIndex() + 1);
        }
        return sumRowLengths;
    }
    
    /**
     * @return the size of the bounding box that encompasses all data.  This is
     * the number of data points that will be extracted by the OpendapDataReader.
     */
    public int getBoundingBoxSize()
    {
        return (this.maxXIndex - this.minXIndex + 1) *
               (this.maxYIndex - this.minYIndex + 1);
    }
    
}
