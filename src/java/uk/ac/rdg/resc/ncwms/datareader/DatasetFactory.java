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

package uk.ac.rdg.resc.ncwms.datareader;

import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasetFactory;
import ucar.nc2.util.CancelTask;

/**
 * A {@link NetcdfDatasetFactory} that opens datasets without enhancing them,
 * but adds the coordinate systems
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
class DatasetFactory implements NetcdfDatasetFactory
{
    private static DatasetFactory df = new DatasetFactory();
    
    /** Creates a new instance of DatasetFactory */
    private DatasetFactory()
    {
    }
    
    /**
     * Gets the DatasetFactory: there is only ever one instance of this
     */
    public static DatasetFactory get()
    {
        return df;
    }

    /**
     * Opens the dataset without enhancement and adds the coordinate systems
     */
    public NetcdfDataset openDataset(String location, CancelTask cancelTask)
        throws java.io.IOException
    {
        NetcdfDataset nc = NetcdfDataset.openDataset(location, false, cancelTask);
        CoordSysBuilder.addCoordinateSystems(nc, null);
        return nc;
    }
    
}
