//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P a g e R h y t h m                                      //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.math.Histogram;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.TupletInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code PageRhythm} handles rhythm data on a sheet page.
 * <p>
 * Rhythm is governed by time signatures found in staff header, discovered later down the staff, or
 * even inferred from measures content.
 * <p>
 * When the RHYTHMS step is launched, we already have valid information which is no longer called
 * into question: head-chords and beams.
 * Additional information comes from some of the symbols candidates discovered during the SYMBOLS
 * step, collectively referred to by the "FRAT" acronym:
 * <ul>
 * <li><b>F</b>: Flags.</li>
 * <li><b>R</b>: Rest chords.</li>
 * <li><b>A</b>: Augmentation dots.</li>
 * <li><b>T</b>: Tuplets for head & rest chords.</li>
 * </ul>
 * <p>
 * <p>
 * We have tried very hard to use the FRATs as adjustment variables to come up with a "good"
 * configuration within each stack. Unfortunately, this turned out to be impractical.
 * So now we check the "time correctness" of each stack regarding its time slots and voices.
 * <p>
 * Processing is done system per system <b>sequentially</b> because of impact of potential
 * key-sig changes on the following systems. Hence, parallelism is NOT provided for this step.
 * Consistently, within a system, processing is done measure stack after measure stack.
 * <p>
 * Time sig can be inferred from stacks actual content, but this is a chicken & egg problem.
 * We check whether the page starts with a time-sig indication. If not, we'll need two passes, the
 * first pass to determine expected duration and the second pass to determine time signature and
 * more precise fit.
 * <p>
 * TODO: Key signature changes are still to be implemented.
 *
 * @author Hervé Bitteur
 */
public class PageRhythm
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageRhythm.class);

    /** Adjustable rhythm classes. (FRAT: Flag, RestChord, AugmentationDot, Tuplet) */
    public static final Class<?>[] FRAT_CLASSES = new Class<?>[]{
        FlagInter.class, RestChordInter.class,
        AugmentationDotInter.class, TupletInter.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** The page being processed.. */
    private final Page page;

    /** Sequence of time-sig ranges found in page. */
    private final List<Range> ranges = new ArrayList<Range>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageRhythm} object.
     *
     * @param page the dedicated page
     */
    public PageRhythm (Page page)
    {
        this.page = page;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Process rhythm information in this page.
     */
    public void process ()
    {
        // Populate all stacks in page with potential time signatures, and derive ranges.
        populateTimeSignatures(); // -> ranges (ts are assigned to their containing measure)

        // Populate all stacks/measures in page with their FRATs
        populateFRATs();

        // Check typical duration for each range
        retrieveRangeDurations();

        // For each range, adjust TS if needed, then process each contained measure
        processRanges();
    }

    //---------------//
    // populateFRATs //
    //---------------//
    /**
     * Add all FRAT inters to their proper stack (and measure).
     */
    private void populateFRATs ()
    {
        for (SystemInfo system : page.getSystems()) {
            List<Inter> systemFrats = system.getSig().inters(FRAT_CLASSES);
            Collections.sort(systemFrats, Inter.byAbscissa);

            for (MeasureStack stack : system.getMeasureStacks()) {
                final List<Inter> frats = stack.filter(systemFrats);
                logger.debug("{} frats: {} {}", stack, frats.size(), Inters.ids(frats));

                for (Inter inter : frats) {
                    stack.addInter(inter);
                }
            }
        }
    }

    //------------------------//
    // populateTimeSignatures //
    //------------------------//
    /**
     * Populate page stacks with the time signatures found and derive ranges where each
     * time signature applies.
     */
    private void populateTimeSignatures ()
    {
        for (SystemInfo system : page.getSystems()) {
            List<Inter> systemTimes = system.getSig().inters(AbstractTimeInter.class);

            if (!systemTimes.isEmpty()) {
                Collections.sort(systemTimes, Inter.byAbscissa);

                for (MeasureStack stack : system.getMeasureStacks()) {
                    boolean found = false;
                    List<Inter> stackTimes = stack.filter(systemTimes);

                    for (Inter inter : stackTimes) {
                        AbstractTimeInter ts = (AbstractTimeInter) inter;
                        stack.addTimeSignature(ts);
                        systemTimes.remove(ts);
                        found = true;
                    }

                    if (found) {
                        ranges.add(new Range(stack.getIdValue(), stack.getTimeSignature()));
                    }
                }
            }
        }

        // If there was no time sig at beginning of page
        if (ranges.isEmpty() || (ranges.get(0).startId > 1)) {
            ranges.add(0, new Range(1, null));
        }

        // Assign the stopId values
        for (int i = 0; i < (ranges.size() - 1); i++) {
            ranges.get(i).stopId = ranges.get(i + 1).startId - 1;
        }

        ranges.get(ranges.size() - 1).stopId = page.getLastSystem().getLastMeasureStack()
                .getIdValue();
    }

    //---------------//
    // processRanges //
    //---------------//
    /**
     * Within each range, build the time slots and voices for each stack.
     */
    private void processRanges ()
    {
        final Iterator<Range> it = ranges.iterator();
        Range range = it.next(); // Current range

        for (SystemInfo system : page.getSystems()) {
            for (MeasureStack stack : system.getMeasureStacks()) {
                if (stack.getIdValue() == range.startId) {
                    logger.debug("Starting {}", range);

                    // Adjust time signature?
                    if ((range.duration != null)
                        && ((range.ts == null)
                            || !range.ts.getTimeRational().getValue().equals(range.duration))) {
                        logger.info(
                                "{}{} should update to {}-based time sig?",
                                stack.getSystem().getLogPrefix(),
                                range,
                                range.duration);
                    }
                }

                try {
                    logger.debug("\n--- Processing {} expDur: {} ---", stack, range.duration);
                    new StackTuner(stack, false).process(range.duration);
                } catch (Exception ex) {
                    logger.warn("Error on stack " + stack + " " + ex, ex);
                }

                // End of range?
                if (stack.getIdValue() == range.stopId) {
                    if (it.hasNext()) {
                        range = it.next();
                    }
                }
            }

            // Refine voices IDs (and thus colors) across all measures of the system
            Voices.refineSystem(system);
        }
    }

    //--------------------------//
    // retrieveExpectedDuration //
    //--------------------------//
    /**
     * Determine a suitable duration value for the provided range.
     * This is based on stacks / voices material found in this range.
     *
     * @param range the range of stacks to analyze
     * @return the guessed duration value
     */
    private Rational retrieveExpectedDuration (Range range)
    {
        Histogram<Rational> histo = new Histogram<Rational>();
        int stackNb = 0;
        int voiceNb = 0;

        SystemLoop:
        for (SystemInfo system : page.getSystems()) {
            for (MeasureStack stack : system.getMeasureStacks()) {
                int stackId = stack.getIdValue();

                if (stackId < range.startId) {
                    continue;
                } else if (stackId > range.stopId) {
                    break SystemLoop;
                }

                stackNb++;

                for (Voice voice : stack.getVoices()) {
                    Rational dur = voice.getDuration();

                    if (dur != null) {
                        histo.increaseCount(dur, 1);
                        voiceNb++;
                    }
                }
            }
        }

        // We aim at a duration value in the set: [1/2, 3/4, 1, 5/4]
        Rational avgGuess = null;
        final Rational minDur = new Rational(1, 2);

        ///final Rational maxDur = new Rational(5, 4);
        final Rational maxDur = new Rational(3, 2);
        double val = 0.0;
        int count = 0;

        for (Rational r : histo.bucketSet()) {
            if ((r.compareTo(minDur) >= 0) && (r.compareTo(maxDur) <= 0)) {
                int nb = histo.getCount(r);
                count += nb;
                val += (nb * r.doubleValue());
            }
        }

        if (count != 0) {
            val /= count;

            int quarters = (int) Math.rint(val * 4);
            avgGuess = new Rational(quarters, 4);
        }

        Rational topGuess = histo.getMaxBucket();
        logger.info(
                "{} Durations avgGuess:{} topGuess:{} avgValue:{} stacks:{} voices:{} {}",
                range,
                avgGuess,
                topGuess,
                String.format("%.2f", val),
                stackNb,
                voiceNb,
                histo);

        return avgGuess;
    }

    //------------------------//
    // retrieveRangeDurations //
    //------------------------//
    /**
     * Analyze the ranges of stacks, each range being governed by a time signature
     * (if any for the start), to retrieve typical stack duration and check with the
     * time signature.
     */
    private void retrieveRangeDurations ()
    {
        // Launch a raw processing to determine expected measure duration
        // on the range of first system & stacks before first time signature
        final Iterator<Range> it = ranges.iterator();
        Range range = it.next(); // Current range

        for (SystemInfo system : page.getSystems()) {
            for (MeasureStack stack : system.getMeasureStacks()) {
                try {
                    logger.debug("\n--- Raw processing {} ---", stack);
                    new StackTuner(stack, true).process(null);
                } catch (Exception ex) {
                    logger.warn("Error on stack " + stack + " " + ex, ex);
                }

                // End of range?
                if (stack.getIdValue() == range.stopId) {
                    // Use CURRENT MATERIAL of voices to determine expected duration on this range
                    Rational duration = retrieveExpectedDuration(range);
                    range.duration = duration;

                    if (it.hasNext()) {
                        range = it.next();
                    }
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Range //
    //-------//
    /**
     * Describes a range of stack governed by a time signature.
     * The very first range in sheet may have no time signature.
     */
    private static class Range
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int startId; // Id of first stack

        int stopId; // Id of last stack

        AbstractTimeInter ts; // Time signature found in first stack of range, if any

        Rational duration; // Inferred duration for the range

        //~ Constructors ---------------------------------------------------------------------------
        public Range (int startId,
                      AbstractTimeInter ts)
        {
            this.startId = startId;
            this.ts = ts;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{");
            sb.append(startId).append('-').append(stopId);

            if (ts != null) {
                sb.append(" ts:").append(ts.getTimeRational());
            }

            if (duration != null) {
                sb.append(" dur:").append(duration);
            }

            sb.append("}");

            return sb.toString();
        }
    }
}
