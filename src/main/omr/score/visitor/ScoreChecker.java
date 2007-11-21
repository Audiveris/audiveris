//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e C h e c k e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.glyph.Shape;

import omr.score.Score;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;

import omr.util.Logger;

/**
 * Class <code>ScoreChecker</code> can visit the score hierarchy perform
 * global checking on score nodes.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreChecker
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreChecker.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScoreChecker //
    //--------------//
    /**
     * Creates a new ScoreChecker object.
     */
    public ScoreChecker ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Checking score ...");
        }

        score.acceptChildren(this);

        return false;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Checking " + timeSignature);
        }

        try {
            Shape shape = timeSignature.getShape();

            if (shape == null) {
                if ((timeSignature.getNumerator() == null) ||
                    (timeSignature.getDenominator() == null)) {
                    timeSignature.addError(
                        "Time signature with no rational value");
                }
            } else if (shape == Shape.NO_LEGAL_SHAPE) {
                timeSignature.addError("Illegal " + timeSignature);
            } else if (Shape.SingleTimes.contains(shape)) {
                timeSignature.addError(
                    "Orphan time signature shape : " + shape);
            }
        } catch (InvalidTimeSignature its) {
        }

        return true;
    }
}
