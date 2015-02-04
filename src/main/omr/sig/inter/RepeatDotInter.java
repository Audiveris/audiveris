//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e p e a t D o t I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.Staff;

/**
 * Class {@code RepeatDotInter} represents a repeat dot, near a bar line.
 *
 * @author Hervé Bitteur
 */
public class RepeatDotInter
        extends AbstractPitchedInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new RepeatDotInter object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     * @param staff the related staff
     * @param pitch dot pitch
     */
    public RepeatDotInter (Glyph glyph,
                           double grade,
                           Staff staff,
                           int pitch)
    {
        super(glyph, null, Shape.REPEAT_DOT, grade, staff, pitch);
    }
}
