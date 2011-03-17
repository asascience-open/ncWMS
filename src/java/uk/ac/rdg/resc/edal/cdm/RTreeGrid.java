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

import com.infomatiq.jsi.Rectangle;
import com.infomatiq.jsi.rtree.RTree;
import gnu.trove.TIntProcedure;
import java.util.ArrayList;
import java.util.List;
import uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import java.util.Map;
import java.util.Properties;
import org.khelekore.prtree.MBR;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.cdm.CurvilinearGrid.Cell;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.GridCoordinatesImpl;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Utils;

/**
 * A HorizontalGrid that uses an RTree to look up the nearest neighbour of a point.
 */
final class RTreeGrid extends AbstractCurvilinearGrid
{
    private static final Logger logger = LoggerFactory.getLogger(RTreeGrid.class);

    /**
     * In-memory cache of LookUpTableGrid objects to save expensive re-generation of same object
     * @todo The CurvilinearGrid objects can be very big.  Really we only need to key
     * on the arrays of lon and lat: all other quantities can be calculated from
     * these.  This means that we could make other large objects available for
     * garbage collection.
     */
    private static final Map<CurvilinearGrid, RTreeGrid> CACHE =
            CollectionUtils.newHashMap();

    private final RTree rtree;

    /**
     * The passed-in coordSys must have 2D horizontal coordinate axes.
     */
    public static RTreeGrid generate(GridCoordSystem coordSys)
    {
        CurvilinearGrid curvGrid = new CurvilinearGrid(coordSys);

        synchronized(CACHE)
        {
            RTreeGrid rTreeGrid = CACHE.get(curvGrid);
            if (rTreeGrid == null)
            {
                logger.debug("Need to generate new rtree");
                // Create the RTree for this coordinate system
                RTree rtree = new RTree();
                rtree.init(new Properties()); // Todo: set max node entries?
                int i = 0;
                for (Cell cell : curvGrid.getCells())
                {
                    MBR mbr = cell.getMinimumBoundingRectangle();
                    // Filter out NaN values
                    if (!Double.isNaN(mbr.getMinX()) &&
                        !Double.isNaN(mbr.getMinY()) &&
                        !Double.isNaN(mbr.getMaxX()) &&
                        !Double.isNaN(mbr.getMaxY()))
                    {
                        Rectangle rect = new Rectangle(
                            (float)mbr.getMinX(),
                            (float)mbr.getMinY(),
                            (float)mbr.getMaxX(),
                            (float)mbr.getMaxY()
                        );
                        rtree.add(rect, i);
                    }
                    i++;
                }
                logger.debug("Generated new rtree");
                // Create the RTreeGrid
                rTreeGrid = new RTreeGrid(curvGrid, rtree);
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
    private RTreeGrid(CurvilinearGrid curvGrid, RTree rtree)
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
        float lon = (float)lonLatPos.getLongitude();
        float lat = (float)lonLatPos.getLatitude();
        
        // Create a rectangle representing the target point
        Rectangle rect = new Rectangle(lon, lat, lon, lat);
        IndexCollector indexCollector = new IndexCollector();

        // Query the rTree
        this.rtree.intersects(rect, indexCollector);

        // Now look through the list of intersecting rectangles, finding which
        // one contains the target point
        int ni = this.curvGrid.getNi();
        for (int i : indexCollector.indices)
        {
            int celli = i % ni;
            int cellj = i / ni;
            Cell cell = this.curvGrid.getCell(celli, cellj);
            if (cell.contains(lonLatPos))
            {
                return new GridCoordinatesImpl(celli, cellj);
            }
        }

        return null;
    }

    /** Collects indices of intersecting rectanges from an RTree query */
    private static final class IndexCollector implements TIntProcedure
    {
        private List<Integer> indices = new ArrayList<Integer>();

        @Override public boolean execute(int i) {
            indices.add(i);
            return true; // Allow further invocations of this procedure
        }
    }

    public static void main(String[] args) throws Exception
    {
        Runtime rt = Runtime.getRuntime();
        NetcdfDataset nc = NetcdfDataset.openDataset("C:\\Godiva2_data\\EUMETSAT_TEST\\xc_yc\\W_XX-EUMETSAT-Darmstadt,VIS+IR+IMAGERY,MET7+MVIRI_C_EUMS_20091110120000.nc");
        GridDatatype grid = CdmUtils.getGridDatatype(nc, "ch1");
        long memUsed = getMemoryUsed(rt);
        long start = System.nanoTime();
        HorizontalGrid rTreeGrid = RTreeGrid.generate(grid.getCoordinateSystem());
        long finish = System.nanoTime();
        long rTreeFootprint = getMemoryUsed(rt) - memUsed;
        System.out.println("Rtree constructed in " + (finish - start) / 1e9 + " seconds");
        System.out.println("Rtree consumes " + rTreeFootprint + " bytes");
    }

    private static long getMemoryUsed(Runtime rt) throws Exception
    {
        System.gc();
        Thread.sleep(5000);
        return rt.totalMemory() - rt.freeMemory();
    }
}
