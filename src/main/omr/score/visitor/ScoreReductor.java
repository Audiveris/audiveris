//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e R e d u c t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.score.Chord;
import omr.score.Score;
import omr.score.ScorePart;

/**
 * Class <code>ScoreReductor</code> can visit the score hierarchy to simplify
 * all duration values.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreReductor
    extends AbstractScoreVisitor
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScoreReductor //
    //---------------//
    /**
     * Creates a new ScoreReductor object.
     */
    public ScoreReductor ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        Integer duration = chord.getDuration();

        // Special case for whole chords
        if (duration == null) {
            duration = chord.getMeasure()
                            .getExpectedDuration();
        }

        chord.getPart()
             .getScorePart()
             .addDuration(duration);

        return false;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        // Collect duration values for each part
        score.acceptChildren(this);

        // Compute and remember greatest duration divisor for each part
        for (ScorePart part : score.getPartList()) {
            part.computeDurationDivisor();
        }

        return false;
    }
}
