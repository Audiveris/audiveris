//----------------------------------------------------------------------------//
//                                                                            //
//                          V o i d H e a d I n t e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import java.awt.Rectangle;
import omr.glyph.Shape;

/**
 * Class {@code VoidHeadInter} represents a void note head
 * interpretation.
 *
 * @author Hervé Bitteur
 */
public class VoidHeadInter
        extends BasicInter
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // VoidHeadInter //
    //---------------//
    /**
     * Creates a new VoidHeadInter object.
     *
     * @param box the object bounds
     * @param grade the assignment quality
     */
    public VoidHeadInter (Rectangle box,
                          double grade)
    {
        super(box, Shape.NOTEHEAD_VOID, grade);
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
