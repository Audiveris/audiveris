//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h a r t P l o t t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.util;

import org.audiveris.omr.ui.OmrGui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.Color;
import java.awt.Point;

import javax.swing.WindowConstants;

/**
 * Class {@code ChartPlotter} handles a frame filled by JFreeChart XYSeries.
 *
 * @author Hervé Bitteur
 */
public class ChartPlotter
{

    /** Height of marks below zero line. */
    public static final double MARK = 2.5;

    /** Collection of series. */
    protected final XYSeriesCollection dataset = new XYSeriesCollection();

    /** Resulting chart. */
    protected final JFreeChart chart;

    /** Line and item renderer. */
    protected final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

    /**
     * Creates a new {@code ChartPlotter} object.
     *
     * @param title  chart title
     * @param xLabel x-axis label
     * @param yLabel y-axis label
     */
    public ChartPlotter (String title,
                         String xLabel,
                         String yLabel)
    {
        chart = ChartFactory.createXYLineChart(
                title,
                xLabel,
                yLabel,
                dataset,
                PlotOrientation.VERTICAL,
                true, // Show legend
                false, // Show tool tips
                false // urls
        );

        final XYPlot plot = (XYPlot) chart.getPlot();
        plot.setRenderer(renderer);
    }

    //-----//
    // add //
    //-----//
    /**
     * Add a series to the chart.
     *
     * @param series        populated series
     * @param color         display color
     * @param displayShapes true to display a shape at each item
     */
    public void add (XYSeries series,
                     Color color,
                     boolean displayShapes)
    {
        dataset.addSeries(series);

        final int index = dataset.getSeriesIndex(series.getKey());
        renderer.setSeriesPaint(index, color);
        renderer.setSeriesShapesVisible(index, displayShapes);
    }

    //-----//
    // add //
    //-----//
    /**
     * Add a series, with no shapes, to the chart.
     *
     * @param series populated series
     * @param color  display color
     */
    public void add (XYSeries series,
                     Color color)
    {
        add(series, color, false);
    }

    //---------//
    // display //
    //---------//
    /**
     * Wrap chart into a frame with specific title and display the frame at provided
     * location.
     *
     * @param title    frame title
     * @param location frame location
     */
    public void display (String title,
                         Point location)
    {
        ChartFrame frame = new ChartFrame(title, chart, true);
        frame.setName("ChartFrame"); // For SAF life cycle
        frame.setIconImage(OmrGui.getApplication().getMainFrame().getIconImage());
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLocation(location);
        frame.setVisible(true);
    }

    //---------//
    // display //
    //---------//
    /**
     * Wrap chart into a frame with chart title and display at provided location.
     *
     * @param location frame location
     */
    public void display (Point location)
    {
        display(chart.getTitle().getText(), location);
    }

    //---------------//
    // setChartTitle //
    //---------------//
    /**
     * Assign a new title for the chart.
     *
     * @param chartTitle new chart title
     */
    public void setChartTitle (String chartTitle)
    {
        chart.setTitle(chartTitle);
    }
}
