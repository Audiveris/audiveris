//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e R e d u c t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
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
        chord.getPart()
             .getScorePart()
             .addDuration(chord.getDuration());

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
