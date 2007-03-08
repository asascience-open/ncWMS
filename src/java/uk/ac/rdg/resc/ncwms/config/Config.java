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

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import org.apache.log4j.Logger;
import simple.xml.Element;
import simple.xml.ElementList;
import simple.xml.Root;
import simple.xml.Serializer;
import simple.xml.load.Commit;
import simple.xml.load.PersistenceException;
import simple.xml.load.Persister;
import simple.xml.load.Validate;

/**
 * Configuration of the server.  We use Simple XML Serialization
 * (http://simple.sourceforge.net/) to convert to and from XML.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
@Root(name="config")
public class Config
{
    private static final Logger logger = Logger.getLogger(Config.class);
    
    @Element(name="contact", required=false)
    private Contact contact;
    @Element(name="server")
    private Server server;
    @ElementList(name="users", type=User.class)
    private Vector<User> userList;
    @ElementList(name="datasets", type=Dataset.class)
    private Vector<Dataset> datasetList;
    
    private Date lastUpdateTime; // Time of the last update to this configuration
                                 // or any of the contained metadata
    
    /**
     * This contains the map of dataset IDs to Dataset objects
     */
    private Hashtable<String, Dataset> datasets;
    /**
     * This contains the map of usernames to User objects
     */
    private Hashtable<String, User> users;
    
    /** Creates a new instance of Config */
    public Config()
    {
        this.datasets = new Hashtable<String, Dataset>();
        this.users = new Hashtable<String, User>();
        this.lastUpdateTime = new Date();
        this.server = null;
        this.contact = new Contact();
    }
    
    /**
     * Reads configuration information from disk
     * @param configFilePath Full path to the configuration file
     * @throws Exception if there was an error reading the configuration
     * @todo create a new object if the config file doesn't already exist
     */
    public static Config readConfig(String configFilePath) throws Exception
    {
        Serializer serializer = new Persister();
        File configFile = new File(configFilePath);
        Config config = serializer.read(Config.class, configFile);
        logger.debug("Loaded configuration from {}", configFilePath);
        return config;
    }
    
    /**
     * Saves configuration information to the disk
     * @param configFilePath Full path to the configuration file
     * @throws Exception if there was an error reading the configuration
     */
    public void saveConfig(String configFilePath) throws Exception
    {
        Serializer serializer = new Persister();
        File configFile = new File(configFilePath);
        serializer.write(this, configFile);
    }
    
    /**
     * Checks that the data we have read are valid.  Checks that there are no
     * duplicate dataset IDs or usernames.
     */
    @Validate
    public void validate() throws PersistenceException
    {
        List<String> dsIds = new ArrayList<String>();
        for (Dataset ds : datasetList)
        {
            String dsId = ds.getId();
            if (dsIds.contains(dsId))
            {
                throw new PersistenceException("Duplicate dataset id %s", dsId);
            }
            dsIds.add(dsId);
        }
        List<String> usernames = new ArrayList<String>();
        for (User user : userList)
        {
            String username = user.getUsername();
            if (usernames.contains(username))
            {
                throw new PersistenceException("Duplicate username %s", username);
            }
            usernames.add(username);
        }
    }
    
    /**
     * Called when we have checked that the configuration is valid.  Populates
     * the datasets and users hashtables
     */
    @Commit
    public void build()
    {
        for (Dataset ds : datasetList)
        {
            this.datasets.put(ds.getId(), ds);
        }
        for (User user : userList)
        {
            this.users.put(user.getUsername(), user);
        }
    }
    
    /**
     * @return true if the server has been adequately configured.  This method
     * is used by the {@link GlobalFilter} to redirect users to an error page
     * if they try to use the server before it is configured.
     */
    public boolean isReady()
    {
        return this.getServer() != null; // TODO: check all the compulsory options
    }
    
    public Hashtable<String, Dataset> getDatasets()
    {
        return datasets;
    }
    
    public Hashtable<String, User> getUsers()
    {
        return users;
    }
    
    public void setLastUpdateTime(Date date)
    {
        this.lastUpdateTime = date;
    }
    
    public Date getLastUpdateTime()
    {
        return this.lastUpdateTime;
    }
    
    /**
     * @return the time of the last change to the configuration or metadata,
     * in seconds since the epoch
     */
    public double getLastUpdateTimeSeconds()
    {
        return this.lastUpdateTime.getTime() / 1000.0;
    }

    public Server getServer()
    {
        return server;
    }

    public void setServer(Server server)
    {
        this.server = server;
    }

    public Contact getContact()
    {
        return contact;
    }

    public void setContact(Contact contact)
    {
        this.contact = contact;
    }
    
}
