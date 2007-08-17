/*
 * ThreddsConfig.java
 *
 * Created on 6 June 2007, 14:22
 * 
 *
 * Pauline Mak - Testing to see whether we can read in some 
 * a THREDDS catalog :)
 */
package uk.ac.rdg.resc.ncwms.config.thredds;

import java.io.File;
import simple.xml.Attribute;
import simple.xml.Root;
import simple.xml.load.Commit;


import simple.xml.load.PersistenceException;
import simple.xml.load.Persister;
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Hashtable;
import uk.ac.rdg.resc.ncwms.config.*;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvProperty;
import thredds.catalog.InvAccess;
import thredds.catalog.InvDatasetScan;
import thredds.crawlabledataset.CrawlableDataset;
import thredds.crawlabledataset.CrawlableDatasetFilter;
import java.net.URI;
import thredds.catalog.InvService;
import java.net.URISyntaxException;
import java.io.IOException;
/**
 *
 * @author pmak
 */
public class ThreddsConfig 
{
    private static final Logger logger = Logger.getLogger(ThreddsConfig.class);
    protected ArrayList<Dataset> foundDatasets;
    
    /** Creates a new instance of ThreddsConfig */
    public ThreddsConfig() 
    {
        super();
        foundDatasets = new ArrayList<Dataset>();
    }
    
    public ThreddsConfig(String _threddsCatalogLocation) throws Exception
    {
        foundDatasets = new ArrayList<Dataset>();
        readConfig(new File(_threddsCatalogLocation));
    }
    
    public ArrayList<Dataset> getFoundDatasets()
    {
        System.out.println("getting Found Datsets: " + foundDatasets.size());
        return this.foundDatasets;
    }
    
    /**
     * Reads configuration information from disk
     * @param configFile The configuration file
     * @throws Exception if there was an error reading the configuration
     * @todo create a new object if the config file doesn't already exist
     */
    public void readConfig(File configFile) throws Exception
    {
        if(configFile.getName().equalsIgnoreCase("catalog.xml"))
        {
            logger.debug("Loaded THREDDS configuration from {}", configFile.getPath());
            
            InvCatalogImpl catalog = makeCatalog(configFile.toURI());
            convert(catalog);
        }
        else        
            throw new Exception("This is not a THREDDS catalog.xml file!");
    }
    
    public void readConfig(String url) throws Exception
    {
        ThreddsConfig config = new ThreddsConfig();
        
        System.out.println(url);
        
        if(url.endsWith("catalog.xml"))
        {
            logger.debug("Loaded THREDDS configuration from {}", url);
            try
            {
                InvCatalogImpl catalog = makeCatalog(new URI(url));
                catalog.getDatasets();

                if(catalog != null)
                    config.convert(catalog);   
                else
                    logger.debug("Error reading catalog - no catalog was created");
            }
            catch(URISyntaxException uriException)
            {
                logger.debug("Cannot make URL from catalog location: " + url);
            }
        }
        else
            throw new Exception("This is not a THREDDS catalog.xml file!");
    }
    
    
    private InvCatalogImpl makeCatalog(URI catalogLocation)
    {
        System.out.println("Config location: " + catalogLocation.toString());
        StringBuffer buff = new StringBuffer();
        
        InvCatalogFactory factory = new InvCatalogFactory("default", true);
        InvCatalogImpl catalog = factory.readXML(catalogLocation);

        if (!catalog.check( buff, true)) 
        {
            System.out.println("Invalid catalog");
            System.out.println("validation output=\n" + buff);
        }
        else
        {
            System.out.println("Validate ok \n<"+ buff.toString()+">");
            try
            {
                catalog.writeXML(System.out);
            }
            catch(IOException ioe)
            {
                System.err.println("Cannot write out catalog...");
            }
        }

       return catalog; 
    }
    
    
    private void handleDirectory(InvDatasetScan ds, CrawlableDataset cd)
    {
        try
        {
            List crawlables =cd.listDatasets(ds.getFilter());

            if(crawlables != null)
            {
                for(int k = 0; k < crawlables.size(); k++)
                {
                    CrawlableDataset cd2 = (CrawlableDataset)(crawlables.get(k));
                    
                    if(cd2.isCollection())
                    {
                        handleDirectory(ds, cd2);
                    }
                    else
                    {
                        Dataset ncwmsDataset = new Dataset();
                        ncwmsDataset.setLocation(cd2.getPath());
                        ncwmsDataset.setTitle(cd2.getName());
                        ncwmsDataset.setQueryable(true);
                        ncwmsDataset.setId(ds.getPath()+ "/" + cd2.getName());
                        this.foundDatasets.add(ncwmsDataset);
                    }
                }
            }
        }
        catch(IOException ioe)
        {
            System.out.println("Error with IO - " + ioe.toString());
        } 
    }
    
    private void convert(InvCatalogImpl catalog)
    {
        List unconvertedDatasets = catalog.getDatasets();
        
        System.out.println("Number of datasets found in catalog: " + unconvertedDatasets.size());
        
        for(int i = 0; i < unconvertedDatasets.size(); i++)
        {
            InvDatasetImpl ds = (InvDatasetImpl) unconvertedDatasets.get(i);
            
            if(ds instanceof InvDatasetScan)
            {
                InvDatasetScan scan = (InvDatasetScan)ds;
                CrawlableDataset cd = scan.requestCrawlableDataset(scan.getPath());
                handleDirectory(scan, cd);
            }
            else if(ds instanceof InvCatalogRef)
            {
                InvCatalogRef catref = (InvCatalogRef) ds;
                String href = catref.getXlinkHref();
                
                URI hrefUri = catalog.getBaseURI().resolve(href);
                href = hrefUri.toString();
                
                try
                {
                    //this is so not tested
                    System.out.println("encountered catalogRef");
                    URI newURI = new URI(href);
                    InvCatalogImpl newCatalog = makeCatalog(newURI);
                    convert(catalog);
                }
                catch(URISyntaxException uriException)
                {
                    logger.debug("Cannot make URL from catalog location: " + href);
                }
            }
            else
            {
                System.out.println("have found soemthing else...");
                if(ds.hasNestedDatasets())
                {
                    //do the catalog thing again
                }
                else
                {
                    
                }
            }
        }
    }
    
    private void doDatasetScan(InvDatasetScan dataset)
    {
        CrawlableDataset cd = dataset.requestCrawlableDataset(dataset.getPath());
        
        try
        {
            // TODO: unchecked conversion here: compiler whinges
            List<CrawlableDataset> crawlables = cd.listDatasets();
            
            if(crawlables != null)
            {
                for(CrawlableDataset d: crawlables)
                {
                    Dataset ncwmsDataset = new Dataset();
                    ncwmsDataset.setLocation(d.getPath());
                    ncwmsDataset.setTitle(d.getName());
                    ncwmsDataset.setQueryable(true);
                    ncwmsDataset.setId(dataset.getID() + "/" + d.getName());
                    this.foundDatasets.add(ncwmsDataset);
                }
            }
        }
        catch(IOException ioe)
        {
            System.out.println("Error with IO - " + ioe.toString());
        }
    }
}
