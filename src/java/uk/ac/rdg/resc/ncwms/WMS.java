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

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import uk.ac.rdg.resc.ncwms.config.NcWMSConfig;
import uk.ac.rdg.resc.ncwms.exceptions.NcWMSConfigException;
import uk.ac.rdg.resc.ncwms.exceptions.OperationNotSupportedException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSException;
import uk.ac.rdg.resc.ncwms.exceptions.WMSInternalError;
import uk.ac.rdg.resc.ncwms.ogc.capabilities.WMSCapabilities;

/**
 * Description of WMS.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class WMS extends HttpServlet
{
    /**
     * The version of this WMS
     */
    public static String VERSION = "1.3.0";
    /**
     * Standard lon-lat coord system
     */
    public static String CRS_84 = "CRS:84";
    
    private NcWMSConfig config; // Configuration information
    
    /**
     * Initializes the servlet: loads the config file.
     * @param servletConfig The servlet configuration object
     * @throws ServletException if the servlet could not be initialized (e.g.
     * could not find config file)
     */
    public void init(ServletConfig servletConfig) throws ServletException
    {
        super.init(servletConfig);
        // Set up the default projections
        // Read the metadata for all the datasets
        
        // TODO: is this the most sensible place for the configuration files?
        InputStream is =
            servletConfig.getServletContext().getResourceAsStream("/WEB-INF/classes/ncWMS.xml");
        if (is == null)
        {
            throw new ServletException("Could not locate configuration file ncWMS.xml");
        }
        try
        {
            this.config = new NcWMSConfig(is);
        }
        catch(NcWMSConfigException nce)
        {
            // TODO: log the error here
            throw new ServletException(nce);
        }
    }
    
    /**
     * Processes GET request.
     * @param request servlet request
     * @param response servlet response
     * @todo should we throw all Exceptions as WMSExceptions to help
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        try
        {
            RequestParser reqParser = new RequestParser(request.getQueryString());
            if (!reqParser.getParameterValue("SERVICE").equals("WMS"))
            {
                throw new WMSException("SERVICE parameter must be WMS");
            }
            String req = reqParser.getParameterValue("REQUEST");
            if (req.equals("GetCapabilities"))
            {
                // Get the capabilities document
                WMSCapabilities wmsCap = GetCapabilities.getCapabilities(reqParser,
                    this.config, request.getRequestURL());
                // Marshal the object into XML and output to the client
                try
                {
                    JAXBContext context =
                        JAXBContext.newInstance("uk.ac.rdg.resc.ncwms.ogc.capabilities");
                    Marshaller marshaller = context.createMarshaller();
                    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                        "http://www.opengis.net/wms http://schemas.opengis.net/wms/1.3.0/capabilities_1_3_0.xsd");
                    PrintWriter out = response.getWriter();
                    response.setContentType("text/xml;charset=UTF-8");
                    marshaller.marshal(wmsCap, out);
                    out.close();
                }
                catch(JAXBException jaxbe)
                {
                    throw new ServletException(jaxbe);
                }
            }
            else if (req.equals("GetMap"))
            {
                GetMap.getMap(reqParser, this.config, response);
            }
            else if (req.equals("GetFeatureInfo"))
            {
                throw new OperationNotSupportedException(req);
            }
            else
            {
                throw new WMSException("Invalid operation");
            }
        }
        catch(WMSInternalError wmsie)
        {
            // TODO: in future we could wrap this as a WMSException to help
            // clients: for the moment we just throw it
            throw new ServletException(wmsie);
        }
        catch(WMSException wmse)
        {
            try
            {
                // Marshal the exception to XML and send to the client
                JAXBContext context =
                    JAXBContext.newInstance("uk.ac.rdg.resc.ncwms.ogc.exceptions");
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
                    "http://www.opengis.net/ogc http://schemas.opengis.net/wms/1.3.0/exceptions_1_3_0.xsd");
                PrintWriter out = response.getWriter();
                response.setContentType("text/xml;charset=UTF-8");
                marshaller.marshal(wmse.getServiceExceptionReport(), out);
                out.close();
            }
            catch(JAXBException jaxbe)
            {
                throw new ServletException(jaxbe);
            }
        }
    }
    
    /**
     * Handles the HTTP <code>POST</code> method.  Not supported.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        throw new ServletException("POST method is not supported on this server");
    }
    
    /**
     * @return a short description of the servlet.
     */
    public String getServletInfo()
    {
        return "Web Map Service for NetCDF data";
    }
}
