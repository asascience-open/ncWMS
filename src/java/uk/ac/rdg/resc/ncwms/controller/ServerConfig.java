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

package uk.ac.rdg.resc.ncwms.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.ncwms.coords.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;

/**
 * Top-level configuration object that contains metadata about the server itself
 * and the set of {@link Dataset}s that the server exposes.
 * @author Jon
 */
public interface ServerConfig
{
    /** Returns a human-readable title for this server */
    public String getTitle();

    /** Returns the maximum image that can be requested through GetMap */
    public int getMaxImageWidth();

    /** Returns the maximum height that can be requested through GetMap */
    public int getMaxImageHeight();

    /** Returns a (perhaps-lengthy) description of this server */
    public String getAbstract();
    
    /** Returns a set of keywords that help to describe this server */
    public Set<String> getKeywords();

    /**
     * Returns the {@link Layer} with the given unique name, or null if the
     * given name does not match a {@link Layer}.
     * @param name the Layer's name, which is unique on this server (usually a
     * combination of the {@link Dataset#getId() dataset's id} and the
     * {@link Layer#getId() layer's id}.
     * @return the {@link Layer} with the given unique name, or null if the
     * given name does not match a {@link Layer}.
     * @throws LayerNotDefinedException if there is no layer with the given name.
     */
    public Layer getLayerByUniqueName(String name) throws LayerNotDefinedException;

    /**
     * Reads a grid of data from the given layer, used by the GetMap operation.
     * Many implementations will
     * simply call {@link ScalarLayer#readPointList(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.datareader.PointList) layer.readPointList()} to
     * implement this method, but others may choose to implement in a different
     * way, perhaps to allow for caching of the data.  If implementations return
     * cached data they must indicate this by setting {@link UsageLogEntry#setUsedCache(boolean)}.
     * @param layer The layer containing the data
     * @param time The time instant for which we require data.  If this does not
     * match a time instant in {@link Layer#getTimeValues()} an {@link InvalidDimensionValueException}
     * will be thrown.  (If this Layer has no time axis, this parameter will be ignored.)
     * @param elevation The elevation for which we require data (in the
     * {@link Layer#getElevationUnits() units of this Layer's elevation axis}).  If
     * this does not match a valid {@link Layer#getElevationValues() elevation value}
     * in this Layer, this method will throw an {@link InvalidDimensionValueException}.
     * (If this Layer has no elevation axis, this parameter will be ignored.)
     * @param grid The grid of points, one point per pixel in the image that will
     * be created in the GetMap operation
     * @param usageLogEntry
     * @return a List of data values, one for each point in
     * the {@code grid}, in the same order.
     * @throws InvalidDimensionValueException if {@code dateTime} or {@code elevation}
     * do not represent valid values along the time and elevation axes.
     * @throws IOException if there was an error reading from the data source
     */
    public List<Float> readDataGrid(ScalarLayer layer, DateTime dateTime,
        double elevation, HorizontalGrid grid, UsageLogEntry usageLogEntry)
        throws InvalidDimensionValueException, IOException;

    /**
     * Returns the {@link Dataset} with the given unique id, or null if the given
     * id doesn't match a dataset.
     * @param datasetId the dataset's identifier
     * @return the {@link Dataset} with the given unique id, or null if the given
     * id doesn't match a dataset.
     * @throws IOException if there was an i/o error reading from the underlying
     * data
     */
    public Dataset getDatasetById(String datasetId) throws IOException;

    /**
     * <p>Returns true if this server is allowed to produce a Capabilities document
     * that includes {@link #getDatasets() all datasets} on this server.  This
     * document could get extremely large so return true with caution.</p>
     * @return true if this server is allowed to produce a Capabilities document
     * that includes all datasets
     */
    public boolean getAllowsGlobalCapabilities();

    /**
     * Gets an unmodifiable Map of dataset IDs to Dataset objects for all datasets
     * on this server, or null if this is not possible on this server.
     * @throws IOException if an i/o error occurs reading from the underlying
     * data store
     */
    public Map<String, ? extends Dataset> getAllDatasets() throws IOException;

    /**
     * <p>Returns the date/time at which the data on this server were last updated.
     * This is used for Capabilities document version control in the
     * UPDATESEQUENCE part of the Capabilities document.</p>
     * <p>If the data on this server are constantly being updated, the safest
     * thing to do is to return the current date/time.  This will mean that
     * clients should never cache the Capabilities document.</p>
     * @return the date/time at which the data on this server were last updated.
     */
    public DateTime getLastUpdateTime();

    /**
     * Returns the web address of the organization that is providing this service.
     * @return the web address of the organization that is providing this service.
     */
    public String getServiceProviderUrl();

    public String getContactName();

    public String getContactOrganization();

    public String getContactTelephone();

    public String getContactEmail();

}
