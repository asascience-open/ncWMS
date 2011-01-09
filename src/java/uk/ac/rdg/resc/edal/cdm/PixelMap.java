/*
 * Copyright (c) 2010 The University of Reading
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RectilinearGrid;
import uk.ac.rdg.resc.edal.coverage.grid.ReferenceableAxis;
import uk.ac.rdg.resc.edal.coverage.grid.RegularAxis;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularAxisImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.Utils;

/**
 *<p>Maps real-world points to i and j indices of corresponding
 * points within the source data.  This is a very important class in ncWMS.  A
 * PixelMap is constructed using the following general algorithm:</p>
 *
 * <pre>
 * For each point in the given {@link PointList}:
 *    1. Find the x-y coordinates of this point in the CRS of the PointList
 *    2. Transform these x-y coordinates into latitude and longitude
 *    3. Use the given {@link HorizontalCoordSys} to transform lat-lon into the
 *       index values (i and j) of the nearest cell in the source grid
 *    4. Add the mapping (point -> i,j) to the pixel map
 * </pre>
 *
 * <p>(A more efficient algorithm is used for the special case in which both the
 * requested CRS and the CRS of the data are lat-lon.)</p>
 *
 * <p>The resulting PixelMap is then used by {@link DataReadingStrategy}s to work out what
 * data to read from the source data files.  A variety of strategies are possible
 * for reading these data points, each of which may be optimal in a certain
 * situation.</p>
 *
 * @author Jon Blower
 * @todo Perhaps we can think of a more appropriate name for this class?
 * @todo equals() and hashCode(), particularly if we're going to cache instances
 * of this class.
 * @todo It may be possible to create an alternative version of this class for
 * cases where both source and target grids are lat-lon.  In this case, the
 * pixelmap should also be a RectilinearGrid, meaning that there would be no need
 * to store mapping information in HashMaps etc.  (Profiling shows that getting
 * and putting data from/to the HashMaps is a bottleneck.)
 * @see DataReadingStrategy
 */
public final class PixelMap implements Iterable<PixelMap.PixelMapEntry>
{
    private static final Logger logger = LoggerFactory.getLogger(PixelMap.class);

    /**
     * <p>Maps points in the source grid to points in the target grid.  Each entry
     * in this array represents a mapping from one source grid point (high four
     * bytes) to one target grid point (low four bytes).</p>
     * <p>We use an array of longs instead of an array of objects (e.g. Pair<Integer>
     * or similar) because each object carries a memory overhead.</p>
     */
    private long[] pixelMapEntries;
    /** The number of entries in this pixel map */
    private int numEntries = 0;
    /**
     * The array of pixel map entries will grow in size as required by this
     * number of longs.  This means that the array of pixel map entries will
     * never be more than {@code chunkSize - 1} greater than it has to be.
     */
    private final int chunkSize;

    /**
     * Maps a point in the source grid to corresponding points in the target grid.
     */
    public static interface PixelMapEntry
    {
        /** Gets the i index of this point in the source grid */
        public int getSourceGridIIndex();
        /** Gets the j index of this point in the source grid */
        public int getSourceGridJIndex();
        /** Gets the array of all target grid points that correspond with this
         * source grid point.  Each grid point is expressed as a single integer
         * {@code j * width + i}.*/
        public List<Integer> getTargetGridPoints();
    }

    private final int sourceGridISize;

    // These define the bounding box (in terms of axis indices) of the data
    // to extract from the source files
    private int minIIndex = Integer.MAX_VALUE;
    private int minJIndex = Integer.MAX_VALUE;
    private int maxIIndex = -1;
    private int maxJIndex = -1;

    /**
     * Creates a PixelMap that maps from points within the grid of source
     * data ({@code sourceGrid}) to points within the required target domain.
     */
    public PixelMap(HorizontalGrid sourceGrid, Domain<HorizontalPosition> targetDomain)
    {
        logger.debug("Creating PixelMap: Source CRS: {}, Target CRS: {}",
                sourceGrid.getCoordinateReferenceSystem().getName(),
                targetDomain.getCoordinateReferenceSystem().getName());
        logger.debug("SourceGrid class: {}, targetDomain class: {}",
                sourceGrid.getClass(), targetDomain.getClass());

        this.sourceGridISize = sourceGrid.getGridExtent().getSpan(0);

        // Create an estimate of a suitable chunk size.  We don't want this to
        // be too small because we would have to do many array copy operations
        // to grow the array in put().  Conversely we don't want it to be too
        // large and lead to wasted space.
        this.chunkSize = targetDomain.size() < 1000
            ? targetDomain.size()
            : targetDomain.size() / 10;
        this.pixelMapEntries = new long[this.chunkSize];

        long start = System.currentTimeMillis();
        if (sourceGrid instanceof RectilinearGrid && targetDomain instanceof RectilinearGrid &&
            Utils.isWgs84LonLat(sourceGrid.getCoordinateReferenceSystem()) &&
            Utils.isWgs84LonLat(targetDomain.getCoordinateReferenceSystem()))
        {
            // We can gain efficiency if the source and target grids are both
            // rectilinear lat-lon grids (i.e. they have separable latitude and
            // longitude axes).

            // TODO: could also be efficient for any matching CRS?  But how test
            // for CRS equality, when one CRS will have been created from an EPSG code
            // and the other will have been inferred from the source data file (e.g. NetCDF)
            this.initFromGrid((RectilinearGrid)sourceGrid, (RectilinearGrid)targetDomain);
        }
        else
        {
            try
            {
                this.initFromPointList(sourceGrid, targetDomain);
            }
            catch(TransformException te)
            {
                // Shouldn't happen, and there's nothing we can do about it if it
                // does (except perhaps to log the exception).
                throw new RuntimeException(te);
            }
        }

        // Sort the array. Because the source grid indices are the high four bytes
        // in the array, the sorted array will be in order of increasing source
        // grid index, then increasing target grid index.
        Arrays.sort(this.pixelMapEntries, 0, this.numEntries);

        logger.debug("Built pixel map in {} ms", System.currentTimeMillis() - start);
    }

    private void initFromPointList(HorizontalGrid sourceGrid, Domain<HorizontalPosition> targetDomain)
            throws TransformException
    {
        logger.debug("Using generic method based on iterating over the domain");
        int pixelIndex = 0;
        // Find the nearest grid coordinates to all the points in the domain
        for (GridCoordinates gridCoords : sourceGrid.findNearestGridPoints(targetDomain))
        {
            if (gridCoords != null)
            {
                this.put(
                    gridCoords.getCoordinateValue(0),
                    gridCoords.getCoordinateValue(1),
                    pixelIndex
                );
            }
            pixelIndex++;
        }
    }

    /**
     * Generates a PixelMap for reading data from the given source grid and
     * projecting onto the target grid.
     * @param sourceGrid The source grid in WGS84 lat-lon coordinates
     * @param targetGrid The target grid in WGS84 lat-lon coordinates
     */
    private void initFromGrid(RectilinearGrid sourceGrid, RectilinearGrid targetGrid)
    {
        logger.debug("Using optimized method for lat-lon coordinates with 1D axes");

        ReferenceableAxis sourceGridXAxis = sourceGrid.getXAxis();
        ReferenceableAxis sourceGridYAxis = sourceGrid.getYAxis();

        ReferenceableAxis targetGridXAxis = targetGrid.getXAxis();
        ReferenceableAxis targetGridYAxis = targetGrid.getYAxis();

        // Calculate the indices along the x axis
        int[] xIndices = new int[targetGridXAxis.getSize()];
        List<Double> targetGridLons = targetGridXAxis.getCoordinateValues();
        for (int i = 0; i < targetGridLons.size(); i++)
        {
            double lon = targetGridLons.get(i);
            xIndices[i] = sourceGridXAxis.getNearestCoordinateIndex(lon);
        }

        // Now cycle through the latitude values in the target grid
        int pixelIndex = 0;
        for (double lat : targetGridYAxis.getCoordinateValues())
        {
            if (lat >= -90.0 && lat <= 90.0)
            {
                int yIndex = sourceGridYAxis.getNearestCoordinateIndex(lat);
                for (int xIndex : xIndices)
                {
                    this.put(xIndex, yIndex, pixelIndex);
                    pixelIndex++;
                }
            }
            else
            {
                // We still need to increment the pixel index value
                pixelIndex += xIndices.length;
            }
        }
    }

    /**
     * Adds a new pixel index to this map.  Does nothing if either i or j is
     * negative.
     * @param i The i index of the point in the source data
     * @param j The j index of the point in the source data
     * @param targetGridIndex The index of the corresponding point in the target domain
     */
    private void put(int i, int j, int targetGridIndex)
    {
        // If either of the indices are negative there is no data for this
        // target grid point
        if (i < 0 || j < 0) return;

        // Modify the bounding box if necessary
        if (i < this.minIIndex) this.minIIndex = i;
        if (i > this.maxIIndex) this.maxIIndex = i;
        if (j < this.minJIndex) this.minJIndex = j;
        if (j > this.maxJIndex) this.maxJIndex = j;

        // Calculate a single integer representing this grid point in the source grid
        // TODO: watch out for overflows (would only happen with a very large grid!)
        int sourceGridIndex = j * this.sourceGridISize + i;

        // See if we need to grow the array of pixel map entries
        if (this.numEntries >= this.pixelMapEntries.length)
        {
            long[] newArray = new long[this.pixelMapEntries.length + this.chunkSize];
            System.arraycopy(this.pixelMapEntries, 0, newArray, 0, this.pixelMapEntries.length);
            this.pixelMapEntries = newArray;
        }

        // Make an entry in the pixel map.  The source grid index becomes the
        // high four bytes of the entry (long value) and the targetGridIndex
        // becomes the low four bytes.
        this.pixelMapEntries[this.numEntries] = (long)sourceGridIndex << 32 | targetGridIndex;
        this.numEntries++;
    }

    /**
     * Returns true if this PixelMap does not contain any data: this will happen
     * if there is no intersection between the requested data and the data on disk.
     * @return true if this PixelMap does not contain any data: this will happen
     * if there is no intersection between the requested data and the data on disk
     */
    public boolean isEmpty()
    {
        return this.numEntries == 0;
    }

    /**
     * Gets the minimum i index in the whole pixel map
     * @return the minimum i index in the whole pixel map
     */
    public int getMinIIndex()
    {
        return minIIndex;
    }

    /**
     * Gets the minimum j index in the whole pixel map
     * @return the minimum j index in the whole pixel map
     */
    public int getMinJIndex()
    {
        return minJIndex;
    }

    /**
     * Gets the maximum i index in the whole pixel map
     * @return the maximum i index in the whole pixel map
     */
    public int getMaxIIndex()
    {
        return maxIIndex;
    }

    /**
     * Gets the maximum j index in the whole pixel map
     * @return the maximum j index in the whole pixel map
     */
    public int getMaxJIndex()
    {
        return maxJIndex;
    }

    /**
     * <p>Gets the number of unique i-j pairs in this pixel map. When combined
     * with the size of the resulting image we can quantify the under- or
     * oversampling.  This is the number of data points that will be extracted
     * by the {@link DataReadingStrategy#PIXEL_BY_PIXEL PIXEL_BY_PIXEL} data
     * reading strategy.</p>
     * <p>This implementation counts the number of unique pairs by cycling through
     * the {@link #iterator()} and so is not a cheap operation.  Use sparingly,
     * e.g. for debugging.</p>
     * @return the number of unique i-j pairs in this pixel map.
     */
    public int getNumUniqueIJPairs()
    {
        int count = 0;
        for (PixelMapEntry pme : this) count++;
        return count;
    }

    /**
     * Gets the sum of the lengths of each row of data points,
     * {@literal i.e.} sum(imax - imin + 1).  This is the number of data points that will
     * be extracted by the {@link DataReadingStrategy#SCANLINE SCANLINE} data
     * reading strategy.
     * @return the sum of the lengths of each row of data points
     * @todo could reinstate this by moving the Scanline-generating code from
     * DataReadingStrategy to this class and counting the lengths of the scanlines
     */
    /*public int getSumRowLengths()
    {
        int sumRowLengths = 0;
        for (Row row : this.pixelMap.values())
        {
            sumRowLengths += (row.getMaxIIndex() - row.getMinIIndex() + 1);
        }
        return sumRowLengths;
    }*/

    /**
     * Gets the size of the i-j bounding box that encompasses all data.  This is
     * the number of data points that will be extracted using the
     * {@link DataReadingStrategy#BOUNDING_BOX BOUNDING_BOX} data reading strategy.
     * @return the size of the i-j bounding box that encompasses all data.
     */
    public int getBoundingBoxSize()
    {
        return (this.maxIIndex - this.minIIndex + 1) *
               (this.maxJIndex - this.minJIndex + 1);
    }

    /**
     * Returns an unmodifiable iterator over all the {@link PixelMapEntry}s in this PixelMap.
     */
    @Override
    public Iterator<PixelMapEntry> iterator()
    {
        return new Iterator<PixelMapEntry>()
        {
            /** Index in the array of entries */
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < PixelMap.this.numEntries;
            }

            @Override
            public PixelMapEntry next() {
                long packed = PixelMap.this.pixelMapEntries[this.index];
                // Unpack the source grid and target grid indices
                int[] unpacked = unpack(packed);
                this.index++;
                final int sourceGridIndex = unpacked[0];
                final List<Integer> targetGridIndices = new ArrayList<Integer>();
                targetGridIndices.add(unpacked[1]);

                // Now find all the other entries that use the same source grid
                // index
                boolean done = false;
                while (!done && this.hasNext()) {
                    packed = PixelMap.this.pixelMapEntries[this.index];
                    unpacked = unpack(packed);
                    if (unpacked[0] == sourceGridIndex) {
                        targetGridIndices.add(unpacked[1]);
                        this.index++;
                    } else {
                        done = true;
                    }
                }

                return new PixelMapEntry() {

                    @Override
                    public int getSourceGridIIndex() {
                        return sourceGridIndex % PixelMap.this.sourceGridISize;
                    }

                    @Override
                    public int getSourceGridJIndex() {
                        return sourceGridIndex / PixelMap.this.sourceGridISize;
                    }

                    @Override
                    public List<Integer> getTargetGridPoints() {
                        return targetGridIndices;
                    }

                };
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

        };
    }

    /** Unpacks a long integer into two 4-byte integers (first value in array
     * are the high 4 bytes of the long). */
    private static int[] unpack(long packed)
    {
        return new int[] {
            (int)(packed >> 32),
            (int)(packed & 0xffffffff)
        };
    }

    public static void main(String[] args) throws Exception
    {
        RegularAxis lonAxis = new RegularAxisImpl("lon", 64.01358, 0.045, 3474, true);
        RegularAxis latAxis = new RegularAxisImpl("lat", 80.12541, -0.045, 3564, false);
        RegularGrid sourceDomain = new RegularGridImpl(lonAxis, latAxis, DefaultGeographicCRS.WGS84);
        RegularGrid targetDomain = new RegularGridImpl(DefaultGeographicBoundingBox.WORLD, 2048, 2048);

        Runtime rt = Runtime.getRuntime();

        long startMemUsed = memUsed(rt);
        long start = System.nanoTime();
        PixelMap pixelMap = new PixelMap(sourceDomain, targetDomain);
        long finish = System.nanoTime();
        long memUsed = memUsed(rt) - startMemUsed;

        System.out.println("Built PixelMap in " + ((finish - start) / 1.e6));
        //System.out.println("Number of entries " + pixelMap.numEntries + " (" + pixelMap.pixelMapEntries.length + ")");
        //System.out.println("Num unique pairs = " + pixelMap.getNumUniqueIJPairs());
        //System.out.println("Total insert time " + (pixelMap.insertTime / 1.e6));
        //System.out.println("Stuff shifted " + pixelMap.stuffShifted);
        System.out.println("mem used " + memUsed);
        // With compression:    222 ms, 370k   840x400
        // Without compression: 166ms, 2.7M
        // With compression:    222 ms, 370k   512x512
        // Without compression: 140ms, 2.1M
    }

    private static final long memUsed(Runtime rt)
    {
        System.gc();
        return rt.totalMemory() - rt.freeMemory();
    }

}
