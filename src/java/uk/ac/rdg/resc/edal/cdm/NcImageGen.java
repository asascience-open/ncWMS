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
import java.util.Collection;
import java.util.List;

import javax.imageio.ImageIO;

import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.joda.time.DateTime;
import org.opengis.metadata.extent.GeographicBoundingBox;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDataset;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.grid.RegularGrid;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.ncwms.graphics.ImageProducer;

/**
 * <p>Diagnostic tool for testing CDM datasets without loading them into ncWMS.
 * If there are any problems reading data using ncWMS, use this tool to find out
 * more information.</p>
 * @author Jon
 */
public final class NcImageGen
{
    public static void main(String[] args) throws IOException
    {
        if (args.length != 9) {
            System.err.println("Usage: NcImageGen <filename> <output_width> <output_height> <min_lon> <max_lon> <min_lat> <max_lat> <height> <tIndex>");
            System.exit(-1);
        }
        String filename = args[0];
        int xsize = Integer.parseInt(args[1]);
        int ysize = Integer.parseInt(args[2]);
        double lonmin = Double.parseDouble(args[3]);
        double lonmax = Double.parseDouble(args[4]);
        double latmin = Double.parseDouble(args[5]);
        double latmax = Double.parseDouble(args[6]);
        double zVal = Double.parseDouble(args[7]);
        int tIndex = Integer.parseInt(args[8]);
        
        
        GeographicBoundingBox bbox = new DefaultGeographicBoundingBox(lonmin, lonmax, latmin, latmax);
        
        // Open the file using the Unidata CDM
        NetcdfDataset nc = null;
        try {
            nc = NetcdfDataset.openDataset(filename);

            GridDataset gd = CdmUtils.getGridDataset(nc);
            // Read all the metadata from the file
            Collection<CoverageMetadata> coverages = CdmUtils.readCoverageMetadata(gd);
            // Cycle through all the coverages, printing out information from each
            for (CoverageMetadata cm : coverages) {
            	List<Double> zVals = cm.getElevationValues();
            	double min_diff = 1000000.0;
            	int zIndex = 0;
            	for(int i = 0; i < zVals.size(); i++){
            		double z = zVals.get(i);
            		if(Math.abs(z - zVal) <  min_diff) {
            			min_diff = Math.abs(z - zVal);
            			zIndex = i; 
            		}
            	}
            	
            	if(zVals.isEmpty()) {
            		System.out.println("Variable: "+cm.getId()+" only has one point in the vertical axis.");
            	} else {
            		System.out.println("Variable: "+cm.getId()+" has "+zVals.size()+" elevation data.  Nearest elevation to " +
            				zVal+" is "+ zVals.get(zIndex)+".");
            	}
            	
            	List<DateTime> tVals = cm.getTimeValues();
            	
            	if(tVals.isEmpty() || tVals.size() == 1) {
            		System.out.println("Variable: "+cm.getId()+" only has one point in the time axis.");
            		tIndex = 0;
            	} else if(tIndex > tVals.size()){
            		System.out.println("Variable: "+cm.getId()+" has "+tVals.size()+" time values.  Index " +
            				tIndex+" is too large.  Using maximum, which corresponds to "+
            				tVals.get(tVals.size()-1)+".");
            		tIndex = tVals.size() - 1;
            	} else if(tIndex < 0){
            		System.out.println("Variable: "+cm.getId()+" has "+tVals.size()+" time values.  Index " +
            				tIndex+" is negative.  Using first time value, which corresponds to "+
            				tVals.get(0)+".");
            		tIndex = 0;
            	} else {            		
            		System.out.println("Variable: "+cm.getId()+" has "+tVals.size()+" time values.  Index " +
            				tIndex+" corresponds to "+ tVals.get(tIndex)+".");
            	}
            	
                List<Float> data = readData(nc, cm, bbox, xsize, ysize, tIndex, zIndex);
                BufferedImage image = createImage(data, xsize, ysize);
                String imageFilename = cm.getId().toLowerCase() + ".png";
                ImageIO.write(image, "png", new File(imageFilename));
            }
        } finally {
            if (nc != null) nc.close();
        }
    }

    /**
     * Generates an image of the given variable covering its geographic bounding
     * box.
     */
    private static List<Float> readData(NetcdfDataset nc, CoverageMetadata cm,
            GeographicBoundingBox bbox, int width, int height, int tIndex, int zIndex)
            throws IOException
    {
        // First create the grid representing the pixel values
        RegularGrid targetGrid = new RegularGridImpl(bbox, width, height);
        // Read data from the source file onto this grid
        return CdmUtils.readHorizontalPoints(
                nc,
                cm.getId(),
                cm.getHorizontalGrid(),
                tIndex,
                zIndex,
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
}
