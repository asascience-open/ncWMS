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

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * A one-dimensional coordinate axis, whose values are not equally spaced.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class Irregular1DCoordAxis extends OneDCoordAxis
{
    private double[] values;
    private CoordinateAxis1D axis;
    
    /**
     * Creates a new instance of Irregular1DCoordAxis
     */
    public Irregular1DCoordAxis(CoordinateAxis1D axis1D)
    {
        super(axis1D);
        this.axis = axis1D;
        this.values = axis1D.getCoordValues();
        // Make sure the values are sorted into *ascending* order
        // (do we need this?  will the coordinate values not always be in 
        // ascending order?)
        //Arrays.sort(this.values);
    }
    
    /**
     * Uses a binary search algorithm to find the index of the point on the axis
     * whose value is closest to the given one.
     * @param point The {@link LatLonPoint}, which will have lon in range
     * [-180,180] and lat in range [-90,90]
     * @return the index that is nearest to this point, or -1 if the point is
     * out of range for the axis
     */
    public int getIndex(LatLonPoint point)
    {
        if (this.isLongitude)
        {
            return -1; // TODO: deal with irregular longitude axes (rare)
        }
        double target = point.getLatitude();
        //return this.axis.findCoordElement(target);
        return findNearest(this.values, target);
    }
    
    /**
     * Performs a binary search to find the index of the element of the array
     * whose value is closest to the target
     * @param values The array to search
     * @param target The value to search for
     * @return the index of the element in values whose value is closest to target,
     * or -1 if the target is out of range
     */
    private static int findNearest(double[] values, double target)
    {
        // Check that the point is within range
        if (target < values[0] || target > values[values.length - 1])
        {
            return -1;
        }
        
        // do a binary search to find the nearest index
        int low = 0;
        int high = values.length - 1;
        while (low <= high)
        {
            int mid = (low + high) >> 1;
            double midVal = values[mid];
            if (midVal == target)
            {
                return mid;
            }
            else if (midVal < target)
            {
                low = mid + 1;
            }
            else if (midVal > target)
            {
                high = mid - 1;
            }
        }
        
        // If we've got this far we have to decide between values[low]
        // and values[high]
        if (Math.abs(target - values[low]) < Math.abs(target - values[high]))
        {
            return low;
        }
        return high;
    }
    
    /**
     * Simple test harness
     */
    public static void main(String[] args) throws Exception
    {
        NetcdfDataset nc = NetcdfDataset.openDataset("C:\\data\\OA_20060830.nc", true, null);
        GridDataset gd = new GridDataset(nc);
        GeoGrid gg = gd.findGridByName("temperature");
        
        /*EnhancedCoordAxis axis =
            EnhancedCoordAxis.create(gg.getCoordinateSystem().getYHorizAxis());
        System.out.println(axis.getClass().getName());
        for (double lat = -90; lat < 90; lat++)
        {
            LatLonPoint point = new LatLonPointImpl(lat, 0);
            System.out.println("Latitude: " + lat + " index " +
                axis.getIndex(point));
        }*/
        Array arr = gg.readYXData(0, 0);
        //Index index = arr.getIndex();
        IndexIterator it = arr.getIndexIteratorFast();
        while (it.hasNext())
        {
            float val = it.getFloatNext();
            System.out.println("" + val);
        }
        
        nc.close();
    }
    
}
