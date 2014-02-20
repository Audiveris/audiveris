//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S m a l l V o i d H e a d I n t e r                              //
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
 * Class {@code SmallVoidHeadInter} represents a small void note head interpretation.
 *
 * @author Hervé Bitteur
 */
public class SmallVoidHeadInter
        extends AbstractNoteInter
{
    //~ Constructors -------------------------------------------------------------------------------

    //--------------------//
    // SmallVoidHeadInter //
    //--------------------//
    /**
     * Creates a new SmallVoidHeadInter object.
     *
     * @param descriptor the shape template descriptor
     * @param box        the object bounds
     * @param impacts    the grade details
     * @param pitch      the note pitch
     */
    public SmallVoidHeadInter (ShapeDescriptor descriptor,
                               Rectangle box,
                               GradeImpacts impacts,
                               int pitch)
    {
        super(descriptor, box, Shape.NOTEHEAD_VOID_SMALL, impacts, pitch);
    }
}
