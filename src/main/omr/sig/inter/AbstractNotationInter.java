//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            A b s t r a c t N o t a t i o n I n t e r                           //
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

import omr.sig.GradeImpacts;

import java.awt.Rectangle;

/**
 * Class {@code AbstractNotationInter} represents any notation.
 *
 * @author Hervé Bitteur
 */
public class AbstractNotationInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code AbstractNotationInter} object.
     *
     * @param glyph the glyph to interpret
     * @param box   the precise object bounds (if different from glyph bounds)
     * @param shape the possible shape
     * @param grade the interpretation quality
     */
    public AbstractNotationInter (Glyph glyph,
                                  Rectangle box,
                                  Shape shape,
                                  double grade)
    {
        super(glyph, box, shape, grade);
    }

    /**
     * Creates a new {@code AbstractNotationInter} object.
     *
     * @param glyph   the glyph to interpret
     * @param box     the precise object bounds (if different from glyph bounds)
     * @param shape   the possible shape
     * @param impacts assignment details
     */
    public AbstractNotationInter (Glyph glyph,
                                  Rectangle box,
                                  Shape shape,
                                  GradeImpacts impacts)
    {
        super(glyph, box, shape, impacts);
    }
}
