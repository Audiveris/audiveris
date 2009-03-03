//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e T i m e F i x e r                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.log.Logger;

import omr.score.Score;
import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.TimeSignature.InvalidTimeSignature;

import java.util.Map;
import java.util.TreeMap;

/**
 * Class <code>ScoreTimeFixer</code> can visit the score hierarchy to compute
 * all measure and system start times and durations.
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

    //~ Instance fields --------------------------------------------------------

    /** Map of Measure id -> Measure duration, whatever the containing part */
    private final Map<Integer, Integer> measureDurations = new TreeMap<Integer, Integer>();

    /** Pass number */
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
        } catch (InvalidTimeSignature ex) {
        }

        return false; // Dead end, we don't go lower than measures
    }

    //-------------//
    // visit Score //
    //-------------//
    /**
     * Score hierarchy entry point (not used, but provided for completeness)
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
    public boolean visit (ScoreSystem system)
    {
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

        return false; // No default browsing this way
    }
}
