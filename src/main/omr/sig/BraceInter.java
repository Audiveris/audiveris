//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B r a c e I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

/**
 * Class {@code BraceInter} represents a brace or a bracket.
 *
 * @author Hervé Bitteur
 */
public class BraceInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BraceInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public BraceInter (Glyph glyph,
                       Shape shape,
                       double grade)
    {
        super(glyph, null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a Brace inter.
     *
     * @param shape precise shape
     * @param glyph underlying glyph
     * @param grade evaluation value
     * @return the created instance or null if failed
     */
    public static Inter create (Shape shape,
                                Glyph glyph,
                                double grade)
    {
        return new BraceInter(glyph, shape, grade);
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }
}
