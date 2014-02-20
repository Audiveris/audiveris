//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    V o i d H e a d I n t e r                                   //
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
 * Class {@code VoidHeadInter} represents a void note head interpretation.
 *
 * @author Hervé Bitteur
 */
public class VoidHeadInter
        extends AbstractNoteInter
{
    //~ Constructors -------------------------------------------------------------------------------

    //---------------//
    // VoidHeadInter //
    //---------------//
    /**
     * Creates a new VoidHeadInter object.
     *
     * @param descriptor the shape template descriptor
     * @param box        the object bounds
     * @param impacts    the grade details
     * @param pitch      the note pitch
     */
    public VoidHeadInter (ShapeDescriptor descriptor,
                          Rectangle box,
                          GradeImpacts impacts,
                          int pitch)
    {
        super(descriptor, box, Shape.NOTEHEAD_VOID, impacts, pitch);
    }
}
