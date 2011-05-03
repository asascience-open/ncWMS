/*
 * Copyright (c) 2011 The University of Reading
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import javax.imageio.ImageIO;
import org.opengis.metadata.extent.GeographicBoundingBox;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.edal.util.Ranges;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer;

/**
 * <p>Diagnostic tool for testing CDM datasets without loading them into ncWMS.
 * If there are any problems reading data using ncWMS, use this tool to find out
 * more information.</p>
 * @author Jon
 */
public final class NcDiag
{
    public static void main(String[] args) throws IOException
    {
        if (args.length != 1) {
            System.err.println("Usage: NcDiag <filename>");
            System.exit(-1);
        }
        String filename = args[0];
        PrintStream ps = System.out;
        printHeader(ps, filename);
        // Open the file using the Unidata CDM
        NetcdfDataset nc = null;
        try {
            nc = NetcdfDataset.openDataset(filename);
            // TODO: print information about the NetCDF file as a whole
            GridDataset gd = CdmUtils.getGridDataset(nc);
            // Read all the metadata from the file
            Collection<CoverageMetadata> coverages = CdmUtils.readCoverageMetadata(gd);
            // Cycle through all the coverages, printing out information from each
            for (CoverageMetadata cm : coverages) {
                printInfo(ps, nc, cm);
            }
        } finally {
            if (nc != null) nc.close();
            printFooter(ps);
            ps.close();
        }
    }

    /**
     * Prints the HTTP header to the given PrintStream
     */
    private static void printHeader(PrintStream ps, String filename)
    {
        ps.println("<html>");
        ps.printf("<head><title>Report from %s</title></head>%n", filename);
        ps.println("<body>");
        ps.printf("<h1>Report from %s</h1>%n", filename);
    }

    /**
     * Prints information about the given variable.
     */
    private static void printInfo(PrintStream ps, NetcdfDataset nc, CoverageMetadata cm)
            throws IOException
    {
        ps.println("<hr />");
        ps.printf("<h2>Variable %s</h2>%n", cm.getId());
        ps.println("<table>");
        ps.println("<tbody>");
        printTableLine(ps, "Title", cm.getTitle());
        printTableLine(ps, "Units", cm.getUnits());
        printTableLine(ps, "Description", cm.getDescription());
        GeographicBoundingBox bbox = cm.getGeographicBoundingBox();
        printTableLine(ps, "Geographic Bounding box",
            String.format("%f,%f,%f,%f",
                bbox.getWestBoundLongitude(),
                bbox.getSouthBoundLatitude(),
                bbox.getEastBoundLongitude(),
                bbox.getNorthBoundLatitude()
            )
        );
        printTableLine(ps, "Elevation axis", String.format("%d values", cm.getElevationValues().size()));
        printTableLine(ps, "Time axis (" + cm.getChronology() + ")",
            String.format("%d values", cm.getTimeValues().size())
        );
        ps.println("</tbody>");
        ps.println("</table>");
        // Create an image of this variable and save it
        int width = 256;
        int height = 256;
        List<Float> data = readData(nc, cm, width, height);
        Range<Float> dataRange = Ranges.findMinMax(data);
        BufferedImage im = createImage(data, width, height);
        String imageFilename = cm.getId() + ".png";
        ImageIO.write(im, "png", new File(imageFilename));
        ps.printf("<p>Data min: %f, max: %f<br />", dataRange.getMinimum(), dataRange.getMaximum());
        ps.printf("<img src=\"%s\" width=\"%d\" height=\"%d\" /></p>%n",
                imageFilename, width, height);
    }

    private static void printTableLine(PrintStream ps, String title, String value)
    {
        ps.printf("<tr><td><b>%s:</b></td><td>%s</td></tr>%n", title, value);
    }

    /**
     * Generates an image of the given variable covering its geographic bounding
     * box.
     */
    private static List<Float> readData(NetcdfDataset nc,
            CoverageMetadata cm, int width, int height)
            throws IOException
    {
        // First create the grid representing the pixel values
        RegularGrid targetGrid = new RegularGridImpl(cm.getGeographicBoundingBox(),
                width, height);
        // Read data from the source file onto this grid
        return CdmUtils.readHorizontalPoints(
                nc,
                cm.getId(),
                cm.getHorizontalGrid(),
                0, // Read from first t and z index
                0,
                targetGrid
        );
    }

    private static BufferedImage createImage(List<Float> data, int width, int height)
    {
        // Create an object to transform data into images
        ImageProducer ip = new ImageProducer.Builder()
                .width(width)
                .height(height)
                .build();
        // Turn the data into an image and return it
        ip.addFrame(data, null); // no label needed
        return ip.getRenderedFrames().get(0);
    }

    /**
     * Prints the HTTP footer to the given PrintStream
     */
    private static void printFooter(PrintStream ps)
    {
        ps.println("</body>");
        ps.println("</html>");
    }

}
