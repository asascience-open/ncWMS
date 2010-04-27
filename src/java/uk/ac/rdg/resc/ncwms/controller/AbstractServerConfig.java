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

package uk.ac.rdg.resc.ncwms.controller;

import java.io.IOException;
import java.util.List;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.ncwms.coords.HorizontalGrid;
import uk.ac.rdg.resc.ncwms.exceptions.InvalidDimensionValueException;
import uk.ac.rdg.resc.ncwms.exceptions.LayerNotDefinedException;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.util.WmsUtils;
import uk.ac.rdg.resc.ncwms.wms.Dataset;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;

/**
 * Partial implementation of the {@link ServerConfig} interface, providing
 * default implementations of some methods
 * @author Jon
 */
public abstract class AbstractServerConfig implements ServerConfig
{
    /**
     * {@inheritDoc}
     * <p>This implementation assumes that the unique layer name is comprised
     * of the {@link Dataset} id and the {@link Layer} id, separated by a forward
     * slash.  It calls {@link #getDatasetById(java.lang.String)} to retrieve
     * the Dataset, then {@link Dataset#
     * @throws LayerNotDefinedException if the given name does not match a layer
     * on this server
     */
    @Override
    public Layer getLayerByUniqueName(String uniqueLayerName)
        throws LayerNotDefinedException
    {
        try
        {
            String[] els = WmsUtils.parseUniqueLayerName(uniqueLayerName);
            Dataset ds = this.getDatasetById(els[0]);
            if (ds == null) throw new NullPointerException();
            Layer layer = ds.getLayerById(els[1]);
            if (layer == null) throw new NullPointerException();
            return layer;
        }
        catch(Exception e)
        {
            throw new LayerNotDefinedException(uniqueLayerName);
        }
    }

    /**
     * {@inheritDoc}
     * <p>This implementation simply defers to {@link
     * ScalarLayer#readPointList(org.joda.time.DateTime, double,
     * uk.ac.rdg.resc.ncwms.datareader.PointList) layer.readPointList()},
     * ignoring the usage log entry.  No data are cached.</p>
     */
    @Override
    public List<Float> readDataGrid(ScalarLayer layer, DateTime dateTime,
        double elevation, HorizontalGrid grid, UsageLogEntry usageLogEntry)
        throws InvalidDimensionValueException, IOException
    {
        return layer.readPointList(dateTime, elevation, grid);
    }

}
