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
package omr.sig.inter;

import omr.glyph.Shape;

import omr.image.Anchored.Anchor;
import omr.image.ShapeDescriptor;

import omr.sheet.Staff;

import omr.sig.GradeImpacts;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code SmallVoidHeadInter} represents a small void note head interpretation.
 *
 * @author Hervé Bitteur
 */
public class SmallVoidHeadInter
        extends AbstractHeadInter
{

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SmallVoidHeadInter object.
     *
     * @param descriptor the shape template descriptor
     * @param pivot      the template pivot
     * @param anchor     relative pivot configuration
     * @param box        the object bounds
     * @param impacts    the grade details
     * @param staff      the related staff
     * @param pitch      the note pitch
     */
    public SmallVoidHeadInter (ShapeDescriptor descriptor,
                               Point pivot,
                               Anchor anchor,
                               Rectangle box,
                               GradeImpacts impacts,
                               Staff staff,
                               int pitch)
    {
        super(descriptor, pivot, anchor, box, Shape.NOTEHEAD_VOID_SMALL, impacts, staff, pitch);
    }
}
