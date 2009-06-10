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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.Range;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;

/**
 * A DataReader (not expected to be efficient) that reads each pixel individually
 * from the source data
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class PixelByPixelDataReader extends DefaultDataReader
{
    private static final Logger logger = LoggerFactory.getLogger(PixelByPixelDataReader.class);
    
    /**
     * Reads data from the given GeoGrid and populates the given pixel array.
     * This method reads each pixel with a separate request to the data source
     * and is not expected to be efficient.
     */
    @Override
    protected void populatePixelArray(float[] picData, Range tRange, Range zRange,
        PixelMap pixelMap, GridDatatype grid) throws Exception
    {
        long start = System.currentTimeMillis();
        // Get a VariableDS for unpacking and checking for missing data
        VariableDS var = grid.getVariable();

        // Now create the picture from the data array
        for (int j : pixelMap.getJIndices())
        {
            Range yRange = new Range(j, j);
            for (int i : pixelMap.getIIndices(j))
            {
                Range xRange = new Range(i, i);
                GridDatatype subset = grid.makeSubset(null, null, tRange, zRange, yRange, xRange);
                // Read all of the x-y data in this subset
                Array xySlice = subset.readDataSlice(0, 0, -1, -1);
                Index index = xySlice.getIndex();
                float val = xySlice.getFloat(index.set(0, 0));
                val = (float)var.convertScaleOffsetMissing(val);
                for (int pixelIndex : pixelMap.getPixelIndices(i, j))
                {
                    picData[pixelIndex] = val;
                }
            }
        }
        logger.debug("Read data pixel-by-pixel in {} ms",
            (System.currentTimeMillis() - start));
    }
    
}
