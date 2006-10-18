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

package uk.ac.rdg.resc.ncwms.dataprovider;

import java.io.IOException;
import java.util.Date;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.exceptions.WMSInternalError;

/**
 * A DataLayer is an entity that can be logically represented as a single Layer
 * in a WMS (e.g. a Variable in a NetCDF file).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public interface DataLayer
{
    /**
     * @return a unique ID for this DataLayer
     */
    public String getID();
    
    /**
     * @return a human-readable title for this DataLayer
     */
    public String getTitle();
    
    /**
     * @return values along the time axis, or null if there is no time dimension
     * in this layer
     */
    public Date[] getTValues();
    
    /**
     * @return a String describing the units of the vertical axis, or null if
     * there is no vertical axis
     */
    public String getZAxisUnits();
    
    /**
     * @return values along the vertical axis (depth, elevation, sigma etc) or null
     * if there is no vertical axis in this layer
     */
    public double[] getZValues();
    
    /**
     * @return the bounding box for this layer in lat-lon space
     */
    public LatLonRect getLatLonBoundingBox();
    
    /**
     * @return the x-y coordinates (in this DataLayer's coordinate
     * system) of the given point in latitude-longitude space.  Returns a
     * {@link XYPoint} of two integers if the point is within range or null otherwise.
     * @todo Allow different interpolation methods
     */
    public XYPoint getXYCoordElement(LatLonPoint point);
    
    /**
     * Opens the underlying dataset in preparation for reading data with
     * getScanline().  Used by {@link MapBuilder}.  Must create a new object
     * with each invocation, for thread safety reasons.
     * @return Object representing the underlying dataset
     * @throws IOException if there was an error opening the dataset
     */
    public Object open() throws IOException;
    
    /**
     * Gets an object representing the specific variable that is represented
     * by this layer.
     * @param dataSource The source dataset, as returned by this.open()
     * @return an Object representing the specific variable
     */
    public Object getVariable(Object dataSource);
    
    /**
     * Gets a line of data at a given time, elevation and latitude.  This will
     * be called after a call to open().
     * @param var An object representing the specific variable in questions, as
     * obtained from this.getVariable()
     * @param t The t index of the line of data
     * @param z The z index of the line of data
     * @param y The y index of the line of data
     * @param xFirst The first x index in the line of data
     * @param xLast The last x index in the line of data
     * @return Array of floating-point values representing data from xFirst to
     * xLast inclusive
     * @throws IOException if there was an IO error reading the data
     * @throws WMSInternalError if there was another type of internal error reading the data
     * (e.g. unsupported data type)
     */
    public float[] getScanline(Object var, int t, int z, int y,
        int xFirst, int xLast) throws IOException, WMSInternalError;
    
    /**
     * Close the underlying dataset after reading data with getScanline().
     * Used by {@link GetMap}.  Does nothing if dataSource is null.
     * @param dataSource The dataset object as obtained from this.open()
     */
    public void close(Object dataSource);
    
}
