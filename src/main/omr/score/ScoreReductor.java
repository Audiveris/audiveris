//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e R e d u c t o r                          //
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
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.visitor.AbstractScoreVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code ScoreReductor} can visit the score hierarchy to simplify
 * all duration values.
 *
 * @author Hervé Bitteur
 */
public class ScoreReductor
        extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(ScoreReductor.class);

    //~ Instance fields --------------------------------------------------------
    /** Set of all different duration values */
    private final SortedSet<Rational> durations = new TreeSet<>();

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
                duration = chord.getMeasure().getExpectedDuration();
            } else {
                duration = chord.getDuration();
            }

            if (duration != null) {
                durations.add(duration);
            }
        } catch (InvalidTimeSignature ex) {
            // Ignored here (TBC)
        } catch (Exception ex) {
            logger.warn(
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
            logger.warn(
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
        Rational divisor = Rational.gcd(durationArray);
        logger.debug("durations={} divisor={}",
                Arrays.deepToString(durationArray), divisor);

        return divisor.den;
    }
}
