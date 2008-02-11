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

/**
 * Key that is used to identify a particular image tile in the cache.  Simple
 * Java bean.
 * @deprecated Has not kept up with other developments, so is not currently used.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class ImageTileKey
{
    private String datasetId;    // The identifier of the dataset
    private String variableId;   // The identifier of the variable
    private String crs;          // The CRS used for this tile
    private float[] bbox;        // Bounding box as [minX, minY, maxX, maxY]
    private int width;           // Width of tile in pixels
    private int height;          // Height of tile in pixels
    private double time;         // value of time for this tile (s since epoch),
                                 // mirroring Layer (TODO: change to long!!)
                                 // TODO: watch out - overwriting of new data for a given
                                 // time (e.g. analysis replacing a forecast).  Should
                                 // we clear the cache when reloading metadata?
    private String elevation;    // value of elevation for this tile (might not always be numeric)
    
    /**
     * Creates a new instance of ImageTileKey
     */
    public ImageTileKey()
    {
    }

    public String getCrs()
    {
        return crs;
    }

    public void setCrs(String crs)
    {
        this.crs = crs;
    }

    public float[] getBbox()
    {
        return bbox;
    }

    public void setBbox(float[] bbox)
    {
        this.bbox = bbox;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public int getHeight()
    {
        return height;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    public double getTime()
    {
        return time;
    }

    public void setTime(double time)
    {
        this.time = time;
    }

    public String getElevation()
    {
        return elevation;
    }

    public void setElevation(String elevation)
    {
        this.elevation = elevation;
    }
    
    public String toString()
    {
        StringBuffer buf = new StringBuffer(this.getDatasetId());
        buf.append("/");
        buf.append(this.getVariableId());
        buf.append(", ");
        buf.append(this.crs);
        buf.append(", [");
        for (int i = 0; i < this.bbox.length; i++)
        {
            buf.append(this.bbox[i]);
            if (i < this.bbox.length - 1) buf.append(",");
        }
        buf.append("], ");
        buf.append(this.width);
        buf.append(", ");
        buf.append(this.height);
        buf.append(", ");
        buf.append(this.time);
        buf.append(", ");
        buf.append(this.elevation);
        return buf.toString();
    }

    public String getDatasetId()
    {
        return datasetId;
    }

    public void setDatasetId(String datasetId)
    {
        this.datasetId = datasetId;
    }

    public String getVariableId()
    {
        return variableId;
    }

    public void setVariableId(String variableId)
    {
        this.variableId = variableId;
    }
    
}
