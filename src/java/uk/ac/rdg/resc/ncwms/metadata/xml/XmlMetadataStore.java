package uk.ac.rdg.resc.ncwms.metadata.xml;
/*
 * XMLMetadataStore.java
 *
 * Created on 01 October 2007, 11:23
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.simpleframework.xml.load.Persister;
import uk.ac.rdg.resc.ncwms.metadata.Layer;
import uk.ac.rdg.resc.ncwms.metadata.MetadataStore;
import org.apache.log4j.Logger;
import uk.ac.rdg.resc.ncwms.metadata.LayerImpl;
import uk.ac.rdg.resc.ncwms.config.Dataset;
import uk.ac.rdg.resc.ncwms.metadata.Regular1DCoordAxis;
import uk.ac.rdg.resc.ncwms.metadata.TimestepInfo;
import uk.ac.rdg.resc.ncwms.utils.WmsUtils;

/**
 *
 * @author ads
 */
public class XmlMetadataStore  extends MetadataStore  {
    
    private static final Logger logger = Logger.getLogger(XmlMetadataStore.class);
    private Datacollection collection=null;
    private String metadataFileLocation = "/metadata.xml";
    
    
    private MetaDataset getDataset(String dataset) throws Exception{
        MetaDataset myDataset=null;
        boolean found=false;
        int i=0;
        int numDatasets = collection.getDataset().size();
        
        while (found==false && i<numDatasets){
            myDataset=collection.getDataset().get(i);
            if (myDataset.getId().equalsIgnoreCase(dataset)) {
                found=true;
            } else{
                i++;
            }
        }
        return myDataset;
    }
    
    private Variable getVariable(MetaDataset dataset, String internalVarname) throws Exception{
        Variable myVariable=null;
        
        boolean found=false;
        int i=0;
        int numVariable = dataset.getVariable().size();
        
        while (found==false && i<numVariable){
            myVariable=dataset.getVariable().get(i);
            if (myVariable.getInternalName().equalsIgnoreCase(internalVarname)) {
                found=true;
            } else{
                i++;
            }
        }
        return myVariable;
    }
    
    private RegularAxis getRegularAxis(MetaDataset dataset, String axisname){
        RegularAxis myAxis=null;
        
        boolean found=false;
        int i=0;
        int numAxis = dataset.getRegularAxis().size();
        
        while (found==false && i<numAxis){
            myAxis=dataset.getRegularAxis().get(i);
            if (myAxis.getAxisName().equalsIgnoreCase(axisname)) {
                found=true;
            } else{
                i++;
            }
        }
        return myAxis;
    }
    
    private IrregularAxis getIrregularAxis(MetaDataset dataset, String axisname){
        IrregularAxis myAxis=null;
        
        boolean found=false;
        int i=0;
        int numAxis = dataset.getRegularAxis().size();
        
        while (found==false && i<numAxis){
            myAxis=dataset.getIrregularAxis().get(i);
            if (myAxis.getAxisName().equalsIgnoreCase(axisname)) {
                found=true;
            } else{
                i++;
            }
        }
        return myAxis;
    }
    
    private Grid getGrid(MetaDataset dataset, String gridname){
        Grid myGrid = null;
        boolean found=false;
        int i=0;
        int numGrids=dataset.getGrid().size();
        
        while (found==false && i<numGrids){
            myGrid=dataset.getGrid().get(i);
            if (myGrid.getName().equalsIgnoreCase(gridname)) {
                found=true;
            } else{
                i++;
            }
        }
        return myGrid;
    }
    
    /**
     * Initializes this store.
     * @throws Exception if there was an error initializing the database
     * @todo can the environment be shared with the cache of image data?
     */
    public void init() throws Exception {
        
        //this should be similar to config
        // File metadata = new File("F:\\ECOOP\\metadata.xml");
        
        File metadata = new File(metadataFileLocation);
        
        if (metadata.exists()) {
            collection = new Persister().read(Datacollection.class, metadata);
            
        } else {
            logger.debug("XML Metadata file not found ");
        }
        logger.debug("Data Collection read from XML metadata file ");
    }
    
    
    /**
     * Gets a Layer object from a dataset
     * @param datasetId The ID of the dataset to which the layer belongs
     * @param layerId The unique ID of the layer within the dataset
     * @return The corresponding Layer, or null if there is no corresponding
     * layer in the store.
     * @throws Exception if an error occurs reading from the persistent store
     */
    public Layer getLayer(String datasetId, String layerId)
    throws Exception {
        
        MetaDataset myDataset = getDataset(datasetId);
        Variable myVariable = getVariable(myDataset,layerId);
        
        logger.debug("Dataset and Variable found:  " + datasetId + " " + layerId);
        
        
        LayerImpl layer = new LayerImpl();
        
        String gridName = myVariable.getGridName();
        layer.setId(myVariable.getInternalName());
        layer.setTitle(myVariable.getName());
        layer.setAbstract(myVariable.getName());
        layer.setUnits(myVariable.getUnits());
        //layer.setValidMin(new Double(myVariable.getValidMin()).doubleValue());
        //layer.setValidMax(new Double(myVariable.getValidMax()).doubleValue());
        
        
        //obtain grid for this variable
        Grid myGrid = getGrid(myDataset,gridName);
        
        //get start and end values for all the axes in the grid
        List<Axis> myAxes=myGrid.getAxis();
        
        double minx=0;
        double miny=0;
        double maxx=0;
        double maxy=0;
        
        for (int j=0; j<myAxes.size(); j++){
            Axis axis = myAxes.get(j);
            
            //handle regular axes
            if (axis.getType().equalsIgnoreCase("y")) {
                
                RegularAxis regularaxis = getRegularAxis(myDataset, axis.getName());
                Regular1DCoordAxis myY1Daxis = new Regular1DCoordAxis(new Double(regularaxis.getStart()).doubleValue(),new Double(regularaxis.getStride()).doubleValue(),new Integer(regularaxis.getCount()).intValue(),false);
                layer.setYaxis(myY1Daxis);
                
            }
            
            if (axis.getType().equalsIgnoreCase("x")) {
                RegularAxis regularaxis = getRegularAxis(myDataset, axis.getName());
                Regular1DCoordAxis myX1Daxis = new Regular1DCoordAxis(new Double(regularaxis.getStart()).doubleValue(),new Double(regularaxis.getStride()).doubleValue(),new Integer(regularaxis.getCount()).intValue(),true);
                layer.setXaxis(myX1Daxis);
            }
            
            if (axis.getType().equalsIgnoreCase("z")){
                IrregularAxis irregularaxis = getIrregularAxis(myDataset, axis.getName());
                String value = irregularaxis.getValue();
                layer.setZunits(irregularaxis.getUnits());
                //System.out.println("Z values:  " + value);
                String[] values = value.split(",");
                double[] valArray = new double[values.length];
                String direction = irregularaxis.getPositive();
                
                if (direction.equalsIgnoreCase("down")){
                    layer.setZpositive(false);
                    for (int k=0; k<values.length; k++){
                        valArray[k]=0.0-new Double(values[k]).doubleValue();
                    }
                    layer.setZvalues(valArray);
                    
                    
                } else {
                    layer.setZpositive(true);
                    for (int k=0; k<values.length; k++){
                        valArray[k]=new Double(values[k]).doubleValue();
                    }
                    layer.setZvalues(valArray);
                    
                }
            }
        }
        
        Datafiles myDatafile = myDataset.getDatafiles();
        String root = myDatafile.getRoot();
        List<FileDetails> myFiles = myDatafile.getFileDetails();
        
        for (int m = 0; m < myFiles.size(); m++) {
            TimestepInfo tInfo = new TimestepInfo(WmsUtils.iso8601ToDate(myFiles.get(m).getStartDate()), (root+myFiles.get(m).getLocation()), new Integer(myFiles.get(m).getStart()).intValue());
            layer.addTimestepInfo(tInfo);
        }
        
        //layer.setBbox(new double[]{minx, miny, maxx, maxy});
        //layer.setBbox(new double[]{-180.0, -89.0, 180.0, 90.0});
        
        Dataset ds = this.config.getDatasets().get(datasetId);
        this.addDatasetProperty(layer, ds);
        return layer;
    }
    
    
    /**
     * Gets all the Layers that belong to a dataset
     * @param datasetId The unique ID of the dataset, as defined in the config
     * file
     * @return a Collection of Layer objects that belong to this dataset
     * @throws Exception if an error occurs reading from the persistent store
     */
    public Collection<Layer> getLayersInDataset(String datasetId)
    throws Exception {
        
        MetaDataset myDataset = getDataset(datasetId);
        logger.debug("Dataset found:  " + datasetId);
        List<Variable> myVariable = myDataset.getVariable();
        List<Layer> layers = new ArrayList<Layer>();
        
        for (int i=0; i<myVariable.size(); i++) {
            String gridName = myVariable.get(i).getGridName();
            LayerImpl layer = new LayerImpl();
            layer.setId(myVariable.get(i).getInternalName());
            layer.setTitle(myVariable.get(i).getName());
            layer.setAbstract(myVariable.get(i).getName());
            layer.setUnits(myVariable.get(i).getUnits());
            
            //layer.setValidMin(new Double(myVariable.get(i).getValidMin()).doubleValue());
            //layer.setValidMax(new Double(myVariable.get(i).getValidMax()).doubleValue());
            
            //obtain grid for this variable
            Grid myGrid = getGrid(myDataset,gridName);
            
            //get start and end values for all the axes in the grid
            List<Axis> myAxes=myGrid.getAxis();
            
            double minx=0;
            double miny=0;
            double maxx=0;
            double maxy=0;
            
            for (int j=0; j<myAxes.size(); j++){
                Axis axis = myAxes.get(j);
                
                //handle regular axes
                if (axis.getType().equalsIgnoreCase("y")) {
                    
                    RegularAxis regularaxis = getRegularAxis(myDataset, axis.getName());
                    Regular1DCoordAxis myY1Daxis = new Regular1DCoordAxis(new Double(regularaxis.getStart()).doubleValue(),new Double(regularaxis.getStride()).doubleValue(),new Integer(regularaxis.getCount()).intValue(),false);
                    layer.setYaxis(myY1Daxis);
                }
                
                if (axis.getType().equalsIgnoreCase("x")) {
                    RegularAxis regularaxis = getRegularAxis(myDataset, axis.getName());
                    Regular1DCoordAxis myX1Daxis = new Regular1DCoordAxis(new Double(regularaxis.getStart()).doubleValue(),new Double(regularaxis.getStride()).doubleValue(),new Integer(regularaxis.getCount()).intValue(),true);
                    layer.setXaxis(myX1Daxis);
                }
                
                if (axis.getType().equalsIgnoreCase("z")){
                    IrregularAxis irregularaxis = getIrregularAxis(myDataset, axis.getName());
                    String value = irregularaxis.getValue();
                    layer.setZunits(irregularaxis.getUnits());
                    String direction = irregularaxis.getPositive();
                    String[] values = value.split(",");
                    double[] valArray = new double[values.length];
                    
                    if (direction.equalsIgnoreCase("down")){
                        layer.setZpositive(false);
                        for (int k=0; k<values.length; k++){
                            valArray[k]=0.0-new Double(values[k]).doubleValue();
                        }
                        layer.setZvalues(valArray);
                        
                        
                    } else {
                        layer.setZpositive(true);
                        for (int k=0; k<values.length; k++){
                            valArray[k]=new Double(values[k]).doubleValue();
                        }
                        layer.setZvalues(valArray);
                        
                    }
                }
            }
            Datafiles myDatafile = myDataset.getDatafiles();
            String root = myDatafile.getRoot();
            List<FileDetails> myFiles = myDatafile.getFileDetails();
            
            for (int m = 0; m < myFiles.size(); m++) {
                TimestepInfo tInfo = new TimestepInfo(WmsUtils.iso8601ToDate(myFiles.get(m).getStartDate()), (root+myFiles.get(m).getLocation()), new Integer(myFiles.get(m).getStart()).intValue());
                layer.addTimestepInfo(tInfo);
            }
            
            //layer.setBbox(new double[]{minx, miny, maxx, maxy});
            //layer.setBbox(new double[]{-180.0, -89.0, 180.0, 90.0});
            
            Dataset ds = this.config.getDatasets().get(datasetId);
            addDatasetProperty(layer, ds);
            layers.add(layer);
        }
        return layers;
    }
    
    /**
     * Sets the Layers that belong to the dataset with the given id, overwriting
     * all previous layers in the dataset.  This method should also update
     * the lastUpdateTime for the dataset (to harmonize with this.getLastUpdateTime()).
     * @param datasetId The ID of the dataset.
     * @param layers The Layers that belong to the dataset.  Maps layer IDs
     * (unique within a dataset) to Layer objects.
     * @throws Exception if an error occurs writing to the persistent store
     */
    public void setLayersInDataset(String datasetId, Map<String, Layer> layers)
    throws Exception {
        
    }
    
    
    /**
     * @return the time of the last update of the dataset with the given id,
     * or null if the dataset has not yet been loaded into this store.  If an
     * error occurs loading the last update time (which should be unlikely)
     * implementing classes should log the error and return null.
     */
    public Date getLastUpdateTime(String datasetId) {
        
        MetaDataset myDataset=null;
        boolean found=false;
        int i=0;
        int numDatasets = collection.getDataset().size();
        
        while (found==false && i<numDatasets){
            myDataset=collection.getDataset().get(i);
            if (myDataset.getId().equalsIgnoreCase(datasetId)) {
                found=true;
            } else{
                i++;
            }
        }
        return WmsUtils.iso8601ToDate(myDataset.getLastModified());
        
    }
    
    
    /*
    protected EnhancedCoordAxis getCoordAxis(String filename, String var, String axistype) throws IOException {
        logger.debug("Reading metadata for file {}", filename);
        NetcdfDataset nc = null;
        try {
            // We use openDataset() rather than acquiring from cache
            // because we need to enhance the dataset
            nc = NetcdfDataset.openDataset(filename, true, null);
            GridDataset gd = new GridDataset(nc);
            GeoGrid gg = (GeoGrid)gd.findGridByName(var);
            GridCoordSys coordSys = gg.getCoordinateSystem();
     
            if (axistype.equalsIgnoreCase("x")){
                return EnhancedCoordAxis.create(coordSys.getXHorizAxis());
            } else {
                return EnhancedCoordAxis.create(coordSys.getXHorizAxis());
     
            }
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ex) {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
     
    protected double getVarValidMax(String filename, String var) throws IOException {
        logger.debug("Reading metadata for file {}", filename);
        NetcdfDataset nc = null;
        try {
            // We use openDataset() rather than acquiring from cache
            // because we need to enhance the dataset
            nc = NetcdfDataset.openDataset(filename, true, null);
            GridDataset gd = new GridDataset(nc);
            GeoGrid gg = (GeoGrid)gd.findGridByName(var);
            return gg.getVariable().getValidMin();
     
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ex) {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
     
    protected double getVarValidMin(String filename, String var) throws IOException {
        logger.debug("Reading metadata for file {}", filename);
        NetcdfDataset nc = null;
        try {
            // We use openDataset() rather than acquiring from cache
            // because we need to enhance the dataset
            nc = NetcdfDataset.openDataset(filename, true, null);
            GridDataset gd = new GridDataset(nc);
            GeoGrid gg = (GeoGrid)gd.findGridByName(var);
            return gg.getVariable().getValidMax();
     
        } finally {
            if (nc != null) {
                try {
                    nc.close();
                } catch (IOException ex) {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
     */
    
    
}
