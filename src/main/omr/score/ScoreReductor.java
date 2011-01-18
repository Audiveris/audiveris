//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e R e d u c t o r                          //
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

import omr.math.Rational;

import omr.score.entity.Chord;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.AbstractScoreVisitor;

import java.util.*;

/**
 * Class <code>ScoreReductor</code> can visit the score hierarchy to simplify
 * all duration values.
 *
 * @author Hervé Bitteur
 */
public class ScoreReductor
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreReductor.class);

    //~ Instance fields --------------------------------------------------------

    /** Set of all different duration values */
    private final SortedSet<Rational> durations = new TreeSet<Rational>();

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
        Rational duration;

        try {
            // Special case for whole chords
            if (chord.isWholeDuration()) {
                duration = chord.getMeasure()
                                .getExpectedDuration();
            } else {
                duration = chord.getDuration();
            }

            if (duration != null) {
                durations.add(duration);
            }
        } catch (InvalidTimeSignature ex) {
            // Ignored here (TBC)
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + chord,
                ex);
        }

        return false;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        try {
            // Collect duration values for each part
            score.acceptChildren(this);

            // Compute and remember greatest duration divisor for the score
            score.setDurationDivisor(computeDurationDivisor());
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + score,
                ex);
        }

        return false;
    }

    //------------------------//
    // computeDurationDivisor //
    //------------------------//
    private int computeDurationDivisor ()
    {
        Rational[] durationArray = durations.toArray(
            new Rational[durations.size()]);
        Rational   divisor = Rational.gcd(durationArray);

        if (logger.isFineEnabled()) {
            logger.fine(
                "durations=" + Arrays.deepToString(durationArray) +
                " divisor=" + divisor);
        }

        return divisor.den;
    }
}
