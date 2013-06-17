//----------------------------------------------------------------------------//
//                                                                            //
//                     D u r a t i o n R e t r i e v e r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.math.Rational;

import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.MeasureId.PageBased;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.AbstractScoreVisitor;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Class {@code DurationRetriever} can visit a page hierarchy to compute
 * the actual duration of every measure
 *
 * @author Hervé Bitteur
 */
public class DurationRetriever
        extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            DurationRetriever.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Map of Measure id -> Measure duration, whatever the containing part */
    private final Map<PageBased, Rational> measureDurations = new HashMap<>();

    /** Pass number, since we need 2 passes per system */
    private int pass = 1;

    //~ Constructors -----------------------------------------------------------
    //
    //-------------------//
    // DurationRetriever //
    //-------------------//
    /**
     * Creates a new DurationRetriever object.
     */
    public DurationRetriever ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        try {
            logger.debug("Visiting Part#{} {}",
                    measure.getPart().getId(), measure);

            Rational measureDur = Rational.ZERO;

            // Whole/multi rests are handled outside of slots
            for (Slot slot : measure.getSlots()) {
                if (slot.getStartTime() != null) {
                    for (Chord chord : slot.getChords()) {
                        Rational chordEnd = slot.getStartTime().plus(chord.
                                getDuration());

                        if (chordEnd.compareTo(measureDur) > 0) {
                            measureDur = chordEnd;
                        }
                    }
                }
            }

            if (!measureDur.equals(Rational.ZERO)) {
                // Make sure the measure duration is not bigger than limit
                if (measureDur.compareTo(measure.getExpectedDuration()) <= 0) {
                    measure.setActualDuration(measureDur);
                } else {
                    measure.setActualDuration(measure.getExpectedDuration());
                }

                measureDurations.put(measure.getPageId(), measureDur);
                logger.debug("{}: {}", measure.getPageId(), measureDur);
            } else if (!measure.getWholeChords().isEmpty()) {
                if (pass > 1) {
                    Rational dur = measureDurations.get(measure.getPageId());

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
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + measure,
                    ex);
        }

        return false; // Dead end, we don't go deeper than measure level
    }

    //------------//
    // visit Page //
    //------------//
    /**
     * Page hierarchy entry point
     *
     * @param page the page for which measure durations are to be computed
     * @return false, since no further processing is required after this node
     */
    @Override
    public boolean visit (Page page)
    {
        // Delegate to children
        page.acceptChildren(this);

        return false; // No default browsing this way
    }

    //-------------//
    // visit Score //
    //-------------//
    /**
     * Score hierarchy entry point, to delegate to all pages
     *
     * @param score the score to process
     * @return false, since no further processing is required after this node
     */
    @Override
    public boolean visit (Score score)
    {
        for (TreeNode pn : score.getPages()) {
            Page page = (Page) pn;
            page.accept(this);
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
        logger.debug("Visiting {}", system);

        // 2 passes are needed, to get the actual duration of whole notes
        // Since the measure duration may be specified in another system part
        for (pass = 1; pass <= 2; pass++) {
            logger.debug("Pass #{}", pass);

            // Browse the (SystemParts and the) Measures
            system.acceptChildren(this);

            logger.debug("Durations:{}", measureDurations);
        }

        return false; // No default browsing this way
    }
}
