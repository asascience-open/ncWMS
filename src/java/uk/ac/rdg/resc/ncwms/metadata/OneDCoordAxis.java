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

package uk.ac.rdg.resc.ncwms.metadata;

import com.sleepycat.persist.model.Persistent;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import uk.ac.rdg.resc.ncwms.metadata.projection.HorizontalProjection;

/**
 * Enhances a {@link CoordinateAxis1D} by providing an efficient means of finding
 * the index for a given value.  The implementation of CoordinateAxis1D.findCoordElement()
 * in the current version of the Java NetCDF libraries (2.2.22) is inefficient and
 * profiling reveals that it is a major bottleneck in data extraction.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent
public abstract class OneDCoordAxis extends CoordAxis
{
    protected HorizontalProjection proj = null; // null Signifies the lat-lon projection
    protected int size; // Number of points on this axis
    
    /**
     * Static factory convenience method for creating a 1-D coordinate axis
     * from classes that are returned from the Java NetCDF libraries.
     * @param axis1D A {@link CoordinateAxis1D}
     * @param proj The projection used by this coordinate system
     * @throws IllegalArgumentException if the given axis is not of type latitude,
     * longitude, GeoX or GeoY.
     */
    public static OneDCoordAxis create(CoordinateAxis1D axis1D, ProjectionImpl proj)
    {
        AxisType axisType = axis1D.getAxisType();
        if (axisType == AxisType.Lon || axisType == AxisType.Lat ||
            axisType == AxisType.GeoX || axisType == AxisType.GeoY)
        {
            OneDCoordAxis theAxis = null;
            HorizontalProjection theProj = proj.isLatLon() ? null : HorizontalProjection.create(proj);
            if (axis1D.isRegular())
            {
                theAxis = new Regular1DCoordAxis(axis1D.getStart(),
                    axis1D.getIncrement(), (int)axis1D.getSize(), axisType, theProj);
            }
            else
            {
                theAxis = new Irregular1DCoordAxis(axis1D.getCoordValues(), axisType, theProj);
            }
            return theAxis;
        }
        else
        {
            throw new IllegalArgumentException("Illegal axis type " + axisType);
        }
    }
    
    protected OneDCoordAxis(AxisType type, int size)
    {
        super(type);
        this.size = size;
    }

    public final int getIndex(LatLonPoint point)
    {
        if (this.axisType == AxisType.Lon)
        {
            return this.getIndex(point.getLongitude());
        }
        else if (this.axisType == AxisType.Lat)
        {
            if (point.getLatitude() >= -90.0 && point.getLatitude() <= 90.0)
            {
                return this.getIndex(point.getLatitude());
            }
            else
            {
                return -1;
            }
        }
        else if (this.proj != null)
        {
            // This axis comes from a projected coordinate system
            ProjectionPoint projPoint = this.proj.latLonToProj(point);
            if (this.axisType == AxisType.GeoX)
            {
                return this.getIndex(projPoint.getX());
            }
            else if (this.axisType == AxisType.GeoY)
            {
                return this.getIndex(projPoint.getY());
            }
            else
            {
                throw new IllegalStateException("Unknown axis type " + this.axisType);
            }
        }
        else
        {
            throw new IllegalStateException("Axis is not latitude or " +
                "longitude but there is no projection");
        }
    }
    
    /**
     * Given a value along this coordinate axis, this method returns the nearest
     * index to this point, or -1 if the value is out of range for this axis.
     * This default method simply uses the CoordinateAxis1D.findCoordElement() method,
     * but this method can be overridden with more efficient algorithms if necessary.
     * @param coordValue The coordinate value (may be a latitude or longitude value,
     * or it may be a value in a projected coordinate system).
     * @return the index corresponding with this value, or -1 if the value is
     * out of range for this axis.
     */
    public abstract int getIndex(double coordValue);

    public int getSize()
    {
        return size;
    }
    
    /**
     * @return true if this is a longitude axis (but false if this is a GeoX axis)
     */
    public boolean isLongitude()
    {
        return this.axisType == AxisType.Lon;
    }
}
