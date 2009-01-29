
/* Copyright (c) 2006 The University of Reading
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

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.DateUnit;
import uk.ac.rdg.resc.ncwms.metadata.LUTCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;

/**
 * Description of NemoDataReader
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NemoDataReaderTwoDegree extends NemoDataReader
{
    private static final Logger logger = LoggerFactory.getLogger(NemoDataReaderTwoDegree.class);
    
    @Override
    protected void findAndUpdateLayers(String location, Map<String, LayerImpl> layers)
        throws IOException
    {
        NetcdfDataset nc = null;
        
        try
        {
            // Open the dataset.  We use the dataset cache here for the following
            // reason: if the cache has swallowed all of our available file handles,
            // we can't bypass the cache as there will be no file handles left.
            nc = NetcdfDataset.acquireDataset(location, null);
            
            // Get the depth values and units
            Variable depth = nc.findVariable("deptht");
            float[] fzVals = (float[])depth.read().copyTo1DJavaArray();
            // Copy to an array of doubles
            double[] zVals = new double[fzVals.length];
            for (int i = 0; i < fzVals.length; i++)
            {
                zVals[i] = -fzVals[i];
            }
            String zUnits = depth.getUnitsString();
            
            // Get the time values and units
            Variable time = nc.findVariable("time_counter");
            float[] ftVals = (float[])time.read().copyTo1DJavaArray();
            DateUnit dateUnit = null;
            try
            {
                dateUnit = new DateUnit(time.getUnitsString());
            }
            catch(Exception e)
            {
                // Shouldn't happen if file is well formed
                logger.error("Malformed time units string " + time.getUnitsString());
                // IOException not ideal here but didn't want to create new exception
                // type just for this rare case
                throw new IOException("Malformed time units string " + time.getUnitsString());
            }

            for (Variable var : nc.getVariables())
            {
                // We ignore the coordinate axes
                if (!var.getName().equals("nav_lon") && !var.getName().equals("nav_lat")
                    && !var.getName().equals("deptht") && !var.getName().equals("time_counter"))
                {
                    LayerImpl layer;
                    if (layers.containsKey(var.getName()))
                    {
                        layer = (LayerImpl)layers.get(var.getName());
                    }
                    else
                    {
                        layer = new LayerImpl();
                        layer.setId(var.getName());
                        layer.setAbstract(var.getDescription());
                        layer.setTitle(var.getDescription()); // TODO: standard_names are not set: set these in NcML?
                        layer.setUnits(var.getUnitsString());
                        layer.setZpositive(false);
                        // TODO: check for the presence of a z axis in a neater way
                        if (var.getRank() == 4)
                        {
                            layer.setZvalues(zVals);
                            layer.setZunits(zUnits);
                        }

                        layer.setXaxis(LUTCoordAxis.createAxis(this.getXAxisLUTLocation(), AxisType.GeoX));
                        layer.setYaxis(LUTCoordAxis.createAxis(this.getYAxisLUTLocation(), AxisType.GeoY));
                        
                        // Add this new layer to the map
                        layers.put(layer.getId(), layer);
                    }
                    
                    // Set the time axis
                    for (int i = 0; i < ftVals.length; i++)
                    {
                        Date timestep = dateUnit.makeDate(ftVals[i]);
                        layer.addTimestepInfo(new TimestepInfo(timestep, location, i));
                    }
                }
            }
        }
        finally
        {
            if (nc != null)
            {
                try
                {
                    nc.close();
                }
                catch (IOException ex)
                {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
    
    /**
     * Gets the location of the x axis' LUT
     */
    protected String getXAxisLUTLocation()
    {
        return "/uk/ac/rdg/resc/ncwms/metadata/NEMO_2DEG.zip/ORCA2_LUT_i_3601_1801.dat";
    }
    
    /**
     * Gets the location of the y axis' LUT
     */
    protected String getYAxisLUTLocation()
    {
        return "/uk/ac/rdg/resc/ncwms/metadata/NEMO_2DEG.zip/ORCA2_LUT_j_3601_1801.dat";
    }
}
