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

import omr.glyph.Shape;

import java.awt.Rectangle;

/**
 * Class {@code VoidHeadInter} represents a void note head
 * interpretation.
 *
 * @author Hervé Bitteur
 */
public class VoidHeadInter
        extends AbstractNoteInter
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // VoidHeadInter //
    //---------------//
    /**
     * Creates a new VoidHeadInter object.
     *
     * @param box   the object bounds
     * @param grade the assignment quality
     * @param pitch the note pitch
     */
    public VoidHeadInter (Rectangle box,
                          double grade,
                          int pitch)
    {
        super(box, Shape.NOTEHEAD_VOID, grade, pitch);
    }
}
