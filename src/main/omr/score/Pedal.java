//----------------------------------------------------------------------------//
//                                                                            //
//                                 P e d a l                                  //
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

import omr.sheet.PixelRectangle;

/**
 * Class <code>Pedal</code> represents a pedal (start) or pedal up (stop) event
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Pedal
    extends Direction
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
     * @param chord the chord related to the mark
     * @param glyph the underlying glyph
     */
    public Pedal (Measure     measure,
                  SystemPoint point,
                  Chord       chord,
                  Glyph       glyph)
    {
        super(glyph.getShape() == Shape.PEDAL_MARK, measure, point, chord);
        addGlyph(glyph);
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
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Pedal ");
        sb.append(super.toString());
        sb.append("}");

        return sb.toString();
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
    static void populate (Glyph       glyph,
                          Measure     measure,
                          SystemPoint point)
    {
        new Pedal(measure, point, findChord(measure, point), glyph);
    }
}
