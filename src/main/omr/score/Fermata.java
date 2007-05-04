//----------------------------------------------------------------------------//
//                                                                            //
//                               F e r m a t a                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.visitor.ScoreVisitor;

import omr.util.Logger;

/**
 * Class <code>Fermata</code> represents a fermata event (upright or inverted)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Fermata
    extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Fermata.class);

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Fermata //
    //---------//
    /**
     * Creates a new instance of Fermata event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark
     * @param glyph the underlying glyph
     */
    public Fermata (Measure     measure,
                    SystemPoint point,
                    Chord       chord,
                    Glyph       glyph)
    {
        super(measure, point, chord, glyph);
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
     * Used by ScoreBuilder to allocate the fermata marks
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    static void populate (Glyph       glyph,
                          Measure     measure,
                          SystemPoint point)
    {
        // A Fermata relates to the note on the same time slot
        // With placement depending on fermata upright / inverted
        // TBD: Fermata is said to apply to barline as well, not yet implemented
        Slot  slot = measure.getClosestSlot(point);
        Chord chord = (glyph.getShape() == Shape.FERMATA)
                      ? slot.getChordBelow(point) : slot.getChordAbove(point);

        if (chord != null) {
            new Fermata(measure, point, chord, glyph);
        }
    }
}
