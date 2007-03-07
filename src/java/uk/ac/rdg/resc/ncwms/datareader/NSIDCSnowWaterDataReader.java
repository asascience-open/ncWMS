/*
 * Copyright (c) 2006 The University of Reading
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

/**
 * DataReader for NSIDC snow/water data
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NSIDCSnowWaterDataReader extends DataReader
{
    private static final Logger logger = Logger.getLogger(NSIDCSnowWaterDataReader.class);
    
    /**
     * The number of rows of data in the grid
     */
    private static final int ROWS = 721;
    /**
     * The number of columns of data in the grid
     */
    private static final int COLS = 721;
    /**
     * radius of the earth (km), authalic sphere based on International datum
     */
    private static final double RE_KM = 6371.228;
    /**
     * nominal cell size in kilometers
     */
    private static final double CELL_KM = 25.067525;
    
    private static FilenameFilter FILENAME_FILTER = new FilenameFilter()
    {
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".v01.NSIDC8");
        }
    };
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("'NL'yyyyMM'.v01.NSIDC8'");
    
    /**
     * Creates a new instance of NSIDCSnowWaterDataReader
     */
    public NSIDCSnowWaterDataReader()
    {
    }
    
    public Hashtable<String, VariableMetadata> getVariableMetadata()
        throws IOException
    {
        Hashtable<String, VariableMetadata> vars = new Hashtable<String, VariableMetadata>();
        VariableMetadata vm = new VariableMetadata();
        
        vm.setId("swe");
        vm.setTitle("snow_water_equivalent");
        vm.setUnits("mm");
        vm.setValidMin(0.0);
        vm.setValidMax(500.0); // TODO: is this OK?
        vm.setBbox(new double[]{-180.0, 0.0, 180.0, 90.0});
        
        // Search the source directory for files
        String[] filenames = getFilenames(location);
        for (String filename : filenames)
        {
            try
            {
                Date timestep = DATE_FORMAT.parse(filename);
                File f = new File(location, filename);
                vm.addTimestepInfo(new VariableMetadata.TimestepInfo(timestep, f.getPath(), 0));
            }
            catch(ParseException pe)
            {
                logger.error("Error parsing filename " + filename, pe);
                // TODO: not really an IOException
                throw new IOException("Error parsing filename " + filename);
            }
        }
        
        vars.put(vm.getId(), vm);
        return vars;
    }
    
    /**
     * @return a list of filenames containing the data
     */
    private static String[] getFilenames(String location)
    {
        File dir = new File(location);
        return dir.list(FILENAME_FILTER);
    }
    
    protected float[] read(VariableMetadata vm, int tIndex,
        int zIndex, float[] latValues, float[] lonValues, float fillValue)
        throws WMSExceptionInJava
    {
        // Find the file containing the data
        VariableMetadata.TimestepInfo tInfo = vm.getTimestepInfo(tIndex);
        logger.debug("Got file " + tInfo.getFilename());
        
        // Create an array to hold the data
        float[] picData = new float[lonValues.length * latValues.length];
        Arrays.fill(picData, fillValue);
        
        FileInputStream fin = null;
        ByteBuffer data = null;
        // Read the whole of the file into memory
        try
        {
            fin = new FileInputStream(tInfo.getFilename());
            data = ByteBuffer.allocate(ROWS * COLS * 2);
            data.order(ByteOrder.LITTLE_ENDIAN);
            // Read the whole of the file into memory
            int numBytesRead = fin.getChannel().read(data);
        }
        catch(IOException ioe)
        {
            logger.error("IOException reading from " + tInfo.getFilename(), ioe);
            throw new WMSExceptionInJava("Internal error: IOException reading from "
                + tInfo.getFilename() + ": " + ioe.getMessage());
        }
        finally
        {
            try { if (fin != null) fin.close(); } catch (IOException ioe) {}
        }
        
        for (int j = 0; j < latValues.length; j++)
        {
            if (latValues[j] >= 0.0 && latValues[j] <= 90.0)
            {
                for (int i = 0; i < lonValues.length; i++)
                {
                    // Find the index in the source data
                    int dataIndex = latLonToIndex(latValues[j], lonValues[i]);
                    // two bytes per pixel
                    short val = data.getShort(dataIndex * 2);
                    int picIndex = (j * lonValues.length) + i;
                    if (val > 0) picData[picIndex] = val;
                }
            }
        }
        
        return picData;
    }
    
    /**
     * convert geographic coordinates (spherical earth) to
     *	azimuthal equal area or equal area cylindrical grid coordinates
     *
     *	status = ezlh_convert (grid, lat, lon, &r, &s)
     *
     *	input : grid - projection name "[NSM][lh]"
     *          where l = "low"  = 25km resolution
     *                     h = "high" = 12.5km resolution
     *		lat, lon - geo. coords. (decimal degrees)
     *
     *	output: r, s - column, row coordinates
     *
     *	result: status = 0 indicates normal successful completion
     *			-1 indicates error status (point not on grid)
     */
    private static int latLonToIndex(double lat, double lon)
    {        
        double Rg = RE_KM / CELL_KM;
        
        double r0 = (COLS - 1) / 2.0;
        double s0 = (ROWS - 1) / 2.0;
        
        double phi = Math.toRadians(lat);
        double lam = Math.toRadians(lon);
        
        double rho = 2 * Rg * Math.sin(Math.PI / 4.0 - phi / 2.0);
        
        int col = (int)Math.round(r0 + rho * Math.sin(lam));
        int row = (int)Math.round(s0 + rho * Math.cos(lam));
        
        int index = row * COLS + col;
        return index;
    }
    
}
