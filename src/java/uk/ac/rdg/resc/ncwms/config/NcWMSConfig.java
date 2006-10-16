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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Hashtable;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import uk.ac.rdg.resc.ncwms.dataprovider.DataProvider;
import uk.ac.rdg.resc.ncwms.exceptions.NcWMSConfigException;

/**
 * Object containing configuration information for this WMS.  This object is
 * created when the WMS servlet is initialized (WMS.init()).  
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
    private String fees;
    private String accessConstraints;
    private int layerLimit; // The maximum number of layers that can be
                            // read simultaneously
    private int maxWidth;   // The maximum width of a generated picture in pixels
    private int maxHeight;  // The maximum height of a generated picture in pixels
    
    // Maps unique IDs to corresponding DataProvider objects
    private Hashtable<String, DataProvider> dps;
    
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
            
            // Read the service metadata
            this.name = this.getServiceMetadata("name");
            this.title = this.getServiceMetadata("name");
            this.href = this.getServiceMetadata("href");
            this.fees = this.getServiceMetadata("fees", "none");
            this.accessConstraints =
                this.getServiceMetadata("accessConstraints", "none");
            this.layerLimit = this.getIntegerMetadataValue("layerLimit", 1);
            this.maxWidth = this.getIntegerMetadataValue("maxWidth", 1000);
            this.maxHeight = this.getIntegerMetadataValue("maxHeight", 1000);
        
            // Now read the list of datasets
            this.dps = new Hashtable<String, DataProvider>();
            for (Object dsNodeObj : this.doc.selectNodes("/ncWMS/datasets/dataset"))
            {
                Node dsNode = (Node)dsNodeObj;
                String dsID = getAttributeValue(dsNode, "id");
                String dsTitle = getAttributeValue(dsNode, "title");
                String dsLocation = getAttributeValue(dsNode, "location");
                if (this.dps.containsKey(dsID))
                {
                    throw new NcWMSConfigException("Dataset id " + dsID +
                        " is not unique");
                }
                try
                {
                    DataProvider provider =
                        DataProvider.create(dsID, dsTitle, dsLocation);
                    this.dps.put(dsID, provider);
                }
                catch(IOException ioe)
                {
                    throw new NcWMSConfigException("Could not read metadata from the \""
                        + dsTitle + "\" dataset");
                }
            }
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
    private String getServiceMetadata(String tag, String def)
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
    private String getServiceMetadata(String tag) throws NcWMSConfigException
    {
        return getServiceMetadata(tag, null);
    }
    
    /**
     * Gets a positive, non-zero integer from the given tag
     * @param tag The tag name from which to read the integer
     * @param def The default value if none is provided
     * @return the integer
     * @throws NcWMSConfigException if the integer was not valid or was not
     * positive, non-zero
     */
    private int getIntegerMetadataValue(String tag, int def)
        throws NcWMSConfigException
    {
        String val = this.getServiceMetadata(tag, "" + def);
        try
        {
            int i = Integer.parseInt(val);
            if (i < 1)
            {
                throw new NcWMSConfigException(tag + " must be postive and non-zero");
            }
            return i;
        }
        catch(NumberFormatException nfe)
        {
            throw new NcWMSConfigException("Invalid integer for " + tag);
        }
    }
    
    /**
     * Gets the value of the given attribute of the given node
     * @param node The node containing the attribute
     * @param att The name of the attribute
     * @return The value of the attribute
     * @throws NcWMSConfigException if the node does not contain the given
     * attribute or the attribute is empty
     * @todo Error messages are not as helpful as they could be: they should
     * identify the missing attribute more specifically
     */
    private static String getAttributeValue(Node node, String att)
        throws NcWMSConfigException
    {
        Node attNode = node.selectSingleNode("@" + att);
        if (attNode == null)
        {
            throw new NcWMSConfigException("Attribute " + att + " missing");
        }
        String attValue = attNode.getText().trim();
        if (attValue.equals(""))
        {
            throw new NcWMSConfigException("Attribute " + att + " must not be empty");
        }
        return attValue;
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

    public String getFees()
    {
        return this.fees;
    }

    public String getAccessConstraints()
    {
        return this.accessConstraints;
    }

    /**
     * @return the maximum number of layers that can be read simultaneously
     * from this server
     */
    public int getLayerLimit()
    {
        return this.layerLimit;
    }

    /**
     * @return The maximum width of a generated image in pixels
     */
    public int getMaxImageWidth()
    {
        return this.maxWidth;
    }

    /**
     * @return The maximum height of a generated image in pixels
     */
    public int getMaxImageHeight()
    {
        return this.maxHeight;
    }
    
    /**
     * @return a {@link Collection} of all the {@link DataProvider}s that will
     * provide data that is displayed by this WMS
     */
    public Collection<DataProvider> getDataProviders()
    {
        return this.dps.values();
    }
    
    /**
     * Gets the DataProvider with the given id
     * @param id The unique id of the DataProvider
     * @return the DataProvider, or null if it does not exist
     */
    public DataProvider getDataProvider(String id)
    {
        return this.dps.get(id);
    }
    
    public static void main(String[] args) throws Exception
    {
        FileInputStream fin =
            new FileInputStream("C:\\Documents and Settings\\jdb\\My Documents\\java\\ncWMS\\src\\java\\ncWMS.xml");
        NcWMSConfig conf = new NcWMSConfig(fin);
        System.out.println("Name: " + conf.getName());
        System.out.println("Title: " + conf.getTitle());
        System.out.println("Href: " + conf.getHref());
        System.out.println("Fees: " + conf.getFees());
        System.out.println("Access constraints: " + conf.getAccessConstraints());
        System.out.println("Layer Limit: " + conf.getLayerLimit());
        System.out.println("Max width: " + conf.getMaxImageWidth());
        System.out.println("Max height: " + conf.getMaxImageHeight());
    }
    
}
