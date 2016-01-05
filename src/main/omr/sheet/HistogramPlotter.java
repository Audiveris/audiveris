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
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new HistogramPlotter object.
     *
     * @param sheet      related sheet
     * @param name       DOCUMENT ME!
     * @param values     DOCUMENT ME!
     * @param histo      DOCUMENT ME!
     * @param peak       DOCUMENT ME!
     * @param secondPeak DOCUMENT ME!
     * @param upper      DOCUMENT ME!
     */
    public HistogramPlotter (Sheet sheet,
                             String name,
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
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void plot (Point upperLeft,
                      String xLabel,
                      Double spreadRatio,
                      Double quorumRatio)
    {
        // All values, quorum line & spread line
        plotValues();

        if (quorumRatio != null) {
            plotQuorumLine(quorumRatio);
        }

        if (spreadRatio != null) {
            plotSpreadLine("", spreadRatio, (Histogram.PeakEntry) peak);
        }

        // Second peak spread line?
        if ((secondPeak != null) && (spreadRatio != null)) {
            plotSpreadLine("Second", spreadRatio, secondPeak);
        }

        // Chart
        JFreeChart chart = ChartFactory.createXYLineChart(
                sheet.getId() + " (" + name + " runs)", // Title
                xLabel, // X-Axis label
                "Counts", // Y-Axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation
                true, // Show legend
                false, // Show tool tips
                false // urls
        );

        // Hosting frame
        ChartFrame frame = new ChartFrame(sheet.getId() + " - " + name + " runs", chart, true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setLocation(upperLeft);
        frame.setVisible(true);
    }

    private void plotQuorumLine (double quorumRatio)
    {
        int threshold = histo.getQuorumValue(quorumRatio);
        String pc = (int) (quorumRatio * 100) + "%";
        XYSeries series = new XYSeries("Quorum@" + pc + ":" + threshold);
        series.add(0, threshold);
        series.add(upper, threshold);
        dataset.addSeries(series);
    }

    private void plotSpreadLine (String prefix,
                                 double spreadRatio,
                                 Histogram.PeakEntry<Double> peak)
    {
        if (peak != null) {
            int threshold = histo.getQuorumValue(peak.getValue() * spreadRatio);
            String pc = (int) (spreadRatio * 100) + "%";
            XYSeries series = new XYSeries(prefix + "Spread@" + pc + ":" + threshold);
            series.add((double) peak.getKey().first, threshold);
            series.add((double) peak.getKey().second, threshold);
            dataset.addSeries(series);
        }
    }

    private void plotValues ()
    {
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

        XYSeries series = new XYSeries(
                "Peak:" + key + "(" + (int) (peak.getValue() * 100) + "%)"
                + ((secondPeak != null) ? (" & " + secKey) : ""));

        for (int i = 0; i <= upper; i++) {
            series.add(i, values[i]);
        }

        dataset.addSeries(series);

        // Derivative
        XYSeries derivative = new XYSeries("Derivative");
        derivative.add(0, 0);

        for (int i = 1; i <= upper; i++) {
            derivative.add(i, values[i] - values[i - 1]);
        }

        dataset.addSeries(derivative);
    }
}
