//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e F i x e r                                     //
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
package omr.score;

import omr.math.Rational;

import omr.sheet.Part;
import omr.sheet.PartBarline;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code MeasureFixer} visits the score hierarchy to fix measures:.
 * <ul>
 * <li>Detect implicit measures (as pickup measures)</li>
 * <li>Detect first half repeat measures</li>
 * <li>Detect implicit measures (as second half repeats)</li>
 * <li>Detect inside barlines (empty measures) </li>
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
    public void process (Page page)
    {
        logger.debug("{} Visiting {}", getClass().getSimpleName(), page);

        for (SystemInfo system : page.getSystems()) {
            process(system);
        }

        // Remember the number of measures in this page
        page.computeMeasureCount();

        // Remember the delta of measure ids in this page
        page.setDeltaMeasureId(page.getLastSystem().getLastMeasureStack().getIdValue());
    }

    //---------------//
    // process Score //
    //---------------//
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
    private boolean isEmpty ()
    {
        return stack.getCurrentDuration().equals(Rational.ZERO);
    }

    //----------//
    // isPickup //
    //----------//
    /**
     * Check for an implicit pickup stack at the beginning of a system
     *
     * @param system the containing system
     * @return true if so
     */
    private boolean isPickup (int im,
                              SystemInfo system)
    {
        return (system.getIndexInPage() == 0) && (im == 0) && (stackTermination != null)
               && (stackTermination.compareTo(Rational.ZERO) < 0);
    }

    //-------------//
    // isRealStart //
    //-------------//
    /**
     * Check for a stack in second position, while following an empty stack
     *
     * @return true if so
     */
    private boolean isRealStart (int im)
    {
        return (im == 1) && (prevStack.getCurrentDuration().equals(Rational.ZERO));

        ///&& (stackTermination != null); // Too strict!
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
        PartBarline barline = prevMeasure.getRightBarline();

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

        // Stack indices to remove
        List<Integer> toRemove = new ArrayList<Integer>();

        // Use a loop on stacks, across all system parts
        final int idxMax = system.getMeasureStacks().size() - 1;

        for (int idx = 0; idx <= idxMax; idx++) {
            logger.debug("idx:{}", idx);
            stack = system.getMeasureStacks().get(idx);
            // First, compute voices terminations
            stack.checkDuration();

            // Check if all voices in all parts exhibit the same termination
            stackTermination = getStackTermination();

            logger.debug(
                    "stackFinal:{}{}",
                    stackTermination,
                    (stackTermination != null) ? ("=" + stackTermination) : "");

            if (isEmpty()) {
                logger.debug("empty");

                // This whole stack is empty (no notes/rests, hence no voices)
                // We will merge with the following stack, if any
                if (idx < idxMax) {
                    setId(
                            (lastId != null) ? (lastId + 1)
                                    : ((prevSystemLastId != null)
                                            ? (prevSystemLastId + 1) : 1));
                } else {
                    // This is just a cautionary stack at right end of system
                    logger.debug("cautionary");
                    stack.setCautionary();

                    // We use the same id value that preceding stack, with "C" annotation
                    if (lastId != null) {
                        setId(lastId);
                    }
                }
            } else if (isPickup(idx, system)) {
                logger.debug("pickup");
                stack.setPickup();
                setId(
                        (lastId != null) ? (-lastId)
                                : ((prevSystemLastId != null) ? (-prevSystemLastId) : 0));
            } else if (isSecondRepeatHalf()) {
                logger.debug("secondHalf");

                // Shorten actual duration for (non-implicit) previous stack
                prevStack.shorten(prevStackTermination);
                prevStack.setFirstHalf();

                stack.setSecondHalf();
                setId((lastId != null) ? lastId : prevSystemLastId);
            } else if (isRealStart(idx)) {
                logger.debug("realStart");
                prevStack.mergeWithRight(stack);
                toRemove.add(idx);
            } else {
                logger.debug("normal");

                // Normal measure
                setId(
                        (lastId != null) ? (lastId + 1)
                                : ((prevSystemLastId != null) ? (prevSystemLastId + 1) : 1));
            }

            // For next measure stack
            prevStack = stack;
            prevStackTermination = stackTermination;
        }

        removeStacks(toRemove, system); // Remove stacks if needed

        // For next system
        prevSystemLastId = lastId;
        lastId = null;
    }

    //--------------//
    // removeStacks //
    //--------------//
    /**
     * Remove the stacks that correspond to the provided indices
     *
     * @param toRemove sequence of indices to remove, perhaps empty
     * @param system   the containing system
     */
    private void removeStacks (List<Integer> toRemove,
                               SystemInfo system)
    {
        if (toRemove.isEmpty()) {
            return;
        }

        // Remove measures from their containing part
        for (Part part : system.getParts()) {
            int index = -1;

            for (Iterator<Measure> it = part.getMeasures().iterator(); it.hasNext();) {
                index++;
                it.next();

                if (toRemove.contains(index)) {
                    it.remove();
                }
            }
        }

        // Remove stacks from system
        int index = -1;

        for (Iterator<MeasureStack> it = system.getMeasureStacks().iterator(); it.hasNext();) {
            index++;
            it.next();

            if (toRemove.contains(index)) {
                it.remove();
            }
        }
    }

    //-------//
    // setId //
    //-------//
    private void setId (int id)
    {
        logger.debug("-> id={}", id);

        stack.setIdValue(id);

        // Side effect: remember the numeric value as last id
        lastId = id;
    }
}
