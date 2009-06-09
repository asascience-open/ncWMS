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
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.lut.CurvilinearGrid.Cell;

/**
 * A look-up table generator that uses an Rtree spatial data index to find
 * the nearest grid point to a given longitude-latitude point.  This object is
 * stateless so only one {@link RtreeLutGenerator#INSTANCE instance} is ever created.
 * This instance is immutable and thread-safe.
 * @author Adit Santokhee
 * @author Jon Blower
 */
public final class RtreeLutGenerator implements LutGenerator
{
    private static final Logger logger = LoggerFactory.getLogger(RtreeLutGenerator.class);

    private static final String NORTH_POLAR_STEREOGRAPHIC_CODE = "EPSG:32661";
    private static final String SOUTH_POLAR_STEREOGRAPHIC_CODE = "EPSG:32761";

    /** Singleton instance */
    public static final RtreeLutGenerator INSTANCE = new RtreeLutGenerator();

    /** Private constructor to prevent direct instantiation */
    private RtreeLutGenerator() {}

    public LookUpTable generateLut(LutCacheKey key) throws Exception
    {
        logger.debug("Generating LUT for key {}", key);

        // Create three RTrees, one for the high latitudes, one for the low
        // latitudes and one for the mid-latitudes.  We allow some overlap
        // to ensure that points don't fall between the gaps.
        List<RtreeWrapper> rTrees = new ArrayList<RtreeWrapper>();
        rTrees.add(new RtreeWrapper(-90.0, -65.0, SOUTH_POLAR_STEREOGRAPHIC_CODE));
        rTrees.add(new RtreeWrapper(-70.0,  70.0, null)); // Will use WGS84 lon-lat coordinates
        rTrees.add(new RtreeWrapper( 65.0,  90.0, NORTH_POLAR_STEREOGRAPHIC_CODE));

        // Add the points from the source coordinate system to the Rtrees.
        long nanoTime = System.nanoTime();
        logger.debug("Building rTrees...");
        for (Cell cell : key.getSourceGrid())
        {
            // Add this point to each of the RTrees: the rTrees will ignore
            // any points that are outside of their latitude ranges
            for (RtreeWrapper rTree : rTrees)
            {
                rTree.addCell(cell);
            }
        }
        logger.debug("Built rTrees in {} seconds", (System.nanoTime() - nanoTime) / 1000000000.0);
        
        // Now we build up the look-up table.
        LookUpTable lut = new LookUpTable(key);
        nanoTime = System.nanoTime();
        Regular1DCoordAxis lonAxis = key.getLutLonAxis();
        Regular1DCoordAxis latAxis = key.getLutLatAxis();
        for (int latIndex = 0; latIndex < latAxis.getSize(); latIndex++)
        {
            double lat = latAxis.getCoordValue(latIndex);
            for (int lonIndex = 0; lonIndex < lonAxis.getSize(); lonIndex++)
            {
                double lon = lonAxis.getCoordValue(lonIndex);
                // Look through each of the RTrees till we find a match
                for (RtreeWrapper rTree : rTrees)
                {
                    Cell containingCell = rTree.findContainingCell(lon, lat);
                    if (containingCell != null)
                    {
                        // TODO: check that the lat-lon point falls in the grid cell
                        // Add these coordinates to the look-up table.
                        lut.setGridCoordinates(lonIndex, latIndex, containingCell.getGridCoords());
                        break;
                    }
                }
            }
        }
        logger.debug("Built look-up table in {} seconds", (System.nanoTime() - nanoTime) / 1000000000.0);

        return lut;
    }

}
