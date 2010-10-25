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

package uk.ac.rdg.resc.edal.cdm;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RectilinearGrid;
import uk.ac.rdg.resc.edal.coverage.grid.ReferenceableAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
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
final class PixelMap
{
    private static final Logger logger = LoggerFactory.getLogger(PixelMap.class);

    /**
     * First entry in each int[] is the source grid index: subsequent entries
     * are the corresponding target grid indices.  Sorted according to
     * {@link #PIXEL_MAP_ENTRY_COMPARATOR}.
     */
    private final ArrayList<int[]> pixelMapEntries = CollectionUtils.newArrayList();

    /** Sorts pixel map entries in ascending order of their index in the source grid */
    private static final Comparator<int[]> PIXEL_MAP_ENTRY_COMPARATOR =
            new Comparator<int[]>()
    {
        @Override
        public int compare(int[] o1, int[] o2)
        {
            // Compare the source grid indices only
            return o1[0] - o2[0];
        }
    };

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
         * source grid point */
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
     * @param sorted if this is true, the {@link #getJIndices()} and
     * {@link #getIIndices(int)} will return sets of integers in ascending order.
     * Creating a sorted pixel map is generally slower, but may lead to improved
     * i/o performance (can make better use of underlying buffers, and there is
     * less seeking).
     */
    public PixelMap(HorizontalGrid sourceGrid, Domain<HorizontalPosition> targetDomain)
            throws TransformException
    {
        logger.debug("Creating PixelMap: Source CRS: {}, Target CRS: {}",
                sourceGrid.getCoordinateReferenceSystem().getName(),
                targetDomain.getCoordinateReferenceSystem().getName());
        logger.debug("SourceGrid class: {}, targetDomain class: {}",
                sourceGrid.getClass(), targetDomain.getClass());

        this.sourceGridISize = sourceGrid.getGridExtent().getSpan(0);

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
            this.initFromPointList(sourceGrid, targetDomain);
        }

        this.pixelMapEntries.trimToSize();

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
     * @throws TransformException if the necessary transformations could not be performed
     */
    private void initFromGrid(RectilinearGrid sourceGrid, RectilinearGrid targetGrid)
            throws TransformException
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

        // Find this entry in the pixel map
        // Create a dummy entry; the search function will only search on the source grid index
        int[] pixelMapEntry = new int[2];
        pixelMapEntry[0] = sourceGridIndex;
        int entryIndex = Collections.binarySearch(pixelMapEntries, pixelMapEntry, PIXEL_MAP_ENTRY_COMPARATOR);

        if (entryIndex >= 0)
        {
            // We already have an entry for this source grid point
            // We replace the dummy PME with the one from the list
            pixelMapEntry = this.pixelMapEntries.get(entryIndex);
            // Grow the array by one to make room for the new target index
            int[] newArray = new int[pixelMapEntry.length + 1];
            System.arraycopy(pixelMapEntry, 0, newArray, 0, pixelMapEntry.length);
            newArray[newArray.length - 1] = targetGridIndex;
            // Replace the entry with the new one
            this.pixelMapEntries.set(entryIndex, newArray);
        }
        else
        {
            // This source grid point doesn't exist in the list
            pixelMapEntry[1] = targetGridIndex;
            // Insert in the list in the correct position
            int insertionPoint = -entryIndex - 1;
            this.pixelMapEntries.add(insertionPoint, pixelMapEntry);
        }
    }

    /**
     * Returns true if this PixelMap does not contain any data: this will happen
     * if there is no intersection between the requested data and the data on disk.
     * @return true if this PixelMap does not contain any data: this will happen
     * if there is no intersection between the requested data and the data on disk
     */
    public boolean isEmpty()
    {
        return this.pixelMapEntries.size() == 0;
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
     * Gets the number of unique i-j pairs in this pixel map. When combined
     * with the size of the resulting image we can quantify the under- or
     * oversampling.  This is the number of data points that will be extracted
     * by the {@link DataReadingStrategy#PIXEL_BY_PIXEL PIXEL_BY_PIXEL} data
     * reading strategy.
     * @return the number of unique i-j pairs in this pixel map.
     */
    public int getNumUniqueIJPairs()
    {
        return this.pixelMapEntries.size();
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
     * Returns an unmodifiable list of all the {@link PixelMapEntry}s in this PixelMap.
     */
    public List<PixelMapEntry> getEntries()
    {
        return new AbstractList<PixelMapEntry>()
        {
            @Override
            public PixelMapEntry get(int index) {
                final int[] pixelMapEntry = PixelMap.this.pixelMapEntries.get(index);
                return new PixelMapEntry() {

                    @Override
                    public int getSourceGridIIndex() {
                        int sourceGridIndex = pixelMapEntry[0];
                        return sourceGridIndex % PixelMap.this.sourceGridISize;
                    }

                    @Override
                    public int getSourceGridJIndex() {
                        int sourceGridIndex = pixelMapEntry[0];
                        return sourceGridIndex / PixelMap.this.sourceGridISize;
                    }

                    @Override
                    public List<Integer> getTargetGridPoints() {
                        return new AbstractList<Integer>() {
                            @Override public Integer get(int index) {
                                return pixelMapEntry[index + 1];
                            }

                            @Override public int size() {
                                return pixelMapEntry.length - 1;
                            }
                        };
                    }

                };
            }

            @Override
            public int size() {
                return PixelMap.this.pixelMapEntries.size();
            }
        };
    }

}
