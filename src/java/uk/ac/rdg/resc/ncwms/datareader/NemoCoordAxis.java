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
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;
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
    
    public static final NemoCoordAxis I_AXIS = createAxis("i");
    public static final NemoCoordAxis J_AXIS = createAxis("j");
    
    private short[] indices;
    
    private static final NemoCoordAxis createAxis(String axis)
    {
        // Read the relevant axis data from the lookup tables
        String filename = "C:\\data\\NEMO\\ORCA025_" + axis + "lt_4x4.dat";
        logger.debug("Reading lookup data from {}", filename);
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(filename));
            String line = null;
            int i = 0;
            short[] indices = new short[1441 * 721];
            do
            {
                line = reader.readLine();
                if (line != null)
                {
                    StringTokenizer tok = new StringTokenizer(line);
                    while(tok.hasMoreTokens())
                    {
                        indices[i] = Short.parseShort(tok.nextToken());
                        i++;
                    }
                }
            } while (line != null);
            logger.debug("Read {} items of lookup data", i);
            return new NemoCoordAxis(indices);
        }
        catch(IOException ioe)
        {
            logger.error("IO error reading from " + filename, ioe);
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
        return null; // Do something here?
    }
    
    /** Creates a new instance of NemoCoordAxis */
    private NemoCoordAxis(short[] indices)
    {
        this.indices = indices;
    }
    
    public int getIndex(LatLonPoint point)
    {
        // TODO: use Math.round() instead of int()?
        if (point.getLatitude() < -90.0 || point.getLatitude() > 90.0)
        {
            return -1;
        }
        int latIndex = (int)((point.getLatitude() + 90) * 4);
        int lonIndex = (int)((point.getLongitude() + 180) * 4);
        return this.indices[latIndex * 1441 + lonIndex];
    }
    
}
