//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B r e a t h M a r k I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;

import java.awt.Point;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BreathMarkInter} represents a comma-shaped break mark above a staff.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "breath-mark")
public class BreathMarkInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code BreathMarkInter} object.
     *
     * @param glyph the breathMark glyph
     * @param grade the interpretation quality
     */
    public BreathMarkInter (Glyph glyph,
                            double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, Shape.BREATH_MARK, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BreathMarkInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a breathMark inter.
     *
     * @param glyph  the breathMark glyph
     * @param grade  the interpretation quality
     * @param system the related system
     * @return the created breathMark or null
     */
    public static BreathMarkInter create (Glyph glyph,
                                          double grade,
                                          SystemInfo system)
    {
        // Look for staff below
        final Point center = glyph.getCenter();
        final Staff staff = system.getStaffAtOrBelow(center);

        if (staff == null) {
            return null;
        }

        final BreathMarkInter breathMark = new BreathMarkInter(glyph, grade);
        breathMark.setStaff(staff);

        return breathMark;
    }
}
