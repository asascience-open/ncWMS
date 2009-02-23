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

package uk.ac.rdg.resc.ncwms.cache;

import java.io.File;
import java.io.Serializable;
import uk.ac.rdg.resc.ncwms.datareader.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.Longitude;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Key that is used to identify a particular data array (tile) in a
 * {@link TileCache}.  TileCacheKeys are immutable.
 *
 * @see TileCache
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class TileCacheKey implements Serializable
{
    private String layerId;               // The unique identifier of this layer
    private String crsCode;               // The CRS code used for this tile
    private double[] bbox;                // Bounding box as [minX, minY, maxX, maxY]
    private int width;                    // Width of tile in pixels
    private int height;                   // Height of tile in pixels
    private String filepath;              // Full path to the file containing the data
    private long lastModified = 0;        // The time at which the file was last modified
                                          // (used to check for changes to the file).  Not
                                          // used for OPeNDAP datasets.
    private long fileSize = 0;            // The size of the file in bytes
                                          // (used to check for changes to the file)
                                          // Not used for OPeNDAP datasets.
    private int tIndex;                   // The t index of this tile in the file
    private int zIndex;                   // The z index of this tile in the file
    private long datasetLastModified = 0; // The time (in ms since the epoch) at which
                                          // the relevant Dataset was modified (not used
                                          // for local files)
    
    // TileCacheKeys are immutable so these properties can be stored to save
    // repeated recomputation:
    private String str;        // String representation of this key
    private int hashCode;      // Hash code for this key
    
    /**
     * Creates a key for the storing and locating of data arrays in a TileCache.
     * If the filepath represents a local file (including an NcML file) then we
     * store the last modified time of the file and the file size so that the
     * key won't match the cache if the contents of the file change.  If the
     * filepath represents an NcML file or OPeNDAP aggregation we store the 
     * last-modified time of the relevant {@link uk.ac.rdg.resc.ncwms.config.Dataset Dataset}
     * object, meaning that when the metadata for the Dataset is reloaded all
     * the Keys relevant to this Dataset become invalid.  See the Javadoc
     * comments for {@link TileCache}.
     * @throws IllegalArgumentException if the given filepath exists on the server
     * but does not represent a file (e.g. it is a directory)
     */
    public TileCacheKey(String filepath, Layer layer, HorizontalGrid grid,
        int tIndex, int zIndex)
    {
        this.layerId = layer.getId();
        this.setGrid(grid);
        this.filepath = filepath;
        File f = new File(filepath);
        if (f.exists())
        {
            if (f.isFile())
            {
                // This is a local data file or an NcML file
                this.lastModified = f.lastModified();
                this.fileSize = f.length();
            }
            else
            {
                throw new IllegalArgumentException(filepath +
                    " exists but is not a valid file on this server");
            }
        }
        if (WmsUtils.isOpendapLocation(filepath) || WmsUtils.isNcmlAggregation(filepath))
        {
            // This is an OPeNDAP dataset or NcML aggregation, so we need
            // to store the last-modified time of the relevant Dataset
            this.datasetLastModified = layer.getDataset().getLastUpdate().getMillis();
        }
        this.tIndex = tIndex;
        this.zIndex = zIndex;
        
        // Create a String representation of this key
        StringBuffer buf = new StringBuffer();
        buf.append(this.layerId);
        buf.append(",");
        buf.append(this.crsCode);
        buf.append(",{");
        for (double bboxVal : this.bbox)
        {
            buf.append(bboxVal);
            buf.append(",");
        }
        buf.append("},");
        buf.append(this.width);
        buf.append(",");
        buf.append(this.height);
        buf.append(",");
        buf.append(this.filepath);
        buf.append(",");
        buf.append(this.lastModified);
        buf.append(",");
        buf.append(this.fileSize);
        buf.append(",");
        buf.append(this.tIndex);
        buf.append(",");
        buf.append(this.zIndex);
        buf.append(",");
        buf.append(this.datasetLastModified);
        
        // Create and store the string representations and hash code for this
        // key.  The key is immutable so these will not change.
        this.str = buf.toString();
        this.hashCode = this.str.hashCode();
    }
    
    /**
     * Returns an integer code that is used by ehcache to test for equality
     * of TileCacheKeys.  Two different TileCacheKeys can theoretically generate
     * the same hash code, although this is unlikely.  Ehcache uses this to reduce
     * the search space before calling {@link #equals} to check for definite equality.
     * (Note that just implementing equals() will not do!)
     */
    @Override
    public int hashCode()
    {
        return this.hashCode;
    }
    
    /**
     * @return a string representation of this key
     */
    @Override
    public String toString()
    {
        return this.str;
    }
    
    /**
     * This is called by ehcache after the hashcodes of the objects have been
     * compared for equality.
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == null) return false;
        if (this == o) return true;
        if (!(o instanceof TileCacheKey)) return false;
        
        TileCacheKey other = (TileCacheKey)o;
        
        // For speed we start with the cheap comparisons (i.e. not the string
        // comparisons) and the things that are most likely to be different.
        if (this.tIndex == other.tIndex &&
            this.zIndex == other.zIndex &&
            this.fileSize == other.fileSize &&
            this.lastModified == other.lastModified &&
            this.datasetLastModified == other.datasetLastModified &&
            this.width == other.width &&
            this.height == other.height &&
            this.crsCode.equals(other.crsCode) &&
            this.filepath.equals(other.filepath) &&
            this.layerId.equals(other.layerId) &&
            this.bbox.length == other.bbox.length)
        {
            // Now we can compare the bboxes
            for (int i = 0; i < this.bbox.length; i++)
            {
                if (Double.compare(this.bbox[i], other.bbox[i]) != 0) return false;
            }
            // If we've got this far they are equal
            return true;
        }
        return false;
    }
    
    /**
     * Sets the properties of this Key that relate to the horizontal grid of the layer.
     * Some CRSs have multiple, equivalent, codes (e.g. CRS:84 and EPSG:4326).
     * Furthermore, for CRSs with longitude axes, some apparently-different
     * bounding boxes are functionally equivalent (e.g. 360 degrees = 0 degrees).
     * This method sets the CRS and bbox to standard values to ensure that
     * data are retrieved accurately and without unnecessary repetition.
     */
    private void setGrid(HorizontalGrid grid)
    {
        this.width = grid.getWidth();
        this.height = grid.getHeight();
        if (grid.isLatLon())
        {
            this.crsCode = HorizontalGrid.PLATE_CARREE_CRS_CODE;
            // Constrain longitudes to range [-180,180]
            this.bbox = new double[] {
                Longitude.constrain180(grid.getBbox()[0]),
                grid.getBbox()[1],
                Longitude.constrain180(grid.getBbox()[2]),
                grid.getBbox()[3]
            };
        }
        else
        {
            this.crsCode = grid.getCrsCode();
            // We are paranoid and create a clone so that we know for sure
            // that the bounding box will not be altered in another class:
            // guarantees immutability of the TileCacheKey object
            this.bbox = (double[])grid.getBbox().clone();
        }
    }
}
