//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C a e s u r a I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code CaesuraInter} represents a caesura sign above a staff.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "caesura")
public class CaesuraInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CaesuraInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code CaesuraInter} object.
     *
     * @param glyph the caesura glyph
     * @param grade the interpretation quality
     */
    public CaesuraInter (Glyph glyph,
                         double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, Shape.CAESURA, grade);
    }

    /**
     * Creates a new {@code CaesuraInter} object.
     *
     * @param annotationId ID of the original annotation if any
     * @param bounds       the bounding box
     * @param grade        the interpretation quality
     */
    public CaesuraInter (int annotationId,
                         Rectangle bounds,
                         double grade)
    {
        super(annotationId, bounds, OmrShape.caesura, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private CaesuraInter ()
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
     * (Try to) create a caesura inter.
     *
     * @param glyph  the caesura glyph
     * @param grade  the interpretation quality
     * @param system the related system
     * @return the created caesura or null
     */
    public static CaesuraInter create (Glyph glyph,
                                       double grade,
                                       SystemInfo system)
    {
        // Look for staff below
        final Point center = glyph.getCenter();
        final Staff staff = system.getStaffAtOrBelow(center);

        if (staff == null) {
            return null;
        }

        final CaesuraInter caesura = new CaesuraInter(glyph, grade);
        caesura.setStaff(staff);

        return caesura;
    }

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a caesura inter.
     *
     * @param annotationId ID of the original annotation if any
     * @param bounds       the bounding box
     * @param grade        the interpretation quality
     * @param system       the related system
     * @return the created caesura or null
     */
    public static CaesuraInter create (int annotationId,
                                       Rectangle bounds,
                                       double grade,
                                       SystemInfo system)
    {
        // Look for staff below
        final Point center = GeoUtil.centerOf(bounds);
        final Staff staff = system.getStaffAtOrBelow(center);

        if (staff == null) {
            logger.warn(
                    "No staff for CaesuraInter based on {}",
                    system.getAnnotation(annotationId));

            return null;
        }

        final CaesuraInter caesura = new CaesuraInter(annotationId, bounds, grade);
        caesura.setStaff(staff);

        return caesura;
    }
}
