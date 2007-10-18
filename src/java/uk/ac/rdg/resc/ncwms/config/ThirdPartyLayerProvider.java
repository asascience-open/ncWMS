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

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.load.PersistenceException;
import org.simpleframework.xml.load.Validate;

/**
 * A third-party server that provides layers to the Godiva2 site.  Note that
 * these layers are not integrated into ncWMS, so they do not appear in the
 * GetCapabilities document.  This class is used by
 * uk.ac.rdg.resc.ncwms.controller.MetadataController.
 *
 * Third-party servers can be other ncWMS servers or "plain" WMS servers.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="thirdpartylayerprovider")
public class ThirdPartyLayerProvider
{
    /**
     * The types of third party layer providers: ncWMS servers
     * or "plain" WMS servers
     */
    public static enum Type {NCWMS, WMS};
    
    @Attribute(name="title")
    private String title;
    
    @Attribute(name="type")
    private String strType;
    private Type type;
    
    @Attribute(name="url")
    private String url;
    
    // TODO: also have a VERSION string
    
    // For third-party plain WMSs we'll need a cache of Layer objects
    
    /**
     * Checks that the given type of the server is valid
     */
    @Validate
    public void checkType() throws PersistenceException
    {
        if (this.strType.equalsIgnoreCase("ncwms"))
        {
            this.type = Type.NCWMS;
        }
        else if (this.strType.equalsIgnoreCase("wms"))
        {
            this.type = Type.WMS;
        }
        else
        {
            throw new PersistenceException("Unknown type of third-party layer provider");
        }
    }

    public Type getType()
    {
        return type;
    }

    /**
     * The URL to the third party provider.  Also the unique identifier for the
     * server.
     */
    public String getUrl()
    {
        return url;
    }

    public String getTitle()
    {
        return title;
    }
    
}
