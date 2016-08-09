//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S m a l l V o i d H e a d I n t e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
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
 * Class {@code SmallVoidHeadInter} represents a small void note head interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-void-head")
public class SmallVoidHeadInter
        extends AbstractHeadInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SmallVoidHeadInter object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public SmallVoidHeadInter (Point pivot,
                               Anchor anchor,
                               Rectangle bounds,
                               GradeImpacts impacts,
                               Staff staff,
                               int pitch)
    {
        super(pivot, anchor, bounds, Shape.NOTEHEAD_VOID_SMALL, impacts, staff, pitch);
    }

    private SmallVoidHeadInter ()
    {
        super(null, null, null, null, null, null, 0);
    }
}
