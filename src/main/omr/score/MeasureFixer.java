//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e F i x e r                                     //
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
import omr.score.entity.TimeSignature.InvalidTimeSignature;

import omr.sheet.Measure;
import omr.sheet.Part;
import omr.sheet.PartBarline;
import omr.sheet.SystemInfo;
import omr.sheet.Voice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class {@code MeasureFixer} visits the score hierarchy to fix measures:
 * <ul>
 * <li>Detect implicit measures (as pickup measures)</li>
 * <li>Detect first half repeat measures</li>
 * <li>Detect implicit measures (as second half repeats)</li>
 * <li>Detect inside bar-lines (empty measures) </li>
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
    /** Current "vertical" pile of measures in current system. */
    private List<Measure> verticals;

    /** Termination of current measure. */
    private Rational measureTermination;

    /** Vertical pile of previous measure. */
    private List<Measure> prevVerticals;

    /** Termination of previous measure. */
    private Rational prevMeasureTermination;

    /** The latest id assigned to a measure. (in the previous system) */
    private Integer prevSystemLastId;

    /** The latest id assigned to a measure. (in the current system) */
    private Integer lastId;

    //~ Constructors -------------------------------------------------------------------------------
    //--------------//
    // MeasureFixer //
    //--------------//
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
            visit(system);
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

    //-----------------------//
    // getMeasureTermination //
    //-----------------------//
    private Rational getMeasureTermination ()
    {
        Rational termination = null;

        for (Measure measure : verticals) {
            if (measure.isDummy()) {
                continue;
            }

            for (Voice voice : measure.getVoices()) {
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
        }

        return termination;
    }

    //---------//
    // isEmpty //
    //---------//
    /**
     * Check for an empty measure: perhaps clef and key sig, but no note or rest
     *
     * @return true if so
     */
    private boolean isEmpty ()
    {
        return verticals.get(0).getActualDuration().equals(Rational.ZERO);
    }

    //----------//
    // isPickup //
    //----------//
    /**
     * Check for an implicit pickup measure at the beginning of a system
     *
     * @param system the containing system
     * @return true if so
     */
    private boolean isPickup (int im,
                              SystemInfo system)
    {
        return (system.getIndexInPage() == 0) && (im == 0) && (measureTermination != null)
               && (measureTermination.compareTo(Rational.ZERO) < 0);
    }

    //-------------//
    // isRealStart //
    //-------------//
    /**
     * Check for a measure in second position, while following an empty measure
     *
     * @return true if so
     */
    private boolean isRealStart (int im)
    {
        return (im == 1) && (prevVerticals.get(0).getActualDuration().equals(Rational.ZERO));

        ///&& (measureTermination != null); // Too strict!
    }

    //--------------------//
    // isSecondRepeatHalf //
    //--------------------//
    /**
     * Check for an implicit measure as the second half of a repeat sequence
     *
     * @return true if so
     */
    private boolean isSecondRepeatHalf ()
    {
        // Check for partial first half
        if ((prevMeasureTermination == null)
            || (prevMeasureTermination.compareTo(Rational.ZERO) >= 0)) {
            return false;
        }

        // Check for partial second half
        if ((measureTermination == null) || (measureTermination.compareTo(Rational.ZERO) >= 0)) {
            return false;
        }

        // Check for a suitable repeat barline in between
        Measure prevMeasure = prevVerticals.get(0);
        PartBarline barline = prevMeasure.getBarline();

        if ((barline == null) || !barline.isRightRepeat()) {
            return false;
        }

        // Check for an exact duration sum (TODO: is this too strict?)
        try {
            return prevMeasureTermination.plus(measureTermination).abs().equals(
                    prevMeasure.getExpectedDuration());
        } catch (InvalidTimeSignature its) {
            return false;
        }
    }

    //-------------------//
    // mergeWithPrevious //
    //-------------------//
    /**
     * We have a real start, following an empty measure.
     * We need to merge the vertical measures, right into left
     */
    private void merge ()
    {
        for (int iLine = 0; iLine < verticals.size(); iLine++) {
            Measure left = prevVerticals.get(iLine);
            Measure right = verticals.get(iLine);
            left.mergeWithRight(right);
        }
    }

    //----------------//
    // removeMeasures //
    //----------------//
    /**
     * Remove the vertical measures that correspond to the provided indices
     *
     * @param toRemove sequence of indices to remove, perhaps empty
     * @param system   the containing system
     */
    private void removeMeasures (List<Integer> toRemove,
                                 SystemInfo system)
    {
        if (toRemove.isEmpty()) {
            return;
        }

        for (Part part : system.getAllParts()) {
            int index = -1;

            for (Iterator<Measure> it = part.getMeasures().iterator(); it.hasNext();) {
                index++;
                it.next();

                if (toRemove.contains(index)) {
                    it.remove();
                }
            }
        }
    }

    //-------//
    // setId //
    //-------//
    private void setId (int id)
    {
        logger.debug("-> id={}", id);

        for (Measure measure : verticals) {
            measure.setIdValue(id);
        }

        // Side effect: remember the numeric value as last id
        lastId = id;
    }

    //-----------//
    // setPickup //
    //-----------//
    private void setPickup ()
    {
        for (Measure measure : verticals) {
            measure.setPickup();
        }
    }

    //---------------//
    // setSecondHalf //
    //---------------//
    private void setSecondHalf ()
    {
        for (Measure measure : verticals) {
            measure.setSecondHalf();
        }
    }

    //------------------//
    // shortenFirstHalf //
    //------------------//
    private void shortenFirstHalf ()
    {
        for (Measure measure : prevVerticals) {
            measure.shorten(prevMeasureTermination);
            measure.setFirstHalf();
        }
    }

    //-------------//
    // verticalsOf //
    //-------------//
    /**
     * Report the sequence of vertical measures for a given index
     *
     * @param index the index in the containing part
     * @return the vertical collection of measure with the same index
     */
    private List<Measure> verticalsOf (SystemInfo system,
                                       int index)
    {
        List<Measure> measures = new ArrayList<Measure>();

        for (Part part : system.getAllParts()) {
            measures.add(part.getMeasures().get(index));
        }

        return measures;
    }

    //--------------//
    // visit System //
    //--------------//
    /**
     * Here, we work sequentially on "vertical" measures in this system.
     * Such a vertical measure is the collection of measures, across all parts
     * of the system, one below the other. They share the same id and the
     * same status (such as implicit).
     */
    private void visit (SystemInfo system)
    {
        logger.debug("{} Visiting {}", getClass().getSimpleName(), system);

        // First, compute voices terminations
        for (Part part : system.getAllParts()) {
            for (Measure measure : part.getMeasures()) {
                // Check duration sanity in this measure
                // Record forward items in voices when needed
                measure.checkDuration();
            }
        }

        // Measure indices to remove
        List<Integer> toRemove = new ArrayList<Integer>();

        // Use a loop on "vertical" measures, across all system parts
        final int imMax = system.getFirstRealPart().getMeasures().size() - 1;

        for (int im = 0; im <= imMax; im++) {
            logger.debug("im:{}", im);
            verticals = verticalsOf(system, im);

            // Check if all voices in all parts exhibit the same termination
            measureTermination = getMeasureTermination();

            logger.debug(
                    "measureFinal:{}{}",
                    measureTermination,
                    (measureTermination != null) ? ("=" + measureTermination) : "");

            if (isEmpty()) {
                logger.debug("empty");

                // All this vertical measure is empty (no notes/rests)
                // We will merge with the following measure, if any
                if (im < imMax) {
                    setId(
                            (lastId != null) ? (lastId + 1)
                                    : ((prevSystemLastId != null)
                                            ? (prevSystemLastId + 1) : 1));
                }
            } else if (isPickup(im, system)) {
                logger.debug("pickup");
                setPickup();
                setId(
                        (lastId != null) ? (-lastId)
                                : ((prevSystemLastId != null) ? (-prevSystemLastId) : 0));
            } else if (isSecondRepeatHalf()) {
                logger.debug("secondHalf");

                // Shorten actual duration for (non-implicit) previous measure
                shortenFirstHalf();

                setSecondHalf();
                setId((lastId != null) ? lastId : prevSystemLastId);
            } else if (isRealStart(im)) {
                logger.debug("realStart");
                merge(); // Merge with previous vertical measure
                toRemove.add(im);
            } else {
                logger.debug("normal");

                // Normal measure
                setId(
                        (lastId != null) ? (lastId + 1)
                                : ((prevSystemLastId != null) ? (prevSystemLastId + 1) : 1));
            }

            // For next measure
            prevVerticals = verticals;
            prevMeasureTermination = measureTermination;
        }

        removeMeasures(toRemove, system); // Remove measures if any

        // For next system
        prevSystemLastId = lastId;
        lastId = null;
    }
}
