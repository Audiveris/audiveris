//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              I n t e g e r P e a k F u n c t i o n                             //
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

import org.jfree.data.xy.XYSeries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class {@code IntegerPeakFunction} handles an integer function meant for retrieving
 * peaks in y value.
 * <p>
 * The strategy for retrieving y peaks is based on derivative 'HiLos'.
 * A derivative HiLo is a range in x that begins with strong positive derivatives (Hi) and ends with
 * strong negative derivatives (Lo).
 *
 * @author Hervé Bitteur
 */
public class IntegerPeakFunction
        extends IntegerFunction
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(IntegerPeakFunction.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Histogram title. */
    public final String name;

    /** Relative ratio applied on peak gain. */
    private double minGainRatio;

    /** Threshold for y values, if any. */
    private Integer minValue;

    /** Threshold for y derivatives. */
    private int minDerivative;

    /** Sequence of derivative HiLos, ordered by increasing abscissa. */
    private List<Range> hilos;

    /** To sort peaks by decreasing main value. */
    private final Comparator<Range> byReverseMain = new Comparator<Range>()
    {
        @Override
        public int compare (Range e1,
                            Range e2)
        {
            return Double.compare(getValue(e2.main), getValue(e1.main)); // Highest value first
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates an instance of {@code IntegerPeakFunction} for x in range 1..xMax
     *
     * @param name a name for this histogram
     * @param xMin minimum x value
     * @param xMax maximum x value
     */
    public IntegerPeakFunction (String name,
                                int xMin,
                                int xMax)
    {
        super(xMin, xMax);

        this.name = name;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // getHiLoPeaks //
    //--------------//
    /**
     * Retrieve peaks based on derivative HiLos.
     * A peak can extend from HiLo.min -1 to HiLo.max, unless there is a HiLo or even a peak nearby.
     *
     * @param minGainRatio  value gain ratio for peak extension
     * @param minValue      peak value threshold or null
     * @param minDerivative peak derivative threshold (not null)
     * @return the (perhaps empty but not null) collection of peaks, sorted by decreasing count
     */
    public List<Range> getHiLoPeaks (double minGainRatio,
                                     Integer minValue,
                                     int minDerivative)
    {
        this.minGainRatio = minGainRatio;
        this.minValue = minValue;
        this.minDerivative = minDerivative;

        final List<Range> peaks = new ArrayList<Range>();
        retrieveHiLos();

        // Map: originating hilo -> corresponding peak
        Map<Range, Range> hiloToPeak = new HashMap<Range, Range>();

        // Process hilos by decreasing main value
        List<Range> decreasing = new ArrayList<Range>(hilos);
        Collections.sort(decreasing, byReverseMain);

        // Convert each hilo to a peak with adjusted limits
        for (Range hilo : decreasing) {
            int i = hilos.indexOf(hilo);

            ///int pMin = Math.max(hilo.min - 2, 1);
            int pMin = Math.max(hilo.min - 1, 1);

            if (i > 0) {
                Range prevHiLo = hilos.get(i - 1);
                Range prevPeak = hiloToPeak.get(prevHiLo);
                pMin = Math.max(pMin, (prevPeak != null) ? (prevPeak.max + 1) : (prevHiLo.max + 1));
            }

            ///int pMax = Math.min(hilo.max + 1, counts.length - 1);
            int pMax = hilo.max;

            //
            // if (i < (hilos.size() - 1)) {
            //     Range nextHiLo = hilos.get(i + 1);
            //     Range nextPeak = hiloToPeak.get(nextHiLo);
            //     pMax = Math.min(pMax, (nextPeak != null) ? (nextPeak.min - 1) : (nextHiLo.min - 1));
            // }
            Range peak = createPeak(pMin, hilo.main, pMax);
            hiloToPeak.put(hilo, peak);
            peaks.add(peak);
        }

        // Quorum reached?
        if (!peaks.isEmpty()) {
            Collections.sort(peaks, byReverseMain);

            // Is there a count quorum to check?
            if (minValue != null) {
                if (getValue(peaks.get(0).main) < minValue) {
                    logger.debug("{} count quorum not reached", name);
                    peaks.clear();
                }
            }
        }

        return peaks;
    }

    //-------//
    // print //
    //-------//
    @Override
    public void print (PrintStream stream)
    {
        stream.print(name + " ");
        super.print(stream);
    }

    //------------//
    // createPeak //
    //------------//
    /**
     * Create a peak from an x range.
     *
     * @param pMin minimum acceptable x
     * @param main x at highest y
     * @param pMax maximum acceptable x
     * @return the created peak
     */
    private Range createPeak (int pMin,
                              int main,
                              int pMax)
    {
        int total = getValue(main);
        int lower = main;
        int upper = main;
        boolean modified;
        logger.debug("{} starting at {} w/ {}", name, main, total);

        do {
            modified = false;

            // Inspect both side x values, and pick up the one with largest gain
            int before = (lower == pMin) ? 0 : getValue(lower - 1);
            int after = (upper == pMax) ? 0 : getValue(upper + 1);
            int gain = Math.max(before, after);
            double gainRatio = (double) gain / (total + gain);

            // If side x represents a significant gain for whole peak, take it, otherwise stop
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

    //---------------//
    // retrieveHiLos //
    //---------------//
    /**
     * Retrieve sequence of HiLo's using derivative hysteresis.
     */
    private void retrieveHiLos ()
    {
        logger.debug("Retrieving {} hilos", name);

        hilos = new ArrayList<Range>();

        // First, retrieve sequence of HiLo's using derivative hysteresis
        DerPeak hiPeak = null;
        DerPeak loPeak = null;

        for (int key = xMin + 1; key <= xMax; key++) {
            final int der = getDerivative(key);

            if (der >= minDerivative) {
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
            } else if (der <= -minDerivative) {
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
    // Plotter //
    //---------//
    /**
     * Class {@code Plotter} plots an integer histogram.
     */
    public class Plotter
            extends IntegerFunction.Plotter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Range[] peaks;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new HistogramPlotter object.
         *
         * @param title  title for this histogram
         * @param xLabel label along x axis
         * @param xMax   upper bucket value
         * @param peaks  peaks to plot
         */
        public Plotter (String title,
                        String xLabel,
                        int xMax,
                        Range... peaks)
        {
            super(title, xLabel, xMax);
            this.peaks = peaks;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void plotLevelHigh ()
        {
            for (Range peak : peaks) {
                if (peak != null) {
                    plotPeak(peak);
                }
            }

            if (hilos != null) {
                plotHilos();
            }
        }

        @Override
        protected void plotLevelLow ()
        {
            plotMinDerivatives();
        }

        @Override
        protected void plotLevelMedium ()
        {
            if (minValue != null) {
                plotMinValue();
            }
        }

        private void plotHilos ()
        {
            int rank = 0;

            for (Range hilo : hilos) {
                rank++;

                String suffix = (hilos.size() > 1) ? ("#" + rank) : "";
                XYSeries hiloSeries = new XYSeries("HiLo" + suffix);

                for (int key = hilo.min; key <= hilo.max; key++) {
                    hiloSeries.add(key, getDerivative(key));
                }

                add(hiloSeries, Color.CYAN, true);
            }
        }

        private void plotMinDerivatives ()
        {
            {
                // Derivatives positive threshold
                XYSeries derSeries = new XYSeries("Der+");
                derSeries.add(1, minDerivative);
                derSeries.add(xMax, minDerivative);
                add(derSeries, Color.BLUE, false);
            }

            {
                // Derivatives negative threshold
                XYSeries derSeries = new XYSeries("Der-");
                derSeries.add(1, -minDerivative);
                derSeries.add(xMax, -minDerivative);
                add(derSeries, Color.BLUE, false);
            }
        }

        private void plotMinValue ()
        {
            XYSeries series = new XYSeries("minY");
            series.add(1, minValue);
            series.add(xMax, minValue);
            add(series, Color.RED, false);
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

        private TreeMap<Integer, Double> replay (Range peak)
        {
            TreeMap<Integer, Double> thresholds = new TreeMap<Integer, Double>();
            final int main = peak.main; // mainOf(peak.min, peak.max); // for merged peak?
            final int pMin = peak.min;
            final int pMax = peak.max;
            int total = getValue(main);
            int start = main;
            int stop = main;
            thresholds.put(main, new Double(0));

            do {
                int before = (start == pMin) ? 0 : getValue(start - 1);
                int after = (stop == pMax) ? 0 : getValue(stop + 1);
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

    //---------//
    // DerPeak //
    //---------//
    /**
     * Peak of strong derivatives (all positive or all negative).
     */
    private static class DerPeak
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** x at beginning of range. */
        public final int min;

        /** x at end of range. */
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
}
