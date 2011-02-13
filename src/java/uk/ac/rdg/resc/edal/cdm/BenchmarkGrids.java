package uk.ac.rdg.resc.edal.cdm;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import javax.imageio.ImageIO;
import org.opengis.metadata.extent.GeographicBoundingBox;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import uk.ac.rdg.resc.edal.cdm.CurvilinearGrid.Cell;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.CollectionUtils;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer.Style;

/**
 *
 * @author andy
 */
public class BenchmarkGrids {

    private static void run_imagemaker_comparison(NetcdfDataset nc, GridDatatype gdt, AbstractCurvilinearGrid grid, int size, int warmup_runs, int tries_per_parameter_set) throws Exception {

        List<Double> source_push_results = new ArrayList<Double>();
        List<Double> destination_pull_results = new ArrayList<Double>();

        for (int i = 0; i < tries_per_parameter_set + warmup_runs; i++) {

            ImageProducer ip = new ImageProducer.Builder()
                    .palette(ColorPalette.get(null))
                    .style(Style.BOXFILL)
                    .height(size)
                    .width(size)
                    .build();

            // Build an image using "destination-pull"
            long start = System.nanoTime();
            HorizontalGrid targetDomain = new RegularGridImpl(grid.getExtent(), size, size);
            List<Float> data = CdmUtils.readHorizontalPoints(nc, gdt.getName(), 0, 0, targetDomain);
            ip.addFrame(data, null);
            BufferedImage im = ip.getRenderedFrames().get(0);
            long finish = System.nanoTime();

            ImageIO.write(im, "png", new File("destpull.png"));
            double image_build_time = (finish - start) / 1.e9;
            if (i >= warmup_runs) {
                destination_pull_results.add(image_build_time);
            }

            // Build an image using "source-push"
            start = System.nanoTime();
            float[] dataArr = (float[])gdt.readDataSlice(0, 0, -1, -1).get1DJavaArray(float.class);
            List<Float> dataList = CollectionUtils.listFromFloatArray(dataArr, null);
            im = vectorGraphic(dataList, grid.curvGrid, ip);
            finish = System.nanoTime();

            ImageIO.write(im, "png", new File("sourcepush.png"));
            image_build_time = (finish - start) / 1.e9;
            if (i >= warmup_runs) {
                source_push_results.add(image_build_time);
            }
        }

        printStats("Source-push Times (size=" + size + ")", source_push_results);
        printStats("Destination-pull Times (size=" + size + ")", destination_pull_results);
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
        transform.scale(1.0 / lonStride, 1.0 / latStride);
        // Then we translate by the minimum coordinate values
        transform.translate(-bbox.getWestBoundLongitude(), -bbox.getSouthBoundLatitude());

        g2d.setTransform(transform);

        // Populate the BufferedImages using the information from the curvilinear grid
        // Iterate over all the cells in the grid, painting the i and j indices of the
        // cell onto the BufferedImage
        for (Cell cell : curvGrid.getCells()) {
            // Need to flip the y-axis
            int j = curvGrid.getNj() - 1 - cell.getJ();
            int index = cell.getI() + curvGrid.getNi() * j;
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

    private static void run_querytime_benchmarking(AbstractCurvilinearGrid grid, int size, int warmup_runs, int tries_per_parameter_set) throws Exception {

        List<Double> gridding_times = new ArrayList<Double>();
        HorizontalGrid targetDomain = new RegularGridImpl(grid.getExtent(), size, size);

        for (int i = 0; i < tries_per_parameter_set + warmup_runs; i++) {
            long total_gridding_time = 0;
            int total_gridding_queries = 0;
            for (HorizontalPosition pos : targetDomain.getDomainObjects()) {
                long query_start = System.nanoTime();
                grid.findNearestGridPoint(pos);
                long query_end = System.nanoTime();
                total_gridding_queries++;
                total_gridding_time += (query_end - query_start);
            }

            double produce_data_time = total_gridding_time / (1.e9 * total_gridding_queries);

            if (i >= warmup_runs) {
                gridding_times.add(produce_data_time);
            }
        }

        printStats("Gridding Times", gridding_times);

    }

    private static AbstractCurvilinearGrid getGrid(int grid_type, GridCoordSystem gcs) throws Exception {
        switch (grid_type) {

            case 0:
                return LookUpTableGrid.generate(gcs);
            case 1:
                return KdTreeGrid.generate(gcs);
            case 2:
                return PRTreeGrid.generate(gcs, PRTreeGrid.RTREE_BRANCH_FACTOR);
            case 3:
                return RTreeGrid.generate(gcs);
            default:
                throw new Exception("grid_type must be 0,1,2 or 3");
        }
    }

    public static void run_buildtime_benchmarking(int grid_type, String data_filename, String data_variable, int warmup_runs, int tries_per_parameter_set) throws Exception {

        AbstractCurvilinearGrid index;
        Runtime runtime = Runtime.getRuntime();

        NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
        GridDatatype grid = CdmUtils.getGridDatatype(nc, data_variable);
        GridCoordSystem gcs = grid.getCoordinateSystem();

        ArrayList<Double> build_times = new ArrayList<Double>();
        ArrayList<Double> memory_usages = new ArrayList<Double>();

        for (int i = 0; i < tries_per_parameter_set + warmup_runs; i++) {
            if (i < warmup_runs) {
                System.out.println("Performing warmup run");
            } else {
                System.out.println("Performing benchmark run");
            }

            double memory_usage_before_grid = getHeapUsage(runtime);
            double time_before_index = System.nanoTime();
            index = getGrid(grid_type, gcs);
            double time_after_index = System.nanoTime();
            double memory_usage_after_grid = getHeapUsage(runtime);

            double memory_usage = memory_usage_after_grid - memory_usage_before_grid;
            double index_building_time = (time_after_index - time_before_index) / 1.e9;

            if (i >= warmup_runs) {
                memory_usages.add(memory_usage);
                build_times.add(index_building_time);
            }

            index = null;
            System.gc();
            clearCaches();
            Thread.sleep(5000);
        }

        nc.close();

        printStats("Memory Usage", memory_usages);
        printStats("Build Times", build_times);
    }

    public static void clearCaches() {
        KdTreeGrid.clearCache();
        PRTreeGrid.clearCache();
        RTreeGrid.clearCache();
        LookUpTableGrid.clearCache();
    }

    public static void run_kdtree_tuning(String data_filename, String data_variable, String test_output_filename, int test_output_size, double default_minimum_resolution, double default_expansion_factor, double default_maximum_size, int default_max_iterations) throws Exception {

        NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
        GridDatatype grid = CdmUtils.getGridDatatype(nc, data_variable);
        GridCoordSystem gcs = grid.getCoordinateSystem();

        Scanner input_commands = new Scanner(System.in);


        System.out.println("Building tree");
        KdTreeGrid kd_grid = KdTreeGrid.generate(gcs);
        System.out.println("Tree built");

        HorizontalGrid targetDomain = new RegularGridImpl(kd_grid.getExtent(), test_output_size, test_output_size);

        double current_expansion_factor = default_expansion_factor;
        double current_minimum_resolution = default_minimum_resolution;
        double maximum_size = default_maximum_size;
        int current_max_iterations = default_max_iterations;

        ArrayList<Double> recorded_efs = new ArrayList<Double>();
        ArrayList<Double> recorded_mrs = new ArrayList<Double>();
        ArrayList<Double> recorded_ms = new ArrayList<Double>();
        ArrayList<Integer> recorded_mi = new ArrayList<Integer>();
        ArrayList<Double> recorded_t = new ArrayList<Double>();

        boolean finished = false;
        while (true) {
            boolean input_finished = false;
            while (true) {
                System.out.println("0: Set Maximum Distance\n1: Set Expansion Factor\n2: Set Minimum Resolution\n3: Set Maximum Iterations\n4: Stop\n5: Run\n");
                try {
                    switch (input_commands.nextInt()) {
                        case 0:
                            maximum_size = input_commands.nextDouble();
                            break;
                        case 1:
                            current_expansion_factor = input_commands.nextDouble();
                            break;
                        case 2:
                            current_minimum_resolution = input_commands.nextDouble();
                            break;
                        case 3:
                            current_max_iterations = input_commands.nextInt();
                            break;
                        case 4:
                            input_finished = true;
                            finished = true;
                            break;
                        case 5:
                            input_finished = true;
                            break;
                        default:
                            throw new java.util.InputMismatchException();
                    }

                } catch (java.util.InputMismatchException ex) {
                    System.out.println("Unrecognised input");
                    input_commands.next();
                }

                if (input_finished) {
                    break;
                }
            }
            if (finished) {
                break;
            }

            kd_grid.setQueryingParameters(current_minimum_resolution, current_expansion_factor, maximum_size, current_max_iterations);

            System.out.printf("Running with Expansion Factor = %f, Minimum Resolution = %f, Maximum Distance = %f\n", current_expansion_factor, current_minimum_resolution, maximum_size);
            long start = System.nanoTime();
            List<Float> newData = CdmUtils.readHorizontalPoints(nc, grid, kd_grid, 0, 0, targetDomain);
            long stop = System.nanoTime();
            double duration = (stop - start) / (1.e9 * test_output_size * test_output_size);

            // Write out the image
            ImageProducer ip = new ImageProducer.Builder().palette(ColorPalette.get(null)).style(Style.BOXFILL).height(test_output_size).width(test_output_size).build();
            ip.addFrame(newData, null);
            List<BufferedImage> ims = ip.getRenderedFrames();
            new File(test_output_filename).delete();
            ImageIO.write(ims.get(0), "png", new File(test_output_filename));

            // Add results to table
            recorded_efs.add(current_expansion_factor);
            recorded_mi.add(current_max_iterations);
            recorded_mrs.add(current_minimum_resolution);
            recorded_ms.add(maximum_size);
            recorded_t.add(duration);


            // Print out table of results
            System.out.printf("MR              EF              MS              MI        Tp              Ti\n");
            for (int i = 0; i < recorded_t.size(); i++) {
                System.out.printf("%f\t%f\t%f\t%d\t%f\t%f\n", recorded_mrs.get(i), recorded_efs.get(i), recorded_ms.get(i), recorded_mi.get(i), recorded_t.get(i), recorded_t.get(i) * test_output_size * test_output_size);
            }

        }
    }

    public static void run_test_output(String data_filename, String data_variable, int grid_type, String output_filename, int output_size) throws Exception {

        NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
        GridDatatype grid = CdmUtils.getGridDatatype(nc, data_variable);
        GridCoordSystem gcs = grid.getCoordinateSystem();

        AbstractCurvilinearGrid index = getGrid(grid_type, gcs);

        HorizontalGrid targetDomain = new RegularGridImpl(index.getExtent(), output_size, output_size);

        List<Float> newData = CdmUtils.readHorizontalPoints(nc, grid, index, 0, 0, targetDomain);

        // Write out the image
        ImageProducer ip = new ImageProducer.Builder().palette(ColorPalette.get(null)).style(Style.BOXFILL).height(output_size).width(output_size).build();
        ip.addFrame(newData, null);
        List<BufferedImage> ims = ip.getRenderedFrames();
        new File(output_filename).delete();
        ImageIO.write(ims.get(0), "png", new File(output_filename));

    }

    public static void printStats(String title, List<Double> results) {
        System.out.println(title);
        for (double result : results) {
            System.out.println(result);
        }
        System.out.println("---");

        /// Compute the stats (not very efficient)
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
        System.out.println(mean);
        System.out.println(standard_deviation);
        System.out.println("---");
    }

    public static double getHeapUsage(Runtime r) throws InterruptedException {
        System.gc();
        Thread.sleep(5000);
        return r.totalMemory() - r.freeMemory();
    }

    public static void main(String[] args) throws Exception {
        boolean perform_kdtree_tuning = false;
        boolean perform_buildtime_benchmarking = false;
        boolean perform_querytime_benchmarking = false;
        boolean produce_test_output = false;
        boolean perform_imagemaker_comparison = true; // Compare source-push and destination-pull methods for generating images

        String test_output_filename = "/home/andy/sat_data/test.png";
        int test_output_size = 256;

        // Dataset: 0=ORCA, 1=UCAD, 2=EUMETSAT
        int dataset = 2;

        // Grid Type: 0=LUT, 1=KDTree, 2=PRTree, 3=RTree
        int grid_type = 3;
        int tries_per_parameter_set = 5;
        int warmup_runs = 3;

        String data_filename, data_variable;
        double expansion_factor, max_distance, nominal_resolution;
        int max_iterations = 1;

        switch (dataset) {
            case 0:
                data_filename = "/home/andy/sat_data/orka/ORCA025-R07_y2004_ANNUAL_gridT2.nc";
                data_variable = "sossheig_sqd";
                nominal_resolution = 0.13;
                expansion_factor = 3.25;
                max_distance = 0.75;
                break;
            case 1:
                data_filename = "/home/andy/sat_data/orka/UCA25D.20101118.04.nc";
                data_variable = "sea_level";
                nominal_resolution = 0.005;
                expansion_factor = 2.12;
                max_distance = 0.038;
                break;
            case 2:
                data_filename = "/home/andy/sat_data/W_XX-EUMETSAT-Darmstadt,VIS+IR+IMAGERY,MET7+MVIRI_C_EUMS_20091110120000.nc";
                data_variable = "ch1";
                nominal_resolution = 0.03;
                expansion_factor = 2.5;
                max_distance = 0.2;
                break;
            default:
                throw new Exception("You forgot to set a dataset...");
        }


        if (perform_kdtree_tuning) {
            System.out.println("Performing KDTree tuning");
            run_kdtree_tuning(
                    data_filename, data_variable, test_output_filename, test_output_size, nominal_resolution, expansion_factor, max_distance, max_iterations);
        }

        if (perform_buildtime_benchmarking) {
            System.out.printf("Performing buildtime benchmarking (dataset %d, index %d)\n", dataset, grid_type);
            run_buildtime_benchmarking(
                    grid_type, data_filename, data_variable, warmup_runs, tries_per_parameter_set);
        }

        if (perform_querytime_benchmarking || perform_imagemaker_comparison) {
            NetcdfDataset nc = NetcdfDataset.openDataset(data_filename);
            GridDatatype grid = CdmUtils.getGridDatatype(nc, data_variable);
            GridCoordSystem gcs = grid.getCoordinateSystem();
            AbstractCurvilinearGrid index = getGrid(grid_type, gcs);

            if (index instanceof KdTreeGrid) {
                ((KdTreeGrid) index).setQueryingParameters(nominal_resolution, expansion_factor, max_distance, max_iterations);
            }

            if (perform_querytime_benchmarking) {
                System.out.printf("Performing querytime benchmarking (dataset %d, index %d)\n", dataset, grid_type);
                run_querytime_benchmarking(
                        index, test_output_size, warmup_runs, tries_per_parameter_set);
            }

            if (perform_imagemaker_comparison) {
                System.out.printf("Performing image-maker comparison (dataset %d, index %d)\n", dataset, grid_type);
                List<Integer> sizes = Arrays.asList(256, 512, 1024);
                for (int size : sizes) {
                    run_imagemaker_comparison(
                            nc, grid, index, size, warmup_runs, tries_per_parameter_set);
                }
            }
        }

        if (produce_test_output) {
            run_test_output(data_filename, data_variable, grid_type, test_output_filename, test_output_size);
        }
    }
}
