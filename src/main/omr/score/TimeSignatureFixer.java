//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              T i m e S i g n a t u r e F i x e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Voice;

import omr.sig.inter.AbstractTimeInter;

import omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code TimeSignatureFixer} can visit the score hierarchy to check whether each
 * of the time signatures is consistent with most of measures intrinsic time signature.
 *
 * @author Hervé Bitteur
 */
public class TimeSignatureFixer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TimeSignatureFixer.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TimeSignatureFixer object.
     */
    public TimeSignatureFixer ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Page hierarchy entry point
     *
     * @param page the page to process
     */
    public void process (Page page)
    {
        try {
            SystemInfo firstSystem = page.getFirstSystem();
            MeasureStack stack = firstSystem.getMeasureStacks().get(0);

            // MeasureStack that starts a range of measures with an explicit time sig
            MeasureStack startStack = null;

            // Is this starting time sig a manual one?
            boolean startManual = false;

            // End of range (right before another time sig, or last measure of the page)
            MeasureStack stopStack = null;

            // Remember if current signature is manual, and thus should not be updated
            WrappedBoolean isManual = new WrappedBoolean(false);

            while (stack != null) {
                if (hasTimeSig(stack, isManual)) {
                    if ((startStack != null) && !startManual) {
                        // Complete the ongoing time sig analysis
                        checkTimeSigs(startStack, stopStack);
                    }

                    // Start a new analysis
                    startStack = stack;
                    startManual = isManual.isSet();
                }

                stopStack = stack;
                stack = stack.getFollowingInPage();
            }

            if (startStack != null) {
                if (!startManual) {
                    // Complete the ongoing time sig analysis
                    checkTimeSigs(startStack, stopStack);
                }
            } else {
                // Whole page without explicit time signature
                checkTimeSigs(
                        firstSystem.getFirstMeasureStack(),
                        page.getLastSystem().getLastMeasureStack());
            }
        } catch (Exception ex) {
            logger.warn("TimeSignatureFixer. Error processing " + page, ex);
        }
    }

    //
    //    //---------------//
    //    // visit MeasureStack //
    //    //---------------//
    //    @Override
    //    public boolean visit (MeasureStack measure)
    //    {
    //        measure.setExpectedDuration(null);
    //
    //        return false;
    //    }
    //
    //---------------//
    // checkTimeSigs //
    //---------------//
    /**
     * Perform the analysis on the provided range of measures, retrieving the most
     * significant intrinsic time sig as determined by measures chords.
     * Based on this "intrinsic" time information, modify the explicit time signatures accordingly.
     *
     * @param startStack beginning of the measure range
     * @param stopStack  end of the measure range
     */
    private void checkTimeSigs (MeasureStack startStack,
                                MeasureStack stopStack)
    {
        logger.debug(
                "checkTimeSigs on measure stacks {}..{}",
                startStack.getPageId(),
                stopStack.getPageId());

        // Retrieve the best possible time signature(s)
        final Map<TimeRational, Integer> sigMap = retrieveBestSigs(startStack, stopStack);

        // Sort them by decreasing occurrences
        List<TimeRational> sigs = new ArrayList<TimeRational>(sigMap.keySet());
        Collections.sort(
                sigs,
                new Comparator<TimeRational>()
                {
                    @Override
                    public int compare (TimeRational t1,
                                        TimeRational t2)
                    {
                        return Integer.compare(sigMap.get(t2), sigMap.get(t1));
                    }
                });
        logger.debug(
                "{}Best inferred time sigs in [M#{},M#{}]: {}",
                startStack.getSystem().getPage().getSheet().getLogPrefix(),
                startStack.getIdValue(),
                stopStack.getIdValue(),
                sigs);

        if (!sigs.isEmpty()) {
            TimeRational bestRational = sigs.get(0);
            // BINGO
            //            if (!OldTimeSignature.isAcceptable(bestRational)) {
            //                logger.debug("Time sig too uncommon: {}", bestRational);
            //
            //                return;
            //            }
            //
            logger.debug("Best sig: {}", bestRational);

            // Loop on every staff in the vertical startStack
            for (Measure measure : startStack.getMeasures()) {
                for (Staff staff : measure.getPart().getStaves()) {
                    int staffIndexInPart = measure.getPart().getStaves().indexOf(staff);
                    AbstractTimeInter time = measure.getTimeSignature(staffIndexInPart);

                    if (time != null) {
                        try {
                            TimeRational timeRational = time.getTimeRational();

                            if ((timeRational == null) || !timeRational.equals(bestRational)) {
                                logger.info(
                                        "{}Measure#{} {}T{} {}->{}",
                                        measure.getPart().getSystem().getSheet().getLogPrefix(),
                                        measure.getStack().getPageId(),
                                        staff.getId(),
                                        staff.getId(),
                                        timeRational,
                                        bestRational);
                                time.modify(null, bestRational);
                            }
                        } catch (Exception ex) {
                            logger.warn("Could not check time signature for " + time + "ex:" + ex);
                        }
                    }
                }
            }
        } else {
            logger.debug("No best sig!");
        }
    }

    //------------//
    // hasTimeSig //
    //------------//
    /**
     * Check whether the provided stack contains at least one explicit time signature.
     *
     * @param stack the provided measure stack
     * @return true if a time sig exists in some staff of the stack
     */
    private boolean hasTimeSig (MeasureStack stack,
                                WrappedBoolean isManual)
    {
        isManual.set(false);

        boolean found = false;

        for (Measure measure : stack.getMeasures()) {
            for (Staff staff : measure.getPart().getStaves()) {
                int staffIndexInPart = measure.getPart().getStaves().indexOf(staff);
                AbstractTimeInter time = measure.getTimeSignature(staffIndexInPart);

                if (time != null) {
                    logger.debug("Stack#{} T{} {}", stack.getPageId(), staff.getId(), time);
                    //
                    //                    if (time.isManual()) {
                    //                        isManual.set(true);
                    //                    }
                    found = true;
                }
            }
        }

        return found;
    }

    //------------------//
    // retrieveBestSigs //
    //------------------//
    /**
     * By inspecting each voice in the provided range of measure stacks, determine
     * the best intrinsic time signatures.
     *
     * @param startStack beginning of the stack range
     * @param stopStack  end of the stack range
     * @return a map of possible time signatures, with their occurrence number
     */
    private Map<TimeRational, Integer> retrieveBestSigs (MeasureStack startStack,
                                                         MeasureStack stopStack)
    {
        // Retrieve the significant measure informations
        final Map<TimeRational, Integer> sigs = new LinkedHashMap<TimeRational, Integer>();
        MeasureStack stack = startStack;

        // Loop on stack range
        while (true) {
            // Retrieve info
            logger.debug("Checking stack#{}", stack.getPageId());

            if (logger.isDebugEnabled()) {
                stack.printVoices(null);
            }

            for (Voice voice : stack.getVoices()) {
                TimeRational timeRational = voice.getInferredTimeSignature();
                logger.debug("Voice#{} time inferred: {}", voice.getId(), timeRational);

                if (timeRational != null) {
                    // Update histogram
                    Integer sum = sigs.get(timeRational);

                    if (sum == null) {
                        sum = 1;
                    } else {
                        sum += 1;
                    }

                    sigs.put(timeRational, sum);
                }
            }

            if (stack == stopStack) {
                break; // We are through
            } else {
                stack = stack.getFollowingInPage(); // Move to next measure stack
            }
        }

        return sigs;
    }
}
