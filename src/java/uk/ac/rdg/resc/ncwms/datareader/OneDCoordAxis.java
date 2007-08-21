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

package uk.ac.rdg.resc.ncwms.datareader;

import com.sleepycat.persist.model.Persistent;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.CoordinateAxis1D;

/**
 * A one-dimensional coordinate axis
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Persistent
public abstract class OneDCoordAxis extends EnhancedCoordAxis
{
    
    private int count; // The number of points along the axis
    
    protected boolean isLongitude; // True if this is a longitude axis
    
    /**
     * Creates a new instance of OneDCoordAxis
     * @param axis1D A {@link CoordinateAxis1D}
     */
    protected OneDCoordAxis(CoordinateAxis1D axis1D)
    {
        this((int)axis1D.getSize(), (axis1D.getAxisType() == AxisType.Lon));
    }
    
    /**
     * Creates a new instance of OneDCoordAxis
     * @param axis1D A {@link CoordinateAxis1D}
     */
    protected OneDCoordAxis(int count, boolean isLongitude)
    {
        this.count = count;
        this.isLongitude = isLongitude;
    }
    
    /**
     * Default constructor (used by Berkeley DB).  This can still be protected
     * and apparently the Berkeley DB will get around this (we don't need public
     * setters for the fields for the same reason).
     */
    protected OneDCoordAxis() {}

    public int getCount()
    {
        return count;
    }
    
}
