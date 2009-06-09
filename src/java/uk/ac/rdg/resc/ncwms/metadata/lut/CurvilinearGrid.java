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

package uk.ac.rdg.resc.ncwms.metadata.lut;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ucar.ma2.ArrayDouble;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import uk.ac.rdg.resc.ncwms.metadata.lut.CurvilinearGrid.Cell;

/**
 * A horizontal (2D) grid that is defined by explicitly specifying the longitude and
 * latitude coordinates of its cells.  We assume the WGS84 lat-lon coordinate system.
 * @author Jon
 */
public final class CurvilinearGrid implements Iterable<Cell>
{
    /** The number of grid cells in the i direction */
    private final int ni;
    /** The number of grid cells in the j direction */
    private final int nj;
    /** The longitudes of the centres of the grid cells */
    private final ArrayDouble.D2 longitudes;
    /** The latitudes of the centres of the grid cells */
    private final ArrayDouble.D2 latitudes;
    /** The longitudes of the edges of the grid cells */
    private final ArrayDouble.D2 lonEdges;
    /** The latitudes of the edges of the grid cells */
    private final ArrayDouble.D2 latEdges;

    /**
     * Creates a CurvilinearGrid.
     * @param ni The number of grid cells in the i direction
     * @param nj The number of grid cells in the j direction
     * @param longitudes Array of ni*nj longitude values, with the i direction
     * varying fastest
     * @param latitudes Array of ni*nj latitude values, with the i direction
     * varying fastest
     * @throws IllegalArgumentException if {@code ni <= 0} or {@code nj <= 0}
     * or if the lengths of the arrays are not {@code ni*nj}
     */
    public CurvilinearGrid(int ni, int nj, double[] longitudes, double[] latitudes)
    {
        if (ni <= 0 || nj <= 0)
        {
            throw new IllegalArgumentException("Array dimensions cannot be <= 0");
        }
        if (longitudes.length != ni*nj || latitudes.length != ni*nj)
        {
            throw new IllegalArgumentException("Inconsistent length longitude or latitude arrays");
        }
        this.ni = ni;
        this.nj = nj;
        // Copy the longitude values to the internal array
        this.longitudes = makeArray(ni, nj, longitudes);
        this.latitudes  = makeArray(ni, nj, latitudes);
        // Calculate the edges of the grid cells
        this.lonEdges = CoordinateAxis2D.makeXEdges(this.longitudes);
        this.latEdges = CoordinateAxis2D.makeYEdges(this.latitudes);
    }

    /** Copies values from the given array to an ArrayDouble.2D */
    private static ArrayDouble.D2 makeArray(int ni, int nj, double[] values)
    {
        ArrayDouble.D2 array = new ArrayDouble.D2(ni, nj);
        for (int j = 0; j < nj; j++)
        {
            for (int i = 0; i < ni; i++)
            {
                int index = i + (j * ni);
                array.set(i, j, values[index]);
            }
        }
        return array;
    }

    /**
     * Returns an unmodifiable iterator over the cells in the grid.  In this
     * iterator the i dimension will vary fastest.
     */
    public Iterator<Cell> iterator()
    {
        return new CellIterator();
    }

    private final class CellIterator implements Iterator<Cell>
    {
        int index = 0;

        public boolean hasNext() {
            return this.index < CurvilinearGrid.this.longitudes.getSize();
        }

        public Cell next() {
            int i = this.index % CurvilinearGrid.this.ni;
            int j = this.index / CurvilinearGrid.this.ni;
            this.index++;
            return new Cell(i, j);
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported.");
        }
    }

    /**
     * A cell in the grid.
     */
    public final class Cell
    {
        /** The i coordinate of this cell in the source grid */
        private final int i;
        /** The j coordinate of this cell in the source grid */
        private final int j;

        /** Cell is not instantiable by external clients */
        private Cell(int i, int j)
        {
            this.i = i;
            this.j = j;
        }

        /**
         * Returns a new two-element array [i,j] containing the indices of
         * this cell within the grid
         */
        public int[] getGridCoords()
        {
            return new int[]{this.i, this.j};
        }
        
        /**
         * Returns the lat-lon coordinates of the centre of this grid cell
         */
        public LatLonPoint getCentrePoint()
        {
            return new LatLonPointImpl(
                CurvilinearGrid.this.latitudes.get(this.i, this.j),
                CurvilinearGrid.this.longitudes.get(this.i, this.j)
            );
        }

        /**
         * Returns the lat-lon coordinates of the four corners of this grid cell,
         * travelling in a consistent direction around the cell's centre.  That
         * is to say, a polygon made up of these corner points in this order
         * will not contain any crossing edges.
         * @return a list of four coordinates
         */
        public List<LatLonPoint> getCorners()
        {
            List<LatLonPoint> corners = new ArrayList<LatLonPoint>(4);
            // I think the order is important here
            corners.add(this.getLatLonCornerPoint(i,   j));
            corners.add(this.getLatLonCornerPoint(i+1, j));
            corners.add(this.getLatLonCornerPoint(i+1, j+1));
            corners.add(this.getLatLonCornerPoint(i,   j+1));
            return corners;
        }

        private LatLonPoint getLatLonCornerPoint(int iEdge, int jEdge)
        {
            return new LatLonPointImpl(
                CurvilinearGrid.this.latEdges.get(iEdge, jEdge),
                CurvilinearGrid.this.lonEdges.get(iEdge, jEdge)
            );
        }

        /**
         * Returns true if this cell contains the given point.  This algorithm
         * is based on the "linquad" algorithm from Greg Smith's original
         * Fortran code.
         * @param longitude The longitude of the point
         * @param latitude The latitude of the point
         * @return true if this cell contains the given point
         * @todo document this better, possibly refactor too
         * @todo Move this to the RTree code, since we need to do this calculation
         * in different CRSs.
         */
        public boolean containsPoint(double longitude, double latitude)
        {
            // Determine whether a point p(x,y) lies within or on the boundary of a
            // quadrangle (ABCD) of any shape on a plane.
            double px = longitude;
            double py = latitude;
            double[] pxv = new double[4];
            double[] pyv = new double[4];
            List<LatLonPoint> corners = this.getCorners();
            for (int k = 0; k < 4; k++)
            {
                LatLonPoint corner = corners.get(k);
                pxv[k] = corner.getLongitude();
                pyv[k] = corner.getLatitude();
            }
            
            // Check if the vectorial products PA * PC, PB * PA, PC * PD, and 
            // PD * PB are all negative.
            double zst1 = (px - pxv[0]) * (py - pyv[3]) - (py - pyv[0]) * (px - pxv[3]);
            double zst2 = (px - pxv[3]) * (py - pyv[2]) - (py - pyv[3]) * (px - pxv[2]);
            double zst3 = (px - pxv[2]) * (py - pyv[1]) - (py - pyv[2]) * (px - pxv[1]);
            double zst4 = (px - pxv[1]) * (py - pyv[0]) - (py - pyv[1]) * (px - pxv[0]);

            boolean lin = (zst1 <= 0.0) && (zst2 <= 0.0) && (zst3 <= 0.0) && (zst4 <= 0.0);

            if ((zst1 == 0.0) && (zst2 == 0.0) && (zst3 == 0.0) && (zst4 == 0.0))
            {
                lin = false;
            }
            return lin;
        }

        /**
         * Returns the immediate neighbours of this grid cell: i.e. those cells
         * that adjoin this cell along an edge.
         * @return Up to four neighbouring grid cells.
         */
        public List<Cell> getEdgeNeighbours()
        {
            List<Cell> neighbours = new ArrayList<Cell>(4);
            if (this.i > 0) neighbours.add(new Cell(this.i - 1, this.j));
            if (this.i < CurvilinearGrid.this.ni - 1) neighbours.add(new Cell(this.i + 1, this.j));
            if (this.j > 0) neighbours.add(new Cell(this.i, this.j - 1));
            if (this.j < CurvilinearGrid.this.nj - 1) neighbours.add(new Cell(this.i, this.j + 1));
            return neighbours;
        }
    }
}
