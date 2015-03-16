//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               D u r a t i o n R e t r i e v e r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.math.Rational;

import omr.score.entity.Page;

import omr.sheet.MeasureStack;
import omr.sheet.SystemInfo;
import omr.sheet.Voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code DurationRetriever} can process a page hierarchy to compute
 * the actual duration of every measure.
 * <p>
 * This class does not seem to be used anymore...
 *
 * @author Hervé Bitteur
 */
public class DurationRetriever
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(DurationRetriever.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Map of Measure id -> Measure duration, whatever the containing part. */
    private final Map<Integer, Rational> measureDurations = new TreeMap<Integer, Rational>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code DurationRetriever} object.
     */
    public DurationRetriever ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // process Page //
    //--------------//
    /**
     * Page hierarchy entry point
     *
     * @param page the page for which measure durations are to be computed
     */
    public void process (Page page)
    {
        // Delegate to system
        for (SystemInfo system : page.getSystems()) {
            processSystem(system);
        }
    }

    //---------------//
    // process Score //
    //---------------//
    /**
     * Score hierarchy entry point, to delegate to all pages
     *
     * @param score the score to process
     */
    public void process (Score score)
    {
        // Delegate to pages
        for (Page page : score.getPages()) {
            process(page);
        }
    }

    //---------//
    // process //
    //---------//
    /**
     * Process the measure stack.
     *
     * @param stack measure stack to process
     */
    public void process (MeasureStack stack)
    {
        try {
            Rational measureDur = stack.getCurrentDuration();

            if (!measureDur.equals(Rational.ZERO)) {
                // Make sure the measure duration is not bigger than limit
                if (measureDur.compareTo(stack.getExpectedDuration()) <= 0) {
                    stack.setActualDuration(measureDur);
                } else {
                    stack.setActualDuration(stack.getExpectedDuration());
                }

                measureDurations.put(stack.getIdValue(), measureDur);
                logger.info("{}: {}", stack.getPageId(), measureDur);
            } else if (!stack.getWholeRestChords().isEmpty()) {
                Rational dur = measureDurations.get(stack.getIdValue());

                if (dur != null) {
                    stack.setActualDuration(dur);
                } else {
                    stack.setActualDuration(stack.getExpectedDuration());
                }
            }

            stack.printVoices(null);
            logger.info(
                    "Stack#{} actualDuration: {} currentDuration: {}",
                    stack.getPageId(),
                    stack.getActualDuration(),
                    measureDur);

            for (Voice voice : stack.getVoices()) {
                Rational voiceDur = voice.getDuration();
                logger.info("{} ends at {}", voice, voiceDur);

                if (voiceDur != null) {
                    if (voiceDur.compareTo(stack.getActualDuration()) > 0) {
                        logger.warn("{} Excess detected in {}", stack, voice);
                    } else if (voiceDur.compareTo(stack.getActualDuration()) < 0) {
                        // If voice made of rests, delete it
                        if (voice.isOnlyRest()) {
                            logger.warn("{} Abnormal rest-only {}", stack, voice);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.warn(getClass().getSimpleName() + " Error visiting " + stack, ex);
        }
    }

    //---------------//
    // processSystem //
    //---------------//
    /**
     * System processing. The rest of processing is directly delegated to the measures
     *
     * @param system the system to process
     */
    private void processSystem (SystemInfo system)
    {
        logger.debug("Visiting {}", system);

        // Browse the measure stacks
        for (MeasureStack stack : system.getMeasureStacks()) {
            process(stack);
        }

        logger.debug("Durations:{}", measureDurations);
    }
}
