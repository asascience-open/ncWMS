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

package uk.ac.rdg.resc.ncwms.filters;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.Dataset;

/**
 * Filters all requests to the WMS.  This also gives a place to initialize
 * global objects such as the Config object and the logging system.  This does
 * not filter requests to the administrative pages (JSPs).
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class WMSFilter implements Filter
{
    private static final Logger logger = Logger.getLogger(WMSFilter.class);
    
    private FilterConfig filterConfig = null;
    private Config config = null; // the ncWMS configuration information
    private Timer timer = null;
    
    /**
     * This gets called exactly once, when the webapp is initialized.  We can
     * initialize the logging system and other global objects here.
     */
    public void init(FilterConfig filterConfig) throws ServletException
    {
        System.out.println("FilterConfig = " + filterConfig);
        this.filterConfig = filterConfig;
        
        try
        {
            // Create the directory structure in the user's home space
            File userHome = new File(System.getProperty("user.home"));
            File ncWMSDir = new File(userHome, ".ncWMS");
            mkdir(ncWMSDir);
            File logDir = new File(ncWMSDir, "log");
            mkdir(logDir);
        
            // Load the Log4j configuration file
            String log4jfile = this.filterConfig.getInitParameter("log4j-init-file");
            if (log4jfile != null)
            {
                String prefix = this.filterConfig.getServletContext().getRealPath("/");
                PropertyConfigurator.configure(prefix + log4jfile);
                logger.debug("Logging system initialized");
            }
            
            // Load the ncWMS configuration object
            File configFile = new File(ncWMSDir, "config.xml");
            try
            {
                this.config = Config.readConfig(configFile);
            }
            catch(FileNotFoundException e)
            {
                logger.warn("Configuration file does not exist " + configFile.getPath(), e);
                // Create a blank Config object using default values
                this.config = new Config();
                this.config.setConfigFile(configFile);
                this.config.saveConfig();
            }
            // Store in the servlet context
            this.filterConfig.getServletContext().setAttribute("config", this.config);
            logger.debug("Read ncWMS configuration information");
        }
        catch(Exception e)
        {
            throw new ServletException(e);
        }
        
        // Now start the regular TimerTask that periodically checks to see if
        // the datasets need reloading
        this.timer = new Timer("Dataset reloader", true);
        // TODO: read this interval from an init-param
        int intervalMs = 60 * 1000; // Check every minute
        this.timer.schedule(new DatasetReloader(), 0, intervalMs);
        
        logger.debug("WMSFilter initialized");
    }
    
    /**
     * Creates a directory or throws an Exception if there was an error
     */
    private static void mkdir(File dir) throws Exception
    {
        if (dir.exists())
        {
            if (!dir.isDirectory())
            {
                throw new Exception(dir.getPath() + 
                    " exists but is not a directory");
            }
        }
        else
        {
            boolean created = dir.mkdir();
            if (!created)
            {
                throw new Exception("Could not create " + dir.getPath());
            }
        }
    }
    
    /**
     * Task that runs periodically, refreshing the metadata catalogue
     */
    private class DatasetReloader extends TimerTask
    {
        public void run()
        {
            logger.debug("Checking to see if datasets need reloading...");
            for (Dataset ds : config.getDatasets().values())
            {
                if (ds.needsRefresh())
                {
                    ds.loadMetadata();
                    config.setLastUpdateTime(new Date());
                }
            }
        }
    }
    
    public void destroy()
    {
        this.timer.cancel();
        this.config = null;
        this.filterConfig = null;
        logger.debug("WMSFilter destroyed");
    }
    
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException
    {
        logger.debug("Called WMSFilter.doFilter()");
        
        // TODO: check to see if the config file has been updated manually since
        // the last reload?  Don't forget to free up any resources if so.
        
        chain.doFilter(request, response);
    }
    
}
