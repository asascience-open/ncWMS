/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.ncwms.dataprovider;

import java.io.IOException;
import java.util.List;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridDataset;
import uk.ac.rdg.resc.ncwms.exceptions.NcWMSConfigException;

/**
 * Default {@link DataProvider} - simply uses the NetCDF libraries.  Should work
 * (but may not be optimally efficient) for any data source that can be read
 * by the Java NetCDF libraries (including OPeNDAP sources).
 *
 * @todo Use cached NetCDF files?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class DefaultDataProvider extends DataProvider
{
    
    /**
     * Constructs a {@link DataProvider} for a {@link NetcdfDataset}.
     * Reads the metadata for each {@link DataLayer}.
     * @param location The location of the underlying dataset
     * @throws IOException if there was an io error opening or closing the
     * underlying data file
     * @throws NcWMSConfigException if the metadata was invalid or could not be
     * read.
     */
    public DefaultDataProvider(String location)
        throws IOException, NcWMSConfigException
    {
        // Read the layer metadata
        NetcdfDataset nc = NetcdfDataset.openDataset(location);
        GridDataset gd = new GridDataset(nc);
        List grids = gd.getGrids();
        for (int i = 0; i < grids.size(); i++)
        {
            GeoGrid gg = (GeoGrid)grids.get(i);
            this.layers.put(gg.getName(), new DefaultDataLayer(gg, this));
        }
        nc.close();
    }
    
}
