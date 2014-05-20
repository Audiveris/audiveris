//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  A c c i d e n t a l I n t e r                                 //
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
 * Class {@code AccidentalInter} represents an accidental (#,b, etc).
 *
 * @author Hervé Bitteur
 */
public class AccidentalInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new AccidentalInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    public AccidentalInter (Glyph glyph,
                            Shape shape,
                            double grade)
    {
        super(glyph, null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create an Accidental inter.
     *
     * @param shape precise shape
     * @param glyph underlying glyph
     * @param grade evaluation value
     * @return the created instance or null if failed
     */
    public static AccidentalInter create (Shape shape,
                                          Glyph glyph,
                                          double grade)
    {
        return new AccidentalInter(glyph, shape, grade);
    }
}
