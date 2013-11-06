//----------------------------------------------------------------------------//
//                                                                            //
//                             W h o l e I n t e r                            //
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
 * Class {@code WholeInter} represents a whole note interpretation.
 *
 * @author Hervé Bitteur
 */
public class WholeInter
        extends AbstractNoteInter
{
    //~ Constructors -----------------------------------------------------------

    //------------//
    // WholeInter //
    //------------//
    /**
     * Creates a new WholeInter object.
     *
     * @param box   the object bounds
     * @param grade the assignment quality
     * @param pitch the note pitch
     */
    public WholeInter (Rectangle box,
                       double grade,
                       int pitch)
    {
        super(box, Shape.WHOLE_NOTE, grade, pitch);
    }
}
