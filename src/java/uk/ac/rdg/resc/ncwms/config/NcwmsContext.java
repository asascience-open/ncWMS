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
import org.apache.log4j.Logger;
import simple.xml.load.Persister;

/**
 * Contains information about the context of the ncWMS application, in particular
 * the location of the working directory, which will contain the configuration
 * file, metadata store and caches.  The location of this working directory
 * defaults to $HOME/.ncWMS and can be changed using WMS-servlet.xml.  Also
 * contains methods to save and load the server configuration.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NcwmsContext
{
    private static final Logger logger = Logger.getLogger(NcwmsContext.class);
    
    /**
     * The name of the config file in the ncWMS working directory
     */
    private static final String CONFIG_FILE_NAME = "config.xml";
    
    private File workingDirectory;
    private File configFile; // location of the configuration file
    
    /**
     * Creates a context based on the given directory.
     * @param workingDirectory java.io.File representing the working directory
     * @throws Exception if the directory does not exist and cannot be created
     */
    public NcwmsContext(File workingDirectory) throws Exception
    {
        createDirectory(workingDirectory);
        this.workingDirectory = workingDirectory;
        this.configFile = new File(this.workingDirectory, CONFIG_FILE_NAME);
    }
    
    /**
     * Creates a context based on the default directory ($HOME/.ncWMS)
     * @throws Exception if the directory does not exist and cannot be created
     */
    public NcwmsContext() throws Exception
    {
        this(new File(System.getProperty("user.home"), ".ncWMS"));
    }
    
    /**
     * @return a java.io.File representing the working directory, which is
     * guaranteed to exist and to be a directory (although not guaranteed to be empty)
     */
    public File getWorkingDirectory()
    {
        return this.workingDirectory;
    }
    /**
     * Reads configuration information from the file config.xml in the working
     * directory.  If the configuration file does not exist in the given location
     * it will be created.
     * @throws Exception if there was an error reading the configuration
     */
    public Config loadConfig() throws Exception
    {
        Config config;
        if (this.configFile.exists())
        {
            config = new Persister().read(Config.class, this.configFile);
            logger.debug("Loaded configuration from {}", configFile.getPath());
        }
        else
        {
            // We must make a new config file and save it
            config = new Config();
            saveConfig(config);
            logger.debug("Created new configuration object and saved to {}",
                this.configFile.getPath());
        }
        return config;
    }
    
    /**
     * Saves configuration information to the disk to the place it was last
     * saved
     * @throws Exception if there was an error reading the configuration
     * @throws IllegalStateException if the config file has not previously been
     * saved.
     */
    public void saveConfig(Config config) throws Exception
    {
        if (this.configFile == null)
        {
            throw new IllegalStateException("No location set for config file");
        }
        new Persister().write(config, this.configFile);
    }
    
    /**
     * Creates the working directory, throwing an Exception if the working directory
     * could not be created
     */
    private static void createDirectory(File dir) throws Exception
    {
        if (dir.exists())
        {
            if (dir.isDirectory())
            {
                return;
            }
            else
            {
                throw new Exception(dir.getPath() + 
                    " already exists but it is a regular file");
            }
        }
        else
        {
            boolean created = dir.mkdir();
            if (!created)
            {
                throw new Exception("Could not create working directory "
                    + dir.getPath());
            }
        }
    }
    
}
