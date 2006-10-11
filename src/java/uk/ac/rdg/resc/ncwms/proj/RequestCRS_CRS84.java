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

package uk.ac.rdg.resc.ncwms.proj;

import java.util.Iterator;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * RequestCRS object for Plate Carree projection (lon-lat).  Longitude and
 * latitude axes are regularly-spaced.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class RequestCRS_CRS84 extends RectangularCRS
{
    private double[] lonValues = null;
    private double[] latValues = null;
    
    /**
     * @return array of numbers representing the values along the longitude
     * axis
     */
    public double[] getLongitudeValues()
    {
        if (this.lonValues == null)
        {
            double pixelWidth =
                (this.bbox.getMaxX() - this.bbox.getMinX()) / this.picWidth;
            this.lonValues = new double[this.picWidth];
            for (int i = 0; i < this.picWidth; i++)
            {
                this.lonValues[i] = this.bbox.getMinX() + (i + 0.5) * pixelWidth;
            }
        }
        return this.lonValues;
    }
    
    /**
     * @return array of numbers representing the values along the latitude
     * axis
     */
    public double[] getLatitudeValues()
    {
        if (this.latValues == null)
        {
            double pixelHeight =
                (this.bbox.getMaxY() - this.bbox.getMinY()) / this.picHeight;
            this.latValues = new double[this.picHeight];
            for (int i = 0; i < this.picHeight; i++)
            {
                this.latValues[i] = this.bbox.getMinY() + (i + 0.5) * pixelHeight;
            }
        }
        return this.latValues;
    }
    
    /**
     * Simple test harness
     */
    public static void main(String[] args) throws Exception
    {
        RequestCRS_CRS84 crs = new RequestCRS_CRS84();
        crs.setBoundingBox("0,40,360,90");
        crs.setPictureDimension(10,10);
        int i = 0;
        for (Iterator<LatLonPoint> it = crs.getLatLonPointIterator(); it.hasNext(); )
        {
            LatLonPoint point = it.next();
            System.out.println("Lon: " + point.getLongitude() + ", Lat: "
                + point.getLatitude());
            i++;
        }
        System.out.println("Got " + i + " points");
    }
}
