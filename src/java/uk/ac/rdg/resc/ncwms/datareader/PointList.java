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

package uk.ac.rdg.resc.ncwms.datareader;

import uk.ac.rdg.resc.ncwms.coordsys.CrsHelper;
import java.util.AbstractList;
import java.util.List;
import uk.ac.rdg.resc.ncwms.coordsys.HorizontalPosition;

/**
 * <p>A list of {@link HorizontalPosition}s in a certain coordinate reference system.
 * Instances of this class usually represent requests for data from an operation
 * such as GetMap or GetTransect: the points in this list are the coordinates
 * of real-world points for which we need data.</p>
 * <p>The order of points in this list is important, hence this class supports
 * index-based to the data.  A typical use of this class would be as follows:</p>
 * <pre>
 *    // Process the client's request and get the list of points for which we need data
 *    PointList pointList = request...;
 *    // Use this list of points to extract data
 *    float[] data = dataReader.read(pointList, otherParams...);
 *    // data.length equals pointList.size()
 *    // Furthermore data[i] corresponds to pointList.get(i)
 * </pre>
 * @todo
 * @author Jon
 */
public abstract class PointList
{
    /**
     * Gets the CrsHelper object that wraps this PointList's coordinate
     * reference system and provides convenience methods for transforming
     * between real-world and grid coordinates.
     */
    public abstract CrsHelper getCrsHelper();

    /**
     * Gets the {@link HorizontalPosition} at the given index in this list.
     * @param index The index of the required HorizontalPosition
     * @return the HorizontalPosition at the given index
     * @throws IndexOutOfBoundsException if {@code index < 0 || index > this.size()}
     */
    public abstract HorizontalPosition getPoint(int index);

    /**
     * Returns the number of points in this list
     * @return the number of points in this list
     */
    public abstract int size();

    /**
     * Creates a PointList from the given List of ProjectionPoints with their
     * coordinate reference system
     * @param list The x-y points to wrap as a PointList
     * @param crsHelper Wrapper around the coordinate reference system of the points
     * @return a new PointList that wraps the given list of projection points
     */
    public static PointList fromList(final List<HorizontalPosition> list, final CrsHelper crsHelper)
    {
        return new PointList() {
            @Override public CrsHelper getCrsHelper() { return crsHelper; }
            @Override public HorizontalPosition getPoint(int index) { return list.get(index); }
            @Override public int size() { return list.size(); }
            @Override public List<HorizontalPosition> asList() { return list; }
        };
    }

    /**
     * Creates a PointList containing a single point.  Useful for getFeatureInfo
     * requests
     * @param point The HorizontalPosition to wrap
     * @param crsHelper Wrapper around the coordinate reference system of the points
     * @return a new PointList that wraps the given projection point
     */
    public static PointList fromPoint(final HorizontalPosition point, final CrsHelper crsHelper)
    {
        return new PointList() {
            public CrsHelper getCrsHelper() { return crsHelper; }
            public HorizontalPosition getPoint(int index) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return point;
            }
            public int size() { return 1; }
        };
    }

    /**
     * <p>Returns a view of this PointList as a java.util.List.  This default
     * implementation wraps {@link #getPoint(int)} and {@link #size()} to form
     * a new List.  Subclasses may override this method.</p>
     * <p>Note that PointList
     * cannot inherit from List because it would not be possible to maintain the
     * contract of {@link List#equals(java.lang.Object)} (we would need also to
     * compare the CoordinateReferenceSystem objects).</p>
     * @return
     */
    public List<HorizontalPosition> asList()
    {
        return new AbstractList<HorizontalPosition>() {

            @Override public HorizontalPosition get(int index) {
                return PointList.this.getPoint(index);
            }

            @Override public int size() {
                return PointList.this.size();
            }

        };
    }

}
