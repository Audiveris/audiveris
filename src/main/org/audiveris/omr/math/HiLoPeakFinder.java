//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H i L o P e a k F i n d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.math;

import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.util.ChartPlotter;

import org.jfree.data.xy.XYSeries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Class {@code HiLoPeakFinder} finds value peaks of an IntegerFunction, using
 * derivative hilos.
 * <p>
 * A derivative HiLo is a range in x that begins with strong positive derivatives (Hi) and ends with
 * strong negative derivatives (Lo).
 *
 * @author Hervé Bitteur
 */
public class HiLoPeakFinder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HiLoPeakFinder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Entity title. */
    public final String name;

    /** Underlying integer function. */
    public final IntegerFunction function;

    /** Minimum x for finder domain. */
    public final int xMin;

    /** Maximum x for finder domain. */
    public final int xMax;

    /** Relative ratio applied on peak gain. */
    private double minGainRatio;

    /** Threshold for y values. */
    private int minValue;

    /** Threshold for peak top y value, if any. */
    private Integer minTopValue;

    /** Threshold for y derivatives. */
    private int minDerivative;

    /** Sequence of derivative HiLos, ordered by increasing abscissa. */
    private List<Range> hilos;

    /** Peaks found, ordered by decreasing main value. */
    private List<Range> peaks;

    /** To sort peaks by decreasing main value. */
    private final Comparator<Range> byReverseMainValue = new Comparator<Range>()
    {
        @Override
        public int compare (Range e1,
                            Range e2)
        {
            return Double.compare(function.getValue(e2.main), function.getValue(e1.main));
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HiLoPeakFinder} object on (sub-)domain of provided function.
     *
     * @param name     a name for this finder
     * @param function underlying function x &rarr; y
     * @param xMin     minimum x value
     * @param xMax     maximum x value
     */
    public HiLoPeakFinder (String name,
                           IntegerFunction function,
                           int xMin,
                           int xMax)
    {
        Objects.requireNonNull(name, "PeakFinder needs non-null name");
        Objects.requireNonNull(function, "PeakFinder needs non-null function");
        this.name = name;
        this.function = function;

        if ((xMin < function.getXMin()) || (xMax > function.getXMax())) {
            throw new IllegalArgumentException("PeakFinder domain not included in function domain");
        }

        this.xMin = xMin;
        this.xMax = xMax;
    }

    /**
     * Creates a new {@code HiLoPeakFinder} object aligned on provided function.
     *
     * @param name     a name for this finder
     * @param function underlying function x &rarr; y
     */
    public HiLoPeakFinder (String name,
                           IntegerFunction function)
    {
        Objects.requireNonNull(name, "PeakFinder needs non-null name");
        Objects.requireNonNull(function, "PeakFinder needs non-null function");
        this.name = name;
        this.function = function;

        xMin = function.getXMin();
        xMax = function.getXMax();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // findPeaks //
    //-----------//
    /**
     * Retrieve peaks based on derivative HiLos.
     * A peak can extend from HiLo.min -1 to HiLo.max, unless there is a HiLo or even a peak nearby.
     *
     * @param minValue      minimum value to be part of a peak
     * @param minTopValue   peak top value threshold or null
     * @param minDerivative peak derivative threshold
     * @param minGainRatio  value gain ratio for peak widening
     * @return the (perhaps empty but not null) collection of peaks, sorted by decreasing count
     */
    public List<Range> findPeaks (int minValue,
                                  Integer minTopValue,
                                  int minDerivative,
                                  double minGainRatio)
    {
        this.minValue = minValue;
        this.minTopValue = minTopValue;
        this.minDerivative = minDerivative;
        this.minGainRatio = minGainRatio;

        final List<Range> peaks = new ArrayList<Range>();
        retrieveHiLos();

        // Map: originating hilo -> corresponding peak
        Map<Range, Range> hiloToPeak = new HashMap<Range, Range>();

        // Process hilos by decreasing main value
        List<Range> decreasing = new ArrayList<Range>(hilos);
        Collections.sort(decreasing, byReverseMainValue);

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

        if (!peaks.isEmpty()) {
            Collections.sort(peaks, byReverseMainValue);

            // Is there a min top value to check?
            if (minTopValue != null) {
                for (int i = 0; i < peaks.size(); i++) {
                    Range peak = peaks.get(i);

                    if (function.getValue(peak.main) < minTopValue) {
                        peaks.retainAll(peaks.subList(0, i));
                    }
                }
            }
        }

        this.peaks = peaks;

        return peaks;
    }

    //---------------------//
    // getDerivativeSeries //
    //---------------------//
    /**
     * Report the XY series for function derivatives on finder domain, together with
     * derivative positive and negative threshold.
     *
     * @return the XY derivatives (and thresholds) ready to plot
     */
    public XYSeries getDerivativeSeries ()
    {
        return getDerivativeSeries(xMin + 1, xMax);
    }

    //---------------------//
    // getDerivativeSeries //
    //---------------------//
    /**
     * Report the XY series for function derivatives on provided domain, together with
     * derivative positive and negative threshold.
     *
     * @param x1 lower x bound for plot
     * @param x2 upper x bound for plot
     * @return the XY derivatives (and thresholds) ready to plot
     */
    public XYSeries getDerivativeSeries (int x1,
                                         int x2)
    {
        // Function derivatives
        XYSeries derSeries = function.getDerivativeSeries(x1, x2);

        // Derivative positive threshold
        derSeries.add(x1, null);
        derSeries.add(x1, minDerivative);
        derSeries.add(x2, minDerivative);

        // Derivative negative threshold
        derSeries.add(x1, null);
        derSeries.add(x1, -minDerivative);
        derSeries.add(x2, -minDerivative);

        return derSeries;
    }

    //---------------//
    // getHiloSeries //
    //---------------//
    /**
     * Report the XYSeries for all hilos in finder domain.
     *
     * @return XY hilos ready to plot
     */
    public XYSeries getHiloSeries ()
    {
        final XYSeries hiloSeries = new XYSeries("HiLo", false); // No autosort

        for (Range hilo : hilos) {
            for (int x = hilo.min; x <= hilo.max; x++) {
                hiloSeries.add(x, function.getDerivative(x));
            }

            hiloSeries.add(hilo.max, null); // No line between hilos
        }

        return hiloSeries;
    }

    //---------------//
    // getHiloSeries //
    //---------------//
    /**
     * Report the XYSeries for hilos in provided domain only.
     *
     * @param x1 lower x bound for plot
     * @param x2 upper x bound for plot
     * @return XY hilos ready to plot
     */
    public XYSeries getHiloSeries (int x1,
                                   int x2)
    {
        final XYSeries hiloSeries = new XYSeries("HiLo", false); // No autosort

        for (Range hilo : hilos) {
            if ((hilo.min <= x2) && (hilo.max >= x1)) {
                for (int x = hilo.min; x <= hilo.max; x++) {
                    hiloSeries.add(x, function.getDerivative(x));
                }
            }

            hiloSeries.add(hilo.max, null); // No line between hilos
        }

        return hiloSeries;
    }

    //---------------//
    // getPeakSeries //
    //---------------//
    /**
     * Report the XYSeries for peaks found.
     *
     * @return XY peaks ready to plot
     */
    public XYSeries getPeakSeries ()
    {
        final XYSeries peakSeries = new XYSeries("Peak", false); // No autosort

        for (Range peak : getPeaks()) {
            if (peak != null) {
                final TreeMap<Integer, Double> thresholds = replay(peak);

                for (Map.Entry<Integer, Double> entry : thresholds.entrySet()) {
                    peakSeries.add(entry.getKey(), entry.getValue());
                }

                peakSeries.add(thresholds.lastKey(), null); // No line between peaks
            }
        }

        return peakSeries;
    }

    //---------------//
    // getPeakSeries //
    //---------------//
    /**
     * Report the XYSeries for peaks found.
     *
     * @param x1 lower x bound for plot
     * @param x2 upper x bound for plot
     * @return XY peaks ready to plot
     */
    public XYSeries getPeakSeries (int x1,
                                   int x2)
    {
        final XYSeries peakSeries = new XYSeries("Peak", false); // No autosort

        for (Range peak : getPeaks()) {
            if ((peak != null) && (peak.min <= x2) && (peak.max >= x1)) {
                final TreeMap<Integer, Double> thresholds = replay(peak);

                for (Map.Entry<Integer, Double> entry : thresholds.entrySet()) {
                    peakSeries.add(entry.getKey(), entry.getValue());
                }

                peakSeries.add(thresholds.lastKey(), null); // No line between peaks
            }
        }

        return peakSeries;
    }

    /**
     * @return the peaks
     */
    public List<Range> getPeaks ()
    {
        return peaks;
    }

    //----------------//
    // getValueSeries //
    //----------------//
    /**
     * Report the XY series for function values on finder domain, together with minValue
     * threshold if any.
     *
     * @return the XY values (and threshold if any) ready to plot
     */
    public XYSeries getValueSeries ()
    {
        return getValueSeries(xMin, xMax);
    }

    //----------------//
    // getValueSeries //
    //----------------//
    /**
     * Report the XY series for function values on finder domain, together with
     * minTopValue threshold if any.
     *
     * @param x1 lower x bound for plot
     * @param x2 upper x bound for plot
     * @return the XY values (and threshold if any) ready to plot
     */
    public XYSeries getValueSeries (int x1,
                                    int x2)
    {
        // Function values
        XYSeries valueSeries = function.getValueSeries(x1, x2);

        if (minTopValue != null) {
            valueSeries.add(x1, null); // Cut link with function values
            valueSeries.add(x1, minTopValue);
            valueSeries.add(x2, minTopValue);
        }

        return valueSeries;
    }

    //------//
    // plot //
    //------//
    /**
     * Convenient method to populate the provided plotter with series relevant for this
     * PeakFinder.
     *
     * @param plotter  plotter to populate
     * @param withZero true for zero line
     * @return plotter (for daisy chaining if so desired)
     */
    public ChartPlotter plot (ChartPlotter plotter,
                              boolean withZero)
    {
        return plot(plotter, withZero, xMin, xMax);
    }

    //------//
    // plot //
    //------//
    /**
     * Convenient method to populate the provided plotter with series relevant for this
     * PeakFinder.
     *
     * @param plotter  plotter to populate
     * @param withZero true for zero line
     * @param x1       lower x bound for plot
     * @param x2       upper x bound for plot
     * @return plotter (for daisy chaining if so desired)
     */
    public ChartPlotter plot (ChartPlotter plotter,
                              boolean withZero,
                              int x1,
                              int x2)
    {
        // Peaks
        if ((getPeaks() != null) && !peaks.isEmpty()) {
            plotter.add(getPeakSeries(), Colors.CHART_PEAK);
        }

        // HiLos
        if (hilos != null) {
            plotter.add(getHiloSeries(), Colors.CHART_HILO, true); // With shapes
        }

        // Values (w/ threshold?)
        plotter.add(getValueSeries(x1, x2), Colors.CHART_VALUE);

        // Derivatives (w/ thresholds)
        plotter.add(getDerivativeSeries(x1 + 1, x2), Colors.CHART_DERIVATIVE);

        if (withZero) {
            plotter.add(function.getZeroSeries(x1, x2), Colors.CHART_ZERO);
        }

        return plotter;
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
        int total = function.getValue(main);
        int lower = main;
        int upper = main;
        boolean modified;
        logger.debug("{} starting at {} w/ {}", name, main, total);

        do {
            modified = false;

            // Inspect both side x values, and pick up the one with largest gain
            int before = (lower == pMin) ? 0 : function.getValue(lower - 1);
            int after = (upper == pMax) ? 0 : function.getValue(upper + 1);
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

    private TreeMap<Integer, Double> replay (Range peak)
    {
        TreeMap<Integer, Double> thresholds = new TreeMap<Integer, Double>();
        final int main = peak.main; // argMax(peak.min, peak.max); // for merged peak?
        final int pMin = peak.min;
        final int pMax = peak.max;
        int total = function.getValue(main);
        int start = main;
        int stop = main;
        thresholds.put(main, new Double(0));

        do {
            int before = (start == pMin) ? 0 : function.getValue(start - 1);
            int after = (stop == pMax) ? 0 : function.getValue(stop + 1);
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

    //---------------//
    // retrieveHiLos //
    //---------------//
    /**
     * Retrieve sequence of HiLo's using derivative hysteresis.
     * <p>
     * We look for sequences of one strong positive derivative peak closely followed by one strong
     * negative derivative peak.
     */
    private void retrieveHiLos ()
    {
        logger.debug("Retrieving {} hilos", name);

        hilos = new ArrayList<Range>();

        DerPeak hiPeak = null;
        DerPeak loPeak = null;

        for (int x = xMin + 1; x <= xMax; x++) {
            final int y = function.getValue(x);
            final int der = function.getDerivative(x);

            if (der >= minDerivative) {
                // Strong positive derivatives
                if (loPeak != null) {
                    // End HiLo because a new Hi is starting
                    Range hilo = new Range(
                            hiPeak.min,
                            function.argMax(hiPeak.min, loPeak.max),
                            loPeak.max);
                    logger.debug("built {}", hilo);
                    hilos.add(hilo);
                    loPeak = hiPeak = null;
                }

                if ((hiPeak == null) || hiPeak.finished) {
                    hiPeak = new DerPeak(x, x); // Start a Hi
                } else {
                    hiPeak.max = x; // Extend Hi
                }
            } else if (der <= -minDerivative) {
                // Strong negative derivatives
                if (loPeak == null) {
                    if (hiPeak != null) {
                        loPeak = new DerPeak(x, x); // Start a Lo
                    }
                } else {
                    loPeak.max = x; // Extend Lo
                }
            } else if (loPeak != null) {
                // End HiLo because of weak derivatives
                Range hilo = new Range(
                        hiPeak.min,
                        function.argMax(hiPeak.min, loPeak.max),
                        loPeak.max);
                logger.debug("built {}", hilo);
                hilos.add(hilo);
                loPeak = hiPeak = null;
            } else if (hiPeak != null) {
                // Make sure function is still above minimum
                if (y < minValue) {
                    hiPeak = null;
                } else {
                    hiPeak.finished = true; // Terminate Hi peak
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
