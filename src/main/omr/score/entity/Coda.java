//----------------------------------------------------------------------------//
//                                                                            //
//                                  C o d a                                   //
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

import omr.score.common.SystemPoint;
import omr.score.visitor.ScoreVisitor;

/**
 * Class <code>Coda</code> represents a coda event
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Coda
    extends AbstractDirection
{
    //~ Constructors -----------------------------------------------------------

    //-------//
    // Coda //
    //-------//
    /**
     * Creates a new instance of Coda event
     *
     * @param measure measure that contains this mark
     * @param point location of mark
     * @param chord the chord related to the mark, if any
     * @param glyph the underlying glyph
     */
    public Coda (Measure     measure,
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
     * Used by SystemTranslator to allocate the coda marks
     *
     * @param glyph underlying glyph
     * @param measure measure where the mark is located
     * @param point location for the mark
     */
    public static void populate (Glyph       glyph,
                                 Measure     measure,
                                 SystemPoint point)
    {
        Slot slot = measure.getClosestSlot(point);
        glyph.setTranslation(
            new Coda(measure, point, slot.getChordBelow(point), glyph));
    }
}
