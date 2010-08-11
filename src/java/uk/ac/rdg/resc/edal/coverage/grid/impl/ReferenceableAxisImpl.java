/*
 * Copyright (c) 2010 The University of Reading
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
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR  CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.coverage.grid.impl;

import java.util.Arrays;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import uk.ac.rdg.resc.edal.coverage.grid.ReferenceableAxis;

/**
 * Immutable implementation of a {@link ReferenceableAxis}, whose values are
 * not necessarily regularly spaced along the axis.
 * @author Jon
 */
public final class ReferenceableAxisImpl extends AbstractReferenceableAxis
{
    private final double[] axisValues;
    private double firstValue; // The first value on the axis
    private double lastValue;  // The last value on the axis
    private double minValue;
    private double maxValue;

    /**
     * Creates a ReferenceableAxis from the given array of axis values.  The
     * axis values are copied to internal data structures, therefore subsequent
     * modifications to the array of axis values have no effect on this object.
     * @param name The name of the axis
     * @param axisValues Array of axis values; must be in strictly ascending
     * numerical order
     * @param isLongitude True if this is a longitude axis in degrees (hence
     * values of 0 and 360 are equivalent).
     * @throws NullPointerException if {@code axisValues} is null
     * @throws IllegalArgumentException if the axis values are not in strictly
     * ascending numerical order, or if the array of axis values is empty
     */
    public ReferenceableAxisImpl(String name, double[] axisValues, boolean isLongitude)
    {
        super(name, isLongitude);
        this.axisValues = axisValues.clone();
        this.setup();
    }

    /**
     * Creates a ReferenceableAxis from the given array of axis values.  The
     * axis values are copied to internal data structures, therefore subsequent
     * modifications to the array of axis values have no effect on this object.
     * @param axis The coordinate system axis to which values on this axis
     * are referenceable.
     * @param axisValues Array of axis values; must be in strictly ascending
     * numerical order
     * @param isLongitude True if this is a longitude axis in degrees (hence
     * values of 0 and 360 are equivalent).
     * @throws NullPointerException if {@code axisValues} is null
     * @throws IllegalArgumentException if the axis values are not in strictly
     * ascending numerical order, or if the array of axis values is empty
     */
    public ReferenceableAxisImpl(CoordinateSystemAxis axis, double[] axisValues, boolean isLongitude)
    {
        super(axis, isLongitude);
        this.axisValues = axisValues.clone();
        this.setup();
    }

    /**
     * Checks that the axis values are in ascending order, throwing an
     * IllegalArgumentException if not.
     */
    private void setup()
    {
        if (this.axisValues.length == 0) {
            throw new IllegalArgumentException("Zero-length array");
        }
        double prevVal = this.axisValues[0];
        for (int i = 1; i < this.axisValues.length; i++)
        {
            if (this.axisValues[i] <= prevVal)
            {
                throw new IllegalArgumentException("Coordinate values must increase monotonically");
            }
            prevVal = this.axisValues[i];
        }

        // Now precalculate the axis bounds
        this.firstValue = this.axisValues[0];
        this.lastValue = this.axisValues[this.axisValues.length - 1];
        this.minValue = super.getMinimumValue();
        this.maxValue = super.getMaximumValue();
    }

    @Override
    public double getCoordinateValue(int index) {
        return this.axisValues[index];
    }

    @Override
    protected int doGetCoordinateIndex(double value) {
        // Do a binary search for the coordinate value
        int index = Arrays.binarySearch(this.axisValues, value);
        return index >= 0 ? index : -1;
    }

    @Override
    protected int doGetNearestCoordinateIndex(double value) {
        // The axis values are in ascending order so we can use a binary search
        int index = Arrays.binarySearch(this.axisValues, value);

        // Check for an exact match
        if (index >= 0)  return index;

        // No exact match, but we have the insertion point, i.e. the index of
        // the first element that is greater than the target value
        int insertionPoint = -(index + 1);

        // Deal with the extremes
        if (insertionPoint == 0) return insertionPoint;
        if (insertionPoint == this.axisValues.length) return insertionPoint - 1;
        
        // We need to work out which index is closer: insertionPoint or
        // (insertionPoint - 1)
        double d1 = Math.abs(value - this.getCoordinateValue(insertionPoint));
        double d2 = Math.abs(value - this.getCoordinateValue(insertionPoint - 1));
        if (d1 < d2) return insertionPoint;
        return insertionPoint - 1;
    }

    @Override
    public int getSize() {
        return this.axisValues.length;
    }

    // The following methods are overridden for efficiency reasons.  This object
    // is immutable, so all of these quantities can be precalculated

    @Override protected double getFirstValue() { return this.firstValue; }
    @Override protected double getLastValue() { return this.lastValue; }
    @Override protected double getMinimumValue() { return this.minValue; }
    @Override protected double getMaximumValue() { return this.maxValue; }

}
