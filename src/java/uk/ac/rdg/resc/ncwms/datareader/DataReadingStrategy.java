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
package uk.ac.rdg.resc.ncwms.datareader;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.Range;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;

/**
 * <p>Defines different strategies for reading data from files. The grid below represents the source
 * data.  Black grid squares represent data points that must be read from the source
 * data and will be used to generate the final output (e.g. image):</p>
 * <img src="doc-files/pixelmap_pbp.png">
 * <p>A variety of strategies are possible for reading these data points:</p>
 *
 * <h3>Strategy 1: read data points one at a time</h3>
 * <p>Read each data point individually by iterating through {@link PixelMap#getJIndices}
 *    and {@link PixelMap#getIIndices}.  This minimizes the memory footprint as the minimum
 *    amount of data is read from disk.  However, in general this method is inefficient
 *    as it maximizes the overhead of the low-level data extraction code by making
 *    a large number of small data extractions.  This is the {@link #PIXEL_BY_PIXEL
 *    pixel-by-pixel} strategy and is not recommended for general use.</p>
 *
 * <h3>Strategy 2: read all data points in one operation</h3>
 * <p>Read all data in one operation (potentially including lots of data points
 *       that are not needed) by finding the overall i-j bounding box with
 *       {@link PixelMap#getMinIIndex}, {@link PixelMap#getMaxIIndex}, {@link PixelMap#getMinJIndex}
 *       and {@link PixelMap#getMaxJIndex}.  This minimizes the number
 *       of calls to low-level data extraction code, but may result in a large memory
 *       footprint.  The {@link DataReader} would then subset this data array in-memory.
 *       This is the {@link #BOUNDING_BOX bounding-box} strategy.  This approach is
 *       recommended for remote datasets (e.g. on an OPeNDAP server) and compressed
 *       dataasets as it minimizes the overhead associated with the individual
 *       data-reading operations.</p>
 * <p>This approach is illustrated in the diagram below.  Grey squares represent
 * data points that are read into memory but are discarded because they do not
 * form part of the final image:</p>
 * <img src="doc-files/pixelmap_bbox.png">
 *
 * <h3>Strategy 3: Read "scanlines" of data</h3>
 * <p>A compromise strategy, which balances memory considerations against the overhead
 *       of the low-level data extraction code, works as follows:
 *       <ol>
 *          <li>Iterate through each row (i.e. each j index) that is represented in
 *              the PixelMap using {@link PixelMap#getJIndices}.</li>
 *          <li>For each j index, extract data from the minimum to the maximum i index
 *              in this row (a "scanline") using {@link PixelMap#getMinIIndexInRow} and
 *              {@link PixelMap#getMaxIIndexInRow}.  (This assumes that the data are stored with the i
 *              dimension varying fastest, meaning that the scanline represents
 *              contiguous data in the source files.)</li>
 *       </ol>
 *       Therefore if there are 25 distinct j indices in the PixelMap there will be 25
 *       individual calls to the low-level data extraction code.  This algorithm has
 *       been found to work well in a variety of situations although it may not always
 *       be the most efficient.  This is the {@link #SCANLINE scanline} strategy.</p>
 * <p>This approach is illustrated in the diagram below.  There is now a much smaller
 * amount of "wasted data" (i.e. grey squares) than in Strategy 2, and there are
 * much fewer individual read operations than in Strategy 1.</p>
 * <img src="doc-files/pixelmap_scanline.png">
 * @author Jon
 */
public enum DataReadingStrategy {

    /**
     * Reads "scanlines" of data, leading to a smaller memory footprint than
     * the {@link #BOUNDING_BOX bounding-box} strategy, but a larger number of individual
     * data-reading operations.  Recommended for use when the overhead of
     * a data-reading operation is low, e.g. for local, uncompressed files.
     */
    SCANLINE {
        @Override
        public void populatePixelArray(float[] picData, Range tRange, Range zRange,
            PixelMap pixelMap, GridDatatype grid) throws Exception
        {
            logger.debug("Reading data using a scanline algorithm");
            // Cycle through the y indices, extracting a scanline of
            // data each time from minX to maxX
            logger.debug("Shape of grid: {}", Arrays.toString(grid.getShape()));
            // Get a VariableDS for unpacking and checking for missing data
            VariableDS var = grid.getVariable();
            for (int j : pixelMap.getJIndices()) {
                Range yRange = new Range(j, j);
                // Read a row of data from the source
                int imin = pixelMap.getMinIIndexInRow(j);
                int imax = pixelMap.getMaxIIndexInRow(j);
                Range xRange = new Range(imin, imax);
                // Read a chunk of data - values will not be unpacked or
                // checked for missing values yet
                logger.debug("tRange: {}, zRange: {}, yRange: {}, xRange: {}", new Object[]{tRange, zRange, yRange, xRange});
                GridDatatype subset = grid.makeSubset(null, null, tRange, zRange, yRange, xRange);
                logger.debug("Subset shape = {}", Arrays.toString(subset.getShape()));
                // Read all of the x-y data in this subset
                Array xySlice = subset.readDataSlice(0, 0, -1, -1);
                logger.debug("Slice shape = {}", Arrays.toString(xySlice.getShape()));
                // We now have a 2D array in y,x order.  We don't reduce this array
                // because it will go to zero size if there is only one point in
                // each direction.
                Index index = xySlice.getIndex();

                // Now copy the scanline's data to the picture array
                for (int i : pixelMap.getIIndices(j)) {
                    float val = xySlice.getFloat(index.set(0, i - imin));
                    // We unpack and check for missing values just for
                    // the points we need to display.
                    val = (float) var.convertScaleOffsetMissing(val);
                    // Now we set the value of all the image pixels associated with
                    // this data point.
                    for (int p : pixelMap.getPixelIndices(i, j)) {
                        picData[p] = val;
                    }
                }
            }
        }
    },

    /**
     * Reads all data in a single operation, then subsets in memory.  Recommended
     * in situations in which individual data reads are expensive, e.g. when
     * reading from OPeNDAP datasets or compressed files.
     */
    BOUNDING_BOX {
        @Override
        public void populatePixelArray(float[] picData, Range tRange, Range zRange,
            PixelMap pixelMap, GridDatatype grid) throws Exception
        {
            logger.debug("Reading data using a bounding-box algorithm");
            // Read the whole chunk of x-y data
            Range xRange = new Range(pixelMap.getMinIIndex(), pixelMap.getMaxIIndex());
            Range yRange = new Range(pixelMap.getMinJIndex(), pixelMap.getMaxJIndex());
            logger.debug("Shape of grid: {}", Arrays.toString(grid.getShape()));
            logger.debug("tRange: {}, zRange: {}, yRange: {}, xRange: {}", new
                Object[] {tRange, zRange, yRange, xRange});
            long start = System.currentTimeMillis();
            GridDatatype subset = grid.makeSubset(null, null, tRange, zRange, yRange, xRange);
            // Read all of the x-y data in this subset
            Array xySlice = subset.readDataSlice(0, 0, -1, -1);
            logger.debug("Shape of xySlice = {}", Arrays.toString(xySlice.getShape()));
            long readData = System.currentTimeMillis();
            logger.debug("Read data using bounding box algorithm in {} milliseconds", (readData - start));

            // Now create the picture from the data array
            // Get a VariableDS for unpacking and checking for missing data
            VariableDS var = grid.getVariable();
            Index index = xySlice.getIndex(); // 2D index in y,x order
            for (int j : pixelMap.getJIndices())
            {
                for (int i : pixelMap.getIIndices(j))
                {
                    try
                    {
                        float val = xySlice.getFloat(index.set(j - pixelMap.getMinJIndex(),
                            i - pixelMap.getMinIIndex()));
                        // We unpack and check for missing values just for
                        // the points we need to display.
                        val = (float)var.convertScaleOffsetMissing(val);
                        for (int pixelIndex : pixelMap.getPixelIndices(i, j))
                        {
                            picData[pixelIndex] = val;
                        }
                    }
                    catch(ArrayIndexOutOfBoundsException aioobe)
                    {
                        logger.error("Array index ({},{}) out of bounds",
                            j - pixelMap.getMinJIndex(), i - pixelMap.getMinIIndex());
                        throw aioobe;
                    }
                }
            }
        }
    },

    /**
     * Reads each data point individually.  Generally very inefficient and
     * recommended only for debugging and testing purposes.
     */
    PIXEL_BY_PIXEL {
        @Override
        public void populatePixelArray(float[] picData, Range tRange, Range zRange,
            PixelMap pixelMap, GridDatatype grid) throws Exception
        {
            logger.debug("Reading data using a pixel-by-pixel algorithm");
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
    };

    /**
     * Reads data from the given GridDatatype and populates the given pixel array.
     * This uses a scanline-based algorithm: subclasses can override this to
     * use alternative strategies, e.g. point-by-point or bounding box.
     * @see PixelMap
     */
    public abstract void populatePixelArray(float[] picData, Range tRange, Range zRange,
        PixelMap pixelMap, GridDatatype grid) throws Exception;

    private static final Logger logger = LoggerFactory.getLogger(DataReadingStrategy.class);
}
