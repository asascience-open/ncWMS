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

import java.io.IOException;
import java.util.Date;
import java.util.Hashtable;
import org.apache.log4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.units.DateUnit;

/**
 * Data and metadata reader for ECMWF System 3 Reanalysis Data.  Based on
 * DefaultDataReader, with some fixes for problems within the metadata.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class EcmwfSystem3ReanalysisDataReader extends DefaultDataReader
{
    private static final Logger logger = Logger.getLogger(EcmwfSystem3ReanalysisDataReader.class);
    
    /**
     * Corrects problem with reading bounding box in source data (use of latitude
     * values > +/- 90 degrees causes latitude portion of BBOX to be NaN)
     */
    public Hashtable<String, VariableMetadata> getVariableMetadata(String location)
        throws IOException
    {
        Hashtable<String, VariableMetadata> vars = super.getVariableMetadata(location);
        for (VariableMetadata vm : vars.values())
        {
            vm.setBbox(new double[]{-180.0, -90.0, 180.0, 90.0});
        }
        return vars;
    }
    
    /**
     * Gets array of Dates representing the timesteps of the given variable.
     * In the ECMWF data the time is represented as a separate variable,
     * not a proper dimension!
     * @param nc The NetcdfDataset to which the variable belongs
     * @param gg the variable as a GeoGrid
     * @return Array of {@link Date}s
     * @throws IOException if there was an error reading the timesteps data
     */
    protected Date[] getTimesteps(NetcdfDataset nc, GeoGrid gg)
        throws IOException
    {
        Variable timeVar = nc.findVariable("time");
        Array timeData = timeVar.read();
        Index index = timeData.getIndex();
        index.set(0);
        float timeVal = timeData.getFloat(index);
        
        DateUnit dateUnit = null;
        try
        {
            dateUnit = new DateUnit(timeVar.getUnitsString());
        }
        catch(Exception e)
        {
            throw new IOException("Malformed date units string in " +
                nc.getLocation());
        }
        logger.debug("timeVal = {}, units = {}", timeVal, timeVar.getUnitsString());
        Date d = dateUnit.makeDate(timeVal);
        logger.debug("Date = {}", d.toString());
        return new Date[]{d};
    }
    
}
