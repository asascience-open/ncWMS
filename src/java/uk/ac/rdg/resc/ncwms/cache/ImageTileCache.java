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

package uk.ac.rdg.resc.ncwms.cache;

import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import java.io.File;
import org.apache.log4j.Logger;

/**
 * Uses the Berkeley DB Java software to cache "image" tiles: in fact, this caches
 * the extracted data arrays that are used to create image tiles so that we can
 * change the styling without re-extracting the data.
 * @deprecated Has not kept up with other developments, so is not currently used.
 *
 * @todo Create a Comparator to sort in order of last accessed?
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class ImageTileCache
{
    private static final Logger logger = Logger.getLogger(ImageTileCache.class);
    
    private Environment env;
    private Database db;
    private TupleBinding keyBinding;
    private TupleBinding dataBinding;
    
    /**
     * Creates a new instance of ImageTileCache
     * @param dir The directory in which to create the cache (must exist)
     * @throws DatabaseException if the cache could not be created
     */
    public ImageTileCache(File dir) throws DatabaseException
    {
        // Create the Berkeley DB environment
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setReadOnly(false);
        envConfig.setAllowCreate(true);
        // TODO: set the size of the in-memory cache
        this.env = new Environment(dir, envConfig);
        
        // Create a database to hold the tiles
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(false); // Don't allow >1 entry with same key
        this.db = this.env.openDatabase(null, "tiles", dbConfig);
        
        this.keyBinding = new ImageTileKeyBinding();
        this.dataBinding = new ImageTileDataBinding();
        
        // TODO: create a Timer thread that cleans up old entries
        
        logger.debug("Created ImageTileCache in " + dir.getPath());
    }
    
    /**
     * Retrieves an image tile from the cache (remember this is actually the
     * data that is used to create the tile)
     * @param key The key for this tile
     * @return the image tile as an array of floats, or null if the tile
     * does not exist in the cache
     * @throws DatabaseException if an error occurred
     */
    public float[] getImageTile(ImageTileKey key) throws DatabaseException
    {
        DatabaseEntry dbKey = new DatabaseEntry();
        this.keyBinding.objectToEntry(key, dbKey);
        DatabaseEntry dbData = new DatabaseEntry();
        OperationStatus stat = this.db.get(null, dbKey, dbData, LockMode.DEFAULT);
        if (stat == OperationStatus.SUCCESS)
        {
            logger.debug("Tile found in cache with key: " + key);
            return (float[])this.dataBinding.entryToObject(dbData);
        }
        else
        {
            // TODO: log more info about the key
            logger.debug("No tile found in cache with key: " + key);
            return null;
        }
    }
    
    /**
     * Puts a data array in the cache
     * @param key The ImageTileKey to use to access the data array
     * @param data The data array
     * @throws DatabaseException if an error occurred
     */
    public void putImageTile(ImageTileKey key, float[] data) throws DatabaseException
    {
        DatabaseEntry dbKey = new DatabaseEntry();
        this.keyBinding.objectToEntry(key, dbKey);
        DatabaseEntry dbData = new DatabaseEntry();
        this.dataBinding.objectToEntry(data, dbData);
        logger.debug("Adding tile to cache with key: " + key);
        OperationStatus stat = this.db.put(null, dbKey, dbData);
        if (stat == OperationStatus.KEYEXIST)
        {
            logger.debug("Key already exists: " + key);
        }
    }
    
    /**
     * Closes the underlying database.  Any errors are written to the log files
     * and not thrown from this method.
     */
    public void close()
    {
        if (this.env != null)
        {
            try
            {
                this.db.close();
                this.env.close();
            }
            catch(DatabaseException dbe)
            {
                logger.error("Error closing ImageTileCache database", dbe);
            }
        }
    }
    
}
