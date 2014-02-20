//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       W h o l e I n t e r                                      //
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

import omr.image.ShapeDescriptor;

import java.awt.Rectangle;

/**
 * Class {@code WholeInter} represents a whole note interpretation.
 *
 * @author Hervé Bitteur
 */
public class WholeInter
        extends AbstractNoteInter
{
    //~ Constructors -------------------------------------------------------------------------------

    //------------//
    // WholeInter //
    //------------//
    /**
     * Creates a new WholeInter object.
     *
     * @param descriptor the shape template descriptor
     * @param box        the object bounds
     * @param impacts    the grade details
     * @param pitch      the note pitch
     */
    public WholeInter (ShapeDescriptor descriptor,
                       Rectangle box,
                       GradeImpacts impacts,
                       int pitch)
    {
        super(descriptor, box, Shape.WHOLE_NOTE, impacts, pitch);
    }
}
