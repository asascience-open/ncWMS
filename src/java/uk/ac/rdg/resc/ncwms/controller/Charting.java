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
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;
import org.joda.time.DateTime;
import uk.ac.rdg.resc.edal.geometry.LonLatPosition;
import uk.ac.rdg.resc.edal.geometry.impl.LineString;
import uk.ac.rdg.resc.ncwms.wms.Layer;

/**
 * Code to produce various types of chart.  Used by the {@link AbstractWmsController}.
 * @author Jon
 */
final class Charting
{
    
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
        DecimalFormatSymbols decSym = DecimalFormatSymbols.getInstance(new Locale("us", "US"));
        twoDForm.setDecimalFormatSymbols(decSym);
        return twoDForm.format(d);
    }

}
