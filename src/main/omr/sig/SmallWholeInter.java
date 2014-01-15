//----------------------------------------------------------------------------//
//                                                                            //
//                        S m a l l W h o l e I n t e r                       //
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

import omr.image.ShapeDescriptor;

import java.awt.Rectangle;

/**
 * Class {@code SmallWholeInter} represents a small whole note
 * interpretation.
 *
 * @author Hervé Bitteur
 */
public class SmallWholeInter
        extends AbstractNoteInter
{
    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SmallWholeInter //
    //-----------------//
    /**
     * Creates a new SmallWholeInter object.
     *
     * @param descriptor the shape template descriptor
     * @param box        the object bounds
     * @param impacts    the grade details
     * @param pitch      the note pitch
     */
    public SmallWholeInter (ShapeDescriptor descriptor,
                            Rectangle box,
                            GradeImpacts impacts,
                            int pitch)
    {
        super(descriptor, box, Shape.WHOLE_NOTE_SMALL, impacts, pitch);
    }
}
