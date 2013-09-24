//----------------------------------------------------------------------------//
//                                                                            //
//                         B l a c k H e a d I n t e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

/**
 * Class {@code BlackHeadInter} represents a black note head
 * interpretation.
 *
 * @author Hervé Bitteur
 */
public class BlackHeadInter
        extends BasicInter
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new BlackHeadInter object.
     *
     * @param glyph the underlying glyph
     * @param grade the assignment quality
     */
    public BlackHeadInter (Glyph glyph,
                           double grade)
    {
        super(glyph, Shape.NOTEHEAD_BLACK, grade);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }
}
