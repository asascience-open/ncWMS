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

package uk.ac.rdg.resc.ncwms.usagelog;

import java.awt.Color;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapDataRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapRequest;
import uk.ac.rdg.resc.ncwms.controller.GetMapStyleRequest;
import uk.ac.rdg.resc.ncwms.metadata.Layer;

/**
 * Object that is passed to a UsageLogger to log usage of the ncWMS system.
 * Each request type is different and so not all of the fields in this class
 * will be populated
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class UsageLogEntry
{
    // These fields appear in every log entry
    private Date requestTime = new Date(); // The time at which the request was received
    private String clientIpAddress = null;
    private String clientHost = null;
    private String clientReferrer = null;
    private String clientUserAgent = null;
    private String httpMethod = null;
    private String exceptionClass = null; // Will be non-null if we are logging a failed operation
    private String exceptionMessage = null;
    private String wmsOperation = null;
    
    // These fields pertain to data requests
    private String crs = null;
    private double[] bbox = null;
    private String elevation = null;
    private Integer width = null;
    private Integer height = null;
    private Layer layer = null;
    private Long timeToExtractDataMs = null;
    private Boolean usedCache = false;
    
    // These fields pertain to how data are rendered
    private String outputFormat = null;
    private Boolean transparent = null;
    private String backgroundColor = null;

    /**
     * Sets the properties of this object that can be found from the
     * HTTP servlet request
     */
    public UsageLogEntry(HttpServletRequest httpServletRequest)
    {
        this.clientIpAddress = httpServletRequest.getRemoteAddr();
        this.clientHost = httpServletRequest.getRemoteHost();
        // Note mis-spelling of "referrer"!  Actually part of HTTP spec...
        this.clientReferrer = httpServletRequest.getHeader("Referer");
        this.clientUserAgent = httpServletRequest.getHeader("User-Agent");
        this.httpMethod = httpServletRequest.getMethod();
    }
    
    public void setException(Exception ex)
    {
        this.exceptionClass = ex.getClass().getName();
        this.exceptionMessage = ex.getMessage();
    }
    
    /**
     * Sets the properties of this object that come from the URL parameters 
     * of a GetMap request
     */
    public void setGetMapRequest(GetMapRequest getMapRequest)
    {
        GetMapDataRequest dr = getMapRequest.getDataRequest();
        this.crs = dr.getCrs();
        this.bbox = dr.getBbox();
        this.elevation = dr.getElevationString();
        this.width = dr.getWidth();
        this.height = dr.getHeight();
        // TODO: how deal with dr.getTimeString();?
        // Maybe log the string and the number of frames requested
        
        GetMapStyleRequest sr = getMapRequest.getStyleRequest();
        this.outputFormat = sr.getImageFormat();
        this.transparent = sr.isTransparent();
        Color bgColor = new Color(sr.getBackgroundColour());
        this.backgroundColor = bgColor.getRed() + "," + bgColor.getGreen() + ","
            + bgColor.getBlue();
        // TODO: how deal with sr.getStyles();?
        // Maybe just log the one style (we're only logging one layer after all)
    }
    
    public void setWmsOperation(String op)
    {
        this.wmsOperation = op;
    }
    
    public void setLayer(Layer layer)
    {
        this.layer = layer;
    }

    public void setTimeToExtractDataMs(long timeToExtractDataMs)
    {
        this.timeToExtractDataMs = timeToExtractDataMs;
    }

    public Date getRequestTime()
    {
        return requestTime;
    }

    public String getClientIpAddress()
    {
        return clientIpAddress;
    }

    public String getClientHost()
    {
        return clientHost;
    }

    public String getClientReferrer()
    {
        return clientReferrer;
    }

    public String getClientUserAgent()
    {
        return clientUserAgent;
    }

    public String getHttpMethod()
    {
        return httpMethod;
    }

    public String getExceptionClass()
    {
        return exceptionClass;
    }

    public String getExceptionMessage()
    {
        return exceptionMessage;
    }

    public String getWmsOperation()
    {
        return wmsOperation;
    }
}
