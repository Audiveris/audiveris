//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P a g e R h y t h m                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.rhythm;

import omr.math.Histogram;
import omr.math.Rational;

import omr.score.Page;

import omr.sheet.SystemInfo;

import omr.sig.SigReducer;
import omr.sig.inter.AugmentationDotInter;
import omr.sig.inter.FlagInter;
import omr.sig.inter.Inter;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.TimeInter;
import omr.sig.inter.TupletInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code PageRhythm} handles rhythm data on a sheet page.
 * <ul>
 * <li>Key sig changes.</li>
 * <li>Tuplets for head & rest chords.</li>
 * <li>Augmentation dots.</li>
 * <li>Flags.</li>
 * <li>Chords organized by time slots & voices.</li>
 * <li>Inference and possible adjustment of time signatures.</li>
 * <li>Measures assignment & numbering.</li>
 * </ul>
 * To do so, processing is done system per system <b>sequentially</b> because of impact of potential
 * key-sig changes on the following systems. Hence, parallelism is NOT provided for this step.
 * Consistently, within a system, processing is done measure stack after measure stack.
 * <p>
 * Time sig can be inferred from stacks actual content, but this is a chicken & egg problem.
 * We check whether the page starts with a time-sig indication. If not, we'll need two passes, the
 * first pass to determine expected duration and the second pass to determine time signature and
 * more precise fit.
 *
 * @author Hervé Bitteur
 */
public class PageRhythm
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PageRhythm.class);

    /** Adjustable rhythm classes. (FRAT: Flag, RestChord, AugmentationDot, Tuplet) */
    public static final Class<?>[] rhythmClasses = new Class<?>[]{
        FlagInter.class, // (standard) Flags
        RestChordInter.class, // Chords (rests only)
        AugmentationDotInter.class, // Augmentation dots
        TupletInter.class // Tuplet signs
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated page. */
    private final Page page;

    /** Sequence of Ranges found in page. */
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
    public void process ()
    {
        // Reduce symbols while saving optional rhythm data for each system
        Map<SystemInfo, SystemBackup> optionalsMap = new LinkedHashMap<SystemInfo, SystemBackup>();

        for (SystemInfo system : page.getSystems()) {
            SystemBackup optionals = new SystemBackup(system);
            new SigReducer(system).reduceSymbols(optionals, rhythmClasses);
            optionalsMap.put(system, optionals);
        }

        // Populate all stacks in page with potential time signatures, and derive ranges.
        populateTimeSignatures();

        // Check typical duration for each range
        retrieveDurations();

        // For each range, adjust TS if needed, then process each contained measure
        final Iterator<Range> it = ranges.iterator();

        // Current range
        Range range = it.next();

        for (SystemInfo system : page.getSystems()) {
            // Select relevant rhythm inters at system level
            List<Inter> systemInters = system.getSig().inters(StackTuner.rhythmClasses);
            SystemBackup optionals = optionalsMap.get(system);

            // Process stack after stack
            for (MeasureStack stack : system.getMeasureStacks()) {
                if (stack.getIdValue() == range.startId) {
                    logger.debug("Starting {}", range);

                    // Adjust time signature?
                    if ((range.ts == null)
                        || !range.ts.getTimeRational().getValue().equals(range.duration)) {
                        logger.info("Should update to {}-based time sig", range.duration);
                    }
                }

                try {
                    logger.debug("\n--- Processing {} expDur: {} ---", stack, range.duration);
                    new StackTuner(stack, true).process(systemInters, optionals, range.duration);
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

            // Refine voices ids (and thus colors) across all measures of the system
            new SystemVoiceFixer(system).refine();
        }
    }

    //------------------------//
    // populateTimeSignatures //
    //------------------------//
    /**
     * Populate page stacks with the time signatures found and define ranges.
     */
    private void populateTimeSignatures ()
    {
        for (SystemInfo system : page.getSystems()) {
            List<Inter> systemTimes = system.getSig().inters(TimeInter.class);

            if (!systemTimes.isEmpty()) {
                Collections.sort(systemTimes, Inter.byAbscissa);

                for (MeasureStack stack : system.getMeasureStacks()) {
                    boolean found = false;

                    for (Inter inter : stack.filter(systemTimes)) {
                        TimeInter ts = (TimeInter) inter;
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

    //-------------------//
    // retrieveDurations //
    //-------------------//
    /**
     * Analyze a range of stacks governed by a time signature (if any for the start).
     * Retrieve typical stack duration and check with the time signature.
     */
    private void retrieveDurations ()
    {
        // Launch a raw processing to determine expected measure duration
        // on the range of first system & stacks before first time signature
        final Iterator<Range> it = ranges.iterator();

        // Current range
        Range range = it.next();

        for (SystemInfo system : page.getSystems()) {
            // Select relevant rhythm inters at system level
            List<Inter> systemInters = system.getSig().inters(StackTuner.rhythmClasses);

            // Process stack after stack
            for (MeasureStack stack : system.getMeasureStacks()) {
                try {
                    logger.debug("\n--- Raw processing {} ---", stack);
                    new StackTuner(stack, false).process(systemInters, null, null);
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
                "{}{} Durations avgGuess:{} topGuess:{} avgValue:{} stacks:{} voices:{} {}",
                page.getSheet().getLogPrefix(),
                range,
                avgGuess,
                topGuess,
                val,
                stackNb,
                voiceNb,
                histo);

        return avgGuess;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Range //
    //-------//
    /**
     * Describes a range of stack governed by a time signature.
     * The very first range may have no time signature.
     */
    private static class Range
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int startId; // Id of first stack

        int stopId; // Id of last stack

        TimeInter ts; // Time signature found in first stack of range, if any

        Rational duration; // Inferred duration for the range

        //~ Constructors ---------------------------------------------------------------------------
        public Range (int startId,
                      TimeInter ts)
        {
            this.startId = startId;
            this.ts = ts;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{Range");
            sb.append(' ').append(startId).append('-').append(stopId);

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
