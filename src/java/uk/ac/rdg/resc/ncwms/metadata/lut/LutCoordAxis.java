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

package uk.ac.rdg.resc.ncwms.metadata.lut;

import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis2D;
import uk.ac.rdg.resc.ncwms.metadata.TwoDCoordAxis;

/**
 * Temporary class for wrapping a {@link LookUpTable} as a TwoDCoordAxis.  In
 * future we will need to rework the concept of a TwoDCoordAxis as it is not
 * helpful to have two separate axes: really it's better to use a horizontal
 * coordinate reference system and not expose the individual axes to clients.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class LutCoordAxis extends TwoDCoordAxis
{
    private LookUpTable lut;

    public static LutCoordAxis fromCoordSys(CoordinateAxis2D lonAxis,
        CoordinateAxis2D latAxis, AxisType type) throws Exception
    {
        return new LutCoordAxis(LookUpTable.fromCoordSys(lonAxis, latAxis), type);
    }

    private LutCoordAxis(LookUpTable lut, AxisType type)
    {
        super(type);
        this.lut = lut;
    }

    @Override
    public int getIndex(double x, double y) {
        // We return one or other grid coordinate from the look-up table depending
        // on whether this is an X or Y axis, i.e. the same LUT is used for
        // both axes.
        int[] coords = this.lut.getGridCoordinates(x, y);
        if (coords == null) return -1;
        if (this.getAxisType() == AxisType.GeoX || this.getAxisType() == AxisType.Lon)
        {
            return coords[0];
        }
        return coords[1];
    }
}
