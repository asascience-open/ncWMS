/*
 * Copyright (c) 2010 The University of Reading
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


package uk.ac.rdg.resc.ncwms.cdm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import uk.ac.rdg.resc.ncwms.coords.CrsHelper;
import uk.ac.rdg.resc.ncwms.coords.HorizontalCoordSys;
import uk.ac.rdg.resc.ncwms.coords.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.coords.LonLatPosition;
import uk.ac.rdg.resc.ncwms.coords.PixelMap;
import uk.ac.rdg.resc.ncwms.coords.PointList;
import uk.ac.rdg.resc.ncwms.coords.chrono.ThreeSixtyDayChronology;
import uk.ac.rdg.resc.ncwms.util.TimeUtils;
import uk.ac.rdg.resc.ncwms.wms.Layer;
import uk.ac.rdg.resc.ncwms.wms.ScalarLayer;

/**
 * Contains static helper methods for reading data and metadata from NetCDF files,
 * OPeNDAP servers and other data sources using the Unidata Common Data Model.
 * @author Jon
 */
public final class CdmUtils
{
    private static final Logger logger = LoggerFactory.getLogger(CdmUtils.class);

    /** Enforce non-instantiability */
    private CdmUtils() { throw new AssertionError(); }

    /**
     * Searches through the given GridDataset for GridDatatypes, which are
     * returned as {@link ScalarLayer}s in the passed-in Map.  If this method
     * encounters a GridDatatype that is already represented in the Map of layers,
     * this method only updates the list of the layer's timesteps (through
     * {@link LayerBuilder#setTimeValues(uk.ac.rdg.resc.ncwms.wms.Layer, java.util.List)}).
     * (In this way, time-aggregated layers can be created without creating
     * multiple unnecessary objects.)
     * If the GridDatatype is not represented in the Map of layers, this method
     * creates a new Layer using {@link LayerBuilder#newLayer(java.lang.String)}
     * and populates all its fields using LayerBuilder's various setter methods.
     * @param <L> The type of {@link ScalarLayer} that can be handled by the
     * {@code layerBuilder}, and that will be returned in the Map.
     * @param gd the GridDataset to search
     * @param layerBuilder The {@link LayerBuilder} that creates ScalarLayers
     * of the given type and updates their properties.
     * @param layers Map of {@link Layer#getId() layer id}s to ScalarLayer objects;
     * the Map may be empty but cannot be null.
     * @throws NullPointerException if any of the parameters is null
     */
    public static <L extends ScalarLayer> void findAndUpdateLayers(GridDataset gd,
            LayerBuilder<L> layerBuilder, Map<String, L> layers)
    {
        if (gd == null)           throw new NullPointerException("GridDataset can't be null");
        if (layerBuilder == null) throw new NullPointerException("LayerBuilder can't be null");
        if (layers == null)       throw new NullPointerException("layers can't be null");

        // Search through all coordinate systems, creating appropriate metadata
        // for each.  This allows metadata objects to be shared among Layer objects,
        // saving memory.
        for (Gridset gridset : gd.getGridsets())
        {
            GridCoordSystem coordSys = gridset.getGeoCoordSystem();

            // Look for new variables in this coordinate system.
            List<GridDatatype> grids = gridset.getGrids();
            List<GridDatatype> newGrids = new ArrayList<GridDatatype>();
            for (GridDatatype grid : grids)
            {
                if (layers.containsKey(grid.getName()))
                {
                    logger.debug("We already have data for {}", grid.getName());
                }
                else
                {
                    // We haven't seen this variable before so we must create
                    // a Layer object later
                    logger.debug("{} is a new grid", grid.getName());
                    newGrids.add(grid);
                }
            }

            // We only create all the coordsys-related objects if we have
            // new Layers to create
            if (!newGrids.isEmpty())
            {
                logger.debug("Creating coordinate system objects");
                // Create an object that will map lat-lon points to nearest grid points
                HorizontalCoordSys horizCoordSys = HorizontalCoordSys.fromCoordSys(coordSys);

                boolean zPositive = coordSys.isZPositive();
                CoordinateAxis1D zAxis = coordSys.getVerticalAxis();
                List<Double> zValues = getZValues(zAxis, zPositive);

                // Get the bounding box
                GeographicBoundingBox bbox = getBbox(coordSys);

                // Now add every variable that has this coordinate system
                for (GridDatatype grid : newGrids)
                {
                    logger.debug("Creating new Layer object for {}", grid.getName());
                    L layer = layerBuilder.newLayer(grid.getName());
                    layerBuilder.setTitle(layer, getLayerTitle(grid.getVariable()));
                    layerBuilder.setAbstract(layer, grid.getDescription());
                    layerBuilder.setUnits(layer, grid.getUnitsString());
                    layerBuilder.setHorizontalCoordSys(layer, horizCoordSys);
                    layerBuilder.setGeographicBoundingBox(layer, bbox);
                    layerBuilder.setGridDatatype(layer, grid);

                    if (zAxis != null)
                    {
                        layerBuilder.setElevationAxis(layer, zValues, zPositive, zAxis.getUnitsString());
                    }

                    // Add this layer to the Map
                    layers.put(layer.getId(), layer);
                }
            }

            // Now we add the new timestep information for *all* grids
            // in this Gridset
            List<DateTime> timesteps = getTimesteps(coordSys);
            for (GridDatatype grid : grids)
            {
                L layer = layers.get(grid.getName());
                layerBuilder.setTimeValues(layer, timesteps);
            }
        }
    }

    /** Gets a GridDataset from the given NetcdfDataset */
    public static GridDataset getGridDataset(NetcdfDataset nc) throws IOException
    {
        return (GridDataset)TypedDatasetFactory.open(FeatureType.GRID,
            nc, null, null);
    }

    /**
     * Estimates the optimum {@link DataReadingStrategy} from the given
     * NetcdfDataset.  Essentially, if the data are remote (e.g. OPeNDAP) or
     * compressed, this will return {@link DataReadingStrategy#BOUNDING_BOX},
     * which makes a single i/o call, minimizing the overhead.  If the data
     * are local and uncompressed this will return {@link DataReadingStrategy#SCANLINE},
     * which makes a tradeoff between the number of individual i/o calls and the
     * memory footprint.
     * @param nc The NetcdfDataset from which data will be read.
     * @return an optimum DataReadingStrategy for reading from the dataset
     */
    public static DataReadingStrategy getOptimumDataReadingStrategy(NetcdfDataset nc)
    {
        String fileType = nc.getFileTypeId();
        return fileType.equals("netCDF") || fileType.equals("HDF4")
            ? DataReadingStrategy.SCANLINE
            : DataReadingStrategy.BOUNDING_BOX;
    }

    /**
     * Gets the latitude-longitude bounding box of the given coordinate system
     * in the form [minLon, minLat, maxLon, maxLat]
     */
    private static GeographicBoundingBox getBbox(GridCoordSystem coordSys)
    {
        // TODO: should take into account the cell bounds
        LatLonRect latLonRect = coordSys.getLatLonBoundingBox();
        LatLonPoint lowerLeft = latLonRect.getLowerLeftPoint();
        LatLonPoint upperRight = latLonRect.getUpperRightPoint();
        double minLon = lowerLeft.getLongitude();
        double maxLon = upperRight.getLongitude();
        double minLat = lowerLeft.getLatitude();
        double maxLat = upperRight.getLatitude();
        // Correct the bounding box in case of mistakes or in case it
        // crosses the date line
        if (latLonRect.crossDateline() || minLon >= maxLon)
        {
            minLon = -180.0;
            maxLon = 180.0;
        }
        if (minLat >= maxLat)
        {
            minLat = -90.0;
            maxLat = 90.0;
        }
        // Sometimes the bounding boxes can be NaN, e.g. for a VerticalPerspectiveView
        // that encompasses more than the Earth's disc
        minLon = Double.isNaN(minLon) ? -180.0 : minLon;
        minLat = Double.isNaN(minLat) ?  -90.0 : minLat;
        maxLon = Double.isNaN(maxLon) ?  180.0 : maxLon;
        maxLat = Double.isNaN(maxLat) ?   90.0 : maxLat;
        return new DefaultGeographicBoundingBox(minLon, maxLon, minLat, maxLat);
    }

    /**
     * @return the value of the standard_name attribute of the variable,
     * or the long_name if it does not exist, or the unique id if neither of
     * these attributes exist.
     */
    private static String getLayerTitle(VariableEnhanced var)
    {
        Attribute stdNameAtt = var.findAttributeIgnoreCase("standard_name");
        if (stdNameAtt == null || stdNameAtt.getStringValue().trim().equals(""))
        {
            Attribute longNameAtt = var.findAttributeIgnoreCase("long_name");
            if (longNameAtt == null || longNameAtt.getStringValue().trim().equals(""))
            {
                return var.getName();
            }
            else
            {
                return longNameAtt.getStringValue();
            }
        }
        else
        {
            return stdNameAtt.getStringValue();
        }
    }

    /**
     * @return the values on the z axis, with sign reversed if zPositive == false.
     * Returns an empty list if zAxis is null.
     */
    private static List<Double> getZValues(CoordinateAxis1D zAxis, boolean zPositive)
    {
        List<Double> zValues = new ArrayList<Double>();
        if (zAxis != null)
        {
            for (double zVal : zAxis.getCoordValues())
            {
                zValues.add(zPositive ? zVal : 0.0 - zVal);
            }
        }
        return zValues;
    }

    /**
     * Gets List of DateTimes representing the timesteps of the given coordinate system.
     * @param coordSys The coordinate system containing the time information
     * @return List of TimestepInfo objects, or an empty list if the coordinate
     * system has no time axis
     * @throws IllegalArgumentException if the calendar system of the time axis
     * cannot be handled.
     */
    private static List<DateTime> getTimesteps(GridCoordSystem coordSys)
    {
        if (coordSys.hasTimeAxis1D())
        {
            CoordinateAxis1DTime timeAxis = coordSys.getTimeAxis1D();
            Attribute cal = timeAxis.findAttribute("calendar");
            String calString = cal == null ? null : cal.getStringValue().toLowerCase();
            if (calString == null || calString.equals("gregorian") || calString.equals("standard"))
            {
                List<DateTime> timesteps = new ArrayList<DateTime>();
                // Use the Java NetCDF library's built-in date parsing code
                for (Date date : timeAxis.getTimeDates())
                {
                    timesteps.add(new DateTime(date, DateTimeZone.UTC));
                }
                return timesteps;
            }
            else if (calString.equals("360_day"))
            {
                return getTimesteps360Day(timeAxis);
            }
            else
            {
                throw new IllegalArgumentException("The calendar system "
                    + cal.getStringValue() + " cannot be handled");
            }
        }
        // There is no time axis
        return Collections.emptyList();
    }

    /**
     * Creates a list of DateTimes in the 360-day calendar system.  All of the
     * DateTimes will have a zero time zone offset (i.e. UTC) and will use
     * the {@link ThreeSixtyDayChronology}.
     */
    private static List<DateTime> getTimesteps360Day(CoordinateAxis1DTime timeAxis)
    {
        // Get the units of the time axis, e.g. "days since 1970-1-1 0:0:0"
        String timeAxisUnits = timeAxis.getUnitsString();
        int indexOfSince = timeAxisUnits.indexOf(" since ");

        // Get the units of the time axis, e.g. "days", "months"
        String unitIncrement = timeAxisUnits.substring(0, indexOfSince);
        // Get the number of milliseconds this represents
        long unitLength = TimeUtils.getUnitLengthMillis(unitIncrement);

        // Get the base date of the axis, e.g. "1970-1-1 0:0:0"
        String baseDateTimeString = timeAxisUnits.substring(indexOfSince + " since ".length());
        DateTime baseDateTime = TimeUtils.parseUdunitsTimeString(baseDateTimeString,
                ThreeSixtyDayChronology.getInstanceUTC());

        // Now create and return the axis values
        List<DateTime> timesteps = new ArrayList<DateTime>();
        for (double val : timeAxis.getCoordValues())
        {
            timesteps.add(baseDateTime.plus((long)(unitLength * val)));
        }

        return timesteps;
    }

    /**
     * Reads a set of points at a given time and elevation from the given
     * GridDatatype.
     * @param grid The GridDatatype from which we will read data
     * @param tIndex The time index, or -1 if the grid has no time axis
     * @param zIndex The elevation index, or -1 if the grid has no elevation axis
     * @param pointList The list of points for which we need data
     * @param drStrategy The strategy to use for reading data
     * @param scaleMissingDeferred True if the {@link NetcdfDataset} that
     * contained the GridDatatype was opened with the enhancement mode
     * {@link Enhance#ScaleMissingDefer}.
     * @return a List of floating point numbers, one for each point in the
     * {@code pointList}, in the same order.  Missing values (e.g. land pixels
     * in oceanography data} are represented as nulls.
     * @throws IOException if there was an error reading data from the data source
     */
    public static List<Float> readPointList(GridDatatype grid,
            HorizontalCoordSys horizCoordSys, int tIndex, int zIndex,
            PointList pointList, DataReadingStrategy drStrategy,
            boolean scaleMissingDeferred)
            throws IOException
    {
        try
        {
            // Prevent InvalidRangeExceptions for ranges we're not going to use anyway
            if (tIndex < 0) tIndex = 0;
            if (zIndex < 0) zIndex = 0;
            Range tRange = new Range(tIndex, tIndex);
            Range zRange = new Range(zIndex, zIndex);

            // Create an list to hold the data, filled with nulls
            List<Float> picData = nullArrayList(pointList.size());

            long start = System.currentTimeMillis();
            PixelMap pixelMap = new PixelMap(horizCoordSys, pointList);
            if (pixelMap.isEmpty()) return picData;

            long readMetadata = System.currentTimeMillis();
            logger.debug("Created PixelMap in {} milliseconds", (readMetadata - start));

            // Read the data from the dataset
            drStrategy.populatePixelArray(picData, tRange, zRange, pixelMap, grid, scaleMissingDeferred);

            long builtPic = System.currentTimeMillis();
            logger.debug("Built picture array in {} milliseconds", (builtPic - readMetadata));
            logger.debug("Whole read() operation took {} milliseconds", (builtPic - start));

            return picData;
        }
        catch(InvalidRangeException ire)
        {
            // This is a programming error, and one from which we can't recover
            throw new IllegalStateException(ire);
        }
        catch(TransformException te)
        {
            // This would only happen if there were an internal error transforming
            // between coordinate systems in making the PixelMap.  There is
            // nothing a client could do to recover from this so we turn it into
            // a runtime exception
            // TODO: think of a better exception type
            throw new RuntimeException(te);
        }
    }

    /**
     * Reads a timeseries of points from the given GridDatatype at a given
     * elevation and xy location
     * @param grid The GridDatatype from which we will read data
     * @throws IOException if there was an error reading data from the data source
     * @param tIndices The list of indices along the time axis
     * @param zIndex The elevation index, or -1 if the grid has no elevation axis
     * @param xy The horizontal location of the required timeseries
     * @param scaleMissingDeferred True if the {@link NetcdfDataset} that
     * contained the GridDatatype was opened with the enhancement mode
     * {@link Enhance#ScaleMissingDefer}.
     * @return a list of floating-point numbers, one for each of the time indices.
     * Missing values (e.g. land pixels in oceanography data} are represented as nulls.
     * @throws IOException if there was an error reading data from the data source
     */
    public static List<Float> readTimeseries(GridDatatype grid,
            HorizontalCoordSys horizCoordSys, List<Integer> tIndices,
            int zIndex, HorizontalPosition xy, boolean scaleMissingDeferred)
            throws IOException
    {
        LonLatPosition lonLat;
        if (xy instanceof LonLatPosition)
        {
            lonLat = (LonLatPosition)xy;
        }
        else if (xy.getCoordinateReferenceSystem() == null)
        {
            throw new IllegalArgumentException("Horizontal position must have a"
                + " coordinate reference system");
        }
        else
        {
            CrsHelper crsHelper = CrsHelper.fromCrs(xy.getCoordinateReferenceSystem());
            try
            {
                lonLat = crsHelper.crsToLonLat(xy);
            }
            catch(TransformException te)
            {
                // This would only happen if there were an internal error transforming
                // between coordinate systems in making the PixelMap.  There is
                // nothing a client could do to recover from this so we turn it into
                // a runtime exception
                // TODO: think of a better exception type
                throw new RuntimeException(te);
            }
        }
        int[] gridCoords = horizCoordSys.lonLatToGrid(lonLat);
        if (gridCoords == null)
        {
            // The lon-lat point is outside the domain of the coord sys, so return
            // a list of nulls
            return Collections.nCopies(tIndices.size(), null);
        }

        int firstTIndex = tIndices.get(0);
        int lastTIndex = tIndices.get(tIndices.size() - 1);
        
        // Prevent InvalidRangeExceptions if z or t axes are missing
        if (firstTIndex < 0 || lastTIndex < 0)
        {
            firstTIndex = 0;
            lastTIndex = 0;
        }
        if (zIndex < 0) zIndex = 0;

        try
        {
            Range tRange = new Range(firstTIndex, lastTIndex);
            Range zRange = new Range(zIndex, zIndex);
            Range yRange = new Range(gridCoords[1], gridCoords[1]);
            Range xRange = new Range(gridCoords[0], gridCoords[0]);

            // Now read the data
            GridDatatype subset = grid.makeSubset(null, null, tRange, zRange, yRange, xRange);
            Array arr = subset.readDataSlice(-1, 0, 0, 0);

            // Check for consistency
            if (arr.getSize() != lastTIndex - firstTIndex + 1)
            {
                // This is an internal error
                throw new IllegalStateException("Unexpected array size (got " + arr.getSize()
                    + ", expected " + (lastTIndex - firstTIndex + 1) + ")");
            }

            // Copy the data (which may include many points we don't need) to
            // the required array
            VariableDS var = grid.getVariable();
            List<Float> tsData = new ArrayList<Float>();
            for (int tIndex : tIndices)
            {
                int tIndexOffset = tIndex - firstTIndex;
                if (tIndexOffset < 0) tIndexOffset = 0; // This will happen if the layer has no t axis
                float val = arr.getFloat(tIndexOffset);
                if (scaleMissingDeferred)
                {
                    // Convert scale-offset-missing
                    val = (float)var.convertScaleOffsetMissing(val);
                }
                // Replace missing values with nulls
                tsData.add(Float.isNaN(val) ? null : val);
            }
            return tsData;
        }
        catch(InvalidRangeException ire)
        {
            // This is a programming error, and one from which we can't recover
            throw new IllegalStateException(ire);
        }
    }

    /**
     * Returns true if the given NetcdfDataset uses the {@link Enhance#ScaleMissingDefer}
     * mode.
     */
    public static boolean isScaleMissingDeferred(NetcdfDataset nc)
    {
        return nc.getEnhanceMode().contains(Enhance.ScaleMissingDefer);
    }

    /**
     * Returns an ArrayList of null values of the given length
     */
    private static ArrayList<Float> nullArrayList(int n)
    {
        ArrayList<Float> list = new ArrayList<Float>(n);
        for (int i = 0; i < n; i++)
        {
            list.add((Float)null);
        }
        return list;
    }

}
