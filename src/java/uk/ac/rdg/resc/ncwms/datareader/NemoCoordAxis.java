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
    
    private static final int LUT_RES = 12;  // Resolution of the lookup table in points per degree
    
    public static final NemoCoordAxis I_AXIS;
    public static final NemoCoordAxis J_AXIS;
    
    private short[] indices;
    
    static
    {
        try
        {
            I_AXIS = createAxis("i");
            J_AXIS = createAxis("j");
        }
        catch(IOException ioe)
        {
            // Error has already been logged in createAxis()
            throw new ExceptionInInitializerError(ioe);
        }
    }
    
    private static final NemoCoordAxis createAxis(String axis) throws IOException
    {
        // Read the relevant axis data from the lookup tables
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(
            "/uk/ac/rdg/resc/ncwms/datareader/ORCA025_" + LUT_RES + "x" + LUT_RES + ".zip");
        ZipInputStream zin = new ZipInputStream(in);
        String filename = "ORCA025_" + axis + "lt_" + LUT_RES + "x" + LUT_RES + ".dat";
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
            short[] indices = new short[(360 * LUT_RES) * (180 * LUT_RES - 1)];
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
            return new NemoCoordAxis(indices);
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
    private NemoCoordAxis(short[] indices)
    {
        this.indices = indices;
    }
    
    public int getIndex(LatLonPoint point)
    {
        double minLat = -90.0 + (1.0 / LUT_RES);
        double maxLat = 90.0 - (1.0 / LUT_RES);
        if (point.getLatitude() < minLat || point.getLatitude() > maxLat)
        {
            return -1;
        }
        // TODO: use Math.round() instead of int()?
        int latIndex = (int)((point.getLatitude() - minLat) * LUT_RES);
        double lon = point.getLongitude();
        if (lon < 0.0) lon += 360.0;
        int lonIndex = (int)(lon * LUT_RES);
        return this.indices[latIndex * (360 * LUT_RES) + lonIndex];
    }
    
}
