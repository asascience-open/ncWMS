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

import org.apache.log4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.dataset.EnhanceScaleMissingImpl;
import ucar.nc2.dataset.grid.GeoGrid;

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
    private static final Logger logger = Logger.getLogger(PixelByPixelDataReader.class);
    
    /**
     * Reads data from the given GeoGrid and populates the given pixel array.
     * This method reads each pixel with a separate request to the data source
     * and is not expected to be efficient.
     */
    protected void populatePixelArray(float[] picData, Range tRange, Range zRange,
        PixelMap pixelMap, GeoGrid gg) throws Exception
    {
        long start = System.currentTimeMillis();
        // Get an enhanced version of the variable for dealing with missing values etc
        EnhanceScaleMissingImpl enhanced = getEnhanced(gg);

        // Now create the picture from the data array
        for (int yIndex : pixelMap.getYIndices())
        {
            Range yRange = new Range(yIndex, yIndex);
            for (int xIndex : pixelMap.getXIndices(yIndex))
            {
                Range xRange = new Range(xIndex, xIndex);
                GeoGrid subset = gg.subset(tRange, zRange, yRange, xRange);
                DataChunk dataChunk = new DataChunk(subset.readYXData(0,0));
                float val = (float)enhanced.convertScaleOffsetMissing(dataChunk.getValue(0));
                for (int pixelIndex : pixelMap.getPixelIndices(xIndex, yIndex))
                {
                    picData[pixelIndex] = val;
                }
            }
        }
        logger.debug("Read data pixel-by-pixel in {} ms",
            (System.currentTimeMillis() - start));
    }
    
}
