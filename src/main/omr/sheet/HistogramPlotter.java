//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 H i s t o g r a m P l o t t e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.math.Histogram;

import omr.util.Navigable;

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
 * Class {@code HistogramPlotter} plots a sheet histogram.
 *
 * @author Hervé Bitteur
 */
public class HistogramPlotter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    private final String name;

    private final int[] values;

    private final Histogram<Integer> histo;

    private final Histogram.HistoEntry<? extends Number> peak;

    private final Histogram.PeakEntry<Double> secondPeak;

    private final int upper;

    private final XYSeriesCollection dataset = new XYSeriesCollection();

    private final JFreeChart chart;

    private final XYPlot plot;

    private final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

    // Series index
    int index = -1;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new HistogramPlotter object.
     *
     * @param sheet      related sheet
     * @param name       kind of runs
     * @param xLabel     label along x axis
     * @param values     raw values
     * @param histo      histogram
     * @param peak       first peak
     * @param secondPeak second peak if any
     * @param upper      upper bucket value
     */
    public HistogramPlotter (Sheet sheet,
                             String name,
                             String xLabel,
                             int[] values,
                             Histogram<Integer> histo,
                             Histogram.HistoEntry<? extends Number> peak,
                             Histogram.PeakEntry<Double> secondPeak, // if any
                             int upper)
    {
        this.sheet = sheet;
        this.name = name;
        this.values = values;
        this.histo = histo;
        this.peak = peak;
        this.secondPeak = secondPeak;
        this.upper = upper;

        // Chart
        chart = ChartFactory.createXYLineChart(
                sheet.getId() + " (" + name + " runs)", // Title
                xLabel, // X-Axis label
                "Counts", // Y-Axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation
                true, // Show legend
                false, // Show tool tips
                false // urls
        );

        plot = (XYPlot) chart.getPlot();
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void plot (Point upperLeft,
                      Double spreadRatio,
                      Double quorumRatio,
                      Double secondQuorumRatio)
    {
        plot.setRenderer(renderer);

        // All values, quorum line & spread line
        plotValues();

        if (quorumRatio != null) {
            plotQuorumLine(quorumRatio);
        }

        if (secondQuorumRatio != null) {
            plotSecondQuorumLine(secondQuorumRatio);
        }

        if (spreadRatio != null) {
            plotSpreadLine("", spreadRatio, (Histogram.PeakEntry) peak);
        }

        // Second peak spread line?
        if ((secondPeak != null) && (spreadRatio != null)) {
            plotSpreadLine("2", spreadRatio, secondPeak);
        }

        // Hosting frame
        ChartFrame frame = new ChartFrame(sheet.getId() + " - " + name + " runs", chart, true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLocation(upperLeft);
        frame.setVisible(true);
    }

    private void add (XYSeries series,
                      Color color,
                      boolean displayShapes)
    {
        dataset.addSeries(series);
        renderer.setSeriesPaint(++index, color);
        renderer.setSeriesShapesVisible(index, displayShapes);
    }

    private void plotQuorumLine (double quorumRatio)
    {
        int threshold = histo.getQuorumValue(quorumRatio);
        String pc = (int) (quorumRatio * 100) + "%";
        XYSeries series = new XYSeries("Quorum@" + pc + ":" + threshold);
        series.add(0, threshold);
        series.add(upper, threshold);
        add(series, Color.GREEN, false);
    }

    private void plotSecondQuorumLine (double secondQuorumRatio)
    {
        int threshold = histo.getQuorumValue(secondQuorumRatio);
        String pc = (int) (secondQuorumRatio * 100) + "%";
        XYSeries series = new XYSeries("Quorum2@" + pc + ":" + threshold);
        series.add(0, threshold);
        series.add(upper, threshold);
        add(series, Color.ORANGE, false);
    }

    private void plotSpreadLine (String suffix,
                                 double spreadRatio,
                                 Histogram.PeakEntry<Double> peak)
    {
        if (peak != null) {
            int threshold = histo.getQuorumValue(peak.getValue() * spreadRatio);
            String pc = (int) (spreadRatio * 100) + "%";
            XYSeries series = new XYSeries("Spread" + suffix + "@" + pc + ":" + threshold);
            series.add((double) peak.getKey().first, threshold);
            series.add((double) peak.getKey().second, threshold);
            add(series, Color.YELLOW, true);
        }
    }

    private void plotValues ()
    {
        {
            // Values
            Integer key = null;
            Integer secKey = null;

            if (peak != null) {
                double mainKey = peak.getBest().doubleValue();
                key = (int) mainKey;
            }

            if (secondPeak != null) {
                double secondKey = secondPeak.getBest();
                secKey = (int) secondKey;
            }

            String title = (peak == null) ? "No peak"
                    : ("Peak:" + key + "(" + (int) (peak.getValue() * 100) + "%)"
                       + ((secondPeak != null) ? (" & " + secKey) : ""));
            XYSeries valueSeries = new XYSeries(title);

            for (int i = 0; i <= upper; i++) {
                valueSeries.add(i, values[i]);
            }

            add(valueSeries, Color.RED, false);
        }

        {
            // Derivatives
            XYSeries derivative = new XYSeries("Derivative");
            derivative.add(0, 0);

            for (int i = 1; i <= upper; i++) {
                derivative.add(i, values[i] - values[i - 1]);
            }

            add(derivative, Color.BLUE, false);
        }

        {
            // Zero
            XYSeries zeroSeries = new XYSeries("Zero");
            zeroSeries.add(0, 0);
            zeroSeries.add(upper, 0);
            add(zeroSeries, Color.WHITE, false);
        }
    }
}
