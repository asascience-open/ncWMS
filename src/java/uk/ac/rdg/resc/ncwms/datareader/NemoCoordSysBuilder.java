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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;
import ucar.nc2.NetcdfFile;
import ucar.nc2.VariableIF;
import ucar.nc2.dataset.CoordSysBuilder;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.util.CancelTask;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.vertical.VerticalTransform;

/**
 * Class that builds a coordinate system for NEMO data
 *
 * @author Jon Blower
     * $Revision$
 * $Date$
 * $Log$
 */
public class NemoCoordSysBuilder extends CoordSysBuilder
{
    private static final Logger logger = Logger.getLogger(NemoCoordSysBuilder.class);
    
    /**
     * Static method that returns true if the given NetcdfFile is recognised
     * by this coord sys builder.  This is an alternative to adding a "Conventions"
     * global attribute to the NetCDF file or NCML.
     */
    public static boolean isMine(NetcdfFile ncFile)
    {
        logger.debug("Checking file {} ...", ncFile.getLocation());
        boolean isMine = ncFile.getLocation().contains("NEMO");
        logger.debug("... returning {}", isMine);
        return isMine;
    }

    public void setConventionUsed(String string)
    {
        logger.debug("Called setConventionUsed({})", string);
    }

    public void addUserAdvice(String string)
    {
        logger.debug("Called addUserAdvice({})", string);
    }

    public void buildCoordinateSystems(NetcdfDataset netcdfDataset)
    {
        logger.debug("Called buildCoordinateSystems()");
        super.buildCoordinateSystems(netcdfDataset);
        
    }

    public void augmentDataset(NetcdfDataset netcdfDataset, CancelTask cancelTask) throws IOException
    {
        logger.debug("Called augmentDataset()");
        for (Object csObj : netcdfDataset.getCoordinateSystems())
        {
            CoordinateSystem cs = (CoordinateSystem)csObj;
            logger.debug(cs.toString());
        }
        
    }
    
}

/*class NemoCoordSys //implements GridCoordSys
{
    public NemoCoordSys()
    {
        
    }

    public int findTimeCoordElement(Date date)
    {
        return 0; //TODO
    }

    public String getLevelName(int i)
    {
        return null; //TODO
    }

    public String getTimeName(int i)
    {
        return null; // TODO
    }

    public int[] findXYCoordElement(double d, double d0, int[] i)
    {
        // TODO
    }

    public boolean isComplete(VariableIF variableIF)
    {
    }

    public boolean isZPositive()
    {
        return false;
    }

    public boolean isRegular()
    {
    }
    public boolean isProductSet()
    {
        return false;
    }

    public boolean isLatLon()
    {
    }

    public boolean isImplicit()
    {
    }

    public boolean isGeoXY()
    {
        return true;
    }

    public boolean isGeoReferencing()
    {
        return true;
    }

    public boolean isDate()
    {
        return true;
    }

    public boolean hasVerticalAxis()
    {
        return true;
    }

    public boolean hasTimeAxis()
    {
        return true;
    }

    public CoordinateAxis getZaxis()
    {
    }

    public CoordinateAxis getYaxis()
    {
    }

    public CoordinateAxis getYHorizAxis()
    {
    }

    public CoordinateAxis getXaxis()
    {
    }

    public String getName()
    {
    }

    public CoordinateAxis getLonAxis()
    {
    }

    public ArrayList getLevels()
    {
    }

    public LatLonRect getLatLonBoundingBox()
    {
        return new LatLonRect(new LatLonPointImpl(-90,-180), new LatLonPointImpl(90,180));
    }

    public CoordinateAxis getLatAxis()
    {
    }

    public CoordinateAxis getHeightAxis()
    {
    }

    public ArrayList getDomain()
    {
    }

    public ArrayList getCoordinateTransforms()
    {
        return new ArrayList();
    }

    public ArrayList getCoordinateAxes()
    {
    }

    public ProjectionRect getBoundingBox()
    {
    }

    public CoordinateAxis getPressureAxis()
    {
    }

    public ProjectionImpl getProjection()
    {
    }

    public int getRankDomain()
    {
    }

    public int getRankRange()
    {
    }

    public CoordinateAxis getTaxis()
    {
    }

    public CoordinateAxis1D getTimeAxis()
    {
    }

    public Date[] getTimeDates()
    {
    }

    public ArrayList getTimes()
    {
    }

    public CoordinateAxis1D getVerticalAxis()
    {
    }

    public VerticalTransform getVerticalTransform()
    {
    }

    public CoordinateAxis getXHorizAxis()
    {
    }
}*/
