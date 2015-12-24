//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S m a l l W h o l e I n t e r                                 //
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

import omr.sheet.Staff;

import omr.sig.GradeImpacts;

import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SmallWholeInter} represents a small whole note interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-whole")
public class SmallWholeInter
        extends AbstractHeadInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SmallWholeInter object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public SmallWholeInter (Point pivot,
                            Anchor anchor,
                            Rectangle bounds,
                            GradeImpacts impacts,
                            Staff staff,
                            int pitch)
    {
        super(pivot, anchor, bounds, Shape.WHOLE_NOTE_SMALL, impacts, staff, pitch);
    }

    private SmallWholeInter ()
    {
        super(null, null, null, null, null, null, 0);
    }
}
