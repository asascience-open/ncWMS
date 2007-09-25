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
import java.util.SortedMap;
import java.util.TreeMap;

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
    // These define the bounding box of the data to extract from the source files
    private int minXIndex, minYIndex, maxXIndex, maxYIndex = 0;
    
    // Maps Y indices to Maps of X indices to pixel indices
    //            y            x            pixel
    private Map<Integer, SortedMap<Integer, List<Integer>>> pixelMap =
        new HashMap<Integer, SortedMap<Integer, List<Integer>>>();
    
    /**
     * Adds a new pixel index to this map.  Does nothing if either x or y is
     * negative.
     * @param x The x index of the point in the source data
     * @param y The y index of the point in the source data
     * @param pixel The index of the corresponding point in the picture
     */
    public void put(int x, int y, int pixel)
    {
        // If either of the indices are negative there is no data for this
        // pixel index
        if (x < 0 || y < 0) return;
        
        // Modify the bounding box if necessary
        if (x < getMinXIndex()) minXIndex = x;
        if (x > getMaxXIndex()) maxXIndex = x;
        if (y < getMinYIndex()) minYIndex = y;
        if (y > getMaxYIndex()) maxYIndex = y;
        
        // Get the set of x indices for this row (i.e. this y index),
        // creating a new set if necessary
        // TODO: are we taking a performance hit by using a SortedMap?
        SortedMap<Integer, List<Integer>> row = this.pixelMap.get(y);
        if (row == null)
        {
            row = new TreeMap<Integer, List<Integer>>();
            this.pixelMap.put(y, row);
        }
        
        // Get the set of pixel indices that correspond with this x index,
        // creating a new set if necessary
        List<Integer> pixelIndices = row.get(x);
        if (pixelIndices == null)
        {
            pixelIndices = new ArrayList<Integer>();
            row.put(x, pixelIndices);
        }
        
        // Add the pixel index to the set
        pixelIndices.add(pixel);
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
        return this.getRow(y).keySet();
    }
    
    /**
     * @return a List of all pixel indices that correspond with the given x and
     * y index
     * @throws IllegalArgumentException if there is no row with the given y index
     * or if the given x index is not found in the row
     */
    public List<Integer> getPixelIndices(int x, int y)
    {
        SortedMap<Integer, List<Integer>> row = this.getRow(y);
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
        return this.getRow(y).firstKey();
    }
    
    /**
     * @return the maximum x index in the row with the given y index
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    public int getMaxXIndexInRow(int y)
    {
        return this.getRow(y).lastKey();
    }
    
    /**
     * @return the row with the given y index
     * @throws IllegalArgumentException if there is no row with the given y index
     */
    private SortedMap<Integer, List<Integer>> getRow(int y)
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
    
}
