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

package uk.ac.rdg.resc.edal.coverage.grid;

import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;

/**
 * <p>A {@link Grid} whose points are referenceable to an external coordinate
 * reference system.</p>
 * 
 * <p>Note that the number of dimensions in the external coordinate reference system
 * is not necessarily the same as the number of dimensions in the Grid.  It is
 * perfectly possible to have a two-dimensional grid that samples points from a
 * three-dimensional coordinate system (e.g. a 2D vertical grid in a latitude-longitude-depth
 * coordinate system).</p>
 *
 * <p>The ordering of grid coordinates with respect to real-world coordinates
 * is not defined, since in general the axes of the grid will have no
 * necessary relation to the axes of the real-world coordinate system.
 * Implementations must ensure that they use consistent ordering, which is also
 * respected in the methods of {@link GridValuesMatrix} if this is inherited.
 * (This behaviour is refined in the @link RectilinearGrid} subclass.)</p>
 *
 * @param <DP> The type of direct position to which the points in this grid
 * can be referenced.
 * @author Jon
 */
public interface ReferenceableGrid<DP extends DirectPosition> extends Grid, Domain<DP> {

    /**
     * Transforms a grid coordinates to a direct position.  The returned
     * position's {@link DirectPosition#getCoordinateReferenceSystem() coordinate
     * reference system} will match the {@link #getCoordinateReferenceSystem()
     * coordinate reference system associated with this object}.
     * @param coords The grid coordinates to transform.
     * @return the "real world" coordinates, or null if the grid coordinates are
     * not contained within the {@link #getGridExtent() envelope of the grid}.
     * @throws IllegalArgumentException if the dimension of the grid coordinates
     * does not match the {@link #getDimension() dimension of the grid}.
     */
    public DP transformCoordinates(GridCoordinates coords);

    /**
     * Transforms from a direct position to the grid coordinates of the
     * corresponding grid point.  No interpolation is performed, i.e. the passed
     * position must exactly match a grid point.
     * @param pos - The "real world" coordinates to transform.
     * @return The coordinates of the corresponding grid point, or null if there
     * is no grid point that corresponds with the direct position.
     * @see ReferenceableGrid#inverseTransformCoordinates(org.opengis.geometry.DirectPosition)
     */
    public GridCoordinates inverseTransformCoordinates(DP pos);

    /**
     * Returns the {@link CoordinateReferenceSystem} to which the points in the
     * grid can be referenced.  The {@link #transformCoordinates(org.opengis.coverage.grid.GridCoordinates)}
     * operation will return {@link DirectPosition}s in this CRS.
     * @return the {@link CoordinateReferenceSystem} to which the points in the
     * grid can be referenced.
     */
    @Override
    public CoordinateReferenceSystem getCoordinateReferenceSystem();

    /**
     * Gets the envelope of the grid in the grid's
     * {@link #getCoordinateReferenceSystem() coordinate reference system}.
     */
    public Envelope getExtent();

}
