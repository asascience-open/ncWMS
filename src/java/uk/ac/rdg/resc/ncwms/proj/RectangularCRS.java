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
import java.util.NoSuchElementException;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

/**
 * A RectangularCRS is made up of a pair of perpendicular latitude and longitude
 * axes.  The values along the axes need not be equally spaced.  Examples
 * of RectangularCRSs are Plate Carree (CRS:84), Mercator and Gaussian (but not
 * Reduced Gaussian, in which the number of longitude points per latitude line
 * varies with latitude).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public abstract class RectangularCRS extends RequestCRS
{
    /**
     * @return array of numbers representing the values along the longitude
     * axis
     */
    public abstract double[] getLongitudeValues();
    
    /**
     * @return array of numbers representing the values along the latitude
     * axis
     */
    public abstract double[] getLatitudeValues();
    
    /**
     * @return an Iterator over all the lon-lat points in this projection in
     * the given bounding box, with longitude as the faster-varying axis.
     */
    public Iterator<LatLonPoint> getLatLonPointIterator()
    {
        return new Iterator<LatLonPoint>()
        {
            private double[] lonValues = getLongitudeValues();
            private double[] latValues = getLatitudeValues();
            private int i = 0;

            /**
             * @return the next LatLonPoint.  Longitude is the faster-varying axis.
             */
            public synchronized LatLonPoint next()
            {
                int lonIndex = this.i % this.lonValues.length;
                int latIndex = this.i / this.lonValues.length;
                this.i++;
                if (latIndex < this.latValues.length &&
                    lonIndex < this.lonValues.length)
                {
                    return new LatLonPointImpl(this.latValues[latIndex],
                        this.lonValues[lonIndex]);
                }
                else
                {
                    throw new NoSuchElementException();
                }
            }

            public boolean hasNext()
            {
                return this.i < this.latValues.length * this.lonValues.length;
            }

            /**
             * Operation not supported in this Iterator
             */
            public void remove()
            {
                throw new UnsupportedOperationException("remove()");
            }
        };
    }
}
