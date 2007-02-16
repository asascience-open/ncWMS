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

package uk.ac.rdg.resc.ncwms.datareader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.log4j.Logger;
import ucar.unidata.geoloc.LatLonPoint;

/**
 * Coordinate axis for NEMO data
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NemoCoordAxis extends EnhancedCoordAxis
{
    private static final Logger logger = Logger.getLogger(NemoCoordAxis.class);
    
    public static final NemoCoordAxis ONE_DEGREE_I_AXIS;
    public static final NemoCoordAxis ONE_DEGREE_J_AXIS;
    public static final NemoCoordAxis ONE_QUARTER_DEGREE_I_AXIS;
    public static final NemoCoordAxis ONE_QUARTER_DEGREE_J_AXIS;
    
    private short[] indices;
    private int lutRes;
    
    static
    {
        try
        {
            ONE_DEGREE_I_AXIS = createAxis("1", "i", 4);
            ONE_DEGREE_J_AXIS = createAxis("1", "j", 4);
            ONE_QUARTER_DEGREE_I_AXIS = createAxis("025", "i", 12);
            ONE_QUARTER_DEGREE_J_AXIS = createAxis("025", "j", 12);
        }
        catch(IOException ioe)
        {
            // Error has already been logged in createAxis()
            throw new ExceptionInInitializerError(ioe);
        }
    }
    
    /**
     * Creates a NemoCoordAxis for a NEMO dataset of a certain resolution
     * @param resCode resolution of the model "1" or "025"
     * @param axisCode the axis "i" or "j"
     * @param lutRes the (inverse of the) resolution of the look-up table
     * (12 = 1/12 degree, 4 = 1/4 degree)
     * @return a newly-created NemoCoordAxis
     */
    private static final NemoCoordAxis createAxis(String resCode, String axisCode, int lutRes) throws IOException
    {        
        // Read the relevant axis data from the lookup tables
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(
            "/uk/ac/rdg/resc/ncwms/datareader/ORCA" + resCode + "_" + lutRes + "x" + lutRes + ".zip");
        ZipInputStream zin = new ZipInputStream(in);
        String filename = "ORCA" + resCode + "_" + axisCode + "lt_" + lutRes + "x" + lutRes + ".dat";
        BufferedReader reader = null;
        try
        {
            // Skip to the required entry
            boolean done = false;
            do
            {
                ZipEntry entry = zin.getNextEntry();
                if (entry == null)
                {
                    throw new ExceptionInInitializerError(filename + " not found in zip file");
                }
                else if (entry.getName().equals(filename))
                {
                    done = true;
                }
            } while (!done);

            logger.debug("Reading lookup data from {}", filename);
            reader = new BufferedReader(new InputStreamReader(zin));
            String line = null;
            int i = 0;
            short[] indices = new short[(360 * lutRes) * (180 * lutRes - 1)];
            do
            {
                line = reader.readLine();
                if (line != null)
                {
                    StringTokenizer tok = new StringTokenizer(line);
                    while(tok.hasMoreTokens())
                    {
                        indices[i] = Short.parseShort(tok.nextToken());
                        // Files were produced using FORTRAN, hence indices are 1-based
                        indices[i] -= 1;
                        i++;
                    }
                }
            } while (line != null);
            logger.debug("Read {} items of lookup data", i);
            // Garbage-collect to try to free some memory
            System.gc();
            return new NemoCoordAxis(indices, lutRes);
        }
        catch(IOException ioe)
        {
            logger.error("IO error reading from " + filename, ioe);
            throw ioe;
        }
        catch(RuntimeException rte)
        {
            logger.error("Runtime error: " + rte);
            throw rte;
        }
        finally
        {
            if (reader != null)
            {
                // Close the reader, ignoring error messages
                try { reader.close(); } catch (IOException ioe) {}
            }
        }
    }
    
    /** Creates a new instance of NemoCoordAxis */
    private NemoCoordAxis(short[] indices, int lutRes)
    {
        this.indices = indices;
        this.lutRes = lutRes;
    }
    
    public int getIndex(LatLonPoint point)
    {
        double minLat = -90.0 + (1.0 / this.lutRes);
        double maxLat = 90.0 - (1.0 / this.lutRes);
        if (point.getLatitude() < minLat || point.getLatitude() > maxLat)
        {
            return -1;
        }
        // TODO: use Math.round() instead of int()?
        int latIndex = (int)((point.getLatitude() - minLat) * this.lutRes);
        double lon = point.getLongitude();
        if (lon < 0.0) lon += 360.0;
        int lonIndex = (int)(lon * this.lutRes);
        return this.indices[latIndex * (360 * this.lutRes) + lonIndex];
    }
    
}
