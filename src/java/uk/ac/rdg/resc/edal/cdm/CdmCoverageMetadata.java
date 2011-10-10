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
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package uk.ac.rdg.resc.edal.cdm;

import java.util.List;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;

/**
 * A CoverageMetadata object that stores information about a CDM GridDatatype.
 * @author Jon
 */
final class CdmCoverageMetadata implements CoverageMetadata
{
    private final String id;
    private final String title;
    private final String description;
    private final String units;
    private final GeographicBoundingBox bbox;
    private final HorizontalGrid horizGrid;
    private final Chronology chronology;
    private final List<DateTime> timesteps;
    private final ElevationAxis zAxis;

    /**
     * Constructs a CdmLayerMetadata object.
     * @param grid GridDatatype object from which we will read metadata.  No
     * references to the GridDatatype object are kept in this object.
     */
    public CdmCoverageMetadata(GridDatatype grid, GeographicBoundingBox bbox,
            HorizontalGrid horizGrid, List<DateTime> timesteps, ElevationAxis zAxis)
    {
        this.id = grid.getName();
        this.title = CdmUtils.getVariableTitle(grid.getVariable());
        this.description = grid.getDescription();
        this.units = grid.getUnitsString();
        this.bbox = bbox;
        this.horizGrid = horizGrid;
        this.timesteps = timesteps;
        this.zAxis = zAxis;
        this.chronology = timesteps == null || timesteps.isEmpty()
                ? null
                : timesteps.get(0).getChronology();
    }

    @Override
    public String getId() { return this.id; }

    @Override
    public String getTitle() { return this.title; }

    @Override
    public String getDescription() { return this.description; }

    @Override
    public String getUnits() { return this.units; }

    @Override
    public GeographicBoundingBox getGeographicBoundingBox() { return this.bbox; }

    @Override
    public HorizontalGrid getHorizontalGrid() { return this.horizGrid; }

    @Override
    public Chronology getChronology() { return this.chronology; }

    @Override
    public List<DateTime> getTimeValues() { return this.timesteps; }

    @Override
    public List<Double> getElevationValues() { return this.zAxis.getValues(); }

    @Override
    public String getElevationUnits() { return this.zAxis.getUnits(); }

    @Override
    public boolean isElevationPositive() { return this.zAxis.isPositive(); }

    @Override
    public boolean isElevationPressure() { return this.zAxis.isPressure(); }
    
    ElevationAxis getElevationAxis() { return this.zAxis; }
}
