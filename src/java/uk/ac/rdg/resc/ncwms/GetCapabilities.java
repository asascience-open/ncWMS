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

package uk.ac.rdg.resc.ncwms;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.nc2.units.DateFormatter;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import uk.ac.rdg.resc.ncwms.config.NcWMS;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.BoundingBox;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.Capability;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.DCPType;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.Dimension;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.EXGeographicBoundingBox;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.Exception;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.Get;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.HTTP;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.Layer;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.OnlineResource;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.OperationType;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.Request;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.Service;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.WMSCapabilities;
import uk.ac.rdg.resc.ncwms.proj.RequestCRSFactory;

/**
 * Implements the GetCapabilities operation.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class GetCapabilities
{
    
    /**
     * The getCapabilities operation
     * @param reqParser The RequestParser that was constructed from the query string
     * @param config Configuration object for this WMS
     * @param requestURL StringBuffer containing the URL (minus query string)
     * that was used to request this Capabilities document
     * @return a WMSCapabilities object, ready to be serialized into XML
     * @throws IOException if there was an error reading metadata from the datasets
     */
    public static WMSCapabilities getCapabilities(RequestParser reqParser,
        NcWMS config, StringBuffer requestURL) throws IOException
    {
        // TODO Deal with VERSION, FORMAT and UPDATESEQUENCE
        WMSCapabilities wmsCap = new WMSCapabilities();
        wmsCap.setVersion(WMS.VERSION);
        wmsCap.setService(getService(config));
        wmsCap.setCapability(getCapability(config, requestURL));
        return wmsCap;
    }
    
    /**
     * Gets the Service portion of the Capabilities document
     * @param config Configuration object
     * @return the Service portion of the Capabilities document
     * @todo get this stuff from a configuration file or elsewhere in source code
     */
    private static Service getService(NcWMS config)
    {
        Service service = new Service();
        service.setName(config.getService().getName());
        service.setTitle(config.getService().getTitle());
        service.setFees(config.getService().getFees());
        service.setAccessConstraints(config.getService().getAccessConstraints());
        service.setLayerLimit(config.getService().getLayerLimit());
        service.setMaxHeight(config.getService().getMaxHeight());
        service.setMaxWidth(config.getService().getMaxWidth());
        OnlineResource or = new OnlineResource();
        or.setType("simple");
        or.setHref(config.getService().getHref());
        service.setOnlineResource(or);
        return service;
    }
    
    /**
     * Gets the Capability portion of the Capabilities document
     * @param config Configuration object for this WMS
     * @param requestURL StringBuffer containing the URL (minus query string)
     * @return the Capability portion of the Capabilities document
     * @throws IOException if there was an error reading metadata from the source
     * NetCDF files
     * @todo get these properties from config files or elsewhere
     */
    private static Capability getCapability(NcWMS config, StringBuffer requestURL)
        throws IOException
    {
        Capability cap = new Capability();
        
        Request req = new Request();
        OperationType getCapOp = new OperationType();
        getCapOp.getFormat().add("XML");
        DCPType httpDCPType = getHttpDCPType(requestURL);
        getCapOp.getDCPType().add(httpDCPType);
        OperationType getMapOp = new OperationType();
        getMapOp.getFormat().add("image/png");
        getMapOp.getDCPType().add(httpDCPType);
        req.setGetCapabilities(getCapOp);
        req.setGetMap(getMapOp);
        
        Exception ex = new Exception();
        ex.getFormat().add("XML");
        
        Layer rootLayer = new Layer();
        rootLayer.setTitle("Web Map Server"); // TODO get the title from somewhere
        
        // Get the list of supported CRSs
        for (String crs : RequestCRSFactory.getSupportedCRSCodes())
        {
            rootLayer.getCRS().add(crs);
        }

        // TODO: Style
        
        // Add each dataset as a new Layer hierarchy
        for (Iterator it = config.getDatasets().getDataset().iterator(); it.hasNext(); )
        {
            NcWMS.Datasets.Dataset ds = (NcWMS.Datasets.Dataset)it.next();
            rootLayer.getLayer().add(getLayer(ds));
        }
        
        cap.setRequest(req);
        cap.setException(ex);
        cap.setLayer(rootLayer);
        return cap;
    }
    
    /**
     * @param requestURL StringBuffer containing the URL (minus query string)
     * @return a DCPType describing how to access operations via HTTP
     */
    private static DCPType getHttpDCPType(StringBuffer requestURL)
    {
        DCPType httpDcp = new DCPType();
        HTTP http = new HTTP();
        Get get = new Get();
        OnlineResource or = new OnlineResource();
        or.setType("simple");
        or.setHref(requestURL.toString() + "?");
        get.setOnlineResource(or);
        http.setGet(get);
        httpDcp.setHTTP(http);
        return httpDcp;
    }
    
    /**
     * Reads all the metadata from the given Dataset and outputs it as a Layer object,
     * ready to be incorporated into a Capabilities document.
     * @param the Dataset object
     * @return the Layer object
     * @throws IOException if the dataset could not be read
     */
    private static Layer getLayer(NcWMS.Datasets.Dataset ds) throws IOException
    {
        // Open the dataset TODO: get from cache?
        NetcdfDataset nc = NetcdfDataset.openDataset(ds.getLocation());
        // Wrapping as a GridDataset allows us to get at georeferencing
        GridDataset gd = new GridDataset(nc);
        
        Layer topLayer = new Layer();
        topLayer.setTitle(ds.getTitle());
        
        // The Javadocs say we should create one DateFormatter per thread - why?
        DateFormatter dateFormatter = new DateFormatter();
        // Get a regular lon-lat projection (CRS:84)
        LatLonProjection proj = new LatLonProjection();
        
        for (Iterator iter = gd.getGrids().iterator(); iter.hasNext(); )
        {
            // A GeoGrid is essentially a georeferenced Variable
            GeoGrid var = (GeoGrid)iter.next();
            
            // Create a new Layer for this variable
            Layer layer = new Layer();
            layer.setTitle(var.getDescription());
            // var.getName() is unique within the dataset
            layer.setName(ds.getId() + "/" + var.getName());
            
            // Get the coordinate system for this variable
            // TODO make more efficient: some variables will share coordinate systems
            GridCoordSys coordSys = var.getCoordinateSystem();
            
            // Get the lat-lon bounding box for this variable
            LatLonRect bbox = coordSys.getLatLonBoundingBox();
            ProjectionRect projRect =  proj.latLonToProjBB(bbox);
            ProjectionPoint lowerLeft = projRect.getLowerLeftPoint();
            ProjectionPoint upperRight = projRect.getUpperRightPoint();
            EXGeographicBoundingBox exBbox = new EXGeographicBoundingBox();
            exBbox.setWestBoundLongitude(lowerLeft.getX());
            exBbox.setEastBoundLongitude(upperRight.getX());
            exBbox.setSouthBoundLatitude(lowerLeft.getY());
            exBbox.setNorthBoundLatitude(upperRight.getY());
            layer.setEXGeographicBoundingBox(exBbox);
            // Also need to add as a BoundingBox element for some reason
            BoundingBox crsBbox = new BoundingBox();
            crsBbox.setCRS(WMS.CRS_84);
            crsBbox.setMinx(exBbox.getWestBoundLongitude());
            crsBbox.setMaxx(exBbox.getEastBoundLongitude());
            crsBbox.setMiny(exBbox.getSouthBoundLatitude());
            crsBbox.setMaxy(exBbox.getNorthBoundLatitude());
            layer.getBoundingBox().add(crsBbox);
            
            // Set the level dimension
            if (coordSys.hasVerticalAxis())
            {
                Dimension level = new Dimension();
                CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                level.setName("elevation");
                level.setUnits(zAxis.getUnitsString());
                double[] zValues = zAxis.getCoordValues();
                StringBuffer buf = new StringBuffer();
                boolean firstTime = true;
                for (double z : zValues)
                {
                    if (firstTime) firstTime = false;
                    else buf.append(",");
                    buf.append(coordSys.isZPositive() ? z : -z);
                }
                level.setValue(buf.toString());
                // The default value is the first value in the axis
                level.setDefault("" + (coordSys.isZPositive() ? zValues[0] : -zValues[0]));
                layer.getDimension().add(level);
            }
            
            // Set the time dimension
            if (coordSys.isDate())
            {
                Dimension time = new Dimension();
                time.setName("time");
                time.setUnits("ISO8601");
                Date[] dates = coordSys.getTimeDates();
                StringBuffer buf = new StringBuffer();
                boolean firstTime = true;
                for (Date date : dates)
                {
                    if (firstTime) firstTime = false;
                    else buf.append(",");
                    buf.append(dateFormatter.toDateTimeStringISO(date));
                }
                time.setValue(buf.toString());
                layer.getDimension().add(time);
            }
            
            // Add this layer to the top-level Layer
            topLayer.getLayer().add(layer);
        }
        
        gd.close();
        return topLayer;
    }
    
}
