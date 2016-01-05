//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B l a c k H e a d I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;

import omr.image.Anchored;

import omr.sheet.Staff;

import omr.sig.GradeImpacts;

import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BlackHeadInter} represents a black note head interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "black-head")
public class BlackHeadInter
        extends AbstractHeadInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BlackHeadInter object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public BlackHeadInter (Point pivot,
                           Anchored.Anchor anchor,
                           Rectangle bounds,
                           GradeImpacts impacts,
                           Staff staff,
                           double pitch)
    {
        super(pivot, anchor, bounds, Shape.NOTEHEAD_BLACK, impacts, staff, pitch);
    }

    private BlackHeadInter ()
    {
        super(null, null, null, null, null, null, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // duplicate //
    //-----------//
    public BlackHeadInter duplicate ()
    {
        BlackHeadInter clone = new BlackHeadInter(pivot, anchor, bounds, impacts, staff, pitch);
        clone.setGlyph(this.glyph);
        clone.setMirror(this);

        if (impacts == null) {
            clone.setGrade(this.grade);
        }

        sig.addVertex(clone);
        setMirror(clone);

        return clone;
    }
}
