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

import ucar.ma2.Array;
import uk.ac.rdg.resc.ncwms.exceptions.WMSInternalError;

/**
 * A chunk of data, as read by DataLayer.getScanline().  The purpose of this
 * class is to get around the annoying feature of Java that there is no
 * Array superclass (well, there is, but it's Object).  This means that there
 * is no general way to do arr[i] if you don't know the type of arr in advance.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class DataChunk
{
    private Class dataType;
    private Object array;
    
    /**
     * Creates a new instance of DataChunk from an {@link Array}
     * @param data {@link Array} of data as read by Netcdf libaries
     * @throws WMSInternalError if the type of the data is not supported
     */
    public DataChunk(Array data) throws WMSInternalError
    {
        this.array = data.getStorage();
        this.dataType = data.getElementType();
        if (data.getElementType() != double.class &&
            data.getElementType() != float.class)
        {
            throw new WMSInternalError("Data type " + data.getElementType() +
                " not yet supported by this server");
        }
    }
    
    /**
     * Creates a new instance of DataChunk
     * @param data Array of doubles representing the data
     */
    public DataChunk(double[] data)
    {
        this.array = data;
        this.dataType = double.class;
    }
    
    /**
     * Creates a new instance of DataChunk
     * @param data Array of doubles representing the data
     */
    public DataChunk(float[] data)
    {
        this.array = data;
        this.dataType = float.class;
    }
    
    /**
     * Gets the value at the given index in the chunk as a float.  (We don't 
     * need greater precision for making pictures.)
     * @param index the index of the required value in the chunk
     */
    public float getValue(int index)
    {
        if (this.dataType == double.class)
        {
            return (float)((double[])this.array)[index];
        }
        return ((float[])this.array)[index];
    }
    
}
