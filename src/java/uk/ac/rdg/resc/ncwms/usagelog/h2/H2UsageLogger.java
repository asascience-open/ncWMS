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

package uk.ac.rdg.resc.ncwms.usagelog.h2;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.h2.tools.RunScript;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogEntry;
import uk.ac.rdg.resc.ncwms.usagelog.UsageLogger;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 * UsageLogger that stores data in an H2 database.  The database is run in 
 * embedded mode, i.e. it is private to the ncWMS application.  Note that the H2
 * database is thread-safe so we make no attempt at thread safety here.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class H2UsageLogger implements UsageLogger
{
    /**
     * The log4j logging system
     */
    private static final Logger logger = Logger.getLogger(H2UsageLogger.class);
    
    private Connection conn;
    
    // These properties will be injected by Spring
    private NcwmsContext ncwmsContext;
    
    /**
     * Called by Spring to initialize the database
     * @throws Exception if the database could not be initialized
     */
    public void init() throws Exception
    {
        File usageLogDir = new File(this.ncwmsContext.getWorkingDirectory(), "usagelog");
        // This will create the directory if it doesn't exist, throwing an
        // Exception if there was an error
        WmsUtils.createDirectory(usageLogDir);
        String databasePath = new File(usageLogDir, "usagelog").getCanonicalPath();
        
        // Load the SQL script file that initializes the database.
        // This script file does nothing if the database is already populated
        InputStream scriptIn =
            Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("/uk/ac/rdg/resc/ncwms/usagelog/h2/init.sql");
        if (scriptIn == null)
        {
            throw new Exception("Can't find initialization script init.sql");
        }
        Reader scriptReader = new InputStreamReader(scriptIn);
        
        try
        {
            // Load the database driver
            Class.forName("org.h2.Driver");
            // Get a connection to the database
            this.conn = DriverManager.getConnection("jdbc:h2:" + databasePath);
            // Set auto-commit to true: can't see any reason why not
            this.conn.setAutoCommit(true);

            // Now run the script to initialize the database
            RunScript.execute(this.conn, scriptReader);
        }
        catch(Exception e)
        {
            // Make sure we clean up before closing
            this.close();
            throw e;
        }
        
        logger.info("H2 Usage Logger initialized");
    }
    
    /**
     * Make an entry in the usage log.  This method does not throw an
     * Exception: all problems with the usage logger must be recorded
     * in the log4j text log.  Implementing methods should make sure they
     * set the time to process the request, by taking System.currentTimeMs()
     * and subtracting logEntry.getRequestTime().
     */
    public void logUsage(UsageLogEntry logEntry)
    {
        // Calculate the time to process the request
        long timeToProcessRequest =
            System.currentTimeMillis() - logEntry.getRequestTime().getTime();
        String insertCommand = "INSERT INTO usage_log(request_time, client_ip, " +
            "client_hostname, client_referrer, client_user_agent, http_method, wms_operation) " +
            "VALUES(?,?,?,?,?,?,?)";
        try
        {
            PreparedStatement ps = this.conn.prepareStatement(insertCommand);
            ps.setDate(1, new java.sql.Date(logEntry.getRequestTime().getTime()));
            ps.setString(2, logEntry.getClientIpAddress());
            ps.setString(3, logEntry.getClientHost());
            ps.setString(4, logEntry.getClientReferrer());
            ps.setString(5, logEntry.getClientUserAgent());
            ps.setString(6, logEntry.getHttpMethod());
            ps.setString(7, logEntry.getWmsOperation());
            ps.executeUpdate();
        }
        catch(SQLException sqle)
        {
            logger.error("Error writing to usage log", sqle);
        }
    }
    
    /**
     * Called by Spring to clean up the database
     */
    public void close()
    {
        try
        {
            this.conn.close();
        }
        catch(SQLException sqle)
        {
            logger.error("Error closing H2 Usage Logger", sqle);
        }
        logger.info("H2 Usage Logger closed");
    }

    /**
     * Will be called by Spring to set the context, which is used to find a
     * place to put the database
     */
    public void setNcwmsContext(NcwmsContext ncwmsContext)
    {
        this.ncwmsContext = ncwmsContext;
    }
    
}
