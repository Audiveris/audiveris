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
package omr.sig.inter;

import omr.glyph.Shape;

import omr.image.Anchored;
import omr.image.ShapeDescriptor;

import omr.sheet.Staff;

import omr.sig.GradeImpacts;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code WholeInter} represents a whole note interpretation.
 *
 * @author Hervé Bitteur
 */
public class WholeInter
        extends AbstractHeadInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new WholeInter object.
     *
     * @param descriptor the shape template descriptor
     * @param pivot      the template pivot
     * @param anchor     relative pivot configuration
     * @param box        the object bounds
     * @param impacts    the grade details
     * @param staff      the related staff
     * @param pitch      the note pitch
     */
    public WholeInter (ShapeDescriptor descriptor,
                       Point pivot,
                       Anchored.Anchor anchor,
                       Rectangle box,
                       GradeImpacts impacts,
                       Staff staff,
                       int pitch)
    {
        super(descriptor, pivot, anchor, box, Shape.WHOLE_NOTE, impacts, staff, pitch);
    }
}
