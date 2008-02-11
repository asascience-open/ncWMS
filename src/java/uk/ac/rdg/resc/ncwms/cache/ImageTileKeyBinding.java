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

package uk.ac.rdg.resc.ncwms.cache;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * TupleBinding to convert an ImageTileKey to a form that can be used as a key
 * in the Berkeley DB.
 * @deprecated Has not kept up with other developments, so is not currently used.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class ImageTileKeyBinding extends TupleBinding
{
    
    /**
     * Writes an ImageTileKey to a TupleOutput
     */
    public void objectToEntry(Object obj, TupleOutput to)
    {
        ImageTileKey key = (ImageTileKey)obj;
        to.writeString(key.getDatasetId());
        to.writeString(key.getVariableId());
        to.writeString(key.getCrs());
        to.writeUnsignedByte(key.getBbox().length);
        for (float el : key.getBbox())
        {
            to.writeFloat(el);
        }
        to.writeUnsignedShort(key.getWidth());
        to.writeUnsignedShort(key.getHeight());
        to.writeDouble(key.getTime());
        to.writeString(key.getElevation());
    }
    
    public Object entryToObject(TupleInput ti)
    {
        ImageTileKey key = new ImageTileKey();
        key.setDatasetId(ti.readString());
        key.setVariableId(ti.readString());
        key.setCrs(ti.readString());
        float[] bbox = new float[ti.readUnsignedByte()];
        for (int i = 0; i < bbox.length; i++)
        {
            bbox[i] = ti.readFloat();
        }
        key.setWidth(ti.readUnsignedShort());
        key.setHeight(ti.readUnsignedShort());
        key.setTime(ti.readDouble());
        key.setElevation(ti.readString());
        return key;
    }
    
}
