//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e T i m e F i x e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.log.Logger;

import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.AbstractScoreVisitor;

import omr.util.TreeNode;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class <code>ScoreTimeFixer</code> can visit the score hierarchy to compute
 * all measure and system start times and durations.
 *
 * @author Hervé Bitteur
 */
public class ScoreTimeFixer
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreTimeFixer.class);

    //~ Instance fields --------------------------------------------------------

    /** Map of Measure id -> Measure duration, whatever the containing part */
    private final Map<Integer, Integer> measureDurations = new TreeMap<Integer, Integer>();

    /** Pass number, since we need 2 passes */
    private int pass = 1;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // ScoreTimeFixer //
    //----------------//
    /**
     * Creates a new ScoreTimeFixer object.
     */
    public ScoreTimeFixer ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        try {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Visiting Part#" + measure.getPart().getId() + " " +
                    measure);
            }

            measure.resetStartTime();
            measure.getStartTime(); // Value is cached

            int measureDur = 0;

            // Whole/multi rests are handled outside of slots
            for (Slot slot : measure.getSlots()) {
                if (slot.getStartTime() != null) {
                    for (Chord chord : slot.getChords()) {
                        measureDur = Math.max(
                            measureDur,
                            slot.getStartTime() + chord.getDuration());
                    }
                }
            }

            if (measureDur != 0) {
                // Make sure the measure duration is not bigger than limit (?)
                if (measureDur <= measure.getExpectedDuration()) {
                    measure.setActualDuration(measureDur);
                } else {
                    measure.setActualDuration(measure.getExpectedDuration());
                }

                measureDurations.put(measure.getId(), measureDur);

                if (logger.isFineEnabled()) {
                    logger.fine(measure.getId() + ": " + measureDur);
                }
            } else if (!measure.getWholeChords()
                               .isEmpty()) {
                if (pass > 1) {
                    Integer dur = measureDurations.get(measure.getId());

                    if (dur != null) {
                        measure.setActualDuration(dur);
                    } else {
                        measure.setActualDuration(
                            measure.getExpectedDuration());
                    }
                }
            }
        } catch (InvalidTimeSignature ex) {
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + measure,
                ex);
        }

        return false; // Dead end, we don't go lower than measures
    }

    //------------//
    // visit Page //
    //------------//
    /**
     * Page hierarchy entry point
     *
     * @param page the page to export
     * @return false, since no further processing is required after this node
     */
    @Override
    public boolean visit (Page page)
    {
        try {
            // Delegate to children
            page.acceptChildren(this);

            page.recomputeActualDuration();
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + page,
                ex);
        }

        return false; // No default browsing this way
    }

    //-------------//
    // visit Score //
    //-------------//
    /**
     * Score hierarchy entry point. Determine the startTime of each page,
     * assuming that page internal duration is already available
     *
     * @param score the score to process
     * @return false, since no further processing is required after this node
     */
    @Override
    public boolean visit (Score score)
    {
        try {
            for (TreeNode pn : score.getPages()) {
                Page page = (Page) pn;
                page.recomputeStartTime();
            }
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + score,
                ex);
        }

        return false; // No browsing
    }

    //--------------//
    // visit System //
    //--------------//
    /**
     * System processing. The rest of processing is directly delegated to the
     * measures
     *
     * @param system visit the system to export
     * @return false
     */
    @Override
    public boolean visit (ScoreSystem system)
    {
        try {
            if (logger.isFineEnabled()) {
                logger.fine("Visiting " + system);
            }

            // 2 passes are needed, to get the actual duration of whole notes
            // Since the measure duration may be specified in another system part
            for (pass = 1; pass <= 2; pass++) {
                if (logger.isFineEnabled()) {
                    logger.fine("Pass #" + pass);
                }

                // System time
                system.recomputeStartTime();

                // Browse the (SystemParts and the) Measures
                system.acceptChildren(this);

                // System duration
                system.recomputeActualDuration();

                if (logger.isFineEnabled()) {
                    logger.fine("Durations:" + measureDurations);
                }
            }
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + system,
                ex);
        }

        return false; // No default browsing this way
    }
}
