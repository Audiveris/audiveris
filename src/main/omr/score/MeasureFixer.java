//----------------------------------------------------------------------------//
//                                                                            //
//                          M e a s u r e F i x e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.glyph.Shape;

import omr.log.Logger;

import omr.score.entity.Barline;
import omr.score.entity.Measure;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.SystemPart;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.entity.Voice;
import omr.score.visitor.AbstractScoreVisitor;

import omr.util.TreeNode;
import omr.util.WrappedBoolean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>MeasureFixer</code> visits the score hierarchy to fix measures:
 * <ul>
 * <li>Detect implicit measures (as pickup measures)</li>
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
    private static final Logger logger = Logger.getLogger(MeasureFixer.class);

    //~ Instance fields --------------------------------------------------------

    /** To flag a score modification */
    private final WrappedBoolean modified;
    private int           im; // Current measure index in system
    private List<Measure> verticals = null; // Current vertical measures
    private Integer       measureFinal = null; // Current termination

    // Information to remember from previous vertical measure
    private List<Measure> prevVerticals = null; // Previous vertical measures
    private Integer       prevMeasureFinal = null; // Previous termination

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
    public MeasureFixer (WrappedBoolean modified)
    {
        this.modified = modified;
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // visit Page //
    //------------//
    @Override
    public boolean visit (Page page)
    {
        if (logger.isFineEnabled()) {
            logger.fine(getClass().getSimpleName() + " Visiting " + page);
        }

        page.acceptChildren(this);

        // Remember the number of measures in this page
        page.computeMeasureCount();

        // Remember the delta of measure ids in this page
        page.setDeltaMeasureId(
            Math.abs(
                page.getLastSystem().getLastPart().getLastMeasure().getId()));

        return false;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine(getClass().getSimpleName() + " Visiting " + score);
        }

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
     * @return false
     */
    @Override
    public boolean visit (ScoreSystem system)
    {
        if (logger.isFineEnabled()) {
            logger.fine(getClass().getSimpleName() + " Visiting " + system);
        }

        // Measure indices to remove
        List<Integer> toRemove = new ArrayList<Integer>();

        // Use a loop on "vertical" measures, across all system parts
        final int imMax = system.getFirstRealPart()
                                .getMeasures()
                                .size() - 1;

        for (im = 0; im <= imMax; im++) {
            if (logger.isFineEnabled()) {
                logger.fine("im:" + im);
            }

            verticals = verticalsOf(system, im);

            // Check if all voices in all parts exhibit the same termination
            measureFinal = getMeasureFinal();

            if (isEmpty()) {
                if (logger.isFineEnabled()) {
                    logger.fine("empty");
                }

                // All this vertical measure is empty (no notes/rests)
                // We will merge with the following measure, if any
                if (im < imMax) {
                    setId(
                        (lastId != null) ? (lastId + 1)
                                                : ((prevSystemLastId != null)
                                                   ? (prevSystemLastId + 1) : 1));
                }
            } else if (isPickup()) {
                if (logger.isFineEnabled()) {
                    logger.fine("pickup");
                }

                setImplicit();
                setId(
                    (lastId != null) ? (-lastId)
                                        : ((prevSystemLastId != null)
                                           ? (-prevSystemLastId) : 0));
            } else if (isSecondRepeatHalf()) {
                if (logger.isFineEnabled()) {
                    logger.fine("secondHalf");
                }

                setImplicit();
                setId(-lastId);
            } else if (isRealStart()) {
                if (logger.isFineEnabled()) {
                    logger.fine("realStart");
                }

                merge(); // Merge with previous vertical measure
                toRemove.add(im);
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine("normal");
                }

                // Normal measure
                setId(
                    (lastId != null) ? (lastId + 1)
                                        : ((prevSystemLastId != null)
                                           ? (prevSystemLastId + 1) : 1));
            }

            // For next measure
            prevVerticals = verticals;
            prevMeasureFinal = measureFinal;
        }

        removeMeasures(toRemove, system); // Remove measures if any

        // For next system
        prevSystemLastId = lastId;
        lastId = null;

        return false; // Dead end
    }

    //---------//
    // isEmpty //
    //---------//
    /**
     * Check for an empty measure: perhaps clef and key sig, but no note or rest
     * @return true if so
     */
    private boolean isEmpty ()
    {
        return verticals.get(0)
                        .getActualDuration() == 0;
    }

    //-------//
    // setId //
    //-------//
    private void setId (int id)
    {
        if (logger.isFineEnabled()) {
            logger.fine("-> id=" + id);
        }

        for (Measure measure : verticals) {
            measure.setId(id);
        }

        // Side effect, remember the positive value of last id
        lastId = Math.abs(id);
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

    //-----------------//
    // getMeasureFinal //
    //-----------------//
    private Integer getMeasureFinal ()
    {
        Integer termination = null;

        for (Measure measure : verticals) {
            if (measure.isDummy()) {
                continue;
            }

            for (Voice voice : measure.getVoices()) {
                Integer voiceFinal = voice.getFinalDuration();

                if (voiceFinal != null) {
                    if (termination == null) {
                        termination = voiceFinal;
                    } else if (!voiceFinal.equals(termination)) {
                        if (logger.isFineEnabled()) {
                            logger.fine("Non-consistent durations");
                        }

                        return null;
                    }
                }
            }
        }

        return termination;
    }

    //----------//
    // isPickup //
    //----------//
    /**
     * Check for an implicit pickup measure at the beginning of a system
     * @return true if so
     */
    private boolean isPickup ()
    {
        return (im == 0) && (measureFinal != null) && (measureFinal < 0);
    }

    //-------------//
    // isRealStart //
    //-------------//
    /**
     * Check for a measure in second position, while following an empty measure
     *
     * @return true if so
     */
    private boolean isRealStart ()
    {
        return (im == 1) && (prevVerticals.get(0)
                                          .getActualDuration() == 0) &&
               (measureFinal != null);
    }

    //--------------------//
    // isSecondRepeatHalf //
    //--------------------//
    /**
     * Check for an implicit measure as the second half of a repeat sequence
     * @return true if so
     */
    private boolean isSecondRepeatHalf ()
    {
        // Check for partial first half
        if ((prevMeasureFinal == null) || (prevMeasureFinal >= 0)) {
            return false;
        }

        // Check for partial second half
        if ((measureFinal == null) || (measureFinal >= 0)) {
            return false;
        }

        // Check for a suitable repeat barline in between
        Measure prevMeasure = prevVerticals.get(0);
        Barline barline = prevMeasure.getBarline();
        Shape   shape = barline.getShape();

        if ((shape != Shape.RIGHT_REPEAT_SIGN) &&
            (shape != Shape.BACK_TO_BACK_REPEAT_SIGN)) {
            return false;
        }

        // Check for an exact duration sum (TODO: is this too strict?)
        try {
            return Math.abs(prevMeasureFinal + measureFinal) == prevMeasure.getExpectedDuration();
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
     * @param toRemove sequence of indices to remove, perhaps empty
     * @param system the containing system
     */
    private void removeMeasures (List<Integer> toRemove,
                                 ScoreSystem   system)
    {
        if (toRemove.isEmpty()) {
            return;
        }

        modified.set(true);

        for (TreeNode pn : system.getParts()) {
            SystemPart part = (SystemPart) pn;

            int        index = -1;

            for (Iterator<TreeNode> it = part.getMeasures()
                                             .iterator(); it.hasNext();) {
                index++;
                it.next();

                if (toRemove.contains(index)) {
                    it.remove();
                }
            }
        }
    }

    //-------------//
    // verticalsOf //
    //-------------//
    /**
     * Report the sequence of vertical measures for a given index
     * @param index the index in the parent part
     * @return the vertical collection of measure with the same index
     */
    private List<Measure> verticalsOf (ScoreSystem system,
                                       int         index)
    {
        List<Measure> measures = new ArrayList<Measure>();

        for (TreeNode node : system.getParts()) {
            SystemPart part = (SystemPart) node;
            measures.add((Measure) part.getMeasures().get(index));
        }

        return measures;
    }
}
