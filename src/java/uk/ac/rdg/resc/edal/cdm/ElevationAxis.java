/*
 * Copyright (c) 2011 The University of Reading
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

package uk.ac.rdg.resc.edal.cdm;

import java.util.Collections;
import java.util.List;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dt.GridCoordSystem;
import uk.ac.rdg.resc.edal.util.CollectionUtils;

/**
 * Represents an elevation axis in a layer
 */
class ElevationAxis
{
    private final String units;
    private final List<Double> values;
    private final boolean isPositive;
    private final boolean isPressure;

    /** No reference to the GridCoordSystem object is kept */
    public ElevationAxis(GridCoordSystem coordSys)
    {
        CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
        this.isPositive = coordSys.isZPositive();
        
        if (zAxis == null)
        {
            this.isPressure = false;
            this.units = "";
            this.values = Collections.emptyList();
        }
        else
        {
            this.isPressure = zAxis.getAxisType() == AxisType.Pressure;
            this.units = zAxis.getUnitsString();

            List<Double> zValues = CollectionUtils.newArrayList();
            for (double zVal : zAxis.getCoordValues())
            {
                // Pressure axes have "positive = down" but we don't want to
                // reverse the sign of the values.
                if (this.isPositive || this.isPressure) zValues.add(zVal);
                else zValues.add(-zVal); // This is probably a depth axis
            }
            this.values = Collections.unmodifiableList(zValues);
        }
    }

    public boolean isPositive() {
        return this.isPositive;
    }

    public boolean isPressure() {
        return this.isPressure;
    }

    public String getUnits() {
        return this.units;
    }

    public List<Double> getValues() {
        return this.values;
    }

}
