//----------------------------------------------------------------------------//
//                                                                            //
//                                 P e d a l                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

/**
 * Class <code>Pedal</code> represents a pedal (start) or pedal up (stop) event
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Pedal
    extends AbstractDirection
{
    //~ Constructors -----------------------------------------------------------

    //-------//
    // Pedal //
    //-------//
    /**
     * Creates a new instance of Pedal event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark, if any
     * @param glyph the underlying glyph
     */
    public Pedal (Measure     measure,
                  SystemPoint point,
                  Chord       chord,
                  Glyph       glyph)
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
     * Used by ScoreBuilder to allocate the pedal marks
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    public static void populate (Glyph       glyph,
                                 Measure     measure,
                                 SystemPoint point)
    {
        glyph.setTranslation(
            new Pedal(measure, point, findChord(measure, point), glyph));
    }
}
