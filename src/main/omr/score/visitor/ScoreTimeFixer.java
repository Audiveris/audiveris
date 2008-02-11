//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e T i m e F i x e r                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Score;
import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.Slot;
import omr.score.entity.System;
import omr.score.entity.TimeSignature.InvalidTimeSignature;

import omr.util.Logger;

/**
 * Class <code>ScoreTimeFixer</code> can visit the score hierarchy to compute
 * all measure durations.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreTimeFixer
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreTimeFixer.class);

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
        ///logger.info("Visiting " + measure);
        measure.getStartTime(); // Value is cached

        try {
            int measureDur = 0;

            // Whole/multi rests are handled outside of slots
            for (Slot slot : measure.getSlots()) {
                slot.getStartTime();

                for (Chord chord : slot.getChords()) {
                    measureDur = Math.max(
                        measureDur,
                        slot.getStartTime() + chord.getDuration());
                }
            }

            if (measureDur != 0) {
                // Make sure the measure duration is not bigger than limit
                if (measureDur <= measure.getExpectedDuration()) {
                    measure.setActualDuration(measureDur);
                } else {
                    measure.setActualDuration(measure.getExpectedDuration());
                }
            } else if (!measure.getWholeChords()
                               .isEmpty()) {
                measure.setActualDuration(measure.getExpectedDuration());
            }
        } catch (InvalidTimeSignature ex) {
        }

        return false; // Dead end
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
    public boolean visit (System system)
    {
        ///logger.info("Visiting " + system);

        // System time
        system.recomputeStartTime();

        // Browse the SystemParts and the Measures
        system.acceptChildren(this);

        // System duration
        system.recomputeActualDuration();

        return false; // No default browsing this way
    }
}
