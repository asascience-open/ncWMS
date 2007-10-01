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
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * Contains information about the context of the ncWMS application, in particular
 * the location of the working directory, which will contain the configuration
 * file, metadata store and caches.  The location of this working directory
 * defaults to $HOME/.ncWMS and can be changed using WMS-servlet.xml.  Sets up
 * the logging system.  Also contains methods to save and load the server configuration.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class NcwmsContext implements ApplicationContextAware
{
    private static final Logger logger = Logger.getLogger(NcwmsContext.class);
    
    /**
     * The name of the config file in the ncWMS working directory
     */
    private static final String CONFIG_FILE_NAME = "config.xml";
    /**
     * The name of the log files directory in the working directory
     */
    private static final String LOG_FILE_DIR_NAME = "logs";
    /**
     * The name of the log file
     */
    private static final String LOG_FILE_NAME = "ncWMS.log";
    
    // The default working directory is in the user's home directory and can be
    // overridden by setting a new path in WMS-servlet.xml
    private File workingDirectory = new File(System.getProperty("user.home"), ".ncWMS");
    private File configFile; // location of the configuration file
    private ApplicationContext applicationContext; // Will be set by Spring
    
    private boolean initialized = false;
    
    public NcwmsContext()
    {
        // Set the location of the config file
        this.configFile = new File(this.workingDirectory, CONFIG_FILE_NAME);
    }
    
    /**
     * Does the actual initialization of the context: creates the necessary
     * directories and initializes the logging system
     * @throws Exception if there was an error in setup
     */
    public void init() throws Exception
    {
        // Create the working directory and the directory for log files
        WmsUtils.createDirectory(this.workingDirectory);
        File logDirectory = new File(this.workingDirectory, LOG_FILE_DIR_NAME);
        WmsUtils.createDirectory(logDirectory);
        File logFile = new File(logDirectory, LOG_FILE_NAME);
        
        // Set up the log4j logging system
        Properties logProps = new Properties();
        // Load properties from the config file
        Resource logConfig = this.applicationContext
            .getResource("/WEB-INF/conf/log4j.properties");
        logProps.load(logConfig.getInputStream());
        // Set the location of the log file: see /WEB-INF/conf/log4j.properties
        logProps.put("log4j.appender.R.File", logFile.getPath());
        PropertyConfigurator.configure(logProps);
        
        // Set the headings for the benchmark logger
        // Matches up with code in DefaultDataReader.read()
        Logger.getLogger("ncwms.benchmark").info
        (
            "Dataset," +
            "Variable," +
            "Class," +
            "Pixels in image," +
            "Num unique data points," +
            "Sum row lengths," +
            "Bounding box size," +
            "Time to extract data (ms)"
        );
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
     * Called by Spring to set the working directory
     * @throws IllegalArgumentException if the File does not represent an
     * absolute path
     */
    public void setWorkingDirectory(File workingDirectory)
    {
        if (!workingDirectory.isAbsolute())
        {
            throw new IllegalArgumentException("The working directory must be" +
                " an absolute path");
        }
        this.workingDirectory = workingDirectory;
        // Set the location of the config file
        this.configFile = new File(this.workingDirectory, CONFIG_FILE_NAME);
    }
    
    /**
     * @return a java.io.File representing the location of the config file.
     * Does not guarantee that this file exists
     */
    public File getConfigFile()
    {
        return this.configFile;
    }

    /**
     * Called automatically by Spring
     */
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }
    
}
