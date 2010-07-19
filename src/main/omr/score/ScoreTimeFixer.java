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

import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.entity.Voice;
import omr.score.visitor.AbstractScoreVisitor;

import omr.util.TreeNode;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class <code>ScoreTimeFixer</code> can visit the score hierarchy to compute
 * all measure and system start times and durations.
 *
 * We check if an initial time signature is defined in the first measure and, if
 * so, that it is consistent with most of measures intrinsic time signature.
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

    /** To flag the first system */
    private boolean systemIsFirst = true;

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
        if (logger.isFineEnabled()) {
            logger.fine(
                "Visiting Part#" + measure.getPart().getId() + " " + measure);
        }

        measure.resetStartTime();
        measure.getStartTime(); // Value is cached

        try {
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

            // Check measure intrinsic time signature
            if (pass > 1) {
                checkVoices(measure);
            }
        } catch (InvalidTimeSignature ex) {
        }

        return false; // Dead end, we don't go lower than measures
    }

    //-------------//
    // visit Score //
    //-------------//
    /**
     * Score hierarchy entry point
     *
     * @param score visit the score to export
     * @return false, since no further processing is required after this node
     */
    @Override
    public boolean visit (Score score)
    {
        systemIsFirst = true;

        // Delegate to children
        score.acceptChildren(this);

        return false; // That's all
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
        if (logger.isFineEnabled()) {
            logger.fine("Visiting " + system);
        }

        // First system?
        if (systemIsFirst) {
            checkTimeSignatures(system);
            systemIsFirst = false; // For the following systems
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

        return false; // No default browsing this way
    }

    //---------------------//
    // checkTimeSignatures //
    //---------------------//
    private void checkTimeSignatures (ScoreSystem system)
    {
        // Look for time sigs in every staff of this first system
        for (TreeNode pNode : system.getParts()) {
            SystemPart part = (SystemPart) pNode;
            Measure    measure = part.getFirstMeasure();

            for (TreeNode sNode : part.getStaves()) {
                Staff         staff = (Staff) sNode;
                TimeSignature ts = measure.getTimeSignature(staff);
                if (ts != null) {
                    
                }
            }
        }

        //        for (Glyph glyph : system.getInfo()
        //                                 .getGlyphs()) {
        //            if (ShapeRange.Times.contains(glyph.getShape())) {
        //            }
        //        }
    }

    //-------------//
    // checkVoices //
    //-------------//
    private void checkVoices (Measure measure)
    {
        logger.info(
            "Measure#" + measure.getId() +
            (measure.isPartial() ? " (partial)" : ""));

        for (Voice voice : measure.getVoices()) {
            logger.info("Inferred: " + voice.getInferredTimeSignature());
        }
    }
}
