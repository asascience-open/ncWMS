/*
 * Copyright (c) 2012 The University of Reading
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
package uk.ac.rdg.resc.ncwms.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.geotoolkit.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.geotoolkit.referencing.CRS;
import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import uk.ac.rdg.resc.edal.cdm.CdmUtils;
import uk.ac.rdg.resc.edal.cdm.PixelMap;
import uk.ac.rdg.resc.edal.cdm.PixelMap.PixelMapEntry;
import uk.ac.rdg.resc.edal.coverage.CoverageMetadata;
import uk.ac.rdg.resc.edal.coverage.domain.Domain;
import uk.ac.rdg.resc.edal.coverage.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.coverage.grid.RegularAxis;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularAxisImpl;
import uk.ac.rdg.resc.edal.coverage.grid.impl.RegularGridImpl;
import uk.ac.rdg.resc.edal.geometry.HorizontalPosition;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * A {@link DataReader} that reads data from PHAVEOS L4 data files.  This would 
 * be possible with the {@link DefaultDataReader}, except that the current version
 * of the Java NetCDF libraries have a bug with reading British National Grid
 * coordinate systems.
 * @see http://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2012/msg00079.html
 * @author Jon
 */
public final class PhaveosDataReader extends DataReader {

    /** The British National Grid coordinate system */
    private static final CoordinateReferenceSystem BNG;
    /** The bounding box of the PHAVEOS data in BNG coordinates */
    private static final double[] BNG_BBOX = new double[]{-257750, -92250, 692750, 1268750};
    /** The number of points in the x direction on the source grid */
    private static final int NX = 3802;
    /** The number of points in the y direction on the source grid */
    private static final int NY = 5444;
    /** The grid describing the source data */
    private static final HorizontalGrid SOURCE_GRID;
    /** The approximate geographic bounding box in lat-lon coordinates */
    private static final GeographicBoundingBox GEO_BBOX =
            new DefaultGeographicBoundingBox(-14.11655, 3.45044, 48.72112, 61.19144);

    static {
        try {
            BNG = CRS.decode("EPSG:27700");
            // Construct the source grid, bearing in mind that the y axis runs
            // from top to bottom in the data files
            double xSpacing = (BNG_BBOX[2] - BNG_BBOX[0]) / NX;
            double ySpacing = (BNG_BBOX[1] - BNG_BBOX[3]) / NY;
            RegularAxis xAxis = new RegularAxisImpl("x", BNG_BBOX[0], xSpacing, NX, false);
            RegularAxis yAxis = new RegularAxisImpl("y", BNG_BBOX[3], ySpacing, NY, false);
            SOURCE_GRID = new RegularGridImpl(xAxis, yAxis, BNG);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public List<Float> read(String filename, Layer layer, int tIndex, int zIndex,
            Domain<HorizontalPosition> targetDomain) throws IOException {

        NetcdfDataset nc = null;
        try {
            nc = NetcdfDataset.openDataset(filename);
            ucar.nc2.Variable var = nc.findVariable(layer.getId());

            // We read in data using a "bounding box" strategy
            PixelMap pm = new PixelMap(layer.getHorizontalGrid(), targetDomain);
            int iSize = pm.getMaxIIndex() - pm.getMinIIndex() + 1;
            int jSize = pm.getMaxJIndex() - pm.getMinJIndex() + 1;
            int[] origin = new int[]{pm.getMinJIndex(), pm.getMinIIndex()};
            int[] shape = new int[]{jSize, iSize};
            Array data = var.read(origin, shape);

            // Now copy the data to an array of floats
            Index index = data.getIndex();
            index.set(new int[index.getRank()]);
            float[] arr = new float[(int) targetDomain.size()];
            Arrays.fill(arr, Float.NaN);
            for (PixelMapEntry pme : pm) {
                int i = pme.getSourceGridIIndex() - pm.getMinIIndex();
                int j = pme.getSourceGridJIndex() - pm.getMinJIndex();
                index.set(new int[]{j, i});
                float val = data.getFloat(index);
                for (int targetGridPoint : pme.getTargetGridPoints()) {
                    arr[targetGridPoint] = val > 0.0f ? val : Float.NaN;
                }
            }

            return CdmUtils.wrap(arr);
        } catch (InvalidRangeException ire) {
            // Shouldn't happen: this would be a programming error
            throw new RuntimeException(ire);
        } finally {
            if (nc != null) {
                nc.close();
            }
        }
    }

    @Override
    protected Collection<CoverageMetadata> readLayerMetadata(String location) throws IOException
    {
        // We create a new Coverage for each Variable that has this shape
        // (other variables are coordinate variables or dummy variables)
        int[] expectedShape = new int[]{NY, NX};

        List<CoverageMetadata> cms = new ArrayList<CoverageMetadata>();
        NetcdfDataset nc = null;
        
        try
        {
            nc = NetcdfDataset.openDataset(location);
            DateTime dt = getDateTime(location);
            for (ucar.nc2.Variable var : nc.getVariables())
            {
                if (Arrays.equals(var.getShape(), expectedShape))
                {
                    CoverageMetadata cm = getCoverageMetadata(var, dt);
                    cms.add(cm);
                }
            }
        }
        finally
        {
            if (nc != null)
            {
                nc.close();
            }
        }

        return cms;
    }
    
    /** Uses the name of the file to deduce the date of the data */
    private static DateTime getDateTime(String filename)
    {
        // We strip off the filename from the full path
        filename = new File(filename).getName();
        // The pattern is "PHAVEOS_YYYYMMDD.nc" 
        String dateStr = filename.split("_|\\.")[1];
        int year = Integer.valueOf(dateStr.substring(0, 4));
        int month = Integer.valueOf(dateStr.substring(4, 6));
        int day = Integer.valueOf(dateStr.substring(6, 8));
        return new DateTime(year, month, day, 0, 0, 0, 0, DateTimeZone.UTC);
    }

    private static CoverageMetadata getCoverageMetadata(final ucar.nc2.Variable var, DateTime dt)
    {
        final List<DateTime> times = new ArrayList<DateTime>(1);
        times.add(dt);
        
        return new CoverageMetadata() {

            @Override public String getId() {
                return var.getName();
            }

            @Override public String getTitle() {
                return CdmUtils.getVariableTitle(var);
            }

            @Override public String getDescription() {
                return var.getDescription();
            }

            @Override  public String getUnits() {
                return var.getUnitsString();
            }

            @Override public GeographicBoundingBox getGeographicBoundingBox() {
                return GEO_BBOX;
            }

            @Override public HorizontalGrid getHorizontalGrid() {
                return SOURCE_GRID;
            }

            @Override public Chronology getChronology() {
                return null;
            }

            @Override public List<DateTime> getTimeValues() {
                return times;
            }

            @Override public List<Double> getElevationValues() {
                return Collections.emptyList();
            }

            @Override public String getElevationUnits() {
                return "";
            }

            @Override public boolean isElevationPositive() {
                return false;
            }

            @Override public boolean isElevationPressure() {
                return false;
            }
        };
    }
}
