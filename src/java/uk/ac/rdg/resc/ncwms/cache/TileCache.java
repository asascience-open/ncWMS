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

import java.io.File;
import java.io.Serializable;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;

/**
 * <p>Uses the <a href="http://ehcache.sf.net">EHCache</a> software to cache
 * "image" tiles: in fact, this caches
 * the extracted data arrays that are used to create image tiles so that we can
 * change the styling without re-extracting the data.</p>
 *
 * <p>This object is created by the Spring framework, then injected into the
 * {@link uk.ac.rdg.resc.ncwms.controller.WmsController WmsController}.</p>
 *
 * <p><b>NOT YET WORKING!!</b></p>
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class TileCache
{
    private static final Logger logger = Logger.getLogger(TileCache.class);
    
    private static final String CACHE_NAME = "tilecache";
    private CacheManager cacheManager;
    
    // Injected by Spring to provide the location of the working directory
    private NcwmsContext ncwmsContext;
    
    /**
     * Called by the Spring framework to initialize the cache.  This will be
     * called after all the set() methods have been called.
     */
    public void init()
    {
        // Setting the location of the disk store programmatically is tedious,
        // requiring the creation of lots of objects...
        Configuration config = new Configuration();
        DiskStoreConfiguration diskStore = new DiskStoreConfiguration();
        diskStore.setPath(new File(ncwmsContext.getWorkingDirectory(), "tilecache").getPath());
        config.addDiskStore(diskStore);
        config.addDefaultCache(new CacheConfiguration());
        this.cacheManager = new CacheManager(config);
        
        // Each 256*256 tile will take up 262144 bytes.
        // Default is to allocate 50MB of memory and 500MB of disk ~ 200 and 2000 elements
        // respectively.  TODO make this configurable
        Cache tileCache = new Cache(
            CACHE_NAME,                    // Name for the cache
            200,                           // Maximum number of elements in memory
            MemoryStoreEvictionPolicy.LRU, // evict least-recently-used elements
            true,                          // Use the disk store
            "",                            // disk store path (ignored)
            false,                         // elements are not eternal
            60 * 60 * 24,                  // Elements will last for 1 day in the cache
            0,                             // Ignore time since last access/modification
            true,                          // Will persist cache to disk in between JVM restarts
            1000,                          // number of seconds between clearouts of disk store
            null,                          // no registered event listeners
            null,                          // no bootstrap cache loader
            2000                           // Maximum number of elements on disk
        );
        
        this.cacheManager.addCache(tileCache);
        logger.info("Tile cache started");
    }
    
    /**
     * Called by Spring to shut down the cache
     */
    public void close()
    {
        this.cacheManager.shutdown();
        logger.info("Tile cache shut down");
    }
    
    /**
     * Gets an array of data from this cache, returning null if there is no
     * data matching the given key.
     */
    public float[] get(TileCacheKey key)
    {
        Cache cache = this.cacheManager.getCache(CACHE_NAME);
        Element el = cache.get(key);
        if (el == null)
        {
            logger.debug("Not found in cache");
            return null;
        }
        else
        {
            logger.debug("Found in cache");
            return (float[])el.getValue();
        }
    }
    
    /**
     * Adds an array of data to this cache.
     */
    public void put(TileCacheKey key, float[] data)
    {
        this.cacheManager.getCache(CACHE_NAME).put(new Element(key, data));
        logger.debug("Data put into cache");
    }
    
    /**
     * Called by Spring to set the context, which contains the location of
     * the working directory of the server.
     */
    public void setNcwmsContext(NcwmsContext ncwmsContext)
    {
        this.ncwmsContext = ncwmsContext;
    }
}
