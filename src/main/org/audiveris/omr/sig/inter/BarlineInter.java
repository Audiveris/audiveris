//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B a r l i n e I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.BarGroupRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.RepeatDotBarRelation;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.WrappedBoolean;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BarlineInter} represents an interpretation of barline (thin or thick
 * vertical segment).
 * <p>
 * A barline that spans several staves is modeled as one BarlineInter per staff, interleaved by
 * {@link BarConnectorInter} instances.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "barline")
public class BarlineInter
        extends AbstractStaffVerticalInter
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
                         Double width)
    {
        super(glyph, shape, impacts, median, width);
    }

    /**
     * Creates a new BarlineInter object.
     *
     * @param glyph  the underlying glyph
     * @param shape  the assigned shape
     * @param grade  the assignment quality
     * @param median the median line
     * @param width  the bar line width
     */
    public BarlineInter (Glyph glyph,
                         Shape shape,
                         Double grade,
                         Line2D median,
                         Double width)
    {
        super(glyph, shape, grade, median, width);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BarlineInter ()
    {
        super(null, null, (Double) null, null, null);
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

    //-------//
    // added //
    //-------//
    /**
     * Since a BarlineInter instance is held by its containing staff, make sure staff
     * bar collection is updated.
     *
     * @see #remove(boolean)
     */
    @Override
    public void added ()
    {
        super.added();

        if (staff != null) {
            staff.addBarline(this);
        }
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

    //---------------//
    // getGroupItems //
    //---------------//
    /**
     * Report the sequence of items (barlines and repeat dots if any) this barline
     * is part of.
     *
     * @return the items sequence, composed of at least this barline
     */
    public SortedSet<Inter> getGroupItems ()
    {
        SortedSet<Inter> items = new TreeSet<>(Inters.byFullAbscissa);
        items.add(this);
        browseGroup(this, items);

        return items;
    }

    //------------//
    // getMeasure //
    //------------//
    /**
     * Report the containing measure.
     *
     * @return related measure
     */
    public Measure getMeasure ()
    {
        StaffBarlineInter sb = getStaffBarline();

        if (sb != null) {
            return sb.getMeasure();
        }

        return null;
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

    //----------//
    // setShape //
    //----------//
    /**
     * Allows to modify the barline shape (THIN_BARLINE or THIN_BARLINE).
     *
     * @param shape the new shape
     */
    public void setShape (Shape shape)
    {
        this.shape = shape;
    }

    //-----------------//
    // getStaffBarline //
    //-----------------//
    /**
     * Report the StaffBarlineInter, if any, which contains this barline.
     *
     * @return containing StaffBarlineInter, perhaps null currently
     */
    public StaffBarlineInter getStaffBarline ()
    {
        getPart();

        PartBarline lbp = part.getLeftPartBarline();

        if (lbp != null) {
            StaffBarlineInter sb = lbp.getStaffBarline(part, staff);

            if ((sb != null) && sb.contains(this)) {
                return sb;
            }
        }

        for (Measure measure : part.getMeasures()) {
            for (PartBarline pb : measure.getContainedPartBarlines()) {
                StaffBarlineInter sb = pb.getStaffBarline(part, staff);

                if ((sb != null) && sb.contains(this)) {
                    return sb;
                }
            }
        }

        return null;
    }

    //------------------//
    // getSystemBarline //
    //------------------//
    /**
     * Report the system-level barline structure this barline is involved in.
     *
     * @return the containing "system barline"
     */
    public List<PartBarline> getSystemBarline ()
    {
        final List<PartBarline> systemBarline = new ArrayList<>();
        final StaffBarlineInter staffBarline = getStaffBarline();

        if (staffBarline != null) {
            return staffBarline.getSystemBarline();
        }

        return systemBarline;
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
    /**
     * Tell whether this barline ends the staff on provided side.
     *
     * @param side provided side
     * @return true if so
     */
    public boolean isStaffEnd (HorizontalSide side)
    {
        return staffEnd == side;
    }

    //-----------//
    // preRemove //
    //-----------//
    @Override
    public Set<? extends Inter> preRemove (WrappedBoolean cancel)
    {
        final Set<Inter> inters = new LinkedHashSet<>();
        final Sheet sheet = sig.getSystem().getSheet();

        inters.add(this);

        if (sheet.getStub().getLatestStep().compareTo(Step.MEASURES) < 0) {
            return inters;
        }

        // Now that measures exist, it's whole system height or nothing
        final StaffBarlineInter sb = getStaffBarline();

        if (sb != null) {
            final List<Inter> closure = sb.getClosureToRemove(cancel);

            if ((cancel == null) || !cancel.isSet()) {
                inters.addAll(closure);
            }
        }

        return inters;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Since a BarlineInter instance is held by its containing staff, make sure staff
     * bar collection is updated.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (staff != null) {
            staff.removeBarline(this);
        }

        super.remove(extensive);
    }

    //-------------//
    // setStaffEnd //
    //-------------//
    /**
     * Set this barline as a staff end.
     *
     * @param side which side for the end.
     */
    public void setStaffEnd (HorizontalSide side)
    {
        staffEnd = side;
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

    //-------------//
    // browseGroup //
    //-------------//
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
