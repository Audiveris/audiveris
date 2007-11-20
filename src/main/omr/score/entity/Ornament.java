//----------------------------------------------------------------------------//
//                                                                            //
//                              O r n a m e n t                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.entity;

import omr.glyph.Glyph;

import omr.score.common.SystemPoint;
import omr.score.entity.AbstractNotation;
import omr.score.entity.Chord;
import omr.score.visitor.ScoreVisitor;

import omr.util.Logger;

/**
 * Class <code>Ornament</code> represents an ornament event, a special notation.
 * This should apply to:
 * <pre>
 * trill-mark           standard
 * turn                 standard
 * inverted-turn        standard
 * delayed-turn         nyi
 * shake                nyi
 * wavy-line            nyi
 * mordent              standard
 * inverted-mordent     standard
 * schleifer            nyi
 * tremolo              nyi
 * other-ornament       nyi
 * accidental-mark      nyi
 * </pre>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Ornament
    extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Ornament.class);

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Ornament //
    //----------//
    /**
     * Creates a new instance of Ornament event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark
     * @param glyph the underlying glyph
     */
    public Ornament (Measure     measure,
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
     * Used by ScoreBuilder to allocate the trill marks
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    public static void populate (Glyph       glyph,
                                 Measure     measure,
                                 SystemPoint point)
    {
        // An Ornament relates to the note below on the same time slot
        Slot slot = measure.getClosestSlot(point);

        if (slot != null) {
            Chord chord = slot.getChordBelow(point);

            if (chord != null) {
                glyph.setTranslation(
                    new Ornament(measure, point, chord, glyph));
            }
        }
    }
}
