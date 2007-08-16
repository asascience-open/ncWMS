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
 * Standard linear lat-lon projection
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class PlateCarreeGrid extends RectangularLatLonGrid
{
    public static final String[] KEYS = new String[]{"CRS:84"};

    /**
     * @returns a new array of points along the longitude axis
     */
    public float[] getLonArray()
    {
        float minLon = this.bbox[0];
        float maxLon = this.bbox[2];
        float dx = (maxLon - minLon) / this.width;
        float[] lonArray = new float[this.width];
        for (int i = 0; i < lonArray.length; i++)
        {
            lonArray[i] = minLon + (i + 0.5f) * dx;
        }
        return lonArray;
    }

    /**
     * @returns a new array of points along the latitude axis
     */
    public float[] getLatArray()
    {
        float minLat = this.bbox[1];
        float maxLat = this.bbox[3];
        float dy = (maxLat - minLat) / this.height;
        float[] latArray = new float[this.height];
        for (int i = 0; i < latArray.length; i++)
        {
            // The latitude axis is flipped
            latArray[i] = minLat + (this.height - i - 0.5f) * dy;
        }
        return latArray;
    }
    
}
