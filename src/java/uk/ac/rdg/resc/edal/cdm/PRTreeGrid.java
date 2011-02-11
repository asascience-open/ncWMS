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

package uk.ac.rdg.resc.edal.cdm;

import uk.ac.rdg.resc.edal.cdm.CurvilinearGrid.Cell;
import uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import java.util.Map;
import org.khelekore.prtree.MBRConverter;
import org.khelekore.prtree.PRTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.GridCoordinatesImpl;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Utils;

/**
 * A HorizontalGrid that uses a Priority RTree to look up the nearest neighbour of a point.
 */
final class PRTreeGrid extends AbstractCurvilinearGrid
{
    private static final Logger logger = LoggerFactory.getLogger(PRTreeGrid.class);

    /**
     * In-memory cache of LookUpTableGrid objects to save expensive re-generation of same object
     * @todo The CurvilinearGrid objects can be very big.  Really we only need to key
     * on the arrays of lon and lat: all other quantities can be calculated from
     * these.  This means that we could make other large objects available for
     * garbage collection.
     */
    private static final Map<CurvilinearGrid, PRTreeGrid> CACHE =
            CollectionUtils.newHashMap();

    /** The branch factor of the RTree */
    public static final int RTREE_BRANCH_FACTOR = 10;

    private final PRTree<CurvilinearGrid.Cell> rtree;

    /**
     * An object that can determine the minimum bounding rectangle of a
     * CurvilinearGrid's cells.  We can save memory by not storing the MBR
     * information redundantly (perhaps at the expense of some processing power?)
     */
    private static final MBRConverter<CurvilinearGrid.Cell> MBR_CONVERTER
            = new MBRConverter<CurvilinearGrid.Cell>()
    {
        @Override
        public double getMinX(Cell t) {
            return t.getMinimumBoundingRectangle().getMinX();
        }

        @Override
        public double getMinY(Cell t) {
            return t.getMinimumBoundingRectangle().getMinY();
        }

        @Override
        public double getMaxX(Cell t) {
            return t.getMinimumBoundingRectangle().getMaxX();
        }

        @Override
        public double getMaxY(Cell t) {
            return t.getMinimumBoundingRectangle().getMaxY();
        }
    };

    /**
     * The passed-in coordSys must have 2D horizontal coordinate axes.
     */
    public static PRTreeGrid generate(GridCoordSystem coordSys)
    {
        return generate(coordSys, RTREE_BRANCH_FACTOR);
    }

    /**
     * The passed-in coordSys must have 2D horizontal coordinate axes.
     */
    public static PRTreeGrid generate(GridCoordSystem coordSys, int branchFactor)
    {
        CurvilinearGrid curvGrid = new CurvilinearGrid(coordSys);

        synchronized(CACHE)
        {
            PRTreeGrid rTreeGrid = CACHE.get(curvGrid);
            if (rTreeGrid == null)
            {
                logger.debug("Need to generate new rtree");
                // Create the RTree for this coordinate system
                PRTree<CurvilinearGrid.Cell> rtree =
                        new PRTree<CurvilinearGrid.Cell>(MBR_CONVERTER, branchFactor);
                rtree.load(curvGrid.getCells());
                logger.debug("Generated new rtree");
                // Create the RTreeGrid
                rTreeGrid = new PRTreeGrid(curvGrid, rtree);
                // Now put this in the cache
                CACHE.put(curvGrid, rTreeGrid);
            }
            else
            {
                logger.debug("RTree found in cache");
            }
            return rTreeGrid;
        }
    }

    public static void clearCache() {
        synchronized(CACHE) {
            CACHE.clear();
        }
    }

    /** Private constructor to prevent direct instantiation */
    private PRTreeGrid(CurvilinearGrid curvGrid, PRTree<CurvilinearGrid.Cell> rtree)
    {
        // All points will be returned in WGS84 lon-lat
        super(curvGrid);
        this.rtree = rtree;
    }

    /**
     * @return the nearest grid point to the given lat-lon point, or null if the
     * lat-lon point is not contained within this layer's domain.
     */
    @Override
    public GridCoordinates findNearestGridPoint(HorizontalPosition pos)
    {
        LonLatPosition lonLatPos = Utils.transformToWgs84LonLat(pos);
        double lon = lonLatPos.getLongitude();
        double lat = lonLatPos.getLatitude();

        // Find all cells that intersect this point
        Iterable<CurvilinearGrid.Cell> cells = this.rtree.find(lon, lat, lon, lat);
        for (CurvilinearGrid.Cell cell : cells)
        {
            if (cell.contains(lonLatPos))
            {
                return new GridCoordinatesImpl(cell.getI(), cell.getJ());
            }
        }
        return null;
    }

    public static void main(String[] args) throws Exception
    {
        Runtime rt = Runtime.getRuntime();
        NetcdfDataset nc = NetcdfDataset.openDataset("C:\\Godiva2_data\\UCA25D\\UCA25D.20101118.04.nc");
        GridDatatype grid = CdmUtils.getGridDatatype(nc, "sea_level");


        long memUsed = getMemoryUsed(rt);
        HorizontalGrid rTreeGrid = KdTreeGrid.generate(grid.getCoordinateSystem());
        long rTreeFootprint = getMemoryUsed(rt) - memUsed;
        System.out.println("Rtree consumes " + rTreeFootprint + " bytes");
    }

    private static long getMemoryUsed(Runtime rt) throws Exception
    {
        System.gc();
        Thread.sleep(5000);
        return rt.totalMemory() - rt.freeMemory();
    }
}
