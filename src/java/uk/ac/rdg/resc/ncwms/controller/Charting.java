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

package uk.ac.rdg.resc.ncwms.controller;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.awt.image.IndexColorModel;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.AbstractXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import uk.ac.rdg.resc.edal.geometry.impl.LineString;
import uk.ac.rdg.resc.edal.util.Range;
import uk.ac.rdg.resc.ncwms.graphics.ColorPalette;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * Code to produce various types of chart.  Used by the {@link AbstractWmsController}.
 * @author Jon Blower
 * @author Kevin Yang
 */
final class Charting
{
    private static final Locale US_LOCALE = new Locale("us", "US");
    
    public static JFreeChart createTimeseriesPlot(Layer layer, LonLatPosition lonLat,
            Map<DateTime, Float> tsData)
    {
        TimeSeries ts = new TimeSeries("Data", Millisecond.class);
        for (Entry<DateTime, Float> entry : tsData.entrySet()) {
            ts.add(new Millisecond(entry.getKey().toDate()), entry.getValue());
        }
        TimeSeriesCollection xydataset = new TimeSeriesCollection();
        xydataset.addSeries(ts);

        // Create a chart with no legend, tooltips or URLs
        String title = "Lon: " + lonLat.getLongitude() + ", Lat: " +
                lonLat.getLatitude();
        String yLabel = layer.getTitle() + " (" + layer.getUnits() + ")";
        JFreeChart chart = ChartFactory.createTimeSeriesChart(title,
                "Date / time", yLabel, xydataset, false, false, false);
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesShape(0, new Ellipse2D.Double(-1.0, -1.0, 2.0, 2.0));
        renderer.setSeriesShapesVisible(0, true);
        chart.getXYPlot().setRenderer(renderer);
        chart.getXYPlot().setNoDataMessage("There is no data for your choice");
        chart.getXYPlot().setNoDataMessageFont(new Font("sansserif", Font.BOLD, 32));
        
        return chart;
    }

    public static JFreeChart createTransectPlot(Layer layer, LineString transectDomain,
            List<Float> transectData)
    {
        XYSeries series = new XYSeries("data", true); // TODO: more meaningful title
        for (int i = 0; i < transectData.size(); i++) {
            series.add(i, transectData.get(i));
        }

        XYSeriesCollection xySeriesColl = new XYSeriesCollection();
        xySeriesColl.addSeries(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Transect for " + layer.getTitle(), // title
                "distance along transect (arbitrary units)", // TODO more meaningful x axis label
                layer.getTitle() + " (" + layer.getUnits() + ")",
                xySeriesColl,
                PlotOrientation.VERTICAL,
                false, // show legend
                false, // show tooltips (?)
                false // urls (?)
                );

        XYPlot plot = chart.getXYPlot();
        plot.getRenderer().setSeriesPaint(0, Color.RED);
        if (layer.getDataset().getCopyrightStatement() != null) {
            final TextTitle textTitle = new TextTitle(layer.getDataset().getCopyrightStatement());
            textTitle.setFont(new Font("SansSerif", Font.PLAIN, 10));
            textTitle.setPosition(RectangleEdge.BOTTOM);
            textTitle.setHorizontalAlignment(HorizontalAlignment.RIGHT);
            chart.addSubtitle(textTitle);
        }
        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();

        rangeAxis.setAutoRangeIncludesZero(false);
        plot.setNoDataMessage("There is no data for what you have chosen.");

        //Iterate through control points to show segments of transect
        Double prevCtrlPointDistance = null;
        for (int i = 0; i < transectDomain.getControlPoints().size(); i++) {
            double ctrlPointDistance = transectDomain.getFractionalControlPointDistance(i);
            if (prevCtrlPointDistance != null) {
                //determine start end end value for marker based on index of ctrl point
                IntervalMarker target = new IntervalMarker(
                        transectData.size() * prevCtrlPointDistance,
                        transectData.size() * ctrlPointDistance
                );
                // TODO: printing to two d.p. not always appropriate
                target.setLabel("[" + printTwoDecimals(transectDomain.getControlPoints().get(i - 1).getY())
                        + "," + printTwoDecimals(transectDomain.getControlPoints().get(i - 1).getX()) + "]");
                target.setLabelFont(new Font("SansSerif", Font.ITALIC, 11));
                //alter color of segment and position of label based on odd/even index
                if (i % 2 == 0) {
                    target.setPaint(new Color(222, 222, 255, 128));
                    target.setLabelAnchor(RectangleAnchor.TOP_LEFT);
                    target.setLabelTextAnchor(TextAnchor.TOP_LEFT);
                } else {
                    target.setPaint(new Color(233, 225, 146, 128));
                    target.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
                    target.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
                }
                //add marker to plot
                plot.addDomainMarker(target);

            }
            prevCtrlPointDistance = transectDomain.getFractionalControlPointDistance(i);

        }
        return chart;
    }

    /**
     * Prints a double-precision number to 2 decimal places
     * @param d the double
     * @return rounded value to 2 places, as a String
     */
    private static String printTwoDecimals(double d)
    {
        DecimalFormat twoDForm = new DecimalFormat("#.##");
        // We need to set the Locale properly, otherwise the DecimalFormat doesn't
        // work in locales that use commas instead of points.
        // Thanks to Justino Martinez for this fix!
        DecimalFormatSymbols decSym = DecimalFormatSymbols.getInstance(US_LOCALE);
        twoDForm.setDecimalFormatSymbols(decSym);
        return twoDForm.format(d);
    }

    /**
     * Creates and returns a vertical section chart.
     * @param layer The Layer from which data have been read
     * @param horizPath The horizontal path described by the vertical section
     * @param elevationValues The elevation values for which we have data
     * @param sectionData The data values for the section data. Each
     * List&lt;Float&gt contains the values for each point on the horizontal path
     * for one of the elevation values.
     * @return
     */
    public static JFreeChart createVerticalSectionChart(Layer layer, LineString horizPath,
        List<Double> elevationValues, List<List<Float>> sectionData, Range<Float> colourScaleRange,
        ColorPalette palette, int numColourBands, boolean logarithmic)
    {
        double minElValue = elevationValues.get(0);
        double maxElValue = elevationValues.get(elevationValues.size() - 1);

        // Deal with reversed axes:  TODO: do this properly with positive=down/pressure
        if (minElValue > maxElValue) {
            double temp = minElValue;
            minElValue = maxElValue;
            maxElValue = temp;
        }

        System.out.printf("min %f, max %f%n", minElValue, maxElValue);
        // TODO expand the minElValue and maxElValue a bit

        // The number of elevation values that will be represented in the final
        // dataset.  TODO: calculate this based on the minimum elevation spacing
        int numElValues = 200;

        XYZDataset dataset = new VerticalSectionDataset(elevationValues,
                sectionData, minElValue, maxElValue, numElValues);
        
        NumberAxis xAxis = new NumberAxis("Distance along path");
        NumberAxis yAxis = layer.isElevationPositive()
            ? new NumberAxis("Height (" + layer.getElevationUnits() + ")")
            : new NumberAxis("Depth (" + layer.getElevationUnits() + ")");
        xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        yAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        PaintScale scale = createPaintScale(palette, colourScaleRange,
                numColourBands, logarithmic);

        NumberAxis colorScaleBar = new NumberAxis();
        org.jfree.data.Range colorBarRange = new org.jfree.data.Range(
            colourScaleRange.getMinimum(),
            colourScaleRange.getMaximum()
        );
        colorScaleBar.setRange(colorBarRange);

        PaintScaleLegend paintScaleLegend = new PaintScaleLegend(scale, colorScaleBar);
        paintScaleLegend.setPosition(RectangleEdge.BOTTOM);

        XYBlockRenderer renderer = new XYBlockRenderer();
        double elevationResolution = (maxElValue - minElValue) / numElValues;
        renderer.setBlockHeight(elevationResolution);
        renderer.setPaintScale(scale);

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(Color.white);

        JFreeChart chart = new JFreeChart("Vertical Section Plot", plot);
        chart.removeLegend();
        chart.addSubtitle(paintScaleLegend);
        chart.setBackgroundPaint(Color.white);
        return chart;
    }

    /**
     * An {@link XYZDataset} that is created by interpolating a set of values
     * from a discrete set of elevations.
     */
    private static class VerticalSectionDataset extends AbstractXYZDataset
    {
        private final int horizPathLength;
        private final List<List<Float>> sectionData;
        private final List<Double> elevationValues;
        private final double minElValue;
        private final double elevationResolution;
        private final int numElevations;

        public VerticalSectionDataset(List<Double> elevationValues,
                List<List<Float>> sectionData, double minElValue, double maxElValue,
                int numElevations)
        {
            this.horizPathLength = sectionData.get(0).size();
            this.sectionData = sectionData;
            this.elevationValues = elevationValues;
            this.minElValue = minElValue;
            this.numElevations = numElevations;
            this.elevationResolution = (maxElValue - minElValue) / numElevations;
        }

        @Override
        public int getSeriesCount() { return 1; }

        @Override
        public String getSeriesKey(int series) {
            checkSeries(series);
            return "Vertical section";
        }

        @Override
        public int getItemCount(int series) {
            checkSeries(series);
            return this.horizPathLength * this.numElevations;
        }

        @Override
        public Integer getX(int series, int item) {
            checkSeries(series);
            // The x coordinate is just the integer index of the point along
            // the horizontal path
            return item % this.horizPathLength;
        }

        /**
         * Gets the value of elevation, assuming linear variation between min
         * and max.
         */
        @Override
        public Double getY(int series, int item) {
            checkSeries(series);
            int yIndex = item / this.horizPathLength;
            return this.minElValue + yIndex * this.elevationResolution;
        }

        /**
         * Gets the data value corresponding with the given item, interpolating
         * between the recorded data values using nearest-neighbour interpolation
         */
        @Override
        public Float getZ(int series, int item) {
            checkSeries(series);
            int xIndex = item % this.horizPathLength;
            double elevation = this.getY(series, item);
            // What is the index of the nearest elevation in the list of elevations
            // for which we have data?
            // TODO: factor this out into a utility routine
            int nearestElevationIndex = -1;
            double minDiff = Double.POSITIVE_INFINITY;
            for (int i = 0; i < this.elevationValues.size(); i++) {
                double el = this.elevationValues.get(i);
                double diff = Math.abs(el - elevation);
                if (diff < minDiff) {
                    minDiff = diff;
                    nearestElevationIndex = i;
                }
            }
            return sectionData.get(nearestElevationIndex).get(xIndex);
        }

        /**
         * @throws IllegalArgumentException if the argument is not zero.
         */
        private static void checkSeries(int series) {
            if (series != 0) throw new IllegalArgumentException("Series must be zero");
        }
    }

    /**
     * Creates and returns a JFreeChart {@link PaintScale} that converts data values
     * to {@link Color}s.
     */
    public static PaintScale createPaintScale(ColorPalette colorPalette,
            final Range<Float> colourScaleRange, final int numColourBands,
            final boolean logarithmic)
    {
        final IndexColorModel cm = colorPalette.getColorModel(numColourBands, 100,
                Color.white, true);

        return new PaintScale()
        {
            @Override
            public double getLowerBound() {
                return colourScaleRange.getMinimum();
            }

            @Override
            public double getUpperBound() {
                return colourScaleRange.getMaximum();
            }

            @Override
            public Color getPaint(double value) {
                // TODO: replicate/factor out code in ImageProducer.java
                int index = this.getColourIndex(value);
                return new Color(cm.getRGB(index));
            }

            /**
             * @return the colour index that corresponds to the given value
             * @todo This is adapted from ImageProducer.
             */
            private int getColourIndex(double value)
            {
                if (Double.isNaN(value))
                {
                    return numColourBands; // represents a background pixel
                }
                else if (value < this.getLowerBound() || value > this.getUpperBound())
                {
                    return numColourBands + 1; // represents an out-of-range pixel
                }
                else
                {
                    double min = logarithmic ? Math.log(this.getLowerBound()) : this.getLowerBound();
                    double max = logarithmic ? Math.log(this.getUpperBound()) : this.getUpperBound();
                    double val = logarithmic ? Math.log(value) : value;
                    double frac = (val - min) / (max - min);
                    // Compute and return the index of the corresponding colour
                    int index = (int)(frac * numColourBands);
                    // For values very close to the maximum value in the range, this
                    // index might turn out to be equal to this.numColourBands due to
                    // rounding error.  In this case we subtract one from the index to
                    // ensure that such pixels are not displayed as background pixels.
                    if (index == numColourBands) index--;
                    return index;
                }
            }
        };
    }

}
