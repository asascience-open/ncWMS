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
import uk.ac.rdg.resc.ncwms.config.NcWMS;

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
public class DefaultDataProvider implements DataProvider
{
    private String title;
    private String location;
    private DataLayer[] layers;
    
    /**
     * Constructs a {@link DataProvider} for a {@link NetcdfDataset}
     */
    public DefaultDataProvider(NcWMS.Datasets.Dataset ds) throws IOException
    {
        this.title = ds.getTitle();
        this.location = ds.getLocation();
        
        // Now read the layer metadata
        GridDataset gd = new GridDataset(NetcdfDataset.openDataset(this.location));
        List grids = gd.getGrids();
        this.layers = new DataLayer[grids.size()];
        for (int i = 0; i < grids.size(); i++)
        {
            this.layers[i] = new DefaultDataLayer((GeoGrid)grids.get(i));
        }
        gd.close();
    }
    
    /**
     * @return a human-readable title for this DataProvider
     */
    public String getTitle()
    {
        return this.title;
    }
    
    /**
     * @return all the {@link DataLayer}s that are contained in this
     * DataProvider
     */
    public DataLayer[] getDataLayers()
    {
        return this.layers;
    }
    
}
