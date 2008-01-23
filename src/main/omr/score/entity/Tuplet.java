//----------------------------------------------------------------------------//
//                                                                            //
//                                T u p l e t                                 //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.common.DurationFactor;
import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.SortedSet;

/**
 * Class <code>Tuplet</code> represents a tuplet notation
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Tuplet
    extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Tuplet.class);

    //~ Instance fields --------------------------------------------------------

    /** Related num/den modification factor */
    private final DurationFactor factor;

    /** Chord on last side */
    @Child
    private final Chord lastChord;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Tuplet //
    //--------//
    /**
     * Creates a new instance of Tuplet event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param firstChord the first embraced chord
     * @param lastChord the last embraced chord
     * @param glyph the underlying glyph
     * @param factor the related num/den modification factor
     */
    public Tuplet (Measure        measure,
                   SystemPoint    point,
                   Chord          firstChord,
                   Chord          lastChord,
                   Glyph          glyph,
                   DurationFactor factor)
    {
        super(measure, point, firstChord, glyph);
        this.factor = factor;
        this.lastChord = lastChord;

        // Link last embraced chords to this tuplet instance
        if (lastChord != null) {
            lastChord.addNotation(this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Used by ScoreBuilder to allocate the tuplet marks
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    public static void populate (Glyph       glyph,
                                 Measure     measure,
                                 SystemPoint point)
    {
        // A Tuplet relates to the embraced chords
        // With placement depending on chord beams location (a bit simplistic)
        Shape shape = glyph.getShape();

        // Look for a beam above or below the tuplet mark
        Beam bestBeam = null;
        int  bestDist = Integer.MAX_VALUE;

        for (TreeNode node : measure.getBeams()) {
            Beam beam = (Beam) node;

            // Check abscissa range
            if ((beam.getLeft().x <= point.x) &&
                (beam.getRight().x >= point.x)) {
                // Take closest in ordinate
                final int dist = Math.abs(beam.getCenter().y - point.y);

                if (dist < bestDist) {
                    bestDist = dist;
                    bestBeam = beam;
                }
            }
        }

        if (bestBeam != null) {
            if (logger.isFineEnabled()) {
                logger.fine("tuplet bestBeam: " + bestBeam);
            }

            SortedSet<Chord> chords = bestBeam.getChords();
            DurationFactor   factor = null;

            // Check number of chords
            if (shape == Shape.TUPLET_THREE) {
                if (chords.size() != 3) {
                    bestBeam.addError(glyph, "Expected 3 chords for " + shape);

                    return;
                } else {
                    factor = new DurationFactor(2, 3);
                }
            }

            if (shape == Shape.TUPLET_SIX) {
                if (chords.size() != 6) {
                    bestBeam.addError(glyph, "Expected 6 chords for " + shape);

                    return;
                } else {
                    factor = new DurationFactor(4, 6);
                }
            }

            glyph.setTranslation(
                new Tuplet(
                    measure,
                    point,
                    chords.first(),
                    chords.last(),
                    glyph,
                    factor));

            // Apply the tuplet to each chord of the beam found
            for (Chord chord : chords) {
                chord.setTupletFactor(factor);

                if (logger.isFineEnabled()) {
                    logger.fine("tuplet impact: " + chord);
                }
            }
        }
    }

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        setCenter(computeGlyphCenter(getGlyph()));
    }
}
