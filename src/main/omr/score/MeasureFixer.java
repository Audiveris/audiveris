//----------------------------------------------------------------------------//
//                                                                            //
//                          M e a s u r e F i x e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.glyph.Shape;

import omr.math.Rational;

import omr.score.entity.Barline;
import omr.score.entity.Measure;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.entity.Voice;
import omr.score.visitor.AbstractScoreVisitor;

import omr.util.TreeNode;

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
 * <li>Detect inside barlines (empty measures) </li>
 * <li>Assign final page-based Measure ids</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class MeasureFixer
        extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(MeasureFixer.class);

    //~ Instance fields --------------------------------------------------------
    private int im; // Current measure index in system

    private List<Measure> verticals = null; // Current vertical measures

    private Rational measureTermination = null; // Current termination

    private ScoreSystem system; // Current system

    // Information to remember from previous vertical measure
    private List<Measure> prevVerticals = null; // Previous vertical measures

    private Rational prevMeasureTermination = null; // Previous termination

    /** The latest id assigned to a measure (in the previous system) */
    private Integer prevSystemLastId = null;

    /** The latest id assigned to a measure (in the current system) */
    private Integer lastId = null;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // MeasureFixer //
    //--------------//
    /**
     * Creates a new MeasureFixer object.
     */
    public MeasureFixer ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // visit Page //
    //------------//
    @Override
    public boolean visit (Page page)
    {
        logger.debug("{} Visiting {}", getClass().getSimpleName(), page);
        page.acceptChildren(this);

        // Remember the number of measures in this page
        page.computeMeasureCount();

        // Remember the delta of measure ids in this page
        page.setDeltaMeasureId(
                page.getLastSystem().getLastPart().getLastMeasure().getIdValue());

        return false;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        logger.debug("{} Visiting {}", getClass().getSimpleName(), score);
        score.acceptChildren(this);

        return false;
    }

    //--------------//
    // visit System //
    //--------------//
    /**
     * Here, we work sequentially on "vertical" measures in this system.
     * Such a vertical measure is the collection of measures, across all parts
     * of the system, one below the other. They share the same id and the
     * same status (such as implicit).
     *
     * @return false
     */
    @Override
    public boolean visit (ScoreSystem system)
    {
        logger.debug("{} Visiting {}", getClass().getSimpleName(), system);

        this.system = system;

        // First, compute voices terminations
        system.acceptChildren(this);

        // Measure indices to remove
        List<Integer> toRemove = new ArrayList<>();

        // Use a loop on "vertical" measures, across all system parts
        final int imMax = system.getFirstRealPart().getMeasures().size() - 1;

        for (im = 0; im <= imMax; im++) {
            logger.debug("im:{}", im);
            verticals = verticalsOf(system, im);

            // Check if all voices in all parts exhibit the same termination
            measureTermination = getMeasureTermination();

            logger.debug("measureFinal:{}{}",
                    measureTermination,
                    (measureTermination != null)
                    ? ("=" + measureTermination)
                    : "");

            if (isEmpty()) {
                logger.debug("empty");

                // All this vertical measure is empty (no notes/rests)
                // We will merge with the following measure, if any
                if (im < imMax) {
                    setId((lastId != null) ? (lastId + 1)
                            : ((prevSystemLastId != null)
                            ? (prevSystemLastId + 1) : 1),
                            false);
                }
            } else if (isPickup()) {
                logger.debug("pickup");
                setImplicit();
                setId((lastId != null) ? (-lastId)
                        : ((prevSystemLastId != null)
                        ? (-prevSystemLastId) : 0),
                        false);
            } else if (isSecondRepeatHalf()) {
                logger.debug("secondHalf");

                // Shorten actual duration for (non-implicit) previous measure
                shortenFirstHalf();

                setImplicit();
                setId((lastId != null) ? lastId : prevSystemLastId, true);
            } else if (isRealStart()) {
                logger.debug("realStart");
                merge(); // Merge with previous vertical measure
                toRemove.add(im);
            } else {
                logger.debug("normal");

                // Normal measure
                setId((lastId != null) ? (lastId + 1)
                        : ((prevSystemLastId != null)
                        ? (prevSystemLastId + 1) : 1),
                        false);
            }

            // For next measure
            prevVerticals = verticals;
            prevMeasureTermination = measureTermination;
        }

        removeMeasures(toRemove, system); // Remove measures if any

        // For next system
        prevSystemLastId = lastId;
        lastId = null;

        return false; // Dead end
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        // Check duration sanity in this measure
        // Record forward items in voices when needed
        measure.checkDuration();

        return false;
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
     * Check for an empty measure: perhaps clef and key sig, but no note
     * or rest
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
     * @return true if so
     */
    private boolean isPickup ()
    {
        return (system.getChildIndex() == 0) && (im == 0)
               && (measureTermination != null)
               && (measureTermination.compareTo(Rational.ZERO) < 0);
    }

    //-------------//
    // isRealStart //
    //-------------//
    /**
     * Check for a measure in second position, while following an empty
     * measure
     *
     * @return true if so
     */
    private boolean isRealStart ()
    {
        return (im == 1)
               && (prevVerticals.get(0).getActualDuration().equals(
                Rational.ZERO));
        ///&& (measureTermination != null); // Too strict!
    }

    //--------------------//
    // isSecondRepeatHalf //
    //--------------------//
    /**
     * Check for an implicit measure as the second half of a repeat
     * sequence
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
        if ((measureTermination == null)
            || (measureTermination.compareTo(Rational.ZERO) >= 0)) {
            return false;
        }

        // Check for a suitable repeat barline in between
        Measure prevMeasure = prevVerticals.get(0);
        Barline barline = prevMeasure.getBarline();

        if (barline == null) {
            return false;
        }

        Shape shape = barline.getShape();

        if ((shape != Shape.RIGHT_REPEAT_SIGN)
            && (shape != Shape.BACK_TO_BACK_REPEAT_SIGN)) {
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
     * Remove the vertical measures that correspond to the provided
     * indices
     *
     * @param toRemove sequence of indices to remove, perhaps empty
     * @param system   the containing system
     */
    private void removeMeasures (List<Integer> toRemove,
                                 ScoreSystem system)
    {
        if (toRemove.isEmpty()) {
            return;
        }

        for (TreeNode pn : system.getParts()) {
            SystemPart part = (SystemPart) pn;

            int index = -1;

            for (Iterator<TreeNode> it = part.getMeasures().iterator(); it.
                    hasNext();) {
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
    private void setId (int id,
                        boolean isSecondHalf)
    {
        logger.debug("-> id={}{}", id, isSecondHalf ? " SH" : "");

        for (Measure measure : verticals) {
            measure.setPageId(id, isSecondHalf);
        }

        // Side effect: remember the numeric value as last id
        lastId = id;
    }

    //-------------//
    // setImplicit //
    //-------------//
    private void setImplicit ()
    {
        for (Measure measure : verticals) {
            measure.setImplicit();
        }
    }

    //------------------//
    // shortenFirstHalf //
    //------------------//
    private void shortenFirstHalf ()
    {
        for (Measure measure : prevVerticals) {
            measure.shorten(prevMeasureTermination);
            measure.setFirstHalf(true);
        }
    }

    //-------------//
    // verticalsOf //
    //-------------//
    /**
     * Report the sequence of vertical measures for a given index
     *
     * @param index the index in the parent part
     * @return the vertical collection of measure with the same index
     */
    private List<Measure> verticalsOf (ScoreSystem system,
                                       int index)
    {
        List<Measure> measures = new ArrayList<>();

        for (TreeNode node : system.getParts()) {
            SystemPart part = (SystemPart) node;
            measures.add((Measure) part.getMeasures().get(index));
        }

        return measures;
    }
}
