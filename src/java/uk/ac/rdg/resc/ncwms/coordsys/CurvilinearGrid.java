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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayDouble;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.coordsys.CurvilinearGrid.Cell;

/**
 * A horizontal (2D) grid that is defined by explicitly specifying the longitude and
 * latitude coordinates of its cells.  We assume the WGS84 lat-lon coordinate system.
 * This class holds references to passed-in arrays of longitude and latitude,
 * but does not modify them or provide any public methods to modify them.
 * Modification of these arrays outside this class will cause undefined behaviour.
 *
 * @author Jon
 */
final class CurvilinearGrid implements Iterable<Cell>
{
    private static final Logger logger = LoggerFactory.getLogger(CurvilinearGrid.class);

    /** The number of grid cells in the i direction */
    private final int ni;
    /** The number of grid cells in the j direction */
    private final int nj;
    /** The longitudes of the centres of the grid cells, flattened to a 1D array
        of size ni*nj */
    private final double[] longitudes;
    /** The latitudes of the centres of the grid cells, flattened to a 1D array
        of size ni*nj */
    private final double[] latitudes;
    /** The longitudes of the corners of the grid cells */
    private final ArrayDouble.D2 cornerLons;
    /** The latitudes of the corners of the grid cells */
    private final ArrayDouble.D2 cornerLats;
    /** The lat-lon bounding box of the grid */
    private final LatLonRect latLonBbox;

    /**
     * Creates a CurvilinearGrid from a GridCoordSystem.
     * @param coordSys The GridCoordSystem from which this CurvilinearGrid will
     * be created.
     * @throws IllegalArgumentException if the x and y axes of the provided
     * GridCoordSystem are not 2D coordinate axes of type Lon and Lat respectively
     */
    public CurvilinearGrid(GridCoordSystem coordSys)
    {
        CoordinateAxis xAxis = coordSys.getXHorizAxis();
        CoordinateAxis yAxis = coordSys.getYHorizAxis();
        if (xAxis == null || yAxis == null ||
            !(xAxis instanceof CoordinateAxis2D) || !(yAxis instanceof CoordinateAxis2D) ||
            xAxis.getAxisType() != AxisType.Lon || yAxis.getAxisType() != AxisType.Lat)
        {
            throw new IllegalArgumentException("Coordinate system must consist" +
                " of two-dimensional latitude and longitude axes");
        }
        CoordinateAxis2D lonAxis = (CoordinateAxis2D)coordSys.getXHorizAxis();
        CoordinateAxis2D latAxis = (CoordinateAxis2D)coordSys.getYHorizAxis();

        this.latLonBbox = coordSys.getLatLonBoundingBox();

        this.ni = lonAxis.getShape(1);
        this.nj = lonAxis.getShape(0);
        this.longitudes = lonAxis.getCoordValues();
        this.latitudes  = latAxis.getCoordValues();

        // Make sure all longitudes are in the range [-180,180]
        for (int i = 0; i < this.longitudes.length; i++)
        {
            this.longitudes[i] = Longitude.constrain180(this.longitudes[i]);
        }

        // Calculate the corners of the grid cells
        logger.debug("Making longitude corners");
        this.cornerLons = makeCorners(this.longitudes, true);
        logger.debug("Making latitude corners");
        this.cornerLats = makeCorners(this.latitudes, false);
        logger.debug("Made curvilinear grid");
    }

    /**
     * Gets the location of the midpoint of the cell at indices i, j.  The
     * {@link LatLonPoint#getLongitude() longitude coordinate} of the midpoint
     * will be in the range [-180,180].
     * @throws ArrayIndexOutOfBoundsException if i and j combine to give a point
     * outside the grid.
     */
    public LatLonPoint getMidpoint(int i, int j)
    {
        int index = j * this.ni + i;
        return new LatLonPointImpl(
            this.latitudes[index],
            this.longitudes[index]
        );
    }

    /**
     * Gets the location of the four corners of the cell at indices i, j.  The
     * {@link LatLonPoint#getLongitude() longitude coordinate} of all the corners
     * will be in the range [-180,180].
     * Care must therefore be taken when plotting the cell as, in a 
     * lat vs lon projection, corners of cells that cross the anti-meridian
     * will appear at opposite edges of the plot.  (In order to plot such points
     * it is recommended to {@link #harmonizeLongitudes(double, double)
     * harmonize the longitudes} of all the corners with the centre point of the
     * cell.)
     * @throws ArrayIndexOutOfBoundsException if i and j combine to give a point
     * outside the grid.
     */
    public List<LatLonPoint> getCorners(int i, int j)
    {
        List<LatLonPoint> corners = new ArrayList<LatLonPoint>(4);
        corners.add(getCorner(i, j));
        corners.add(getCorner(i+1, j));
        corners.add(getCorner(i+1, j+1));
        corners.add(getCorner(i, j+1));
        return corners;
    }

    /**
     * Gets the coordinates of the corner with the given indices <i>in the arrays
     * of corner coordinates</i> (not in the arrays of midpoints).
     */
    private LatLonPoint getCorner(int cornerI, int cornerJ)
    {
        return new LatLonPointImpl (
            this.cornerLats.get(cornerJ, cornerI),
            this.cornerLons.get(cornerJ, cornerI)
        );
    }

    /**
     * Adapted from {@link CoordinateAxis2D#makeXEdges(ucar.ma2.ArrayDouble.D2)},
     * taking into account the wrapping of longitude at +/- 180 degrees
     */
    private ArrayDouble.D2 makeCorners(double[] midpoints, boolean isLongitude) {
    ArrayDouble.D2 edges = new ArrayDouble.D2(nj+1, ni+1);

    for (int j=0; j<nj-1; j++) {
      for (int i=0; i<ni-1; i++) {
        // the interior edges are the average of the 4 surrounding midpoints
          int index = j * this.ni + i;
          double midpoint1 = midpoints[index];
          double midpoint2 = midpoints[index];
          double midpoint3 = midpoints[index];
          double midpoint4 = midpoints[index];
          if (isLongitude) {
              // Make sure that all corners are as close together as possible,
              // e.g. see whether we need to use -179 or +181.
              midpoint2 = harmonizeLongitudes(midpoint1, midpoint2);
              midpoint3 = harmonizeLongitudes(midpoint1, midpoint3);
              midpoint4 = harmonizeLongitudes(midpoint1, midpoint4);
          }
          double xval = (midpoint1 + midpoint2 + midpoint3 + midpoint4) / 4.0;
        edges.set(j+1, i+1, xval);
      }
      // extrapolate to exterior points
      edges.set(j+1, 0, edges.get(j+1,1) - (edges.get(j+1,2) - edges.get(j+1,1)));
      edges.set(j+1, ni, edges.get(j+1,ni-1) + (edges.get(j+1,ni-1) - edges.get(j+1,ni-2)));
    }

    // extrapolate to the first and last row
    for (int x=0; x<ni+1; x++) {
      edges.set(0, x, edges.get(1,x) - (edges.get(2,x) - edges.get(1,x)));
      edges.set(nj, x, edges.get(nj-1,x) + (edges.get(nj-1,x) - edges.get(nj-2,x)));
    }

    return edges;
  }

    /**
     * Given a reference longitude and a "test" longitude, this routine returns
     * a longitude point equivalent to the test longitude such that the expression
     * {@code abs(ref - test)} is as small as possible.
     * @param ref Reference longitude, which must be in the range [-180,180]
     * @param test Test longitude
     * @return A longitude point equivalent to the test longitude that minimizes
     * the expression {@code abs(ref - test)}.  This point will not necessarily
     * be in the range [-180,180]
     * @throws IllegalArgumentException if the reference longitude is not in the
     * range [-180,180]
     * @todo unit tests for this
     * @todo move to Longitude class?
     */
    public static double harmonizeLongitudes(double ref, double test)
    {
        if (ref < -180.0 || ref > 180.0)
        {
            throw new IllegalArgumentException("Reference longitude must be " +
                "in the range [-180,180]");
        }
        double lon1 = Longitude.constrain180(test);
        double lon2 = ref < 0.0 ? lon1 - 360.0 : lon1 + 360.0;
        double d1 = Math.abs(ref - lon1);
        double d2 = Math.abs(ref - lon2);
        return d1 < d2 ? lon1 : lon2;
    }

    /** Returns the number of cells in this grid */
    public int size()
    {
        return this.longitudes.length;
    }

    public LatLonRect getLatLonBoundingBox()
    {
        return this.latLonBbox;
    }

    // TODO: could precompute this
    @Override public int hashCode()
    {
        int hashCode = 17;
        hashCode = 31 * hashCode + this.ni;
        hashCode = 31 * hashCode + this.nj;
        hashCode = 31 * hashCode + Arrays.hashCode(this.longitudes);
        hashCode = 31 * hashCode + Arrays.hashCode(this.latitudes);
        return hashCode;
    }

    @Override public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (!(obj instanceof CurvilinearGrid)) return false;
        CurvilinearGrid other = (CurvilinearGrid)obj;
        return this.ni == other.ni &&
               this.nj == other.nj &&
               Arrays.equals(this.longitudes, other.longitudes) &&
               Arrays.equals(this.latitudes, other.latitudes);
    }

    /** Returns an unmodifiable iterator over the cells in this grid, with the
     * i direction varying fastest. */
    public Iterator<Cell> iterator()
    {
        return new CellIterator();
    }

    /** An unmodifiable iterator over the cells in this grid */
    private final class CellIterator implements Iterator<Cell>
    {
        private int index = 0;

        public boolean hasNext() {
            return this.index < CurvilinearGrid.this.size();
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
     * A cell within this curvilinear grid.  Essentially provides convenience
     * methods for finding the centre, the corners and the neighbours of this
     * cell.
     */
    public class Cell
    {
        private final int i;
        private final int j;

        /** Can only be instantiated from the CurvilinearGrid class */
        private Cell(int i, int j)
        {
            this.i = i;
            this.j = j;
        }

        /** Gets the i index of this cell within the curvilinear grid */
        public int getI() { return this.i; }

        /** Gets the j index of this cell within the curvilinear grid */
        public int getJ() { return this.j; }

        /** Gets the centre point of this cell. */
        public LatLonPoint getCentre()
        {
            return CurvilinearGrid.this.getMidpoint(this.i, this.j);
        }

        /**
         * Returns a list of the corners of this
         * @return
         */
        public List<LatLonPoint> getCorners()
        {
            return CurvilinearGrid.this.getCorners(this.i, this.j);
        }

        /**
         * Gets the neighbours of this cell (up to four) that join this cell
         * along an edge.  The order of the cells in the list is such that
         * the centres of the cells can be joined to form a polygon in which
         * the edges do not cross.
         */
        public List<Cell> getEdgeNeighbours()
        {
            List<Cell> neighbours = new ArrayList<Cell>(4);
            if (this.i > 0) {
                neighbours.add(new Cell(this.i - 1, this.j));
            }
            if (this.j > 0) {
                neighbours.add(new Cell(this.i, this.j - 1));
            }
            if (this.i < CurvilinearGrid.this.ni - 1) {
                neighbours.add(new Cell(this.i + 1, this.j));
            }
            if (this.j < CurvilinearGrid.this.nj - 1) {
                neighbours.add(new Cell(this.i, this.j + 1));
            }
            return neighbours;
        }

        @Override public int hashCode()
        {
            int hashCode = 17;
            hashCode = 31 * hashCode + this.i;
            hashCode = 31 * hashCode + this.j;
            return hashCode;
        }

        @Override public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (!(obj instanceof Cell)) return false;
            Cell other = (Cell)obj;
            return this.i == other.i && this.j == other.j;
        }
    }

}
