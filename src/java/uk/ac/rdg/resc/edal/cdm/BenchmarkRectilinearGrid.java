package uk.ac.rdg.resc.edal.cdm;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.opengis.geometry.Envelope;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RectilinearGrid;
import uk.ac.rdg.resc.edal.coverage.grid.ReferenceableAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import uk.ac.rdg.resc.edal.geometry.impl.LonLatPositionImpl;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer.Style;

/**
 * Benchmarking for Rectilinear Grids.
 * 
 * @author andy, guy
 */
public class BenchmarkRectilinearGrid {

    private static void compareSourcePushDestinationPull(NetcdfDataset nc, GridDatatype gdt,
            RectilinearGrid grid, int size, int warmup_runs, int tries_per_parameter_set)
            throws Exception {

        List<Double> source_push_results = new ArrayList<Double>();
        List<Double> destination_pull_results = new ArrayList<Double>();

        for (int i = 0; i < tries_per_parameter_set + warmup_runs; i++) {
            ImageProducer ip = new ImageProducer.Builder().palette(ColorPalette.get(null))
                    .style(Style.BOXFILL).height(size).width(size).build();

            long start, finish;
            double image_build_time;
            BufferedImage im;
            
            // Build an image using "destination-pull"
            start = System.nanoTime();
            HorizontalGrid targetDomain = new RegularGridImpl(grid.getExtent(), size, size);
            List<Float> data = CdmUtils.readHorizontalPoints(nc, gdt.getName(), 0, 0, targetDomain);
            ip.addFrame(data, null);
            im = ip.getRenderedFrames().get(0);
            finish = System.nanoTime();

            ImageIO.write(im, "png", new File("destpull.png"));
            image_build_time = (finish - start) / 1.e9;
            if (i >= warmup_runs) {
                destination_pull_results.add(image_build_time);
            }
            
            // Build an image using "source-push"
//            start = System.nanoTime();
//            float[] dataArr = (float[]) gdt.readDataSlice(0, 0, -1, -1).get1DJavaArray(float.class);
//            List<Float> dataList = CollectionUtils.listFromFloatArray(dataArr, null);
//            im = vectorGraphic(dataList, grid, ip);
//            finish = System.nanoTime();
//
//            ImageIO.write(im, "png", new File("sourcepush.png"));
//            image_build_time = (finish - start) / 1.e9;
//            if (i >= warmup_runs) {
//                source_push_results.add(image_build_time);
//            }
        }

//        printStats("Source-push Times (size=" + size + ")", source_push_results);
        printStats("Destination-pull Times (size=" + size + ")", destination_pull_results);
    }

    private static BufferedImage vectorGraphic(List<Float> data, RectilinearGrid rectGrid,
            ImageProducer ip) throws IOException {
        int width = ip.getPicWidth();
        int height = ip.getPicHeight();
        IndexColorModel cm = ip.getColorModel();
        int[] rgbs = new int[cm.getMapSize()];
        cm.getRGBs(rgbs);
        BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm);
        Graphics2D g2d = im.createGraphics();

        BoundingBox bbox = rectGrid.getExtent();
        
        double lonDiff = bbox.getMaxX() - bbox.getMinX();
        double latDiff = bbox.getMaxY() - bbox.getMinY();

        // This ensures that the highest value of longitude (corresponding
        // with nLon - 1) is getLonMax()
        double lonStride = lonDiff / (width - 1);
        double latStride = latDiff / (height - 1);

        // Create the transform.  We scale by the inverse of the stride length
        AffineTransform transform = new AffineTransform();
        transform.scale(1.0 / lonStride, 1.0 / latStride);
        // Then we translate by the minimum coordinate values
        transform.translate(-bbox.getMinX(), -bbox.getMinY());

        g2d.setTransform(transform);

        int xSize = rectGrid.getAxis(0).getSize();
        int ySize = rectGrid.getAxis(1).getSize();
        // Populate the BufferedImages using the information from the curvilinear grid
        // Iterate over all the cells in the grid, painting the i and j indices of the
        // cell onto the BufferedImage
        for (Cell cell : getCells(rectGrid)) {
            // Need to flip the y-axis
            int j = ySize - 1 - cell.j;
            int index = cell.i + xSize * j;
            Float val = data.get(index);
            int colourIndex = ip.getColourIndex(val);

            // Get a Path representing the boundary of the cell
            Path2D path = cell.path;
            g2d.setPaint(new Color(cm.getRGB(colourIndex)));
            g2d.fill(path);

            // We paint a second copy of the cell, shifted by 360 degrees, to handle
            // the anti-meridian
            // TODO: this bit increases runtime by 30%
            double shiftLon = cell.centre.getLongitude() > 0.0
                    ? -360.0
                    : 360.0;
            path.transform(AffineTransform.getTranslateInstance(shiftLon, 0.0));
            g2d.fill(path);
        }

        return im;
    }
    
    public static class Cell{
        public int i;
        public int j;
        public LonLatPosition centre;
        public Path2D path;
        public Cell(int i, int j, LonLatPosition centre, Path2D path) {
            super();
            this.i = i;
            this.j = j;
            this.centre = centre;
            this.path = path;
        }
    }
    
    private static List<Cell> getCells(RectilinearGrid rGrid){
        final ReferenceableAxis xAxis = rGrid.getAxis(0);
        final ReferenceableAxis yAxis = rGrid.getAxis(1);
        final Envelope xExtent = xAxis.getExtent();
        final Envelope yExtent = yAxis.getExtent();
        final int xSize = xAxis.getSize();
        final int ySize = yAxis.getSize();
        
        return new AbstractList<BenchmarkRectilinearGrid.Cell>() {
            @Override
            public Cell get(int index) {
                int i = index % xSize;
                int j = index / xSize;
                
                double xmin, xmax;
                double xCentre = xAxis.getCoordinateValue(i);
                double ymin, ymax;
                double yCentre = yAxis.getCoordinateValue(j);
                
                if(i==0){
                    xmin = xExtent.getMinimum(0);
                } else {
                    xmin = 0.5*(xAxis.getCoordinateValue(i-1)+xCentre);
                }
                if(i==xSize-1){
                    xmax = xExtent.getMaximum(0);
                } else {
                    xmax = 0.5*(xAxis.getCoordinateValue(i+1)+xCentre);
                }
                if(j==0){
                    ymin = yExtent.getMinimum(0);
                } else {
                    ymin = 0.5*(yAxis.getCoordinateValue(j-1)+yCentre);
                }
                if(j==ySize-1){
                    ymax = yExtent.getMaximum(0);
                } else {
                    ymax = 0.5*(yAxis.getCoordinateValue(j+1)+yCentre);
                }
                
                LonLatPosition centre = new LonLatPositionImpl(xCentre, yCentre);
                Path2D path = new Path2D.Double();
                path.moveTo(xmin, ymin);
                path.lineTo(xmax, ymin);
                path.lineTo(xmax, ymax);
                path.lineTo(xmin, ymax);
                path.closePath();
                
                return new Cell(i, j, centre, path);
            }

            @Override
            public int size() {
                return xSize * ySize;
            }
        };
    }

    public static void printStats(String title, List<Double> results) {
//        System.out.println(title);
//        for (double result : results) {
//            System.out.println(result);
//        }
//        System.out.println("---");

        // / Compute the stats (not very efficient)
        double sum = 0.0;

        for (Double result : results) {
            sum += result;
        }
        double mean = sum / (double) results.size();
        double variance = 0.0;
        for (Double result : results) {
            variance += (result - mean) * (result - mean);
        }

        double standard_deviation = Math.sqrt((1.0 / ((double) results.size() - 1)) * variance);
        System.out.println(mean+",");
//        System.out.println(standard_deviation);
//        System.out.println("---");
    }

    public static void main(String[] args) throws Exception {
        int tries_per_parameter_set = 3;
        int warmup_runs = 2;

        {
            String data_filename = "/home/guy/test_datasets/small.nc";
            String data_variable = "T";
            NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
            GridDatatype gridDT = CdmUtils.getGridDatatype(nc, data_variable);
            GridCoordSystem gcs = gridDT.getCoordinateSystem();
            HorizontalGrid hGrid = CdmUtils.createHorizontalGrid(gcs);
            if(hGrid instanceof RectilinearGrid){
                RectilinearGrid rectilinearGrid = (RectilinearGrid) hGrid;
                for(int i=50; i<1050; i+=50){
                    compareSourcePushDestinationPull(nc, gridDT, rectilinearGrid, i, warmup_runs,
                            tries_per_parameter_set);
                }
            }
        }
        System.out.println();
        {
            String data_filename = "/home/guy/test_datasets/medium.nc";
            String data_variable = "pr";
            NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
            GridDatatype gridDT = CdmUtils.getGridDatatype(nc, data_variable);
            GridCoordSystem gcs = gridDT.getCoordinateSystem();
            HorizontalGrid hGrid = CdmUtils.createHorizontalGrid(gcs);
            if(hGrid instanceof RectilinearGrid){
                RectilinearGrid rectilinearGrid = (RectilinearGrid) hGrid;
                for(int i=50; i<1050; i+=50){
                    compareSourcePushDestinationPull(nc, gridDT, rectilinearGrid, i, warmup_runs,
                            tries_per_parameter_set);
                }
            }
        }
        System.out.println();
        {
            String data_filename = "/home/guy/test_datasets/large.nc";
            String data_variable = "TMP";
            NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
            GridDatatype gridDT = CdmUtils.getGridDatatype(nc, data_variable);
            GridCoordSystem gcs = gridDT.getCoordinateSystem();
            HorizontalGrid hGrid = CdmUtils.createHorizontalGrid(gcs);
            if(hGrid instanceof RectilinearGrid){
                RectilinearGrid rectilinearGrid = (RectilinearGrid) hGrid;
                for(int i=50; i<1050; i+=50){
                    compareSourcePushDestinationPull(nc, gridDT, rectilinearGrid, i, warmup_runs,
                            tries_per_parameter_set);
                }
            }
        }
        System.out.println();
        {
            String data_filename = "/home/guy/test_datasets/larger.nc";
            String data_variable = "Wind";
            NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
            GridDatatype gridDT = CdmUtils.getGridDatatype(nc, data_variable);
            GridCoordSystem gcs = gridDT.getCoordinateSystem();
            HorizontalGrid hGrid = CdmUtils.createHorizontalGrid(gcs);
            if(hGrid instanceof RectilinearGrid){
                RectilinearGrid rectilinearGrid = (RectilinearGrid) hGrid;
                for(int i=50; i<1050; i+=50){
                    compareSourcePushDestinationPull(nc, gridDT, rectilinearGrid, i, warmup_runs,
                            tries_per_parameter_set);
                }
            }
        }
        System.out.println();
        {
            String data_filename = "/home/guy/test_datasets/largest.nc";
            String data_variable = "analysed_sst";
            NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
            GridDatatype gridDT = CdmUtils.getGridDatatype(nc, data_variable);
            GridCoordSystem gcs = gridDT.getCoordinateSystem();
            HorizontalGrid hGrid = CdmUtils.createHorizontalGrid(gcs);
            if(hGrid instanceof RectilinearGrid){
                RectilinearGrid rectilinearGrid = (RectilinearGrid) hGrid;
                for(int i=50; i<1050; i+=50){
                    compareSourcePushDestinationPull(nc, gridDT, rectilinearGrid, i, warmup_runs,
                            tries_per_parameter_set);
                }
            }
        }
        
        
        
    }
}
