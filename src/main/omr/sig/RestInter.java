//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R e s t I n t e r                                       //
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
 * Class {@code RestInter} represents a rest.
 *
 * @author Hervé Bitteur
 */
public class RestInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new RestInter object.
     *
     * @param glyph underlying glyph
     * @param shape rest shape
     * @param grade evaluation value
     */
    public RestInter (Glyph glyph,
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
     * (Try to) create a Clef inter.
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
        return new RestInter(glyph, shape, grade);
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
