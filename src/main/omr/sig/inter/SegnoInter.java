//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S e g n o I n t e r                                      //
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

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.GeoUtil;

import omr.sig.relation.SegnoBarRelation;

import java.awt.Point;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SegnoInter} represents a segno (sign) interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "segno")
public class SegnoInter
        extends AbstractDirectionInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code SegnoInter} object.
     *
     * @param glyph the segno glyph
     * @param grade the interpretation quality
     */
    public SegnoInter (Glyph glyph,
                       double grade)
    {
        super(glyph, glyph.getBounds(), Shape.SEGNO, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    public SegnoInter ()
    {
        super(null, null, null, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // delete //
    //--------//
    @Override
    public void delete ()
    {
        if (staff != null) {
            staff.removeOtherInter(this);
        }

        super.delete();
    }

    //-----------------//
    // linkWithBarline //
    //-----------------//
    /**
     * (Try to) connect this segno with a suitable barline.
     *
     * @return true if successful
     */
    public boolean linkWithBarline ()
    {
        Point center = getCenter();
        List<BarlineInter> bars = getStaff().getBars();
        BarlineInter bar = BarlineInter.getClosestBarline(bars, center);

        if ((bar != null) && (GeoUtil.xOverlap(getBounds(), bar.getBounds()) > 0)) {
            sig.addEdge(this, bar, new SegnoBarRelation());

            return true;
        }

        return false;
    }
}
