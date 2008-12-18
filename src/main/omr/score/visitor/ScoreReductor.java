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

import omr.log.Logger;

import omr.math.GCD;

import omr.score.Score;
import omr.score.entity.Chord;
import omr.score.entity.TimeSignature.InvalidTimeSignature;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

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
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreReductor.class);

    //~ Instance fields --------------------------------------------------------

    /** Set of all different duration values */
    private final SortedSet<Integer> durations = new TreeSet<Integer>();

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
        Integer duration = null;

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
        }

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

        // Compute and remember greatest duration divisor for the score
        score.setDurationDivisor(computeDurationDivisor());

        return false;
    }

    //------------------------//
    // computeDurationDivisor //
    //------------------------//
    private int computeDurationDivisor ()
    {
        Integer[] durationArray = durations.toArray(
            new Integer[durations.size()]);
        int       divisor = GCD.gcd(durationArray);

        if (logger.isFineEnabled()) {
            logger.fine(
                "durations=" + Arrays.deepToString(durationArray) +
                " divisor=" + divisor);
        }

        return divisor;
    }
}
