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

package uk.ac.rdg.resc.edal.coverage;

import java.util.List;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;

/**
 * Contains the metadata about a multidimensional coverage representing a single
 * variable.
 * @author Jon
 */
public interface CoverageMetadata
{
    /** Returns an ID that is unique within the coverages's container. */
    public String getId();

    /** Returns a human-readable title */
    public String getTitle();

    /** Returns a (perhaps-lengthy) description of this coverage */
    public String getDescription();

    /**
     * Returns the coverage's units.
     * @todo What if the coverage has no units?  Empty string or null?
     * @todo Replace with strongly-typed JSR-275 Unit?
     */
    public String getUnits();

    /**
     * Returns the geographic extent of this coverage in latitude-longitude
     * coordinates.  Note that this extent is not necessarily precise so
     * specifying the coordinate system is unnecessary.
     * @return the geographic extent of this coverage in WGS84 latitude-longitude.
     */
    public GeographicBoundingBox getGeographicBoundingBox();

    /**
     * Returns the coverage's horizontal grid, which is an object
     * that translates from real world coordinates to grid coordinates.
     * @return the horizontal grid of this coverage.
     */
    public HorizontalGrid getHorizontalGrid();

    /**
     * Returns the {@link Chronology} used to interpret {@link DateTime}s that
     * represent the {@link #getTimeValues() time values} of this coverage.
     * @return the Chronology used to interpret this coverage's time values, or null
     * if this coverage has no time values.
     */
    public Chronology getChronology();

    /**
     * Returns the list of time instants that are valid for this coverage, in
     * chronological order, or an empty list if this coverage does not have a time axis.
     * @return the list of time instants that are valid for this coverage, in
     * chronological order, or an empty list if this coverage does not have a time axis.
     */
    public List<DateTime> getTimeValues();

    /**
     * Returns the list of elevation values that are valid for this coverage, or
     * an empty list if this coverage does not have a vertical axis.  Note that the
     * values in this list do not have to be ordered (although they usually will
     * be).  Clients must make no assumptions about ordering.
     * @return the list of elevation values that are valid for this coverage, or
     * an empty list if this coverage does not have a vertical axis.
     */
    public List<Double> getElevationValues();

    /**
     * Returns the units of the vertical axis
     * @todo What if the axis has no units?  Empty string or null?
     * @todo Replace with strongly-typed JSR-275 Unit?
     */
    public String getElevationUnits();

    /**
     * Returns true if the positive direction of the elevation axis is up
     * @return true if the positive direction of the elevation axis is up
     * @todo Make the name and meaning of this method clearer
     */
    public boolean isElevationPositive();

    /**
     * Returns true if the vertical axis represents pressure.  In this case the
     * values of elevation will be positive, but will increase downward.
     * @todo This is a lousy name!
     */
    public boolean isElevationPressure();
}
