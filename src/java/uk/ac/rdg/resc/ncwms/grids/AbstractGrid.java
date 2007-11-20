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

package uk.ac.rdg.resc.ncwms.grids;

/**
 * Abstract superclass of all Grids.  All sub-classes must define a public
 * static String[] field called KEYS that lists the CRS codes that they can
 * handle.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class AbstractGrid
{
    protected int width;    // Width of the grid in pixels
    protected int height;   // Height of the grid in pixels
    protected double[] bbox; // Array of four floats representing the bounding box

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

    public double[] getBbox()
    {
        return bbox;
    }

    public void setBbox(double[] bbox)
    {
        this.bbox = bbox;
    }
    
    /**
     * @return the longitude of the given point on the grid
     */
    public abstract double getLongitude(int i, int j);
    
    /**
     * @return the latitude of the given point on the grid
     */
    public abstract double getLatitude(int i, int j);
    
}
