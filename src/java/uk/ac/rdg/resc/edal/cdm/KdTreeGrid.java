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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.List;
import uk.ac.rdg.resc.edal.coverage.grid.GridCoordinates;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import java.util.Map;
import javax.imageio.ImageIO;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.cdm.CurvilinearGrid.Cell;
import uk.ac.rdg.resc.edal.cdm.kdtree.KDTree;
import uk.ac.rdg.resc.edal.cdm.kdtree.Point;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.GridCoordinatesImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;
import uk.ac.rdg.resc.edal.util.Utils;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer.Style;

/**
 * A HorizontalGrid that uses an KdTree to look up the nearest neighbour of a point.
 */
final class KdTreeGrid extends AbstractCurvilinearGrid
{
    private static final Logger logger = LoggerFactory.getLogger(KdTreeGrid.class);

    /**
     * In-memory cache of LookUpTableGrid objects to save expensive re-generation of same object
     * @todo The CurvilinearGrid objects can be very big.  Really we only need to key
     * on the arrays of lon and lat: all other quantities can be calculated from
     * these.  This means that we could make other large objects available for
     * garbage collection.
     */
    private static final Map<CurvilinearGrid, KdTreeGrid> CACHE =
            CollectionUtils.newHashMap();

    private final KDTree kdTree;
    private double max_distance;

    /**
     * The passed-in coordSys must have 2D horizontal coordinate axes.
     */
    public static KdTreeGrid generate(GridCoordSystem coordSys)
    {
        CurvilinearGrid curvGrid = new CurvilinearGrid(coordSys);

        synchronized(CACHE)
        {
        KdTreeGrid kdTreeGrid = CACHE.get(curvGrid);
            if (kdTreeGrid == null)
            {
        logger.debug("Need to generate new kdtree");
        // Create the KdTree for this coordinate system
        long start = System.nanoTime();
        KDTree kdTree = new KDTree(curvGrid);
        kdTree.buildTree();
        long finish = System.nanoTime();
        logger.debug("Generated new kdtree in {} seconds", (finish - start) / 1.e9);
        // Create the Grid
        kdTreeGrid = new KdTreeGrid(curvGrid, kdTree);
        // Now put this in the cache
        CACHE.put(curvGrid, kdTreeGrid);
            }
            else
            {
        logger.debug("kdree found in cache");
        }
        return kdTreeGrid;
        }
    }

    /** Private constructor to prevent direct instantiation */
    private KdTreeGrid(CurvilinearGrid curvGrid, KDTree kdTree)
    {
        // All points will be returned in WGS84 lon-lat
        super(curvGrid);
        this.kdTree = kdTree;
        this.max_distance = Math.sqrt(curvGrid.getMeanCellArea());
    }

    void setQueryingParameters(double nominalMinimumResolution, double expansionFactor, double maxDistance) {
        this.kdTree.setQueryParameters(expansionFactor, nominalMinimumResolution);
        this.max_distance = maxDistance;
    }

    /**
     * @return the nearest grid point to the given lat-lon point, or null if the
     * lat-lon point is not contained within this layer's domain. The grid point
     * is given as a two-dimensional integer array: [i,j].
     */
    @Override
    public GridCoordinates findNearestGridPoint(HorizontalPosition pos)
    {
        LonLatPosition lonLatPos = Utils.transformToWgs84LonLat(pos);
        double lon = lonLatPos.getLongitude();
        double lat = lonLatPos.getLatitude();

        // Find a set of candidate nearest-neighbours from the kd-tree
        List<Point> nns = this.kdTree.approxNearestNeighbour(lat, lon, this.max_distance);

        // Now find the real nearest neighbour
        double shortestDistanceSq = Double.MAX_VALUE;
        CurvilinearGrid.Cell closestCell = null;
        for (Point nn : nns) {
            int i = nn.index % this.curvGrid.getNi();
            int j = nn.index / this.curvGrid.getNi();
            CurvilinearGrid.Cell cell = this.curvGrid.getCell(i, j);
            double distanceSq = cell.findDistanceSq(lonLatPos);
            if (distanceSq < shortestDistanceSq) {
                shortestDistanceSq = distanceSq;
                closestCell = cell;
            }
        }

        if (closestCell == null) return null;

        // Now find whether the NN or one of its neighbours actually contains the
        // point
        if (closestCell.contains(lonLatPos)) {
            return new GridCoordinatesImpl(closestCell.getI(), closestCell.getJ());
        }
        // Do a neighbour search, as sometimes the nearest neighbour does
        // not fall precisely into the cell
        for (CurvilinearGrid.Cell neighbour : closestCell.getNeighbours()) {
            if (neighbour.contains(lonLatPos)) {
                return new GridCoordinatesImpl(neighbour.getI(), neighbour.getJ());
            }
        }

        // Sometimes none of the cells pass the contains() test, possibly because
        // the target position is on the edge of a cell.
        return new GridCoordinatesImpl(closestCell.getI(), closestCell.getJ());
    }

    public static void main(String[] args) throws Exception
    {
        NetcdfDataset nc = NetcdfDataset.openDataset(//"C:\\Godiva2_data\\UCA25D\\UCA25D.20101118.04.nc");
               "C:\\Godiva2_data\\ORCA025-R07-MEAN\\Exp4-Annual\\ORCA025-R07_y2004_ANNUAL_gridT2.nc");
        GridDatatype grid = CdmUtils.getGridDatatype(nc, "sossheig_sqd"); //*/"sea_level");
        int size = 256;
  
        // Read the data from the source
        long start = System.nanoTime();
        float[] data = (float[])grid.readDataSlice(0, 0, -1, -1).get1DJavaArray(float.class);
        List<Float> dataList = CollectionUtils.listFromFloatArray(data, null);
        Range<Float> dataRange = Ranges.findMinMax(dataList);
        System.out.println("Read whole data in " + getTimingMs(start) + " ms");

        ImageProducer ip = new ImageProducer.Builder()
            .palette(ColorPalette.get(null))
            .style(Style.BOXFILL)
            .height(size)
            .width(size)
            .colourScaleRange(dataRange)
            .build();

        start = System.nanoTime();
        CurvilinearGrid curvGrid = new CurvilinearGrid(grid.getCoordinateSystem());
        BufferedImage vg = vectorGraphic(dataList, curvGrid, ip);
        System.out.println("Created vector graphic in " + getTimingMs(start) + " ms");
        ImageIO.write(vg, "png", new File("c:\\vector.png"));

        start = System.nanoTime();
        AbstractCurvilinearGrid kdTreeGrid = KdTreeGrid.generate(grid.getCoordinateSystem());
        System.out.println("Generated lutGrid in " + getTimingMs(start) + " ms");
        HorizontalGrid targetDomain = new RegularGridImpl(kdTreeGrid.getExtent(), size, size);

        start = System.nanoTime();
        PixelMap pixelMap = new PixelMap(kdTreeGrid, targetDomain);
        System.out.println("Created PixelMap in " + getTimingMs(start) + " ms");

        start = System.nanoTime();
        List<Float> newData = CdmUtils.readHorizontalPoints(nc, grid, kdTreeGrid, 0, 0, targetDomain);
        System.out.println("Read data in " + getTimingMs(start) + " ms (includes creating PixelMap again)");
        nc.close();

        start = System.nanoTime();
        ip.addFrame(newData, null);
        List<BufferedImage> ims = ip.getRenderedFrames();
        ImageIO.write(ims.get(0), "png", new File("C:\\lookup.png"));
        System.out.println("Created and wrote image in " + getTimingMs(start) + " ms");

    }

    private static BufferedImage vectorGraphic(List<Float> data, CurvilinearGrid curvGrid, ImageProducer ip)
            throws IOException
    {
        int width = ip.getPicWidth();
        int height = ip.getPicHeight();
        IndexColorModel cm = ip.getColorModel();
        int[] rgbs = new int[cm.getMapSize()];
        cm.getRGBs(rgbs);
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
        Graphics2D g2d = im.createGraphics();

        GeographicBoundingBox bbox = curvGrid.getBoundingBox();

        double lonDiff = bbox.getEastBoundLongitude() - bbox.getWestBoundLongitude();
        double latDiff = bbox.getNorthBoundLatitude() - bbox.getSouthBoundLatitude();

        // This ensures that the highest value of longitude (corresponding
        // with nLon - 1) is getLonMax()
        double lonStride = lonDiff / (width - 1);
        double latStride = latDiff / (height - 1);

        // Create the transform.  We scale by the inverse of the stride length
        AffineTransform transform = new AffineTransform();
        transform.scale(1.0/lonStride, 1.0/latStride);
        // Then we translate by the minimum coordinate values
        transform.translate(-bbox.getWestBoundLongitude(), -bbox.getSouthBoundLatitude());

        g2d.setTransform(transform);

        // Populate the BufferedImages using the information from the curvilinear grid
        // Iterate over all the cells in the grid, painting the i and j indices of the
        // cell onto the BufferedImage
        for (Cell cell : curvGrid.getCells())
        {
            int index = cell.getI() + curvGrid.getNi() * cell.getJ();
            Float val = data.get(index);
            int colourIndex = ip.getColourIndex(val);

            // Get a Path representing the boundary of the cell
            Path2D path = cell.getBoundaryPath();
            //g2d.setPaint(new Color(rgbs[colourIndex]));
            g2d.setPaint(new Color(cm.getRGB(colourIndex)));
            g2d.fill(path);

            // We paint a second copy of the cell, shifted by 360 degrees, to handle
            // the anti-meridian
            // TODO: this bit increases runtime by 30%
            double shiftLon = cell.getCentre().getLongitude() > 0.0
                ? -360.0
                : 360.0;
            path.transform(AffineTransform.getTranslateInstance(shiftLon, 0.0));
            g2d.fill(path);
    }

        return im;
    }

    private static double getTimingMs(long startNs)
    {
        long finishNs = System.nanoTime();
        return (finishNs - startNs) / 1.e6;
    }
}
