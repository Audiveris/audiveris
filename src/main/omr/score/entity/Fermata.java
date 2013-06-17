//----------------------------------------------------------------------------//
//                                                                            //
//                               F e r m a t a                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.score.visitor.ScoreVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

/**
 * Class {@code Fermata} represents a fermata event (upright or
 * inverted)
 *
 * @author Hervé Bitteur
 */
public class Fermata
        extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Fermata.class);

    //~ Constructors -----------------------------------------------------------
    //---------//
    // Fermata //
    //---------//
    /**
     * Creates a new instance of Fermata event.
     *
     * @param measure measure that contains this mark
     * @param point   location of mark
     * @param chord   the chord related to the mark
     * @param glyph   the underlying glyph
     */
    public Fermata (Measure measure,
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
     * Used by SystemTranslator to allocate the fermata marks.
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
            logger.info("Fermata. populate {}", glyph.idString());
        }

        // A Fermata relates to the note on the same time slot
        // With placement depending on fermata upright / inverted.
        // Beware of whole rests which are handled separately.
        //
        // TODO: Fermata is said to apply to barline as well, but this feature 
        // is not yet implemented.
        Chord chord;

        if (glyph.getShape() == Shape.FERMATA) {
            // Look for a chord below
            chord = measure.getClosestChordBelow(point);

            if (chord == null) {
                chord = measure.getClosestWholeChordBelow(point);
            }
        } else {
            // Look for a chord above
            chord = measure.getClosestChordAbove(point);

            if (chord == null) {
                chord = measure.getClosestWholeChordAbove(point);
            }
        }

        if (chord != null) {
            glyph.setTranslation(new Fermata(measure, point, chord, glyph));
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
