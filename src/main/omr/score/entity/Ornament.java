//----------------------------------------------------------------------------//
//                                                                            //
//                              O r n a m e n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.facets.Glyph;

import omr.score.visitor.ScoreVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code Ornament} represents an ornament event, a special notation.
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
 * @author Hervé Bitteur
 */
public class Ornament
        extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            Ornament.class);

    //~ Constructors -----------------------------------------------------------
    //----------//
    // Ornament //
    //----------//
    /**
     * Creates a new instance of Ornament event
     *
     * @param measure measure that contains this mark
     * @param point   location of mark
     * @param chord   the chord related to the mark
     * @param glyph   the underlying glyph
     */
    public Ornament (Measure measure,
                     Point point,
                     Chord chord,
                     Glyph glyph)
    {
        super(measure, point, chord, glyph);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // populate //
    //----------//
    /**
     * Used by SystemTranslator to allocate the trill marks
     *
     * @param glyph   underlying glyph
     * @param measure measure where the mark is located
     * @param point   location for the mark
     */
    public static void populate (Glyph glyph,
                                 Measure measure,
                                 Point point)
    {
        if (glyph.isVip()) {
            logger.info("Ornament. populate {}", glyph.idString());
        }

        // An Ornament relates to the note below on the same time slot
        Slot slot = measure.getClosestSlot(point);

        if (slot != null) {
            Chord chord = slot.getChordJustBelow(point);

            if (chord != null) {
                glyph.setTranslation(
                        new Ornament(measure, point, chord, glyph));
            }
        }
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }
}
