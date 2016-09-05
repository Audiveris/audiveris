//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        C o d a I n t e r                                       //
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

import omr.sig.relation.CodaBarRelation;

import java.awt.Point;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code CodaInter} represents a coda interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "coda")
public class CodaInter
        extends AbstractDirectionInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code CodaInter} object.
     *
     * @param glyph the coda glyph
     * @param grade the interpretation quality
     */
    public CodaInter (Glyph glyph,
                      double grade)
    {
        super(glyph, glyph.getBounds(), Shape.CODA, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    public CodaInter ()
    {
        super(null, null, null, 0);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // linkWithBarline //
    //-----------------//
    /**
     * (Try to) connect this coda with a suitable barline.
     *
     * @return true if successful
     */
    public boolean linkWithBarline ()
    {
        Point center = getCenter();
        List<BarlineInter> bars = getStaff().getBars();
        BarlineInter bar = BarlineInter.getClosestBarline(bars, center);

        if ((bar != null) && (GeoUtil.xOverlap(getBounds(), bar.getBounds()) > 0)) {
            sig.addEdge(this, bar, new CodaBarRelation());

            return true;
        }

        return false;
    }
}
