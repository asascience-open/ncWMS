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

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * A HorizontalCoordSys that is created from a "curvilinear" coordinate system,
 * {@literal i.e.} one in which the latitude/longitude coordinates of each
 * grid point are specified using two-dimensional coordinate axes, which explicitly
 * give the lat/lon of each point in the horizontal plane.  In these coordinate
 * systems, finding the nearest grid point to a given lat/lon point is complex.
 * Therefore we pre-calculate a {@link LookUpTable "look-up table"} of
 * the nearest i-j indices to a set of lat-lon points. Coordinate
 * conversions using such a look-up table are not precise but may suffice for
 * many applications.
 *
 * @author Jon Blower
 */
final class LutCoordSys extends HorizontalCoordSys
{
    private static final Logger logger = LoggerFactory.getLogger(LutCoordSys.class);

    /** In-memory cache of LutCoordSys objects to save expensive re-generation of same object */
    private static final Map<Key, LutCoordSys> CACHE = new HashMap<Key, LutCoordSys>();

    private final LookUpTable lut;

    /**
     * The passed-in coordSys must have 2D horizontal coordinate axes.
     */
    public static LutCoordSys generate(GridCoordSystem coordSys, int resolutionMultiplier,
        LutGenerator lutGenerator)
    {
        CurvilinearGrid curvGrid = new CurvilinearGrid(coordSys);
        Key key = new Key(curvGrid, resolutionMultiplier, lutGenerator);
        synchronized(CACHE)
        {
            LutCoordSys lutCoordSys = CACHE.get(key);
            if (lutCoordSys == null)
            {
                logger.debug("Need to generate new look-up table");
                // Create a blank look-up table
                LookUpTable lut = new LookUpTable(curvGrid, resolutionMultiplier);
                // Populate the look-up table
                lutGenerator.populateLut(lut, curvGrid);
                logger.debug("Generated new look-up table");
                // Create the LutCoordSys
                lutCoordSys = new LutCoordSys(lut);
                // Now put this in the cache
                CACHE.put(key, lutCoordSys);
            }
            else
            {
                logger.debug("Look-up table found in cache");
            }
            return lutCoordSys;
        }
    }

    /** Private constructor to prevent direct instantiation */
    private LutCoordSys(LookUpTable lut)
    {
        this.lut = lut;
    }

    @Override
    public int[] latLonToGrid(LatLonPoint latLonPoint) {
        return this.lut.getGridCoordinates(latLonPoint.getLongitude(), latLonPoint.getLatitude());
    }

    private static final class Key
    {
        private CurvilinearGrid curvGrid;
        private int resolutionMultiplier;
        private LutGenerator lutGenerator;

        public Key(CurvilinearGrid curvGrid, int resolutionMultiplier,
            LutGenerator lutGenerator)
        {
            this.curvGrid = curvGrid;
            this.resolutionMultiplier = resolutionMultiplier;
            this.lutGenerator = lutGenerator;
        }

        @Override public int hashCode()
        {
            int hashCode = 17;
            hashCode = 31 * hashCode + this.curvGrid.hashCode();
            hashCode = 31 * hashCode + this.resolutionMultiplier;
            hashCode = 31 * hashCode + this.lutGenerator.hashCode();
            return hashCode;
        }

        @Override public boolean equals(Object obj)
        {
            if (obj == this) return true;
            if (!(obj instanceof Key)) return false;
            Key other = (Key)obj;
            return this.resolutionMultiplier == other.resolutionMultiplier &&
                   this.lutGenerator.equals(other.lutGenerator) &&
                   this.curvGrid.equals(other.curvGrid);
        }
    }
}
