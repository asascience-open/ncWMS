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
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.rdg.resc.ncwms.config.Config;
import uk.ac.rdg.resc.ncwms.config.NcwmsContext;

/**
 * <p>Uses the <a href="http://ehcache.sf.net">EHCache</a> software to cache
 * arrays of data that have been extracted.  This cache reduces the load on the server
 * in cases where clients make the same requests for data multiple times.  This 
 * happens commonly when clients use a tiling WMS interface such as OpenLayers or
 * Google Maps.  Since the cache stores data arrays and not images, clients can
 * make a new request for the same data in a different style: the data will then
 * be loaded from the cache and styled.  This supports the useful functionality
 * of the Godiva2 website, which allows the user to change the colour scale
 * of an image to increase (or reduce) contrast.</p>
 *
 * <p>It is of course important to ensure that the cache remains consistent with
 * the underlying data.  We wish to avoid, as far as possible, the situation where
 * cached data are used incorrectly because the underlying data have changed.
 * The following measures are taken to preserve cache consistency:</p>
 * <ol>
 * <li>Each item in the cache will expire after a given time interval, which is
 * settable using the administrative interface (through the
 * {@link uk.ac.rdg.resc.ncwms.config.Config Config} class).</li>
 *
 * <li>If we know the exact file (on the local disk) that corresponds with the
 * given cache request, we check the last modified time and size of this file.
 * If either of these has changed then the cached data will not be used.  (This
 * check is achieved by including these quantities in the {@link TileCacheKey}).  This
 * mechanism is used when a dataset is either a single file or a glob aggregation.
 * It does not, however, work correctly for OPeNDAP datasets or NcML aggregations,
 * because we do not have access to the underlying data files in these cases.</li>
 *
 * <li>For OPeNDAP datasets and NcML aggregations, we check the last modified time
 * of the relevant {@link uk.ac.rdg.resc.ncwms.config.Dataset Dataset} object.  This
 * means that when the metadata for the dataset are re-loaded, all items in the cache
 * that come from this dataset become invalid.  This at least ensures that the cache remains
 * consistent with the metadata holdings.  It will often mean that items in the cache
 * become invalid even when the underlying data have not changed, but this is preferable
 * to a situation in which cached data are used incorrectly.  (The latter situation
 * is still possible but is made less likely by this mechanism.)</li>
 * </ol>
 *
 * <p>Items are never explicitly removed from the cache by the ncWMS code: ehcache
 * does the clean-up in a background thread using a least-recently-used (LRU)
 * algorithm.</p>
 *
 * <p>This object is created by the Spring framework, then injected into the
 * {@link uk.ac.rdg.resc.ncwms.controller.WmsController WmsController} and the
 * {@link uk.ac.rdg.resc.ncwms.controller.AdminController AdminController}.</p>
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class TileCache
{
    private static final Logger logger = LoggerFactory.getLogger(TileCache.class);
    
    private static final String CACHE_NAME = "tilecache";
    private CacheManager cacheManager;
    
    // Injected by Spring to provide the location of the working directory
    private NcwmsContext ncwmsContext;
    // Injected by Spring to provide the configuration of the server
    private Config ncwmsConfig;
    
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
        
        Cache tileCache = new Cache(
            CACHE_NAME,                                           // Name for the cache
            this.ncwmsConfig.getCache().getMaxNumItemsInMemory(), // Maximum number of elements in memory
            MemoryStoreEvictionPolicy.LRU,                        // evict least-recently-used elements
            this.ncwmsConfig.getCache().isEnableDiskStore(),      // Use the disk store?
            "",                                                   // disk store path (ignored)
            false,                                                // elements are not eternal
            this.ncwmsConfig.getCache().getElementLifetimeMinutes() * 60, // Elements will last for this number of seconds in the cache
            0,                                                    // Ignore time since last access/modification
            this.ncwmsConfig.getCache().isEnableDiskStore(),      // Will persist cache to disk in between JVM restarts
            1000,                                                 // number of seconds between clearouts of disk store
            null,                                                 // no registered event listeners
            null,                                                 // no bootstrap cache loader
            this.ncwmsConfig.getCache().getMaxNumItemsOnDisk()    // Maximum number of elements on disk
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
     * data matching the given key or of the cache is disabled.
     */
    public float[] get(TileCacheKey key)
    {
        if (!this.ncwmsConfig.getCache().isEnabled())
        {
            logger.debug("Tile cache is disabled");
            return null;
        }
        Cache cache = this.cacheManager.getCache(CACHE_NAME);
        Element el = cache.get(key);
        if (el == null)
        {
            logger.debug("Not found in cache: {}", key);
            return null;
        }
        else
        {
            logger.debug("Found in cache");
            return (float[])el.getValue();
        }
    }
    
    /**
     * Adds an array of data to this cache.  Does nothing if the cache is disabled.
     */
    public void put(TileCacheKey key, float[] data)
    {
        if (!this.ncwmsConfig.getCache().isEnabled())
        {
            logger.debug("Tile cache is disabled: data not put into cache");
            return;
        }
        this.cacheManager.getCache(CACHE_NAME).put(new Element(key, data));
        logger.debug("Data put into cache: {}", key);
    }
    
    /**
     * Called by Spring to set the context, which contains the location of
     * the working directory of the server.
     */
    public void setNcwmsContext(NcwmsContext ncwmsContext)
    {
        this.ncwmsContext = ncwmsContext;
    }

    /**
     * Called by Spring to set the configuration information for the server
     */
    public void setConfig(Config config)
    {
        this.ncwmsConfig = config;
    }
}
