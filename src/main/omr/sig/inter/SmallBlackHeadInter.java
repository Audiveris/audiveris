//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              S m a l l B l a c k H e a d I n t e r                             //
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

import omr.image.Anchored.Anchor;

import omr.sheet.Staff;

import omr.sig.GradeImpacts;

import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SmallBlackHeadInter} represents a small black note head interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-black-head")
public class SmallBlackHeadInter
        extends AbstractHeadInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SmallBlackHeadInter object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public SmallBlackHeadInter (Point pivot,
                                Anchor anchor,
                                Rectangle bounds,
                                GradeImpacts impacts,
                                Staff staff,
                                int pitch)
    {
        super(pivot, anchor, bounds, Shape.NOTEHEAD_BLACK_SMALL, impacts, staff, pitch);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SmallBlackHeadInter ()
    {
        super(null, null, null, null, null, null, 0);
    }
}
