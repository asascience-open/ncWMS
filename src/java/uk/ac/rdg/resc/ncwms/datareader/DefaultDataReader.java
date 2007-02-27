/*
 * Copyright (c) 2006 The University of Reading
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

package uk.ac.rdg.resc.ncwms.datareader;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.apache.oro.io.GlobFilenameFilter;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.EnhanceScaleMissingImpl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.grid.GeoGrid;
import ucar.nc2.dataset.grid.GridCoordSys;
import ucar.nc2.dataset.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.exceptions.WMSExceptionInJava;

/**
 * Default data reading class for CF-compliant NetCDF datasets.
 *
 * @author Jon Blower
 * $Revision$
 * $Date$
 * $Log$
 */
public class DefaultDataReader extends DataReader
{
    private static final Logger logger = Logger.getLogger(DefaultDataReader.class);
    
    /**
     * Reads an array of data from a NetCDF file and projects onto a rectangular
     * lat-lon grid.  Reads data for a single time index only.
     *
     * @param location Location of the NetCDF dataset (full file path, OPeNDAP URL etc)
     * @param vm {@link VariableMetadata} object representing the variable
     * @param tIndex The index along the time axis as found in getmap.py
     * @param zIndex The index along the vertical axis (or 0 if there is no vertical axis)
     * @param latValues Array of latitude values
     * @param lonValues Array of longitude values
     * @param fillValue Value to use for missing data
     * @throws WMSExceptionInJava if an error occurs
     */
    public float[] read(String location, VariableMetadata vm,
        int tIndex, int zIndex, float[] latValues, float[] lonValues,
        float fillValue) throws WMSExceptionInJava
    {
        // Firstly we need to figure out which file actually contains the data
        // for this timestep:  TODO: do this in DataReader?
        VariableMetadata.TimestepInfo tInfo = vm.getTimestepInfo(tIndex);
        int tIndexInFile = 0;
        String filename = location;
        if (tInfo != null)
        {
            tIndexInFile = tInfo.getIndexInFile();
            filename = tInfo.getFilename();
        }
        
        NetcdfDataset nc = null;
        try
        {
            long start = System.currentTimeMillis();
            
            Range tRange = new Range(tIndexInFile, tIndexInFile);
            Range zRange = new Range(zIndex, zIndex);
            
            EnhancedCoordAxis xAxis = vm.getXaxis();
            EnhancedCoordAxis yAxis = vm.getYaxis();
            
            // Create an array to hold the data
            float[] picData = new float[lonValues.length * latValues.length];
            Arrays.fill(picData, fillValue);
            
            // Find the range of x indices
            int minX = -1;
            int maxX = -1;
            int[] xIndices = new int[lonValues.length];
            for (int i = 0; i < lonValues.length; i++)
            {
                xIndices[i] = xAxis.getIndex(new LatLonPointImpl(0.0, lonValues[i]));
                if (xIndices[i] >= 0)
                {
                    if (minX < 0 || xIndices[i] < minX) minX = xIndices[i];
                    if (maxX < 0 || xIndices[i] > maxX) maxX = xIndices[i];
                }
            }
            // TODO: subsample if we are going to read very many more points
            // than we actually need
            if (minX < 0 || maxX < 0)
            {
                // We haven't found any valid data
                return picData;
            }
            Range xRange = new Range(minX, maxX);
            
            long readMetadata = System.currentTimeMillis();
            logger.debug("Read metadata in {} milliseconds", (readMetadata - start));
            
            // Get the dataset from the cache, without enhancing it
            nc = getDataset(filename);
            long openedDS = System.currentTimeMillis();
            logger.debug("Opened NetcdfDataset in {} milliseconds", (openedDS - readMetadata));            
            GridDataset gd = new GridDataset(nc);
            GeoGrid gg = gd.findGridByName(vm.getId());
            // Get an enhanced version of the variable for fast reading of data
            EnhanceScaleMissingImpl enhanced = getEnhanced(gg);
            
            DataChunk dataChunk;
            // Cycle through the latitude values, extracting a scanline of
            // data each time from minX to maxX
            for (int j = 0; j < latValues.length; j++)
            {
                // Check for out-of-range latitude values
                if (latValues[j] >= -90.0 && latValues[j] <= 90.0)
                {
                    int yIndex = yAxis.getIndex(new LatLonPointImpl(latValues[j], 0.0));
                    if (yIndex >= 0)
                    {
                        Range yRange = new Range(yIndex, yIndex);
                        // Read a chunk of data - values will not be unpacked or
                        // checked for missing values yet
                        GeoGrid subset = gg.subset(tRange, zRange, yRange, xRange);
                        dataChunk = new DataChunk(subset.readYXData(0,0).reduce());
                        // Now copy the scanline's data to the picture array
                        for (int i = 0; i < xIndices.length; i++)
                        {
                            if (xIndices[i] >= 0)
                            {
                                int picIndex = j * lonValues.length + i;
                                float val = dataChunk.getValue(xIndices[i] - minX);
                                // We unpack and check for missing values just for
                                // the points we need to display.
                                float pixel = (float)enhanced.convertScaleOffsetMissing(val);
                                picData[picIndex] = Float.isNaN(pixel) ? fillValue : pixel;
                            }
                        }
                    }
                }
            }
            
            long builtPic = System.currentTimeMillis();
            logger.debug("Built picture in {} milliseconds", (builtPic - readMetadata));
            logger.info("Whole read() operation took {} milliseconds", (builtPic - start));
            
            return picData;
        }
        catch(IOException e)
        {
            logger.error("IOException reading from " + nc.getLocation(), e);
            throw new WMSExceptionInJava("IOException: " + e.getMessage());
        }
        catch(InvalidRangeException ire)
        {
            logger.error("InvalidRangeException reading from " + nc.getLocation(), ire);
            throw new WMSExceptionInJava("InvalidRangeException: " + ire.getMessage());
        }
        finally
        {
            if (nc != null)
            {
                try
                {
                    nc.close();
                }
                catch (IOException ex)
                {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
    
    /**
     * Implemented as a function because for some reason we can't access
     * EnhanceScaleMissingImpl() constructor from Jython.
     */
    public static EnhanceScaleMissingImpl getEnhanced(GeoGrid gg)
    {
        return new EnhanceScaleMissingImpl((VariableDS)gg.getVariable());
    }
    
    /**
     * Reads and returns the metadata for all the variables in the dataset
     * at the given location.
     * @param location The location of the NetCDF dataset
     * @return Hashtable of variable IDs mapped to {@link VariableMetadata} objects
     * @throws IOException if there was an error reading from the data source
     */
    public Hashtable<String, VariableMetadata> getVariableMetadata(String location)
        throws IOException
    {
        logger.debug("Reading metadata for dataset {}", location);
        Hashtable<String, VariableMetadata> vars = new Hashtable<String, VariableMetadata>();
        
        String[] filenames = null;
        File locFile = null; // Only used if not an opendap location
        if (this.isOpendapLocation(location))
        {
            filenames = new String[]{location};
        }
        else
        {
            // The location might be a glob expression, in which case the last part
            // of the location path will be the filter expression
            locFile = new File(location);
            GlobFilenameFilter filter = new GlobFilenameFilter(locFile.getName());
            // Loop over all the files that match the glob pattern
            filenames = locFile.getParentFile().list(filter);
        }
        
        NetcdfDataset nc = null;
        try
        {
            for (String filepath : filenames)
            {
                if (!isOpendapLocation(location))
                {
                    // Prepend the full path
                    filepath = new File(locFile.getParentFile(), filepath).getPath();
                }
                logger.debug("Reading metadata from file {}", filepath);
                // We use openDataset() rather than acquiring from cache
                // because we need to enhance the dataset
                nc = NetcdfDataset.openDataset(filepath, true, null);
                GridDataset gd = new GridDataset(nc);
                for (Iterator it = gd.getGrids().iterator(); it.hasNext(); )
                {
                    GeoGrid gg = (GeoGrid)it.next();
                    GridCoordSys coordSys = gg.getCoordinateSystem();
                    // Get the VM object from the hashtable
                    VariableMetadata vm = vars.get(gg.getName());
                    if (vm == null)
                    {
                        // This is the first time we've seen this variable in 
                        // this list of files
                        logger.debug("Creating new VariableMetadata object for {}", gg.getName());
                        vm = new VariableMetadata();
                        vm.setId(gg.getName());
                        vm.setTitle(getStandardName(gg.getVariable().getOriginalVariable()));
                        vm.setAbstract(gg.getDescription());
                        vm.setUnits(gg.getUnitsString());
                        vm.setXaxis(EnhancedCoordAxis.create(coordSys.getXHorizAxis()));
                        vm.setYaxis(EnhancedCoordAxis.create(coordSys.getYHorizAxis()));

                        if (coordSys.hasVerticalAxis())
                        {
                            CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                            vm.setZunits(zAxis.getUnitsString());
                            double[] zVals = zAxis.getCoordValues();
                            vm.setZpositive(coordSys.isZPositive());
                            if (coordSys.isZPositive())
                            {
                                vm.setZvalues(zVals);
                            }
                            else
                            {
                                double[] zVals2 = new double[zVals.length];
                                for (int i = 0; i < zVals.length; i++)
                                {
                                    zVals2[i] = 0.0 - zVals[i];
                                }
                                vm.setZvalues(zVals2);
                            }
                        }

                        // Set the bounding box
                        // TODO: should take into account the cell bounds
                        LatLonRect latLonRect = coordSys.getLatLonBoundingBox();
                        LatLonPoint lowerLeft = latLonRect.getLowerLeftPoint();
                        LatLonPoint upperRight = latLonRect.getUpperRightPoint();
                        double minLon = lowerLeft.getLongitude();
                        double maxLon = upperRight.getLongitude();
                        double minLat = lowerLeft.getLatitude();
                        double maxLat = upperRight.getLatitude();
                        if (latLonRect.crossDateline())
                        {
                            minLon = -180.0;
                            maxLon = 180.0;
                        }
                        vm.setBbox(new double[]{minLon, minLat, maxLon, maxLat});

                        vm.setValidMin(gg.getVariable().getValidMin());
                        vm.setValidMax(gg.getVariable().getValidMax());
                        // Add this to the Hashtable
                        vars.put(vm.getId(), vm);
                    }
                    
                    // Now add the timestep information to the VM object
                    Date[] tVals = this.getTimesteps(nc, gg);
                    for (int i = 0; i < tVals.length; i++)
                    {
                        VariableMetadata.TimestepInfo tInfo = new
                            VariableMetadata.TimestepInfo(tVals[i], filepath, i);
                        vm.addTimestepInfo(tInfo);
                    }
                    
                    // (TODO: for safety we could check that the other axes
                    // match, just in case we're accidentally trying to
                    // aggregate two separate datasets)
                }
            }
            return vars;
        }
        finally
        {
            if (nc != null)
            {
                try
                {
                    nc.close();
                }
                catch (IOException ex)
                {
                    logger.error("IOException closing " + nc.getLocation(), ex);
                }
            }
        }
    }
    
    /**
     * Gets array of Dates representing the timesteps of the given variable.
     * This method is factored out to allow for simple subclasses that read 
     * time information in different ways.
     * @param nc The NetcdfDataset to which the variable belongs
     * @param gg the variable as a GeoGrid
     * @return Array of {@link Date}s
     * @throws IOException if there was an error reading the timesteps data
     */
    protected Date[] getTimesteps(NetcdfDataset nc, GeoGrid gg)
        throws IOException
    {
        GridCoordSys coordSys = gg.getCoordinateSystem();
        if (coordSys.isDate())
        {
            return coordSys.getTimeDates();
        }
        else
        {
            return new Date[]{};
        }
    }
    
    /**
     * @return the value of the standard_name attribute of the variable,
     * or the unique id if it does not exist
     */
    private static String getStandardName(Variable var)
    {
        Attribute stdNameAtt = var.findAttributeIgnoreCase("standard_name");
        return (stdNameAtt == null || stdNameAtt.getStringValue().trim().equals(""))
            ? var.getName() : stdNameAtt.getStringValue();
    }
    
    public static void main(String[] args) throws Exception
    {
        NetcdfDataset nc = NetcdfDataset.openDataset("thredds:file:/M:/raid/data/marine/processing/ag_server_xml/catalog.xml.sample#MERSEA North Atlantic TEP");
    }
    
}
