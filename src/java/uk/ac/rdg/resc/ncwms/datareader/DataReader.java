/*
 * Copyright (c) 2007 The University of Reading
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

import java.io.IOException;
import java.util.Hashtable;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

/**
 * Interface for classes that read data and metadata from NetCDF datasets.
 * Called from nj22dataset.py.
 * @author jdb
 */
public interface DataReader
{
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single time index only.
     *
     * @param location Location of the NetCDF dataset (full file path, OPeNDAP URL etc)
     * @param varID Unique identifier for the required variable in the file
     * @param tIndex The index along the time axis as found in getmap.py
     * @param zValue The value of elevation as specified by the client
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @throws WMSExceptionInJava if an error occurs
     */
    public float[] read(String location, String varID,
        int tIndex, String zValue, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava;
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location.
     * @param location Location of the NetCDF dataset (full file path, OPeNDAP URL etc)
     * @return Hashtable of variable IDs mapped to {@link VariableMetadata} objects
     * @throws IOException if there was an error reading from the data source
     */
    public Hashtable<String, VariableMetadata> getVariableMetadata(String location)
        throws IOException;
}
