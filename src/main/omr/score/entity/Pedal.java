//----------------------------------------------------------------------------//
//                                                                            //
//                                 P e d a l                                  //
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
 * Class {@code Pedal} represents a pedal (start) or pedal up (stop) event
 *
 * @author Hervé Bitteur
 */
public class Pedal
        extends AbstractDirection
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Pedal.class);

    //~ Constructors -----------------------------------------------------------
    //-------//
    // Pedal //
    //-------//
    /**
     * Creates a new instance of Pedal event
     *
     * @param measure measure that contains this mark
     * @param point   location of mark
     * @param chord   the chord related to the mark, if any
     * @param glyph   the underlying glyph
     */
    public Pedal (Measure measure,
                  Point point,
                  Chord chord,
                  Glyph glyph)
    {
        super(
                measure,
                glyph.getShape() == Shape.PEDAL_MARK,
                point,
                chord,
                glyph);
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
     * Used by SystemTranslator to allocate the pedal marks
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
            logger.info("Pedal. populate {}", glyph.idString());
        }

        glyph.setTranslation(
                new Pedal(measure, point, findChord(measure, point), glyph));
    }
}
