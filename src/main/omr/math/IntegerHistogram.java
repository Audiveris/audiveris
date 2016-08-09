//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                I n t e g e r H i s t o g r a m                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.math;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Point;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.WindowConstants;

/**
 * Class {@code IntegerHistogram} is a histogram where keys are integers.
 * <p>
 * In order to retrieve peaks with precise range, we use (rudimentary) derivative counts.
 *
 * @author Hervé Bitteur
 */
public class IntegerHistogram
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(IntegerHistogram.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Histogram title. */
    public final String name;

    /** Buckets, such that counts[k] is the cumulated count for k. */
    private final int[] counts;

    /** Relative ratio applied on peak gain. */
    private final double minGainRatio;

    /** Quorum ratio to be applied on counts, if any. */
    private final Double minCountRatio;

    /** Quorum ratio to be applied on derivatives, if any. */
    private final Double minDerivativeRatio;

    /** Total count. */
    private int totalCount;

    /** Sequence of derivative HiLos. */
    private List<Range> hilos;

    /** To sort peaks by decreasing main count. */
    private final Comparator<Range> byReverseMain = new Comparator<Range>()
    {
        @Override
        public int compare (Range e1,
                            Range e2)
        {
            return Double.compare(counts[e2.main], counts[e1.main]); // Highest count first
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates an instance of {@code IntegerHistogram} for keys in range 1..maxKey.
     *
     * @param name               a name for this histogram
     * @param maxKey             maximum key value
     * @param minGainRatio       gain ratio for peak extension
     * @param minCountRatio      ratio for quorum on counts, if any
     * @param minDerivativeRatio ratio for quorum on derivatives, if any
     */
    public IntegerHistogram (String name,
                             int maxKey,
                             double minGainRatio,
                             Double minCountRatio,
                             Double minDerivativeRatio)
    {
        this.name = name;
        counts = new int[1 + maxKey];
        this.minGainRatio = minGainRatio;
        this.minCountRatio = minCountRatio;
        this.minDerivativeRatio = minDerivativeRatio;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // getDerivative //
    //---------------//
    /**
     * Compute (a kind of) derivative at provided k value.
     *
     * @param key provided k value
     * @return derivative for k
     */
    public int getDerivative (int key)
    {
        return counts[key] - counts[key - 1];
    }

    //--------------//
    // getHiLoPeaks //
    //--------------//
    /**
     * Retrieve peaks based on derivative HiLos.
     * A peak can extend from HiLo.min -1 to HiLo.max, unless there is a HiLo or even a peak nearby.
     *
     * @return the (perhaps empty but not null) collection of peaks, sorted by decreasing count
     */
    public List<Range> getHiLoPeaks ()
    {
        final List<Range> peaks = new ArrayList<Range>();
        retrieveHiLos();

        // Map: originating hilo -> corresponding peak
        Map<Range, Range> hiloToPeak = new HashMap<Range, Range>();

        // Process hilos by decreasing main
        List<Range> decreasing = new ArrayList<Range>(hilos);
        Collections.sort(decreasing, byReverseMain);

        for (Range hilo : decreasing) {
            int i = hilos.indexOf(hilo);

            ///int kMin = Math.max(hilo.min - 2, 1);
            int kMin = Math.max(hilo.min - 1, 1);

            if (i > 0) {
                Range prevHiLo = hilos.get(i - 1);
                Range prevPeak = hiloToPeak.get(prevHiLo);
                kMin = Math.max(kMin, (prevPeak != null) ? (prevPeak.max + 1) : (prevHiLo.max + 1));
            }

            ///int kMax = Math.min(hilo.max + 1, counts.length - 1);
            int kMax = hilo.max;

            //
            //            if (i < (hilos.size() - 1)) {
            //                Range nextHiLo = hilos.get(i + 1);
            //                Range nextPeak = hiloToPeak.get(nextHiLo);
            //                kMax = Math.min(kMax, (nextPeak != null) ? (nextPeak.min - 1) : (nextHiLo.min - 1));
            //            }
            Range peak = createPeak(kMin, hilo.main, kMax);
            hiloToPeak.put(hilo, peak);
            peaks.add(peak);
        }

        if (!peaks.isEmpty()) {
            Collections.sort(peaks, byReverseMain);

            // Is there a count quorum to check?
            if (minCountRatio != null) {
                int minCount = getQuorumValue(minCountRatio);

                if (counts[peaks.get(0).main] < minCount) {
                    logger.debug("{} count quorum not reached", name);
                    peaks.clear();
                }
            }
        }

        return peaks;
    }

    //----------------//
    // getLocalMaxima //
    //----------------//
    /**
     * Report the local maximum points, sorted by decreasing count
     *
     * @param kMin minimum key value
     * @param kMax maximum key value
     * @return the collection of local maxima key, ordered by decreasing count
     */
    public List<Integer> getLocalMaxima (int kMin,
                                         int kMax)
    {
        final List<Integer> maxima = new ArrayList<Integer>();
        Integer prevKey = null;
        int prevCount = 0;
        boolean growing = false;

        for (int key = kMin; key <= kMax; key++) {
            int count = counts[key];

            if (prevKey != null) {
                if (count >= prevCount) {
                    growing = true;
                } else {
                    if (growing) {
                        maxima.add(prevKey); // End of a local max
                    }

                    growing = false;
                }
            }

            prevKey = key;
            prevCount = count;
        }

        // Sort by decreasing count values
        Collections.sort(
                maxima,
                new Comparator<Integer>()
        {
            @Override
            public int compare (Integer k1,
                                Integer k2)
            {
                return Integer.compare(counts[k2], counts[k1]); // Reverse
            }
        });

        return maxima;
    }

    //-----------//
    // getMaxKey //
    //-----------//
    public int getMaxKey ()
    {
        return counts.length - 1;
    }

    //---------------//
    // getTotalCount //
    //---------------//
    /**
     * Report the total counts of all buckets
     *
     * @return the sum of all counts
     */
    public int getTotalCount ()
    {
        return totalCount;
    }

    //-----------//
    // getWeight //
    //-----------//
    public int getWeight ()
    {
        int total = 0;

        for (int i = 0; i < counts.length; i++) {
            total += (i * counts[i]);
        }

        return total;
    }

    //---------------//
    // increaseCount //
    //---------------//
    public void increaseCount (int key,
                               int delta)
    {
        counts[key] += delta;
        totalCount += delta;
    }

    //-------//
    // print //
    //-------//
    public void print (PrintStream stream)
    {
        stream.print(name + " [\n");

        for (int key = 1; key < counts.length; key++) {
            int der = getDerivative(key);
            stream.format(" %d:%d/%+d\n", key, counts[key], der);
        }

        stream.println("]");
    }

    //------------//
    // createPeak //
    //------------//
    /**
     * Create a peak from a key range.
     *
     * @param kMin minimum acceptable key
     * @param main key at highest count
     * @param kMax maximum acceptable key
     * @return the created peak
     */
    private Range createPeak (int kMin,
                              int main,
                              int kMax)
    {
        int total = counts[main];
        int lower = main;
        int upper = main;
        boolean modified;
        logger.debug("{} starting at {} w/ {}", name, main, total);

        do {
            modified = false;

            // Inspect both side buckets, and pick up the one with largest gain
            int before = (lower == kMin) ? 0 : counts[lower - 1];
            int after = (upper == kMax) ? 0 : counts[upper + 1];
            int gain = Math.max(before, after);
            double gainRatio = (double) gain / (total + gain);

            // If side bucket represents a significant gain for whole peak, take it otherwise stop
            if (gainRatio >= minGainRatio) {
                if (before > after) {
                    lower--;
                    logger.debug("  going on at {} w/ {}", lower, gainRatio);
                } else {
                    upper++;
                    logger.debug("  going on at {} w/ {}", upper, gainRatio);
                }

                total += gain;
                modified = true;
            } else {
                logger.debug("{} stopped with {}", name, gainRatio);
            }
        } while (modified);

        Range peak = new Range(lower, main, upper);
        logger.debug("{} peak{}", name, peak);

        return peak;
    }

    //----------------//
    // getQuorumValue //
    //----------------//
    /**
     * Based on the current population, report the quorum value
     * corresponding to the provided quorum ratio
     *
     * @param quorumRatio quorum specified as a percentage of total count
     * @return the quorum value
     */
    private int getQuorumValue (double quorumRatio)
    {
        return (int) Math.rint(quorumRatio * totalCount);
    }

    //--------//
    // mainOf //
    //--------//
    /**
     * Report, within the provided range, the (first) key for highest count.
     *
     * @param start key at beginning of range
     * @param stop  key at end of range
     * @return the main key
     */
    private int mainOf (int start,
                        int stop)
    {
        int main = start;
        int maxCount = counts[main];

        for (int k = start; k <= stop; k++) {
            int count = counts[k];

            if (count > maxCount) {
                maxCount = count;
                main = k;
            }
        }

        return main;
    }

    //---------------//
    // retrieveHiLos //
    //---------------//
    /**
     * Retrieve sequence of HiLo's using derivative hysteresis.
     */
    private void retrieveHiLos ()
    {
        logger.debug("Retrieving {} hilos", name);

        int minDer = getQuorumValue(minDerivativeRatio);
        hilos = new ArrayList<Range>();

        // First, retrieve sequence of HiLo's using derivative hysteresis
        DerPeak hiPeak = null;
        DerPeak loPeak = null;

        for (int key = 1; key < counts.length; key++) {
            final int der = getDerivative(key);

            if (der >= minDer) {
                // Strong positive derivatives
                if (loPeak != null) {
                    // End HiLo because a new Hi is starting
                    Range hilo = new Range(hiPeak.min, mainOf(hiPeak.min, loPeak.max), loPeak.max);
                    logger.debug("built {}", hilo);
                    hilos.add(hilo);
                    loPeak = hiPeak = null;
                }

                if ((hiPeak == null) || hiPeak.finished) {
                    hiPeak = new DerPeak(key, key);
                } else {
                    hiPeak.max = key;
                }
            } else if (der <= -minDer) {
                // Strong negative derivatives
                if (loPeak == null) {
                    if (hiPeak != null) {
                        // Start a Lo
                        loPeak = new DerPeak(key, key);
                    }
                } else {
                    // Refine Lo
                    loPeak.max = key;
                }
            } else if (loPeak != null) {
                // End HiLo because of weak derivatives
                Range hilo = new Range(hiPeak.min, mainOf(hiPeak.min, loPeak.max), loPeak.max);
                logger.debug("built {}", hilo);
                hilos.add(hilo);
                loPeak = hiPeak = null;
            } else if (hiPeak != null) {
                // Terminate Hi peak
                hiPeak.finished = true;
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // DerPeak //
    //---------//
    /**
     * Peak of strong derivative (all positive or all negative).
     */
    public static class DerPeak
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Key at beginning of range. */
        public final int min;

        /** Key at end of range. */
        public int max;

        /** True when peak cannot be extended anymore. */
        private boolean finished;

        //~ Constructors ---------------------------------------------------------------------------
        public DerPeak (int min,
                        int max)
        {
            this.min = min;
            this.max = max;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return String.format("(%d,%d)", min, max);
        }
    }

    //------//
    // HiLo //
    //------//
    /**
     * A HiLo instance represents a region composed of a sequence of strong positive
     * derivatives (Hi) followed by a sequence of strong negative derivatives (Lo).
     * <p>
     * Presence of a HiLo is a good indication for a histogram peak, even when close to another
     * peak.
     */
    public static class HiLo
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Key at beginning of range. */
        public final int min;

        /** Key at end of range. */
        public final int max;

        //~ Constructors ---------------------------------------------------------------------------
        public HiLo (int min,
                     int max)
        {
            this.min = min;
            this.max = max;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return String.format("HiLo(%d,%d)", min, max);
        }
    }

    //---------//
    // Plotter //
    //---------//
    /**
     * Class {@code Plotter} plots an integer histogram.
     */
    public class Plotter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final String title;

        private final Range peak;

        private final Range secondPeak;

        private final int upper;

        private final XYSeriesCollection dataset = new XYSeriesCollection();

        private final JFreeChart chart;

        private final XYPlot plot;

        private final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        // Series index
        int index = -1;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new HistogramPlotter object.
         *
         * @param title      title for this histogram
         * @param xLabel     label along x axis
         * @param peak       first peak
         * @param secondPeak second peak if any
         * @param upper      upper bucket value
         */
        public Plotter (String title,
                        String xLabel,
                        Range peak,
                        Range secondPeak, // if any
                        int upper)
        {
            this.title = title;
            this.peak = peak;
            this.secondPeak = secondPeak;
            this.upper = upper;

            // Chart
            chart = ChartFactory.createXYLineChart(
                    title, // Title
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

        //~ Methods --------------------------------------------------------------------------------
        public void plot (Point upperLeft)
        {
            plot.setRenderer(renderer);

            if (peak != null) {
                plotPeak(peak);
            }

            if (secondPeak != null) {
                plotPeak(secondPeak);
            }

            if (hilos != null) {
                plotHilos();
            }

            plotCounts();

            if (minCountRatio != null) {
                plotMinCount();
            }

            if (minDerivativeRatio != null) {
                plotDerivatives();
                plotMinDerivatives();
            }

            plotZero();

            // Hosting frame
            ChartFrame frame = new ChartFrame(title, chart, true);
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

        private void plotCounts ()
        {
            String str = (peak == null) ? "NO_PEAK"
                    : ("Main:" + peak.main
                       + ((secondPeak != null) ? (" & " + secondPeak.main) : ""));
            XYSeries valueSeries = new XYSeries(str);

            for (int i = 1; i <= upper; i++) {
                valueSeries.add(i, counts[i]);
            }

            add(valueSeries, Color.RED, false);
        }

        private void plotDerivatives ()
        {
            XYSeries derivative = new XYSeries("Derivative");

            for (int i = 1; i <= upper; i++) {
                derivative.add(i, getDerivative(i));
            }

            add(derivative, Color.BLUE, false);
        }

        private void plotHilos ()
        {
            for (Range hilo : hilos) {
                XYSeries hiloSeries = new XYSeries("HiLo");

                for (int key = hilo.min; key <= hilo.max; key++) {
                    hiloSeries.add(key, getDerivative(key));
                }

                add(hiloSeries, Color.CYAN, true);
            }
        }

        private void plotMinCount ()
        {
            int threshold = getQuorumValue(minCountRatio);
            String pc = (int) (minCountRatio * 100) + "%";
            XYSeries series = new XYSeries(pc + "Count");
            series.add(1, threshold);
            series.add(upper, threshold);
            add(series, Color.RED, false);
        }

        private void plotMinDerivatives ()
        {
            int threshold = getQuorumValue(minDerivativeRatio);
            String pc = String.format("%.1f%%", minDerivativeRatio * 100);
            int kMin = 1;
            int kMax = upper;

            {
                // Derivatives positive threshold
                XYSeries derSeries = new XYSeries(pc + "Der+");
                derSeries.add(kMin, threshold);
                derSeries.add(kMax, threshold);
                add(derSeries, Color.BLUE, false);
            }

            {
                // Derivatives negative threshold
                XYSeries derSeries = new XYSeries(pc + "Der-");
                derSeries.add(kMin, -threshold);
                derSeries.add(kMax, -threshold);
                add(derSeries, Color.BLUE, false);
            }
        }

        private void plotPeak (Range peak)
        {
            final TreeMap<Integer, Double> thresholds = replay(peak);
            final String pc = (int) (minGainRatio * 100) + "%";
            XYSeries peakSeries = new XYSeries(pc + "Peak");

            for (Entry<Integer, Double> entry : thresholds.entrySet()) {
                peakSeries.add(entry.getKey(), entry.getValue());
            }

            add(peakSeries, Color.YELLOW, false);
        }

        private void plotZero ()
        {
            XYSeries zeroSeries = new XYSeries("Zero");
            zeroSeries.add(1, 0);
            zeroSeries.add(upper, 0);
            add(zeroSeries, Color.WHITE, false);
        }

        private TreeMap<Integer, Double> replay (Range peak)
        {
            TreeMap<Integer, Double> thresholds = new TreeMap<Integer, Double>();
            final int main = peak.main; // mainOf(peak.min, peak.max); // for merged peak?
            final int kMin = peak.min;
            final int kMax = peak.max;
            int total = counts[main];
            int start = main;
            int stop = main;
            thresholds.put(main, new Double(0));

            do {
                int before = (start == kMin) ? 0 : counts[start - 1];
                int after = (stop == kMax) ? 0 : counts[stop + 1];
                int gain = Math.max(before, after);

                if (gain == 0) {
                    return thresholds;
                }

                total += gain;

                if (before > after) {
                    start--;
                    thresholds.put(start, minGainRatio * total);
                } else {
                    stop++;
                    thresholds.put(stop, minGainRatio * total);
                }
            } while (true);
        }
    }
}
