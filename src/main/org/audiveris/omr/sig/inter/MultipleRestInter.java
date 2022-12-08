//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                M u l t i p l e R e s t I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.MultipleRestNumberRelation;
import org.audiveris.omr.sig.relation.MultipleRestSerifRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.HorizontalSide;

import java.awt.geom.Line2D;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MultipleRestInter</code> represents a multiple measure rest drawn as a
 * thick horizontal line centered on staff middle line with serifs at both ends,
 * completed by a time-like number drawn above the staff.
 * <p>
 * <img alt="MultipleRest" src="https://en.wikipedia.org/wiki/File:15_bars_multirest.png">
 * <p>
 * For Audiveris, this music item is constructed by:
 * <ul>
 * <li>A MultipleRestInter, which is similar to a horizontal beam
 * <li>Two VerticalSerifInter's, similar to stems, linked by MultipleRestSerifRelation's
 * <li>A MeasureNumberInter, similar to a time number, linked by a MultipleRestNumberRelation
 * </ul>
 *
 * @see VerticalSerifInter
 * @see MeasureNumberInter
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "multiple-rest")
public class MultipleRestInter
        extends AbstractHorizontalInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new MultipleRestInter object.
     *
     * @param grade  evaluation grade
     * @param median median beam line
     * @param height beam height
     */
    public MultipleRestInter (Double grade,
                              Line2D median,
                              double height)
    {
        super(Shape.MULTIPLE_REST, grade, median, height);
    }

    /**
     * Creates manually a new MultipleRestInter ghost object.
     *
     * @param grade quality grade
     */
    public MultipleRestInter (Double grade)
    {
        super(Shape.MULTIPLE_REST, grade);
    }

    /**
     * Meant for JAXB.
     */
    private MultipleRestInter ()
    {
        super((Shape) null, (GradeImpacts) null, null, 0);
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
     * Check if this multiple rest is connected to a vertical serif on each side.
     *
     * @return true if abnormal
     */
    @Override
    public boolean checkAbnormal ()
    {
        boolean left = false;
        boolean right = false;

        for (Relation rel : sig.getRelations(this, MultipleRestSerifRelation.class)) {
            final MultipleRestSerifRelation msRel = (MultipleRestSerifRelation) rel;
            final HorizontalSide restSide = msRel.getRestSide();

            if (restSide == HorizontalSide.LEFT) {
                left = true;
            } else if (restSide == HorizontalSide.RIGHT) {
                right = true;
            }
        }

        setAbnormal(!left || !right);

        return isAbnormal();
    }

    //------------------//
    // getMeasureNumber //
    //------------------//
    /**
     * Report the measure number related to this multiple measure rest.
     *
     * @return the related MeasureNumberInter or null
     */
    public MeasureNumberInter getMeasureNumber ()
    {
        for (Relation rel : getSig().getRelations(this, MultipleRestNumberRelation.class)) {
            return (MeasureNumberInter) getSig().getOppositeInter(this, rel);
        }

        return null;
    }
}
