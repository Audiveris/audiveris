//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P a g e R h y t h m                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.TupletInter;
import static org.audiveris.omr.step.OmrStep.RHYTHMS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>PageRhythm</code> handles rhythm data on a sheet page.
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
 * TODO: Key signature changes are still to be implemented.
 *
 * @author Hervé Bitteur
 */
public class PageRhythm
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageRhythm.class);

    /** Adjustable rhythm classes. (FRAT: Flag, RestChord, AugmentationDot, Tuplet) */
    private static final Class<?>[] FRAT_CLASSES = new Class<?>[]
    { FlagInter.class, RestChordInter.class, AugmentationDotInter.class, TupletInter.class };

    //~ Instance fields ----------------------------------------------------------------------------

    /** The page being processed. */
    private final Page page;

    /** Sequence of time-sig ranges found in page. */
    private final List<Range> ranges = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>PageRhythm</code> object.
     *
     * @param page the dedicated page
     */
    public PageRhythm (Page page)
    {
        this.page = page;
    }

    //~ Methods ------------------------------------------------------------------------------------

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
     *
     * @return true if OK
     */
    private boolean populateTimeSignatures ()
    {
        final PageRef pageRef = page.getRef();

        for (SystemInfo system : page.getSystems()) {
            List<Inter> systemTimes = system.getSig().inters(AbstractTimeInter.class);

            if (!systemTimes.isEmpty()) {
                Collections.sort(systemTimes, Inters.byAbscissa);

                for (MeasureStack stack : system.getStacks()) {
                    boolean found = false;
                    List<Inter> stackTimes = stack.filter(systemTimes);

                    for (Inter inter : stackTimes) {
                        AbstractTimeInter ts = (AbstractTimeInter) inter;
                        systemTimes.remove(ts);

                        if (ts.getTimeRational() != null) {
                            stack.addTimeSignature(ts);
                            found = true;
                        }
                    }

                    if (found) {
                        ranges.add(new Range(seqNumOf(stack), stack.getTimeSignature()));
                    }
                }
            }
        }

        // If there was no time sig at beginning of page, insert a range there
        if (ranges.isEmpty() || (ranges.get(0).startSN > 1)) {
            ranges.add(0, new Range(1, null));
        }

        // Assign the timeRational and duration values
        for (int i = 0; i < ranges.size(); i++) {
            final Range range = ranges.get(i);

            if (range.ts != null) {
                final TimeRational timeRational = range.ts.getTimeRational();

                if (timeRational == null) {
                    logger.info("No timeRational value for {}", range.ts);

                    return false;
                }

                range.timeRational = timeRational;
                range.duration = timeRational.getValue();
            } else if (i == 0) {
                // Use time at end of previous page(s) if available within the same score
                PageRef prevPageRef = pageRef.getPrecedingInScore();

                while (prevPageRef != null) {
                    final TimeRational lastTR = prevPageRef.getLastTimeRational();

                    if (lastTR != null) {
                        range.timeRational = lastTR.duplicate();
                        range.duration = lastTR.getValue();
                        logger.info(
                                "{} Time value reused from sheet#{}",
                                page,
                                prevPageRef.getSheetNumber());
                        break;
                    } else if (prevPageRef.getStub().getLatestStep().compareTo(RHYTHMS) < 0) {
                        logger.info(
                                "{} Time value not yet available in sheet#{}",
                                page,
                                prevPageRef.getSheetNumber());
                        break;
                    }

                    // No time info in this page, let's look in the page before
                    prevPageRef = prevPageRef.getPrecedingInScore();
                }
            }
        }

        // Assign the stopSN values
        for (int i = 0; i < (ranges.size() - 1); i++) {
            ranges.get(i).stopSN = ranges.get(i + 1).startSN - 1;
        }

        // Very last range in page
        final Range lastRange = ranges.get(ranges.size() - 1);
        lastRange.stopSN = seqNumOf(page.getLastSystem().getLastStack());

        if ((lastRange.timeRational != null) && (lastRange.ts != null)) {
            page.setLastTimeRational(lastRange.timeRational.duplicate());
        }

        return true;
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
        if (populateTimeSignatures()) {
            // Populate all stacks/measures in page with their FRATs
            populateFRATs();

            // For each range, adjust TS if needed, then process each measure, using StackRhythm 2nd pass
            processRanges();
        }
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
                }

                try {
                    logger.debug("\n--- Processing {} {} expDur:{}", sn, stack, range.duration);
                    new StackRhythm(stack).process(range.duration);
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
        new StackRhythm(stack).process(expectedDuration);

        // Refine voices IDs within the containing system
        Voices.refineSystem(stack.getSystem());
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

    //~ Inner Classes ------------------------------------------------------------------------------

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
        /** 1-based sequence number of first stack in page. */
        final int startSN;

        /** 1-based sequence number of last stack in page. */
        int stopSN;

        /** Time signature found in first stack of range, if any. */
        AbstractTimeInter ts;

        /** Time rational value, if any. */
        TimeRational timeRational;

        /** Inferred measure duration for the range. */
        Rational duration;

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
                sb.append(" tSig:").append(ts.getTimeRational());
            } else if (timeRational != null) {
                sb.append(" tRat:").append(timeRational);
            }

            if (duration != null) {
                sb.append(" dur:").append(duration);
            }

            sb.append("}");

            return sb.toString();
        }
    }
}
