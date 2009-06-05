/*
 * Copyright (c) 2009 The University of Reading
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

package uk.ac.rdg.resc.ncwms.metadata.lut;

import java.io.File;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;

/**
 * A cache for look-up tables.  LUTs are serialized onto disk.
 * @author Jon
 */
public class LutCache
{
    private static final Logger logger = LoggerFactory.getLogger(LutCache.class);
    private static final String CACHE_NAME = "lutcache";
    private CacheManager cacheManager; // TODO: should we share this with TileCache?

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
        diskStore.setPath(new File(this.ncwmsContext.getWorkingDirectory(), "lutcache").getPath());
        config.addDiskStore(diskStore);
        config.addDefaultCache(new CacheConfiguration());
        this.cacheManager = new CacheManager(config);

        Cache lutCache = new Cache(
            CACHE_NAME,                     // Name for the cache
            20,                             // Arbitrarily-chosen maximum number of elements in memory
            MemoryStoreEvictionPolicy.LRU,  // evict least-recently-used elements
            true,                           // Use the disk store
            "",                             // disk store path (ignored)
            true,                           // elements are eternal
            0,                              // Ignore time since creation date
            0,                              // Ignore time since last access/modification
            true,                           // Will persist cache to disk in between JVM restarts
            1000,                           // number of seconds between clearouts of disk store
                                            // (is this relevant since we have no maximum for number of disk items?)
            null,                           // no registered event listeners
            null,                           // no bootstrap cache loader
            0                               // No maximum number of elements on disk
        );

        this.cacheManager.addCache(lutCache);
        logger.info("LUT cache started");
    }

    /**
     * Gets look-up table from this cache, returning null if there is no
     * LUT matching the given key.
     */
    public LookUpTable get(LutCacheKey key)
    {
        Cache cache = this.cacheManager.getCache(CACHE_NAME);
        Element el = cache.get(key);
        if (el == null)
        {
            logger.debug("Not found in LUT cache: {}", key);
            return null;
        }
        else
        {
            logger.debug("Found in LUT cache");
            return (LookUpTable)el.getValue();
        }
    }

    /**
     * Adds a look-up table to this cache.
     */
    public void put(LutCacheKey key, LookUpTable lut)
    {
        this.cacheManager.getCache(CACHE_NAME).put(new Element(key, lut));
        logger.debug("Data put into LUT cache: {}", key);
    }

    /**
     * Called by Spring to shut down the cache
     */
    public void close()
    {
        this.cacheManager.shutdown();
        logger.info("LUT cache shut down");
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
