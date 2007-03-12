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
        this.filterConfig = filterConfig;
        
        // Load the Log4j configuration file
        String log4jfile = this.filterConfig.getInitParameter("log4j-init-file");
        if (log4jfile != null)
        {
            String prefix = this.filterConfig.getServletContext().getRealPath("/");
            PropertyConfigurator.configure(prefix + log4jfile);
            logger.debug("Logging system initialized");
        }
        
        // Load the ncWMS configuration object
        try
        {
            File userHome = new File(System.getProperty("user.home"));
            File ncWMSDir = new File(userHome, ".ncWMS");
            if (ncWMSDir.exists())
            {
                if (ncWMSDir.isFile())
                {
                    throw new Exception(ncWMSDir.getPath() +
                        " exists but is not a directory");
                }
            }
            else
            {
                ncWMSDir.mkdir();
            }
            File configFile = new File(ncWMSDir, "config.xml");
            try
            {
                this.config = Config.readConfig(configFile);
            }
            catch(Exception e)
            {
                logger.warn("Could not load configuration from " + configFile.getPath(), e);
                // Create a blank Config object using default values
                this.config = new Config();
                this.config.saveConfig(configFile);
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
        
        logger.debug("GlobalFilter initialized");
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
        logger.debug("GlobalFilter destroyed");
    }
    
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException
    {
        logger.debug("Called GlobalFilter.doFilter()");
        
        // TODO: check to see if the config file has been updated manually since
        // the last reload?  Don't forget to free up any resources if so.
        
        // Check that we have a configuration loaded and redirect to an
        // error page if not.
        if (this.config.isReady())
        {
            chain.doFilter(request, response);
        }
        else
        {
            // TODO: display the reasons why the config is not complete
            ((HttpServletResponse)response).sendRedirect("admin/servernotready.html");
        }
    }
    
}
