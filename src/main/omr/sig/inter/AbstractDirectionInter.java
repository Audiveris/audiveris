//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           A b s t r a c t D i r e c t i o n I n t e r                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.Glyph;

import omr.sig.GradeImpacts;

import java.awt.Rectangle;

/**
 * Class {@code AbstractDirectionInter} represents any direction.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractDirectionInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code AbstractDirectionInter} object.
     *
     * @param glyph the glyph to interpret
     * @param box   the precise object bounds (if different from glyph bounds)
     * @param shape the possible shape
     * @param grade the interpretation quality
     */
    public AbstractDirectionInter (Glyph glyph,
                                   Rectangle box,
                                   Shape shape,
                                   double grade)
    {
        super(glyph, box, shape, grade);
    }

    /**
     * Creates a new {@code AbstractDirectionInter} object.
     *
     * @param glyph   the glyph to interpret
     * @param box     the precise object bounds (if different from glyph bounds)
     * @param shape   the possible shape
     * @param impacts assignment details
     */
    public AbstractDirectionInter (Glyph glyph,
                                   Rectangle box,
                                   Shape shape,
                                   GradeImpacts impacts)
    {
        super(glyph, box, shape, impacts);
    }
}
