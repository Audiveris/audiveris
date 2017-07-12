//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a r l i n e I n t e r                                    //
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
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.BarGroupRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.RepeatDotBarRelation;
import org.audiveris.omr.util.HorizontalSide;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BarlineInter} represents an interpretation of bar line (thin or thick
 * vertical segment).
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "barline")
public class BarlineInter
        extends AbstractVerticalInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Does this bar line define a staff side?. */
    @XmlAttribute(name = "staff-end")
    private HorizontalSide staffEnd;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BarlineInter object.
     *
     * @param glyph   the underlying glyph
     * @param shape   the assigned shape
     * @param impacts the assignment details
     * @param median  the median line
     * @param width   the bar line width
     */
    public BarlineInter (Glyph glyph,
                         Shape shape,
                         GradeImpacts impacts,
                         Line2D median,
                         double width)
    {
        super(glyph, shape, impacts, median, width);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BarlineInter ()
    {
        super(null, null, null, null, 0);
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
    // delete //
    //--------//
    /**
     * Since a BarlineInter instance is held by its containing staff, make sure staff
     * bar collection is updated.
     */
    @Override
    public void delete ()
    {
        if (staff != null) {
            staff.removeBar(this);
        }

        super.delete();
    }

    //-------------------//
    // getClosestBarline //
    //-------------------//
    /**
     * From a provided Barline collection, report the one which has the closest abscissa
     * to a provided point.
     *
     * @param bars  the collection of bars to browse
     * @param point the reference point
     * @return the abscissa-wise closest barline
     */
    public static BarlineInter getClosestBarline (Collection<BarlineInter> bars,
                                                  Point point)
    {
        BarlineInter bestBar = null;
        int bestDx = Integer.MAX_VALUE;

        for (BarlineInter bar : bars) {
            int dx = Math.abs(bar.getCenter().x - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestBar = bar;
            }
        }

        return bestBar;
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        if (isStaffEnd(HorizontalSide.LEFT)) {
            return super.getDetails() + "/STAFF_LEFT_END";
        } else if (isStaffEnd(HorizontalSide.RIGHT)) {
            return super.getDetails() + "/STAFF_RIGHT_END";
        } else {
            return super.getDetails();
        }
    }

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return getGrade() >= 0.6; // TODO, quick & dirty
    }

    //------------//
    // isStaffEnd //
    //------------//
    public boolean isStaffEnd (HorizontalSide side)
    {
        return staffEnd == side;
    }

    //-------------//
    // setStaffEnd //
    //-------------//
    public void setStaffEnd (HorizontalSide side)
    {
        staffEnd = side;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //------------//
    // getSibling //
    //------------//
    /**
     * Report the sibling barline within a double group, if any
     *
     * @return sibling barline or null
     */
    public BarlineInter getSibling ()
    {
        if (shape == Shape.THIN_BARLINE) {
            for (Relation bgRel : sig.getRelations(this, BarGroupRelation.class)) {
                BarlineInter other = (BarlineInter) sig.getOppositeInter(this, bgRel);

                if (other.getShape() == Shape.THIN_BARLINE) {
                    return other;
                }
            }
        }

        return null;
    }

    /**
     * Report the sequence of items (barlines and repeat dots if any) this barline
     * is part of.
     *
     * @return the items sequence, composed of at least this barline
     */
    public SortedSet<Inter> getGroupItems ()
    {
        SortedSet<Inter> items = new TreeSet<Inter>(Inter.byFullAbscissa);
        items.add(this);
        browseGroup(this, items);

        return items;
    }

    private void browseGroup (BarlineInter bar,
                              SortedSet<Inter> items)
    {
        for (Relation rel : sig.getRelations(
                bar,
                BarGroupRelation.class,
                RepeatDotBarRelation.class)) {
            Inter other = sig.getOppositeInter(bar, rel);

            if (!items.contains(other)) {
                items.add(other);

                if (other instanceof BarlineInter) {
                    browseGroup((BarlineInter) other, items);
                }
            }
        }
    }
}
