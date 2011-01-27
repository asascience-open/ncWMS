/*
 * Copyright (c) 2011 The University of Reading
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
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR  CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.cdm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.GridEnvelope;
import uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.AbstractHorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.GridEnvelopeImpl;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.Utils;

/**
 * Partial implementation of a {@link HorizontalGrid} that is based upon a
 * curvilinear coordinate system ({@literal i.e.} one which is defined by
 * explicitly specifying the latitude and longitude coordinates of each grid
 * point.
 * @author Jon
 */
abstract class AbstractCurvilinearGrid extends AbstractHorizontalGrid
{
    protected final CurvilinearGrid curvGrid;
    private final GridEnvelopeImpl gridExtent;
    private static final List<String> AXIS_NAMES = Collections.unmodifiableList(
            Arrays.asList("i", "j"));

    protected AbstractCurvilinearGrid(CurvilinearGrid curvGrid)
    {
        // All points will be returned in WGS84 lon-lat
        super(DefaultGeographicCRS.WGS84);
        this.curvGrid = curvGrid;
        this.gridExtent = new GridEnvelopeImpl(curvGrid.getNi(), curvGrid.getNj());
    }

    @Override
    protected HorizontalPosition transformCoordinatesNoBoundsCheck(int i, int j)
    {
        return this.curvGrid.getMidpoint(i, j);
    }

    @Override
    public List<String> getAxisNames() { return AXIS_NAMES; }

    @Override
    public GridEnvelope getGridExtent() { return this.gridExtent; }

    @Override
    public BoundingBox getExtent() {
        return Utils.getBoundingBox(this.curvGrid.getBoundingBox());
    }

    /**
     * {@inheritDoc}
     * <p>This implementation uses {@link #findNearestGridPoint(uk.ac.rdg.resc.edal.position.HorizontalPosition)}
     * to find the nearest grid point, then finds the real-world coordinates
     * of the grid point using {@link #transformCoordinates(uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates)}.
     * If the real-world position matches {@code pos}, the grid coordinates
     * will be returned, otherwise null.</p>
     * <p>This behaviour probably isn't very useful.  The
     * {@link #findNearestGridPoint(uk.ac.rdg.resc.edal.geometry.HorizontalPosition)}
     * method is much more useful in general.  Might have to revisit this.</p>
     */
    @Override
    public GridCoordinates inverseTransformCoordinates(HorizontalPosition pos) {
        GridCoordinates nearestGridPoint = this.findNearestGridPoint(pos);
        HorizontalPosition nearestPos = this.transformCoordinates(nearestGridPoint);
        if (nearestPos.equals(pos)) return nearestGridPoint;
        return null;
    }

}
