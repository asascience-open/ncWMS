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
package uk.ac.rdg.resc.edal.cdm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Index;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.cdm.PixelMap.PixelMapEntry;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.config.datareader.DataReader;

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
 *    pixel-by-pixel} strategy.</p>
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
 *       datasets as it minimizes the overhead associated with the individual
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
public enum DataReadingStrategy
{
    /**
     * Reads "scanlines" of data, leading to a smaller memory footprint than
     * the {@link #BOUNDING_BOX bounding-box} strategy, but a larger number of individual
     * data-reading operations.  Recommended for use when the overhead of
     * a data-reading operation is low, e.g. for local, uncompressed files.
     */
    SCANLINE {

        // Stores all the PixelMapEntries at a certain j index
        class Scanline
        {
            private final int jIndex;
            private final List<PixelMapEntry> pixelMapEntries = new ArrayList<PixelMapEntry>();
            
            public Scanline(PixelMapEntry pme)
            {
                this.jIndex = pme.getSourceGridJIndex();
                this.pixelMapEntries.add(pme);
            }
        }
        
        @Override
        protected void populatePixelArray(List<Float> picData,
            PixelMap pixelMap, VariableDS var, RangesList ranges)
            throws IOException
        {
            Iterator<PixelMapEntry> it = pixelMap.getEntries().iterator();
            if (!it.hasNext()) return;
            PixelMapEntry pme = it.next();
            Scanline scanline = new Scanline(pme);
            
            while (it.hasNext())
            {
                pme = it.next();
                int sourceJ = pme.getSourceGridJIndex();
                if (sourceJ == scanline.jIndex)
                {
                    scanline.pixelMapEntries.add(pme);
                }
                else
                {
                    // We have a new scanline.
                    // We read the data for the existing scanline first
                    this.readScanline(picData, var, ranges, scanline);
                    // Now we create a new scanline
                    scanline = new Scanline(pme);
                }
            }
            
            // We must read the last scanline
            this.readScanline(picData, var, ranges, scanline);
        }
        
        private void readScanline(List<Float> picData, VariableDS var, RangesList ranges, Scanline scanline)
                throws IOException
        {
            ranges.setYRange(scanline.jIndex, scanline.jIndex);
            int imin = scanline.pixelMapEntries.get(0).getSourceGridIIndex();
            int imax = scanline.pixelMapEntries.get(scanline.pixelMapEntries.size() - 1).getSourceGridIIndex();
            ranges.setXRange(imin, imax);

            //logger.debug(ranges.toString());

            // Read a chunk of data - values will not be unpacked or
            // checked for missing values yet
            DataChunk dataChunk = DataChunk.readDataChunk(var, ranges);

            // Get an index for the array and set it to zero
            Index index = dataChunk.getIndex();
            index.set(new int[index.getRank()]);

            // Now copy the scanline's data to the picture array
            for (PixelMapEntry pme : scanline.pixelMapEntries)
            {
                index.setDim(ranges.getXAxisIndex(), pme.getSourceGridIIndex() - imin);
                float val = dataChunk.readFloatValue(index);

                // Now we set the value of all the image pixels associated with
                // this data point.
                if (!Float.isNaN(val)) {
                    for (int p : pme.getTargetGridPoints()) {
                        picData.set(p, val);
                    }
                }
            }
        }
    },

    /**
     * Reads all data in a single operation, then subsets in memory.  Recommended
     * in situations in which individual data reads have a high overhead, e.g. when
     * reading from OPeNDAP datasets or compressed files.
     */
    BOUNDING_BOX {
        @Override
        protected void populatePixelArray(List<Float> picData,
            PixelMap pixelMap, VariableDS var, RangesList ranges)
            throws IOException
        {
            // Read the whole chunk of x-y data
            int imin = pixelMap.getMinIIndex();
            int imax = pixelMap.getMaxIIndex();
            int jmin = pixelMap.getMinJIndex();
            int jmax = pixelMap.getMaxJIndex();
            ranges.setXRange(imin, imax);
            ranges.setYRange(jmin, jmax);
//            logger.debug("Shape of grid: {}", Arrays.toString(var.getShape()));
//            logger.debug(ranges.toString());

            DataChunk dataChunk = DataChunk.readDataChunk(var, ranges);

            // Now extract the information we need from the data array
            Index index = dataChunk.getIndex();
            index.set(new int[index.getRank()]);

            for (PixelMapEntry pme : pixelMap.getEntries())
            {
                index.setDim(ranges.getYAxisIndex(), pme.getSourceGridJIndex() - jmin);
                index.setDim(ranges.getXAxisIndex(), pme.getSourceGridIIndex() - imin);
                float val = dataChunk.readFloatValue(index);
                if (!Float.isNaN(val))
                {
                    for (int targetGridPoint : pme.getTargetGridPoints())
                    {
                        picData.set(targetGridPoint, val);
                    }
                }
            }
        }
    },

    /**
     * Reads each data point individually.  Only efficient if the overhead of
     * reading a single point is not large.
     */
    PIXEL_BY_PIXEL {
        @Override
        protected void populatePixelArray(List<Float> picData,
            PixelMap pixelMap, VariableDS var, RangesList ranges)
            throws IOException
        {
            for (PixelMapEntry pme : pixelMap.getEntries())
            {
                ranges.setYRange(pme.getSourceGridJIndex(), pme.getSourceGridJIndex());
                ranges.setXRange(pme.getSourceGridIIndex(), pme.getSourceGridIIndex());
                DataChunk dataChunk = DataChunk.readDataChunk(var, ranges);
                // Get an index and set all elements to zero
                Index index = dataChunk.getIndex();
                index.set(new int[index.getRank()]);
                float val = dataChunk.readFloatValue(index);
                if (!Float.isNaN(val))
                {
                    for (int targetGridPoint : pme.getTargetGridPoints())
                    {
                        picData.set(targetGridPoint, val);
                    }
                }
            }
        }
    };

    /**
     * Reads data from the given GridDatatype and populates the given pixel array.
     * @param picData A List of the correct size, full of nulls.
     * @see PixelMap
     */
    public final List<Float> readData(int tIndex, int zIndex,
        HorizontalGrid sourceGrid, Domain<HorizontalPosition> targetDomain,
        GridDatatype grid) throws IOException
    {
        List<Float> picData = nullArrayList(targetDomain.getDomainObjects().size());
        PixelMap pixelMap;
        try
        {
            long start = System.nanoTime();
            pixelMap = new PixelMap(sourceGrid, targetDomain);
            long finish = System.nanoTime();
            logger.debug("Pixel map created in {} ms and has {} unique i-j pairs",
                    (finish - start) / 1e6, pixelMap.getNumUniqueIJPairs());
        }
        catch (TransformException te)
        {
            throw new RuntimeException(te);
        }
        if (pixelMap.isEmpty()) return picData;

        // Set the time and z ranges
        RangesList rangesList = new RangesList(grid);
        rangesList.setZRange(zIndex, zIndex);
        rangesList.setTRange(tIndex, tIndex);
        
        // Now read the actual data from the source GridDatatype
        long start = System.nanoTime();
        this.populatePixelArray(picData, pixelMap, grid.getVariable(), rangesList);
        long finish = System.nanoTime();
        logger.debug("Data read in {} ms using {} strategy",
                (finish - start) / 1e6, this.name());

        return picData;
    }

    /**
     * Returns an ArrayList with the given length, filled with null values
     */
    private static ArrayList<Float> nullArrayList(int n)
    {
        ArrayList<Float> list = new ArrayList<Float>(n);
        for (int i = 0; i < n; i++)
        {
            list.add((Float)null);
        }
        return list;
    }

    abstract void populatePixelArray(List<Float> picData,
            PixelMap pixelMap, VariableDS var, RangesList ranges)
        throws IOException;

    private static final Logger logger = LoggerFactory.getLogger(DataReadingStrategy.class);
}
