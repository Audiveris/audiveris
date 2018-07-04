//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      M a r k e r I n t e r                                     //
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
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.relation.MarkerBarRelation;

import java.awt.Point;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code MarkerInter} represents a navigation marker.
 * Shape can be coda, segno, dacapo (D.C.), dal segno (D.S.).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "marker")
public class MarkerInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code MarkerInter} object.
     *
     * @param glyph underlying glyph if any
     * @param shape precise shape (CODA, SEGNO, DA_CAPO, DAL_SEGNO)
     * @param grade quality
     */
    public MarkerInter (Glyph glyph,
                        Shape shape,
                        double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private MarkerInter ()
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
     * Create a MarkerInter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance
     */
    public static MarkerInter create (Glyph glyph,
                                      Shape shape,
                                      double grade,
                                      Staff staff)
    {
        MarkerInter marker = new MarkerInter(glyph, shape, grade);
        marker.setStaff(staff);

        return marker;
    }

    //----------------------//
    // linkWithStaffBarline //
    //----------------------//
    /**
     * (Try to) connect this marker with a suitable StaffBarline.
     *
     * @return true if successful
     */
    public boolean linkWithStaffBarline ()
    {
        Point center = getCenter();
        List<StaffBarlineInter> staffBars = getStaff().getStaffBarlines();
        StaffBarlineInter staffBar = StaffBarlineInter.getClosestStaffBarline(staffBars, center);

        if ((staffBar != null) && (GeoUtil.xOverlap(getBounds(), staffBar.getBounds()) > 0)) {
            sig.addEdge(this, staffBar, new MarkerBarRelation());

            return true;
        }

        return false;
    }
}
