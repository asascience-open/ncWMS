/*
 * Copyright (c) 2009 The University of Reading
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

package uk.ac.rdg.resc.ncwms.coordsys;

import ucar.unidata.geoloc.LatLonPoint;

/**
 * Special case of a {@link HorizontalCoordSys} in which the axes are both
 * one-dimensional and are latitude and longitude.  Instances of this class
 * can only be created through
 * {@link HorizontalCoordSys#fromCoordSys(ucar.nc2.dt.GridCoordSystem)}.
 */
public final class LatLonCoordSys extends HorizontalCoordSys
{
    private final OneDCoordAxis lonAxis;
    private final OneDCoordAxis latAxis;

    /** Package-private constructor to prevent direct instantiation */
    LatLonCoordSys(OneDCoordAxis lonAxis, OneDCoordAxis latAxis)
    {
        this.lonAxis = lonAxis;
        this.latAxis = latAxis;
    }

    /**
     * @return the nearest grid point to the given lat-lon point, or null if the
     * lat-lon point is not contained within this layer's domain. The grid point
     * is given as a two-dimensional integer array: [i,j].
     */
    @Override
    public int[] latLonToGrid(LatLonPoint latLonPoint) {
        return new int[]{
            this.getLonIndex(latLonPoint.getLongitude()),
            this.getLatIndex(latLonPoint.getLatitude())
        };
    }

    /**
     * @return the nearest point along the longitude axis to the given
     * longitude coordinate.
     */
    public int getLonIndex(double longitude)
    {
        return this.lonAxis.getIndex(longitude);
    }

    /**
     * @return the nearest point along the latitude axis to the given
     * latitude coordinate.
     */
    public int getLatIndex(double latitude)
    {
        return this.latAxis.getIndex(latitude);
    }
}
