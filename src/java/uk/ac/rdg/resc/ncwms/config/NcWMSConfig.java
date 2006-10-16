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

package uk.ac.rdg.resc.ncwms.config;

import java.io.FileInputStream;
import java.io.InputStream;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import uk.ac.rdg.resc.ncwms.exceptions.NcWMSConfigException;

/**
 * Object containing configuration information for this WMS.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NcWMSConfig
{
    private Document doc; // The Document representing the config file
    private String name;  // The name of this Web Map Service
    private String title; // The human-readable title of this WMS
    private String href;  // URL of the organization running the WMS
    
    /**
     * Creates a new NcWMSConfig object
     * @param in An {@link InputStream} from which the XML configuration 
     * information will be read
     * @throws NcWMSConfigException if the config file could not be parsed or
     * a compulsory piece of config information was missing
     * 
     */
    public NcWMSConfig(InputStream in) throws NcWMSConfigException
    {
        SAXReader reader = new SAXReader();
        try
        {
            this.doc = reader.read(in);
            this.name = this.getServiceMetadata("name");
            this.title = this.getServiceMetadata("name");
            this.href = this.getServiceMetadata("href");
        }
        catch (DocumentException de)
        {
            throw new NcWMSConfigException("Could not parse config file", de);
        }
    }
    
    /**
     * Gets a piece of metadata from the <service> section of the config file.
     * @param tag The name of the tag from which to extract the metadata
     * @param def A default value for the tag if none is provided (set this null
     * to make the metadata item compulsory)
     * @return the piece of metadata
     * @throws NcWMSConfigException if a piece of metadata was missing and no
     * default was provided (i.e. the default was null)
     */
    public String getServiceMetadata(String tag, String def)
        throws NcWMSConfigException
    {
        Node node = this.doc.selectSingleNode("/ncWMS/service/" + tag);
        if (node == null)
        {
            if (def == null)
            {
                throw new NcWMSConfigException("Compulsory tag " + tag +
                    " was not found in the service section of the config file");
            }
            else
            {
                return def;
            }
        }
        else
        {
            return node.getText();
        }
    }
    
    /**
     * Gets a piece of metadata from the <service> section of the config file.
     * @param tag The name of the tag from which to extract the metadata
     * @return the piece of metadata
     * @throws NcWMSConfigException if the piece of metadata was missing
     */
    public String getServiceMetadata(String tag)
        throws NcWMSConfigException
    {
        return getServiceMetadata(tag, null);
    }
    
    /**
     * @return the name of this Web Map Service
     */
    public String getName()
    {
        return this.name;
    }
    
    /**
     * @return the human-readable title of this Web Map Service
     */
    public String getTitle()
    {
        return this.title;
    }
    
    /**
     * @return the URL of the organization that runs this WMS
     */
    public String getHref()
    {
        return this.href;
    }
    
    public static void main(String[] args) throws Exception
    {
        FileInputStream fin =
            new FileInputStream("E:\\Jon's Documents\\work\\java\\ncWMS\\src\\java\\ncWMS.xml");
        NcWMSConfig conf = new NcWMSConfig(fin);
        System.out.println("Name: " + conf.getName());
        System.out.println("Title: " + conf.getTitle());
        System.out.println("Href: " + conf.getHref());
    }
    
}
