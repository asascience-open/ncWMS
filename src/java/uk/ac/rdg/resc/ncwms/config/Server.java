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

package uk.ac.rdg.resc.ncwms.config;

import simple.xml.Element;
import simple.xml.Root;

/**
 * The part of the configuration file that pertains to the server itself.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="server")
public class Server
{
    @Element(name="title")
    private String title; // Title for this ncWMS
    @Element(name="allowFeatureInfo", required=false)
    private boolean allowFeatureInfo; // True if we allow the GetFeatureInfo operation globally
    @Element(name="maxImageWidth", required=false)
    private int maxImageWidth;
    @Element(name="maxImageHeight", required=false)
    private int maxImageHeight;
    @Element(name="abstract", required=false)
    private String abstr; // "Abstract" is a reserved word
    @Element(name="keywords", required=false)
    private String keywords; // Comma-separated list
    @Element(name="url", required=false)
    private String url;
    @Element(name="adminpassword")
    private String adminPassword;
    
    /**
     * Creates a new instance of Server
     */
    public Server()
    {
        this.title = "My ncWMS server";
        this.allowFeatureInfo = true;
        this.maxImageWidth = 1024;
        this.maxImageHeight = 1024;
        this.adminPassword = "ncWMS";
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public boolean isAllowFeatureInfo()
    {
        return allowFeatureInfo;
    }

    public void setAllowFeatureInfo(boolean allowFeatureInfo)
    {
        this.allowFeatureInfo = allowFeatureInfo;
    }

    public int getMaxImageWidth()
    {
        return maxImageWidth;
    }

    public void setMaxImageWidth(int maxImageWidth)
    {
        this.maxImageWidth = maxImageWidth;
    }

    public int getMaxImageHeight()
    {
        return maxImageHeight;
    }

    public void setMaxImageHeight(int maxImageHeight)
    {
        this.maxImageHeight = maxImageHeight;
    }

    public String getAbstract()
    {
        return abstr;
    }

    public void setAbstract(String abstr)
    {
        this.abstr = abstr;
    }

    public String getKeywords()
    {
        return keywords;
    }

    public void setKeywords(String keywords)
    {
        this.keywords = keywords;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getAdminPassword()
    {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword)
    {
        this.adminPassword = adminPassword;
    }
    
}
