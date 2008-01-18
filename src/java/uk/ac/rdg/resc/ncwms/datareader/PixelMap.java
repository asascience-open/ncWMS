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
import ucar.unidata.geoloc.ProjectionPoint;
import uk.ac.rdg.resc.ncwms.metadata.CoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.OneDCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.TwoDCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;

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
     * Generates a PixelMap for the given Layer.  Data read from the Layer will
     * be projected onto the given TargetGrid
     * @throws Exception if the necessary transformations could not be performed
     */
    public PixelMap(Layer layer, TargetGrid grid) throws Exception
    {
        long start = System.currentTimeMillis();
        
        HorizontalProjection dataProj = layer.getHorizontalProjection();
        CoordAxis xAxis = layer.getXaxis();
        CoordAxis yAxis = layer.getYaxis();
        
        // Cycle through each pixel in the picture and work out which
        // x and y index in the source data it corresponds to
        int pixelIndex = 0;
        
        // We can gain efficiency if the target grid is a lat-lon grid and
        // the data exist on a lat-long grid by minimizing the number of
        // calls to axis.getIndex().
        if (dataProj.isLatLon() && grid.isLatLon())
        {
            logger.debug("Using optimized method for lat-lon coordinates");
            // These class casts should always be valid
            OneDCoordAxis xAxis1D = (OneDCoordAxis)xAxis;
            OneDCoordAxis yAxis1D = (OneDCoordAxis)yAxis;
            // Calculate the indices along the x axis.
            int[] xIndices = new int[grid.getXAxisValues().length];
            for (int i = 0; i < grid.getXAxisValues().length; i++)
            {
                xIndices[i] = xAxis1D.getIndex(grid.getXAxisValues()[i]);
            }
            for (double lat : grid.getYAxisValues())
            {
                if (lat >= -90.0 && lat <= 90.0)
                {
                    int yIndex = yAxis1D.getIndex(lat);
                    for (int xIndex : xIndices)
                    {
                        this.put(xIndex, yIndex, pixelIndex);
                        pixelIndex++;
                    }
                }
                else
                {
                    // We still need to increment the pixel index array
                    pixelIndex += xIndices.length;
                }
            }
        }
        else
        {
            logger.debug("Using generic (but slower) method");
            for (double y : grid.getYAxisValues())
            {
                for (double x : grid.getXAxisValues())
                {
                    // Translate this point in the target grid to lat-lon
                    // TODO: the transformer can transform many points at once.
                    // Doing so might be more efficient than this method.
                    LatLonPoint latLon = grid.transformToLatLon(x, y);
                    // Translate this lat-lon point to a point in the data's projection coordinates
                    ProjectionPoint projPoint = dataProj.latLonToProj(latLon);
                    if (grid.isPointValidForCrs(projPoint))
                    {
                        // Translate the projection point to grid point indices i, j
                        int i, j;
                        if (xAxis instanceof OneDCoordAxis && yAxis instanceof OneDCoordAxis)
                        {
                            OneDCoordAxis xAxis1D = (OneDCoordAxis)xAxis;
                            OneDCoordAxis yAxis1D = (OneDCoordAxis)yAxis;
                            i = xAxis1D.getIndex(projPoint.getX());
                            j = yAxis1D.getIndex(projPoint.getY());
                        }
                        else if (xAxis instanceof TwoDCoordAxis && yAxis instanceof TwoDCoordAxis)
                        {
                            TwoDCoordAxis xAxis2D = (TwoDCoordAxis)xAxis;
                            TwoDCoordAxis yAxis2D = (TwoDCoordAxis)yAxis;
                            i = xAxis2D.getIndex(projPoint);
                            j = yAxis2D.getIndex(projPoint);
                        }
                        else
                        {
                            // Shouldn't happen'
                            throw new IllegalStateException("x and y axes are of different types!");
                        }
                        this.put(i, j, pixelIndex); // Ignores negative indices
                    }
                    pixelIndex++;
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
