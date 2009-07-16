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

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;

/**
 * Maps latitude-longitude points to the nearest i,j indices in a Layer's data array.
 * @author Jon
 */
public abstract class HorizontalCoordSys
{
    /** Protected constructor to limit direct instantiation to subclasses */
    protected HorizontalCoordSys() {}

    /**
     * Creates and returns a HorizontalCoordSys from the given grid coordinate system.
     * If both horizontal axes of the coordinate system are 1D and they are
     * latitude and longitude then this will return an instance of {@link LatLonCoordSys}.
     * @param coordSys
     * @return
     */
    public static HorizontalCoordSys fromCoordSys(GridCoordSystem coordSys)
    {
        ProjectionImpl proj = coordSys.getProjection();
        CoordinateAxis xAxis = coordSys.getXHorizAxis();
        CoordinateAxis yAxis = coordSys.getYHorizAxis();
        boolean isLatLon = xAxis.getAxisType() == AxisType.Lon &&
                           yAxis.getAxisType() == AxisType.Lat;

        if (xAxis instanceof CoordinateAxis1D && yAxis instanceof CoordinateAxis1D)
        {
            OneDCoordAxis xAxis1D = OneDCoordAxis.create((CoordinateAxis1D)xAxis);
            OneDCoordAxis yAxis1D = OneDCoordAxis.create((CoordinateAxis1D)yAxis);
            return isLatLon
                ? new LatLonCoordSys(xAxis1D, yAxis1D) // A 1D lat-lon system
                : new Projected1DCoordSys(xAxis1D, yAxis1D, proj); // A 1D projected system
        }
        else if (xAxis instanceof CoordinateAxis2D && yAxis instanceof CoordinateAxis2D)
        {
            // The axis must be 2D so we have to create look-up tables
            if (!isLatLon)
            {
                throw new UnsupportedOperationException("Can't create a HorizontalCoordSys" +
                    " from 2D coordinate axes that are not longitude and latitude.");
            }
            // resolution multiplier of 3 seems to give reasonable results
            return LutCoordSys.generate(coordSys, 3, ToolsUiLutGenerator.INSTANCE);
        }
        else
        {
            // Shouldn't get here
            throw new IllegalStateException("Inconsistent axis types");
        }
    }

    /**
     * @return the nearest grid point to the given lat-lon point, or null if the
     * lat-lon point is not contained within this layer's domain. The grid point
     * is given as a two-dimensional integer array: [i,j].
     */
    public abstract int[] latLonToGrid(LatLonPoint latLonPoint);

    /**
     * A HorizontalCoordSys that consists of two orthogonal 1D axes whose coordinates
     * are not latitude and longitude.
     */
    private static final class Projected1DCoordSys extends HorizontalCoordSys
    {
        private final OneDCoordAxis xAxis;
        private final OneDCoordAxis yAxis;
        private final ProjectionImpl proj;

        private Projected1DCoordSys(OneDCoordAxis xAxis, OneDCoordAxis yAxis, ProjectionImpl proj)
        {
            this.xAxis = xAxis;
            this.yAxis = yAxis;
            this.proj = proj;
        }

        @Override
        public int[] latLonToGrid(LatLonPoint latLonPoint)
        {
            // ProjectionImpls are not thread-safe.  Thanks to Marcos
            // Hermida of Meteogalicia for pointing this out!
            ProjectionPoint point;
            synchronized(this.proj) {
                // This returns a new ProjectionPoint with each invocation
                point = this.proj.latLonToProj(latLonPoint);
            }
            return new int[] {
                this.xAxis.getIndex(point.getX()),
                this.yAxis.getIndex(point.getY())
            };
        }
    }

}
