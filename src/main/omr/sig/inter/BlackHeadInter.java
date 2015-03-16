//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B l a c k H e a d I n t e r                                  //
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
 * Class {@code BlackHeadInter} represents a black note head interpretation.
 *
 * @author Hervé Bitteur
 */
public class BlackHeadInter
        extends AbstractHeadInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BlackHeadInter object.
     *
     * @param descriptor the shape template descriptor
     * @param pivot      the template pivot
     * @param anchor     relative pivot configuration
     * @param box        the object bounds
     * @param impacts    the grade details
     * @param staff      the related staff
     * @param pitch      the note pitch
     */
    public BlackHeadInter (ShapeDescriptor descriptor,
                           Point pivot,
                           Anchored.Anchor anchor,
                           Rectangle box,
                           GradeImpacts impacts,
                           Staff staff,
                           double pitch)
    {
        super(descriptor, pivot, anchor, box, Shape.NOTEHEAD_BLACK, impacts, staff, pitch);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // duplicate //
    //-----------//
    public BlackHeadInter duplicate ()
    {
        BlackHeadInter clone = new BlackHeadInter(
                descriptor,
                pivot,
                anchor,
                box,
                impacts,
                staff,
                pitch);
        clone.setMirror(this);
        sig.addVertex(clone);
        setMirror(clone);

        return clone;
    }
}
