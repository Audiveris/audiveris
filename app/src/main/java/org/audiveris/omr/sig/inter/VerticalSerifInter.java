//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               V e r t i c a l S e r i f I n t e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.MultipleRestSerifRelation;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>VerticalSerifInter</code> represents a vertical serif on a horizontal side of
 * a MultipleRestInter.
 *
 * @see MultipleRestInter
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "vertical-serif")
public class VerticalSerifInter
        extends AbstractVerticalInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private VerticalSerifInter ()
    {
        super(null, null, 0.0);
    }

    /**
     * Creates a new VerticalSerifInter object.
     *
     * @param glyph  the underlying glyph
     * @param grade  the assigned grade
     * @param median the median line
     * @param width  the serif width
     */
    public VerticalSerifInter (Glyph glyph,
                               Double grade,
                               Line2D median,
                               Double width)
    {
        super(glyph, Shape.VERTICAL_SERIF, grade, median, width);
        setAbnormal(true);
    }

    /**
     * Creates a new VerticalSerifInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the grade details
     * @param median  the median line
     * @param width   the serif width
     */
    public VerticalSerifInter (Glyph glyph,
                               GradeImpacts impacts,
                               Line2D median,
                               Double width)
    {
        super(glyph, Shape.VERTICAL_SERIF, impacts, median, width);
        setAbnormal(true);
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

    //---------------//
    // checkAbnormal //
    //---------------//
    /**
     * Check if a multiple rest is connected.
     *
     * @return true if abnormal
     */
    @Override
    public boolean checkAbnormal ()
    {
        setAbnormal(!sig.hasRelation(this, MultipleRestSerifRelation.class));

        return isAbnormal();
    }
}
