//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      R h y t h m s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.math.Histogram;
import omr.math.Rational;

import omr.score.entity.Page;

import omr.sheet.MeasureStack;
import omr.sheet.Sheet;
import omr.sheet.StackTuner;
import omr.sheet.SystemInfo;
import omr.sheet.SystemVoiceFixer;
import omr.sheet.Voice;

import omr.sig.inter.Inter;
import omr.sig.inter.TimeInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code RhythmsStep} is a comprehensive step that handles the timing of every
 * relevant item within a page.
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
public class RhythmsStep
        extends AbstractStep
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RhythmsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimingStep} object.
     */
    public RhythmsStep ()
    {
        super(
                Steps.RHYTHMS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Handle rhythms within measures");
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected void doit (Collection<SystemInfo> systems,
                         Sheet sheet)
            throws StepException
    {
        // Process each page of the sheet
        for (Page page : sheet.getPages()) {
            // Populate all stacks in page with potential time signatures
            final MeasureStack firstStack = populateTimeSignatures(page);

            // Does the page start with a time signature?
            final Rational initialDuration;

            if ((firstStack == null) || (firstStack.getIdValue() > 1)) {
                logger.info("No starting time signature in {}", page);

                if (firstStack != null) {
                    logger.info("First time signature found in {}", firstStack);
                }

                // Launch a raw processing to determine expected measure duration
                // on the range of first system & stacks before first time signature
                Map<MeasureStack, StackTuner> tuners = new LinkedHashMap<MeasureStack, StackTuner>();
                SystemLoop:
                for (SystemInfo system : page.getSystems()) {
                    // Select relevant rhythm inters at system level
                    List<Inter> systemInters = system.getSig().inters(StackTuner.rhythmClasses);

                    // Process stack after stack
                    for (MeasureStack stack : system.getMeasureStacks()) {
                        if (stack == firstStack) {
                            break SystemLoop;
                        }

                        try {
                            logger.info("\n--- Raw processing {} ---", stack);

                            StackTuner tuner = new StackTuner(stack, false);
                            tuners.put(stack, tuner);
                            tuner.process(systemInters, null);
                        } catch (Exception ex) {
                            logger.warn("Error on stack " + stack + " " + ex, ex);
                        }
                    }
                }

                // Use the CURRENT MATERIAL of voices to determine expected duration on this range
                initialDuration = retrieveExpectedDuration(page, firstStack);

                // Reset sig content for each stack processed (and thus perhaps modified)
                for (StackTuner tuner : tuners.values()) {
                    tuner.resetInitials();
                }
            } else {
                initialDuration = firstStack.getTimeSignature().getTimeRational().getValue();
            }

            // Precise processing
            Rational duration = initialDuration; // Expected duration for current stack

            for (SystemInfo system : page.getSystems()) {
                // Select relevant rhythm inters at system level
                List<Inter> systemInters = system.getSig().inters(StackTuner.rhythmClasses);

                // Process stack after stack
                for (MeasureStack stack : system.getMeasureStacks()) {
                    try {
                        if ((firstStack == null) || (stack.getIdValue() < firstStack.getIdValue())) {
                            duration = initialDuration;
                        } else {
                            TimeInter ts = stack.getTimeSignature();

                            if (ts != null) {
                                duration = ts.getTimeRational().getValue();
                            }
                        }

                        logger.info("\n--- Processing {} expectedDuration:{} ---", stack, duration);
                        new StackTuner(stack, true).process(systemInters, duration);
                    } catch (Exception ex) {
                        logger.warn("Error on stack " + stack + " " + ex, ex);
                    }
                }

                // Refine voices ids (and thus colors) across all measures of the system
                new SystemVoiceFixer(system).refine();
            }
        }

        //
        //        for (Page page : sheet.getPages()) {
        //            // Organize chords into time slots & voices
        //            for (SystemInfo system : page.getSystems()) {
        //                new SlotsBuilder(system).buildSlots();
        //            }
        //
        //            //            //            // 1/ Look carefully for time signatures
        //            //            //            page.accept(new TimeSignatureRetriever());
        //            //            //
        //            //            //            // 2/ Adapt time sigs to intrinsic measure & chord durations
        //            //            //            page.accept(new TimeSignatureFixer());
        //            //            //
        //            /**
        //             * Strategy.
        //             * The purpose is to perform as much processing at page level, rather than at score
        //             * level (which needs to handle the data from the sequence of all score pages).
        //             * <p>
        //             * We are optimistic, and try to infer time sig for the page at hand, even if the
        //             * page begins with no time signature. If some adjustments are needed at score level,
        //             * then relevant page data will be later updated accordingly.
        //             */
        //            // - Retrieve the actual duration of every measure
        //            new DurationRetriever().process(page);
        //
        //            //            // - Check all voices timing, assign forward items if needed.
        //            //            // - Detect special measures and assign proper measure ids
        //            //            new MeasureFixer().process(page);
        //        }
    }

    //------------------------//
    // populateTimeSignatures //
    //------------------------//
    /**
     * Populate the page stacks with the time signatures found.
     *
     * @param page the page to process
     * @return the first measure stack where a time signature is located
     */
    private MeasureStack populateTimeSignatures (Page page)
    {
        MeasureStack firstStack = null;

        for (SystemInfo system : page.getSystems()) {
            List<Inter> systemTimes = system.getSig().inters(TimeInter.class);
            Collections.sort(systemTimes, Inter.byAbscissa);

            for (MeasureStack stack : system.getMeasureStacks()) {
                for (Inter ts : stack.filter(systemTimes)) {
                    stack.addTimeSignature((TimeInter) ts);

                    if (firstStack == null) {
                        firstStack = stack;
                    }
                }
            }
        }

        return firstStack;
    }

    //--------------------------//
    // retrieveExpectedDuration //
    //--------------------------//
    /**
     * Determine a suitable value for a global expected measure until the provided
     * firstStack if any.
     * This is based on stacks / voices material found in this range.
     *
     * @param firstStack the first stack where a time signature is found, null for none
     * @return the guessed duration value
     */
    private Rational retrieveExpectedDuration (Page page,
                                               MeasureStack firstStack)
    {
        Histogram<Rational> histo = new Histogram<Rational>();
        int stackNb = 0;
        int voiceNb = 0;

        SystemLoop:
        for (SystemInfo system : page.getSystems()) {
            for (MeasureStack stack : system.getMeasureStacks()) {
                if ((firstStack != null) && (stack.getIdValue() >= firstStack.getIdValue())) {
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
        final Rational maxDur = new Rational(5, 4);
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
                "avgGuess:{} topGuess:{} avgValue:{} stacks:{} voices:{} {}",
                avgGuess,
                topGuess,
                val,
                stackNb,
                voiceNb,
                histo);

        return avgGuess;
    }
}
