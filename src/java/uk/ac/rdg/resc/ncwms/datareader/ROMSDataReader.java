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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.metadata.LUTCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;

/**
 * DataReader for ROMS data from Damian Smyth (damian.smyth@marine.ie)
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class ROMSDataReader extends USGSDataReader
{
    private static final Logger logger = Logger.getLogger(ROMSDataReader.class);
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location, which is the location of a NetCDF file, NcML
     * aggregation, or OPeNDAP location (i.e. one element resulting from the
     * expansion of a glob aggregation).
     * @param filename Full path to the dataset (N.B. not an aggregation)
     * @return List of {@link Layer} objects
     * @throws IOException if there was an error reading from the data source
     */
    protected List<Layer> getLayers(String filename) throws IOException
    {
        logger.debug("Reading metadata for dataset {}", filename);
        List<Layer> layers = new ArrayList<Layer>();
        
        NetcdfDataset nc = null;
        try
        {
            // We use openDataset() rather than acquiring from cache
            // because we need to enhance the dataset
            nc = NetcdfDataset.openDataset(filename, true, null);
            GridDataset gd = new GridDataset(nc);
            for (Iterator it = gd.getGrids().iterator(); it.hasNext(); )
            {
                GeoGrid gg = (GeoGrid)it.next();
                if (!gg.getName().equals("temp") && !gg.getName().equals("salt")
                && !gg.getName().equals("latent") && !gg.getName().equals("sensible")
                && !gg.getName().equals("lwrad") && !gg.getName().equals("evaporation"))
                {
                    // Only display a few datasets for the moment
                    continue;
                }
                GridCoordSys coordSys = gg.getCoordinateSystem();
                logger.debug("Creating new Layer object for {}", gg.getName());
                LayerImpl layer = new LayerImpl();
                layer.setId(gg.getName());
                layer.setTitle(getStandardName(gg.getVariable().getOriginalVariable()));
                layer.setAbstract(gg.getDescription());
                layer.setUnits(gg.getUnitsString());
                layer.setXaxis(LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/LUT_ROMS_1231_721.zip/LUT_i_1231_721.dat"));
                layer.setYaxis(LUTCoordAxis.createAxis("/uk/ac/rdg/resc/ncwms/metadata/LUT_ROMS_1231_721.zip/LUT_j_1231_721.dat"));
                
                if (coordSys.hasVerticalAxis())
                {
                    CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                    layer.setZunits(zAxis.getUnitsString());
                    double[] zVals = zAxis.getCoordValues();
                    layer.setZpositive(false);
                    layer.setZvalues(zVals);
                }
                
                // Set the bounding box
                // TODO: should take into account the cell bounds
                LatLonRect latLonRect = coordSys.getLatLonBoundingBox();
                LatLonPoint lowerLeft = latLonRect.getLowerLeftPoint();
                LatLonPoint upperRight = latLonRect.getUpperRightPoint();
                double minLon = lowerLeft.getLongitude();
                double maxLon = upperRight.getLongitude();
                double minLat = lowerLeft.getLatitude();
                double maxLat = upperRight.getLatitude();
                if (latLonRect.crossDateline())
                {
                    minLon = -180.0;
                    maxLon = 180.0;
                }
                layer.setBbox(new double[]{minLon, minLat, maxLon, maxLat});
                
                // Now add the timestep information to the VM object
                Date[] tVals = this.getTimesteps(nc, gg);
                for (int i = 0; i < tVals.length; i++)
                {
                    TimestepInfo tInfo = new TimestepInfo(tVals[i], filename, i);
                    layer.addTimestepInfo(tInfo);
                }
                // Add this to the List
                layers.add(layer);
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
        return layers;
    }
    
}
