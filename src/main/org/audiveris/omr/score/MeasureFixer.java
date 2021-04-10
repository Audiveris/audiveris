//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e F i x e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.score;

import java.util.ArrayList;
import java.util.List;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code MeasureFixer} visits the score hierarchy to fix measures:.
 * <ul>
 * <li>Detect implicit measures (as pickup measures)</li>
 * <li>Detect first half repeat measures</li>
 * <li>Detect implicit measures (as second half repeats)</li>
 * <li>Detect inside barlines (empty measures)</li>
 * <li>Detect cautionary measures (CKT changes at end of staff)</li>
 * <li>Assign final page-based measure IDs</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class MeasureFixer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MeasureFixer.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Current stack of measures in current system. */
    private MeasureStack stack;

    /** Termination of current measure stack. */
    private Rational stackTermination;

    /** Previous measure stack. */
    private MeasureStack prevStack;

    /** Termination of previous measure stack. */
    private Rational prevStackTermination;

    /** The latest id assigned to a measure stack. (in the previous system) */
    private Integer prevSystemLastId;

    /** The latest id assigned to a measure stack. (in the current system) */
    private Integer lastId;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new MeasureFixer object.
     */
    public MeasureFixer ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // process Page //
    //--------------//
    /**
     * Process the provided page.
     *
     * @param page the page to process
     */
    public void process (Page page)
    {
        logger.debug("{} Visiting {}", getClass().getSimpleName(), page);

        for (SystemInfo system : page.getSystems()) {
            process(system);
        }

        // Remember the number of measures in this page
        page.computeMeasureCount();

        // Remember the delta of measure ids in this page
        page.setDeltaMeasureId(page.getLastSystem().getLastStack().getIdValue());
    }

    //---------------//
    // process Score //
    //---------------//
    /**
     * Process the provided score.
     * <p>
     * Currently not used.
     *
     * @param score the score to process
     */
    public void process (Score score)
    {
        logger.debug("{} Visiting {}", getClass().getSimpleName(), score);

        for (Page page : score.getPages()) {
            process(page);
        }
    }

    //---------------------//
    // getStackTermination //
    //---------------------//
    private Rational getStackTermination ()
    {
        Rational termination = null;

        for (Voice voice : stack.getVoices()) {
            Rational voiceTermination = voice.getTermination();

            if (voiceTermination != null) {
                if (termination == null) {
                    termination = voiceTermination;
                } else if (!voiceTermination.equals(termination)) {
                    logger.debug("Non-consistent voices terminations");

                    return null;
                }
            }
        }

        return termination;
    }

    //---------//
    // isEmpty //
    //---------//
    /**
     * Check for an empty stack: perhaps clef and key or time, but no note or rest.
     *
     * @return true if so
     */
    private boolean isEmpty (MeasureStack stack)
    {
        final Rational actualDuration = stack.getActualDuration();

        return (actualDuration != null) ? actualDuration.equals(Rational.ZERO) : false;
    }

    //----------//
    // isPickup //
    //----------//
    /**
     * Check for an implicit pickup stack at the beginning of a system
     *
     * @param stack the stack to check
     * @return true if so
     */
    private boolean isPickup (MeasureStack stack)
    {
        final SystemInfo system = stack.getSystem();

        return (system.getIndexInPage() == 0) && (stack == system.getFirstStack())
                       && (stackTermination != null) && (stackTermination.compareTo(Rational.ZERO)
                                                                 < 0);
    }

    //-------------//
    // isRealStart //
    //-------------//
    /**
     * Check for a stack in second position, while following an empty stack
     *
     * @param stack the stack to check
     * @return true if so
     */
    private boolean isRealStart (MeasureStack stack)
    {
        final int im = stack.getSystem().getStacks().indexOf(stack);

        return (im == 1) && isEmpty(prevStack);
    }

    //--------------------//
    // isSecondRepeatHalf //
    //--------------------//
    /**
     * Check for an implicit stack as the second half of a repeat sequence
     *
     * @return true if so
     */
    private boolean isSecondRepeatHalf ()
    {
        // Check for partial first half
        if ((prevStackTermination == null) || (prevStackTermination.compareTo(Rational.ZERO) >= 0)) {
            return false;
        }

        // Check for partial second half
        if ((stackTermination == null) || (stackTermination.compareTo(Rational.ZERO) >= 0)) {
            return false;
        }

        // Check for a suitable repeat barline in between
        Measure prevMeasure = prevStack.getFirstMeasure();
        PartBarline barline = prevMeasure.getRightPartBarline();

        if ((barline == null) || !barline.isRightRepeat()) {
            return false;
        }

        // Check for an exact duration sum (TODO: is this too strict?)
        return prevStackTermination.plus(stackTermination).abs()
                .equals(prevStack.getExpectedDuration());
    }

    //----------------//
    // process System //
    //----------------//
    /**
     * Here, we work sequentially on measure stacks in this system.
     */
    private void process (SystemInfo system)
    {
        logger.debug("{} processing {}", getClass().getSimpleName(), system);

        // To gather all warnings about missing time signature in this system
        final List<String> warnings = new ArrayList<>();

        // Loop on stacks in system (the list of stacks being modified on the fly)
        for (int idx = 0; idx < system.getStacks().size(); idx++) {
            stack = system.getStacks().get(idx);

            // First, compute voices terminations
            if (stack.getExpectedDuration() != null) {
                stack.checkDuration();
            } else {
                warnings.add(stack.getPageId());
            }

            // Check if all voices in all parts exhibit the same termination
            stackTermination = getStackTermination();

            logger.debug(
                    "stackFinal:{}{}",
                    stackTermination,
                    (stackTermination != null) ? ("=" + stackTermination) : "");

            if (isEmpty(stack)) {
                logger.debug("empty");

                // This whole stack is empty (no notes/rests, hence no voices)
                // We will merge with the following stack, if any
                if (stack != system.getLastStack()) {
                    setId((lastId != null)
                            ? (lastId + 1)
                            : ((prevSystemLastId != null) ? (prevSystemLastId + 1) : 1));
                } else {
                    // This is just a cautionary stack at right end of system
                    logger.debug("cautionary");
                    stack.setCautionary();

                    // We use the same id value that preceding stack, with "C" annotation
                    if (lastId != null) {
                        setId(lastId);
                    }
                }
            } else if (isPickup(stack)) {
                logger.debug("pickup");
                stack.setPickup();
                setId((lastId != null)
                        ? (-lastId)
                        : ((prevSystemLastId != null) ? (-prevSystemLastId) : 0));
            } else if (isSecondRepeatHalf()) {
                logger.debug("secondHalf");

                // Shorten actual duration for (non-implicit) previous stack
                prevStack.shorten(prevStackTermination);
                prevStack.setFirstHalf();

                stack.setSecondHalf();
                setId((lastId != null) ? lastId : prevSystemLastId);
            } else if (isRealStart(stack)) {
                logger.debug("realStart");
                prevStack.mergeWithRight(stack);

                if (stack.isRepeat(HorizontalSide.LEFT)) {
                    prevStack.addRepeat(HorizontalSide.LEFT);
                }

                system.removeStack(stack);
                idx--;
                stack = prevStack;
            } else {
                logger.debug("normal");

                // Normal measure
                setId(
                        (lastId != null) ? (lastId + 1)
                                : ((prevSystemLastId != null) ? (prevSystemLastId + 1) : 1));
            }

            // Inspect stack for repeat signs
            stack.computeRepeats();

            // For next measure stack
            prevStack = stack;
            prevStackTermination = stackTermination;
        }

        if (!warnings.isEmpty()) {
            logger.warn("{} No target duration for measures local IDs {}"
                                + ", please check time signatures",
                        system, warnings);
        }

        // For next system
        prevSystemLastId = lastId;
        lastId = null;
    }

    //-------//
    // setId //
    //-------//
    private void setId (int id)
    {
        logger.debug("-> id={} left:{} right:{}", id, stack.getLeft(), stack.getRight());

        stack.setIdValue(id);

        // Side effect: remember the numeric value as last id
        lastId = id;
    }
}
