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
import java.util.List;
import ucar.unidata.geoloc.LatLonPoint;
import uk.ac.rdg.resc.ncwms.coordsys.CurvilinearGrid.Cell;

/**
 * <p>A {@link LutGenerator} that uses an algorithm similar to that used by the
 * <a href="http://www.giss.nasa.gov/tools/panoply/">Panoply</a> NetCDF and
 * HDF viewer.  This calculates the extents of grid cells in the {@link CurvilinearGrid}
 * by joining up adjacent grid <i>points</i>.  (Note that this differs from the
 * method used by ToolsUI, in which cells are formed by joining grid <i>corners</i>.)</p>
 * <p>Many thanks to Robert Schmunk for kindly spending the time to
 * explain to me how Panoply works.</p>
 * <p>This class is stateless and therefore only a single {@link #INSTANCE instance}
 * is necessary.</p>
 * @author Jon
 */
final class PanoplyLutGenerator extends BufferedImageLutGenerator
{
    /** Singleton instance. */
    public static final PanoplyLutGenerator INSTANCE = new PanoplyLutGenerator();

    /** Private constructor to prevent direct instantiation */
    private PanoplyLutGenerator() {}

    /**
     * Returns a polygon that joins the centres of the neighbouring cells
     */
    @Override
    protected List<LatLonPoint> getPolygon(Cell cell)
    {
        // Prepare a list of the centres of the neighbouring cells
        List<LatLonPoint> neighbourCentres = new ArrayList<LatLonPoint>(4);
        for (Cell neighbour : cell.getEdgeNeighbours())
        {
            neighbourCentres.add(neighbour.getCentre());
        }
        return neighbourCentres;
    }

}
