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
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.ncwms.util;

import java.util.AbstractList;
import java.util.List;

/**
 * Contains static methods for creating Lists from primitive arrays.
 * @author Jon
 */
public class Lists
{
    
    /**
     * Creates and returns an unmodifiable List that wraps the given array.
     * Changes to the array will be reflected in the List.
     * @param arr The array to wrap as a List
     * @return an unmodifiable List that wraps the array
     * @throws NullPointerException if the array is null
     */
    public static List<Float> fromFloatArray(final float[] arr)
    {
        if (arr == null) throw new NullPointerException("array cannot be null");
        return new AbstractList<Float>()
        {
            @Override public Float get(int index) { return arr[index]; }
            @Override public int size() { return arr.length; }
        };
    }

    /**
     * Creates and returns an unmodifiable List that wraps the given array.
     * Changes to the array will be reflected in the List.
     * @param arr The array to wrap as a List
     * @return an unmodifiable List that wraps the array
     * @throws NullPointerException if the array is null
     */
    public static List<Double> fromDoubleArray(final double[] arr)
    {
        if (arr == null) throw new NullPointerException("array cannot be null");
        return new AbstractList<Double>()
        {
            @Override public Double get(int index) { return arr[index]; }
            @Override public int size() { return arr.length; }
        };
    }

}
