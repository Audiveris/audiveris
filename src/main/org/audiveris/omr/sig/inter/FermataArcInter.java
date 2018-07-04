//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  F e r m a t a A r c I n t e r                                 //
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
 * Class {@code FermataArcInter} represents the arc part of a fermata, either upright or
 * inverted.
 * <p>
 * Combined with a FermataDotInter, it can lead to a (full) FermataInter.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "fermata-arc")
public class FermataArcInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code FermataArcInter} object.
     *
     * @param glyph the fermata arc glyph
     * @param shape FERMATA_ARC or FERMATA_ARC_BELOW
     * @param grade the interpretation quality
     */
    private FermataArcInter (Glyph glyph,
                             Shape shape,
                             double grade)
    {
        super(glyph, glyph.getBounds(), shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private FermataArcInter ()
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
     * (Try to) create a fermata arc inter.
     *
     * @param glyph  the fermata arc glyph
     * @param shape  FERMATA_ARC or FERMATA_ARC_BELOW
     * @param grade  the interpretation quality
     * @param system the related system
     * @return the created fermata arc or null
     */
    public static FermataArcInter create (Glyph glyph,
                                          Shape shape,
                                          double grade,
                                          SystemInfo system)
    {
        // Look for proper staff
        final Point center = glyph.getCenter();
        final Staff staff = (shape == Shape.FERMATA_ARC) ? system.getStaffAtOrBelow(center)
                : system.getStaffAtOrAbove(center);

        if (staff == null) {
            return null;
        }

        final FermataArcInter arc = new FermataArcInter(glyph, shape, grade);
        arc.setStaff(staff);

        return arc;
    }
}
