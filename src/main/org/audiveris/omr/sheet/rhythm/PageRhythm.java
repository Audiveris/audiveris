//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P a g e R h y t h m                                      //
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
 * <li><b>T</b>: Tuplets for head and rest chords.</li>
 * </ul>
 * <p>
 * Note: In earlier software versions, we tried very hard to play with FRATs as adjustment
 * variables to come up with a "good" configuration within each stack.
 * Unfortunately, this took endless computations and led to no practical results.
 * So now we simply check the "time correctness" of each stack regarding its time slots and voices.
 * <p>
 * Processing is done system per system <b>sequentially</b> because of impact of potential
 * key-sig changes on the following systems. Hence, parallelism is NOT provided for this step.
 * Consistently, within a system, processing is done measure stack after measure stack.
 * <p>
 * Time sig can be inferred from stacks actual content, but this is a chicken &amp; egg problem.
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

    private static final Logger logger = LoggerFactory.getLogger(PageRhythm.class);

    /** Adjustable rhythm classes. (FRAT: Flag, RestChord, AugmentationDot, Tuplet) */
    private static final Class<?>[] FRAT_CLASSES = new Class<?>[]{
        FlagInter.class,
        RestChordInter.class,
        AugmentationDotInter.class,
        TupletInter.class};

    /** The page being processed. */
    private final Page page;

    /** Sequence of time-sig ranges found in page. */
    private final List<Range> ranges = new ArrayList<>();

    /**
     * Creates a new {@code PageRhythm} object.
     *
     * @param page the dedicated page
     */
    public PageRhythm (Page page)
    {
        this.page = page;
    }

    //---------//
    // process //
    //---------//
    /**
     * Process rhythm information in this page.
     */
    public void process ()
    {
        // Populate all stacks in page with potential time signatures (TS), and derive ranges.
        populateTimeSignatures(); // -> ranges (TS are assigned to their containing measure)

        // Populate all stacks/measures in page with their FRATs
        populateFRATs();

        // Check typical duration for each range, using StackRhythm failFast pass
        retrieveRangeDurations();

        // For each range, adjust TS if needed, then process each measure, using StackRhythm 2nd pass
        processRanges();
    }

    //----------------//
    // reprocessStack //
    //----------------//
    /**
     * Stack-focused re-processing.
     *
     * @param stack the stack to re-process
     */
    public void reprocessStack (MeasureStack stack)
    {
        logger.debug("PageRhythm.reprocessStack {}", stack);

        Rational expectedDuration = stack.getExpectedDuration();
        new StackRhythm(stack, false).process(expectedDuration);

        // Refine voices IDs within the containing system
        Voices.refineSystem(stack.getSystem());
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
            Collections.sort(systemFrats, Inters.byAbscissa);

            for (MeasureStack stack : system.getStacks()) {
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
                Collections.sort(systemTimes, Inters.byAbscissa);

                for (MeasureStack stack : system.getStacks()) {
                    boolean found = false;
                    List<Inter> stackTimes = stack.filter(systemTimes);

                    for (Inter inter : stackTimes) {
                        AbstractTimeInter ts = (AbstractTimeInter) inter;
                        stack.addTimeSignature(ts);
                        systemTimes.remove(ts);
                        found = true;
                    }

                    if (found) {
                        ranges.add(new Range(seqNumOf(stack), stack.getTimeSignature()));
                    }
                }
            }
        }

        // If there was no time sig at beginning of page
        if (ranges.isEmpty() || (ranges.get(0).startSN > 1)) {
            ranges.add(0, new Range(1, null));
        }

        // Assign the stopSN values
        for (int i = 0; i < (ranges.size() - 1); i++) {
            ranges.get(i).stopSN = ranges.get(i + 1).startSN - 1;
        }

        ranges.get(ranges.size() - 1).stopSN = seqNumOf(page.getLastSystem().getLastStack());
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
            for (MeasureStack stack : system.getStacks()) {
                final int sn = seqNumOf(stack);

                // Start of range?
                if (sn == range.startSN) {
                    logger.debug("Starting {}", range);
                    //
                    //                    // Adjust time signature? (TODO: today we don't adjust anything in fact)
                    //                    if ((range.duration != null) && ((range.ts == null) || !range.ts
                    //                            .getTimeRational().getValue().equals(range.duration))) {
                    //                        logger.info(
                    //                                "{}{} should update to {}-based time sig?",
                    //                                stack.getSystem().getLogPrefix(),
                    //                                range,
                    //                                range.duration);
                    //                    }
                }

                try {
                    logger.debug("\n--- Processing {} {} expDur:{}", sn, stack, range.duration);
                    new StackRhythm(stack, false).process(range.duration);
                } catch (Exception ex) {
                    logger.warn("Error on stack " + stack + " " + ex, ex);
                }

                // End of range?
                if (sn == range.stopSN) {
                    if (it.hasNext()) {
                        range = it.next();
                    }
                }
            }

            // Refine voices IDs (and thus display colors) across all measures of the system
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
        try {
            Histogram<Rational> histo = new Histogram<>();

            SystemLoop:
            for (SystemInfo system : page.getSystems()) {
                for (MeasureStack stack : system.getStacks()) {
                    final int sn = seqNumOf(stack);

                    if (sn < range.startSN) {
                        continue;
                    } else if (sn > range.stopSN) {
                        break SystemLoop;
                    }

                    for (Voice voice : stack.getVoices()) {
                        Rational dur = voice.getDuration();

                        if (dur != null) {
                            histo.increaseCount(dur, 1);
                        }
                    }
                }
            }

            // We aim at a duration value in the set: [1/2, 3/4, 1, 5/4]
            final Rational minDur = new Rational(1, 2);
            final Rational maxDur = new Rational(3, 2);
            Rational avgGuess = null;
            double val = 0.0;
            int count = 0;
            int topNb = -1;
            Rational topGuess = null;

            for (Rational r : histo.bucketSet()) {
                if (r.compareTo(minDur) < 0) {
                    continue;
                }

                if (r.compareTo(maxDur) > 0) {
                    break;
                }

                int nb = histo.getCount(r);

                // Average
                count += nb;
                val += (nb * r.doubleValue());

                // Top
                if (nb > topNb) {
                    topNb = nb;
                    topGuess = r;
                }
            }

            if (count != 0) {
                val /= count;

                int quarters = (int) Math.rint(val * 4);
                avgGuess = new Rational(quarters, 4);
            }

            logger.info("{}", histo);
            logger.info(
                    "{} Durations topGuess:{} avgGuess:{} avgValue:{} stacks:{}",
                    range,
                    topGuess,
                    avgGuess,
                    String.format("%.2f", val),
                    range.stopSN - range.startSN + 1);

            return topGuess;
        } catch (Exception ex) {
            logger.warn("{} error in retrieveExpectedDuration {}", range, ex.toString(), ex);
            return null;
        }
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
            for (MeasureStack stack : system.getStacks()) {
                final int sn = seqNumOf(stack);

                try {
                    logger.debug("\n--- Raw processing {} {} ---", sn, stack);
                    new StackRhythm(stack, true).process(null);
                } catch (Exception ex) {
                    logger.warn("Error on stack " + stack + " " + ex, ex);
                }

                // End of range?
                if (sn == range.stopSN) {
                    // If range is governed by a time signature, let's use it!
                    ///if ((range.ts != null) && range.ts.isManual()) {
                    if (range.ts != null) {
                        range.duration = range.ts.getTimeRational().getValue();
                        logger.debug("{} manual:{}", range, range.duration);
                    } else {
                        // Use CURRENT MATERIAL of voices to determine expected duration on this range
                        Rational guess = retrieveExpectedDuration(range);

                        if (guess != null) {
                            range.duration = guess;
                        } else if (range.ts != null) {
                            range.duration = range.ts.getTimeRational().getValue();
                        }

                        logger.debug("{} guess:{}", range, guess);
                    }

                    if (it.hasNext()) {
                        range = it.next();
                    }
                }
            }
        }
    }

    //----------//
    // seqNumOf //
    //----------//
    /**
     * Report the 1-based sequence number of the provided stack in sheet.
     *
     * @param stack the provided stack
     * @return the stack sequence number
     */
    private int seqNumOf (MeasureStack stack)
    {
        final SystemInfo stackSystem = stack.getSystem();
        int sn = 1;

        for (SystemInfo system : page.getSystems()) {
            if (system != stackSystem) {
                sn += system.getStacks().size();
            } else {
                sn += system.getStacks().indexOf(stack);
                break;
            }
        }

        return sn;
    }

    //-------//
    // Range //
    //-------//
    /**
     * This private class describes a range of stacks governed by a time signature.
     * <p>
     * The very first range in sheet may have no time signature.
     * <p>
     * NOTA: We use a local 1-based sequence number to identify all stacks in page,
     * because the stack id value can be modified with measure renumbering,
     * it can be zero (pickup measure) and may not be unique (second half or cautionary measure).
     */
    private static class Range
    {

        final int startSN; // 1-based sequence number of first stack in page

        int stopSN; // 1-based sequence number of last stack in page

        AbstractTimeInter ts; // Time signature found in first stack of range, if any

        Rational duration; // Inferred measure duration for the range

        Range (int startSN,
               AbstractTimeInter ts)
        {
            this.startSN = startSN;
            this.ts = ts;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{");
            sb.append("SN").append(startSN).append("-");

            if (stopSN != 0) {
                sb.append(stopSN);
            }

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
