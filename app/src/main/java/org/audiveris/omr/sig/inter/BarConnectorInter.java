//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                B a r C o n n e c t o r I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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

import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.grid.BarConnection;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;

import java.awt.Color;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>BarConnectorInter</code> represents a vertical connector between two bar
 * lines across staves.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bar-connector")
public class BarConnectorInter
        extends AbstractVerticalConnectorInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private BarConnectorInter ()
    {
    }

    /**
     * Creates a new <code>BarConnectorInter</code> object.
     *
     * @param connection the underlying connection
     * @param shape      the assigned shape
     * @param impacts    the assignment details
     */
    public BarConnectorInter (BarConnection connection,
                              Shape shape,
                              GradeImpacts impacts)
    {
        super(connection, shape, impacts);
    }

    /**
     * Creates a new <code>BarConnectorInter</code> object.
     *
     * @param shape  the assigned shape
     * @param grade  quality
     * @param median vertical segment
     * @param width  segment width
     */
    public BarConnectorInter (Shape shape,
                              Double grade,
                              Line2D median,
                              double width)
    {
        super(shape, grade, median, width);
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

    //----------//
    // getColor //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * A connector that stands on its two bar lines is not shown as a weak one, even though its
     * own grade is.
     */
    @Override
    public Color getColor ()
    {
        if (!isAbnormal() && isVouchedByBars()) {
            return shape.getColor();
        }

        return super.getColor();
    }

    //-----------------//
    // isVouchedByBars //
    //-----------------//
    /**
     * Report whether the two bar lines this connector connects are still there.
     * <p>
     * The connection between two bar lines has been accepted on its own (alignment, gap and white
     * ratio), only its grade can be poor, typically when the page is curled so that the bar lines
     * lean differently from the sheet slope.
     * Nothing in the SIG supports a connector, so its grade alone would then get it purged,
     * whereas its bar lines support each other.
     *
     * @return true if the connector still links two bar lines
     */
    public boolean isVouchedByBars ()
    {
        int bars = 0;

        for (Relation rel : sig.getRelations(this, NoExclusion.class)) {
            if (sig.getOppositeInter(this, rel) instanceof BarlineInter) {
                bars++;
            }
        }

        return bars >= 2;
    }

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return getGrade() >= Grades.goodBarConnectorGrade;
    }
}
