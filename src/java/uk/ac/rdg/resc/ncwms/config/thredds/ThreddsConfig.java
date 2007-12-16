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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDatasetImpl;
import thredds.catalog.InvDatasetScan;
import thredds.crawlabledataset.CrawlableDataset;
import uk.ac.rdg.resc.ncwms.config.Dataset;

/**
 * Reads in Datasets from a THREDDS catalog
 * @author pmak
 */
public class ThreddsConfig 
{
    private static final Logger logger = Logger.getLogger(ThreddsConfig.class);
    
    /**
     * Gets a List of Dataset objects by reading from the the THREDDS catalog
     * at the given location
     * @throws Exception if there was an error reading the THREDDS catalog
     */
    public static List<Dataset> readThreddsDatasets(String threddsCatalogLocation)
        throws Exception
    {
        File threddsCatalogFile = new File(threddsCatalogLocation);
        
        // Skip the check on the filename at the moment
        if(1 == 1) //threddsConfigFile.getName().equalsIgnoreCase("catalog.xml"))
        {
            logger.debug("Loading THREDDS configuration from {}", threddsCatalogFile.getPath());
            
            InvCatalogImpl catalog = makeCatalog(threddsCatalogFile.toURI());
            return convert(catalog);
        }
        else
        {
            throw new Exception("This is not a THREDDS catalog.xml file!");
        }
    }
    
    private static InvCatalogImpl makeCatalog(URI catalogLocation)
    {
        logger.debug("Config location: " + catalogLocation.toString());
        StringBuffer buff = new StringBuffer();
        
        InvCatalogFactory factory = new InvCatalogFactory("default", true);
        InvCatalogImpl catalog = factory.readXML(catalogLocation);

        if (!catalog.check( buff, true)) 
        {
            logger.debug("Invalid catalog");
            logger.debug("validation output=\n" + buff);
        }
        else
        {
            logger.debug("Validate ok \n<"+ buff.toString()+">");
            try
            {
                catalog.writeXML(System.out);
            }
            catch(IOException ioe)
            {
                logger.error("Cannot write out catalog...");
            }
        }

        return catalog; 
    }
    
    /**
     * Converts datasets from the THREDDS catalog into ncWMS Dataset objects
     */
    private static List<Dataset> convert(InvCatalogImpl catalog) throws IOException
    {
        List unconvertedDatasets = catalog.getDatasets();
        
        System.out.println("Number of datasets found in catalog: " + unconvertedDatasets.size());
        
        List<Dataset> ncwmsDatasets = new ArrayList<Dataset>();
        
        for(int i = 0; i < unconvertedDatasets.size(); i++)
        {
            InvDatasetImpl ds = (InvDatasetImpl) unconvertedDatasets.get(i);
            
            if(ds instanceof InvDatasetScan)
            {
                InvDatasetScan scan = (InvDatasetScan)ds;
                CrawlableDataset cd = scan.requestCrawlableDataset(scan.getPath());
                handleDirectory(scan, cd, ncwmsDatasets);
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
                System.out.println("have found something else...");
                if(ds.hasNestedDatasets())
                {
                    //do the catalog thing again
                }
                else
                {
                    
                }
            }
        }
        return ncwmsDatasets;
    }
    
    private static void handleDirectory(InvDatasetScan ds, CrawlableDataset cd,
        List<Dataset> ncwmsDatasets)
    {
        try
        {
            List crawlables = cd.listDatasets(ds.getFilter());

            if(crawlables != null)
            {
                for(Object crawlable : crawlables)
                {
                    CrawlableDataset cd2 = (CrawlableDataset)crawlable;
                    
                    if(cd2.isCollection())
                    {
                        handleDirectory(ds, cd2, ncwmsDatasets);
                    }
                    else
                    {
                        Dataset ncwmsDataset = new Dataset();
                        ncwmsDataset.setLocation(cd2.getPath());
                        ncwmsDataset.setTitle(cd2.getName());
                        ncwmsDataset.setQueryable(true);
                        ncwmsDataset.setId(ds.getPath()+ "/" + cd2.getName());
                        ncwmsDatasets.add(ncwmsDataset);
                    }
                }
            }
        }
        catch(IOException ioe)
        {
            System.out.println("Error with IO - " + ioe.toString());
        } 
    }
}
