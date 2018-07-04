//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S t a f f B a r l i n e I n t e r                               //
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

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.PartBarline.Style;
import static org.audiveris.omr.sheet.PartBarline.Style.*;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.EndingBarRelation;
import org.audiveris.omr.sig.relation.FermataBarRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.RepeatDotBarRelation;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.HorizontalSide;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code StaffBarlineInter} represents a logical barline for one staff only.
 * <p>
 * A {@link PartBarline} is a logical barline for one part, that is made of one
 * {@code StaffBarlineInter} for each staff in the part.
 * <p>
 * A {@code StaffBarlineInter} is an ensemble composed of a horizontal sequence of one or several
 * {@link BarlineInter} instances.
 * <p>
 * Barline-related entities such as repeat dot(s), ending(s), fermata(s), segno or coda are
 * implemented as separate inters, linked to a barline by proper relation.
 * <p>
 * Reference abscissa (quoting Michael Good): the best approximation that's application-independent
 * would probably be to use the center of the barline. In back-to-back barlines that would be the
 * center of the thick barline. For double barlines like light-light or light-heavy this would be
 * the center of the rightmost barline.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "staff-barline")
public class StaffBarlineInter
        extends AbstractInter
        implements InterEnsemble
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Transient data
    //---------------
    //
    private Style style;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code StaffBarlineInter} object from a shape.
     *
     * @param shape THIN_BARLINE, THICK_BARLINE, DOUBLE_BARLINE,FINAL_BARLINE,
     *              REVERSE_FINAL_BARLINE, LEFT_REPEAT_SIGN,
     *              RIGHT_REPEAT_SIGN,BACK_TO_BACK_REPEAT_SIGN
     * @param grade quality
     */
    public StaffBarlineInter (Shape shape,
                              double grade)
    {
        this.shape = shape;
        this.grade = grade;

        if (shape != null) {
            style = toStyle(shape);
        }
    }

    /**
     * Creates a new {@code StaffBarlineInter} object from its Barline members.
     *
     * @param members the barline members
     */
    public StaffBarlineInter (Collection<? extends Inter> members)
    {
        if (members.isEmpty()) {
            return;
        }

        double g = 0;
        Staff s = null;

        for (Inter b : members) {
            if (sig == null) {
                sig = b.getSig();
                sig.addVertex(this);
            }

            addMember(b);
            g += b.getGrade();
            s = b.getStaff();

            if (b.isFrozen()) {
                this.freeze();
            }
        }

        setGrade(g / members.size());
        setStaff(s);
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    private StaffBarlineInter ()
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

    //-----------//
    // addMember //
    //-----------//
    @Override
    public void addMember (Inter member)
    {
        if (!(member instanceof BarlineInter)) {
            throw new IllegalArgumentException(
                    "Only BarlineInter can be added to StaffBarlineInter");
        }

        EnsembleHelper.addMember(this, member);
    }

    //----------//
    // contains //
    //----------//
    public boolean contains (BarlineInter barline)
    {
        return sig.getRelation(this, barline, Containment.class) != null;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers());
        }

        return new Rectangle(bounds);
    }

    //------------------------//
    // getClosestStaffBarline //
    //------------------------//
    /**
     * From a provided StaffBarline collection, report the one which has the closest
     * abscissa to a provided point.
     *
     * @param bars  the collection of StaffBarlineInter to browse
     * @param point the reference point
     * @return the abscissa-wise closest barline
     */
    public static StaffBarlineInter getClosestStaffBarline (Collection<StaffBarlineInter> bars,
                                                            Point point)
    {
        StaffBarlineInter bestBar = null;
        int bestDx = Integer.MAX_VALUE;

        for (StaffBarlineInter bar : bars) {
            int dx = Math.abs(bar.getCenter().x - point.x);

            if (dx < bestDx) {
                bestDx = dx;
                bestBar = bar;
            }
        }

        return bestBar;
    }

    //-----------//
    // getEnding //
    //-----------//
    /**
     * Report related ending, if any, with bar on desired side of ending.
     *
     * @param side horizontal side of barline WRT ending
     * @return the ending found or null
     */
    public EndingInter getEnding (HorizontalSide side)
    {
        // Use of bar members if any
        final List<Inter> bars = getMembers();

        for (Inter bar : bars) {
            for (Relation rel : sig.getRelations(bar, EndingBarRelation.class)) {
                EndingBarRelation ebRel = (EndingBarRelation) rel;

                if (ebRel.getEndingSide() == side) {
                    return (EndingInter) sig.getOppositeInter(bar, rel);
                }
            }
        }

        // Use of direct relation
        for (Relation rel : sig.getRelations(this, EndingBarRelation.class)) {
            EndingBarRelation ebRel = (EndingBarRelation) rel;

            if (ebRel.getEndingSide() == side) {
                return (EndingInter) sig.getOppositeInter(this, rel);
            }
        }

        return null;
    }

    //-------------//
    // getFermatas //
    //-------------//
    /**
     * Convenient method to report related fermata signs, if any
     *
     * @return set of (maximum two) fermata inters, perhaps empty but not null
     */
    public Set<FermataInter> getFermatas ()
    {
        // Use of bar members if any
        final List<Inter> bars = getMembers();
        Set<FermataInter> fermatas = null;

        for (Inter bar : bars) {
            SIGraph sig = bar.getSig();

            for (Relation rel : sig.getRelations(bar, FermataBarRelation.class)) {
                if (fermatas == null) {
                    fermatas = new LinkedHashSet<FermataInter>();
                }

                fermatas.add((FermataInter) sig.getOppositeInter(bar, rel));
            }
        }

        if (fermatas == null) {
            // Use of direct relation
            for (Relation rel : sig.getRelations(this, FermataBarRelation.class)) {
                if (fermatas == null) {
                    fermatas = new LinkedHashSet<FermataInter>();
                }

                fermatas.add((FermataInter) sig.getOppositeInter(this, rel));
            }
        }

        if (fermatas != null) {
            return fermatas;
        }

        return Collections.emptySet();
    }

    //------------//
    // getLeftBar //
    //------------//
    public BarlineInter getLeftBar ()
    {
        final List<Inter> bars = getMembers();

        if (bars.isEmpty()) {
            return null;
        }

        return (BarlineInter) bars.get(0);
    }

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the center abscissa of the left bar
     *
     * @return abscissa of the left side
     */
    public int getLeftX ()
    {
        final BarlineInter leftBar = getLeftBar();

        if (leftBar != null) {
            return leftBar.getCenter().x;
        } else if (bounds != null) {
            //TODO: refine if shape is known
            return bounds.x;
        }

        throw new IllegalStateException("No abscissa computable for " + this);
    }

    //------------//
    // getMeasure //
    //------------//
    public Measure getMeasure ()
    {
        getPart();

        PartBarline lpb = part.getLeftPartBarline();

        if ((lpb != null) && lpb.contains(this)) {
            return part.getFirstMeasure();
        }

        for (Measure measure : part.getMeasures()) {
            for (PartBarline pb : measure.getContainedPartBarlines()) {
                if (pb.contains(this)) {
                    return measure;
                }
            }
        }

        return null;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<Inter> getMembers ()
    {
        return EnsembleHelper.getMembers(this, Inters.byCenterAbscissa);
    }

    //----------------//
    // getPartBarline //
    //----------------//
    /**
     * Report the containing PartBarline if any currently.
     *
     * @return containing PartBarline
     */
    public PartBarline getPartBarline ()
    {
        getPart();

        PartBarline lpb = part.getLeftPartBarline();

        if ((lpb != null) && lpb.contains(this)) {
            return lpb;
        }

        for (Measure measure : part.getMeasures()) {
            for (PartBarline pb : measure.getContainedPartBarlines()) {
                if (pb.contains(this)) {
                    return pb;
                }
            }
        }

        return null;
    }

    //--------------------//
    // getReferenceCenter //
    //--------------------//
    /**
     * Return the point considered as the reference for this StaffBarline.
     * <p>
     * Preferably the center of the right bar if it exists, otherwise an abscissa based on
     * bounds and shape meant to cope with StaffBarline symbols with no bar members.
     *
     * @return the reference point
     */
    public Point getReferenceCenter ()
    {
        BarlineInter rightBar = getRightBar();

        if (rightBar != null) {
            return rightBar.getCenter();
        }

        // No bar members, use shape and bounds
        if ((shape != null) && (bounds != null)) {
            final int y = bounds.y + (bounds.height / 2);

            switch (shape) {
            case THIN_BARLINE:
            case THICK_BARLINE:
                return GeoUtil.centerOf(bounds);

            case DOUBLE_BARLINE:
                return new Point(bounds.x + (int) Math.rint(0.9 * bounds.width), y);

            case FINAL_BARLINE:
                return new Point(bounds.x + (int) Math.rint(0.7 * bounds.width), y);

            case REVERSE_FINAL_BARLINE:
                return new Point(bounds.x + (int) Math.rint(0.9 * bounds.width), y);

            case LEFT_REPEAT_SIGN:
            case RIGHT_REPEAT_SIGN:
            case BACK_TO_BACK_REPEAT_SIGN:
                return new Point(bounds.x + (int) Math.rint(0.5 * bounds.width), y);

            default:
            }
        }

        return null;
    }

    //------------------//
    // getRelatedInters //
    //------------------//
    /**
     * Report the barline-related entities found.
     *
     * @param relationClass the desired class for bar-entity relation
     * @return the list of related entities found (perhaps empty)
     */
    public List<Inter> getRelatedInters (Class<?> relationClass)
    {
        final List<Inter> bars = getMembers();
        List<Inter> related = null;

        for (Inter bar : bars) {
            for (Relation rel : sig.getRelations(bar, relationClass)) {
                if (related == null) {
                    related = new ArrayList<Inter>();
                }

                related.add(sig.getOppositeInter(bar, rel));
            }
        }

        if (bars.isEmpty()) {
            for (Relation rel : sig.getRelations(this, relationClass)) {
                if (related == null) {
                    related = new ArrayList<Inter>();
                }

                related.add(sig.getOppositeInter(this, rel));
            }
        }

        if (related == null) {
            return Collections.emptyList();
        }

        return related;
    }

    //-------------//
    // getRightBar //
    //-------------//
    public BarlineInter getRightBar ()
    {
        final List<Inter> bars = getMembers();

        if (bars.isEmpty()) {
            return null;
        }

        return (BarlineInter) bars.get(bars.size() - 1);
    }

    //-----------//
    // getRightX //
    //-----------//
    /**
     * Report the center abscissa of the right bar
     *
     * @return abscissa of the right side
     */
    public int getRightX ()
    {
        final BarlineInter rightBar = getRightBar();

        if (rightBar != null) {
            return rightBar.getCenter().x;
        } else if (bounds != null) {
            //TODO: refine if shape is known
            return (bounds.x + bounds.width) - 1;
        }

        throw new IllegalStateException("No abscissa computable for " + this);
    }

    //----------//
    // getStyle //
    //----------//
    public Style getStyle ()
    {
        if (style == null) {
            style = computeStyle();
        }

        return style;
    }

    //------------------//
    // getSystemBarline //
    //------------------//
    /**
     * Report the system-level barline structure this StaffBarline is involved in.
     *
     * @return the containing "system barline"
     */
    public List<PartBarline> getSystemBarline ()
    {
        final List<PartBarline> systemBarline = new ArrayList<PartBarline>();
        Measure measure = getMeasure();

        // Measure barline?
        if (measure != null) {
            MeasureStack stack = measure.getStack();
            PartBarline pb = measure.getRightPartBarline();

            if ((pb != null) && pb.contains(this)) {
                for (Measure m : stack.getMeasures()) {
                    systemBarline.add(m.getRightPartBarline());
                }
            } else {
                pb = measure.getMidPartBarline();

                if ((pb != null) && pb.contains(this)) {
                    for (Measure m : stack.getMeasures()) {
                        systemBarline.add(m.getMidPartBarline());
                    }
                } else {
                    pb = measure.getLeftPartBarline();

                    if ((pb != null) && pb.contains(this)) {
                        for (Measure m : stack.getMeasures()) {
                            systemBarline.add(m.getLeftPartBarline());
                        }
                    }
                }
            }
        } else {
            // Part left bar?
            PartBarline lbp = part.getLeftPartBarline();

            if (lbp != null) {
                StaffBarlineInter sb = lbp.getStaffBarline(part, staff);

                if (sb == this) {
                    // Extend to full system
                    for (Part p : staff.getSystem().getParts()) {
                        systemBarline.add(p.getLeftPartBarline());
                    }
                }
            }
        }

        return systemBarline;
    }

    //---------------//
    // hasDotsOnLeft //
    //---------------//
    public boolean hasDotsOnLeft ()
    {
        if ((shape == Shape.RIGHT_REPEAT_SIGN) || (shape == Shape.BACK_TO_BACK_REPEAT_SIGN)) {
            return true;
        }

        final Point center = getCenter();
        final List<Inter> bars = getMembers();

        for (Inter bar : bars) {
            for (Relation rel : sig.getRelations(bar, RepeatDotBarRelation.class)) {
                Inter dot = sig.getOppositeInter(bar, rel);

                if (dot.getCenter().x < center.x) {
                    return true;
                }
            }
        }

        return false;
    }

    //----------------//
    // hasDotsOnRight //
    //----------------//
    public boolean hasDotsOnRight ()
    {
        if ((shape == Shape.LEFT_REPEAT_SIGN) || (shape == Shape.BACK_TO_BACK_REPEAT_SIGN)) {
            return true;
        }

        final Point center = getCenter();
        final List<Inter> bars = getMembers();

        for (Inter bar : bars) {
            for (Relation rel : sig.getRelations(bar, RepeatDotBarRelation.class)) {
                Inter dot = sig.getOppositeInter(bar, rel);

                if (dot.getCenter().x > center.x) {
                    return true;
                }
            }
        }

        return false;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        //        bounds = null;
        //        style = null;
        //
        //        // Recompute ensemble grade
        //        final List<Inter> bars = getMembers();
        //
        //        if (bars.isEmpty()) {
        //            setGrade(0);
        //        } else {
        //            double g = 0;
        //
        //            for (Inter m : bars) {
        //                g += m.getGrade();
        //            }
        //
        //            setGrade(g / bars.size());
        //        }
    }

    //--------------//
    // isLeftRepeat //
    //--------------//
    public boolean isLeftRepeat ()
    {
        return (getStyle() == HEAVY_LIGHT) && hasDotsOnRight();
    }

    //---------------//
    // isRightRepeat //
    //---------------//
    public boolean isRightRepeat ()
    {
        return (getStyle() == LIGHT_HEAVY) && hasDotsOnLeft();
    }

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (!(member instanceof BarlineInter)) {
            throw new IllegalArgumentException(
                    "Only BarlineInter can be removed from StaffBarlineInter");
        }

        EnsembleHelper.removeMember(this, member);
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return getStyle().toString();
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());
        sb.append(" ").append(getStyle());

        return sb.toString();
    }

    //--------------//
    // computeStyle //
    //--------------//
    private Style computeStyle ()
    {
        switch (getMembers().size()) {
        case 0:
            return NONE;

        case 1:
            return (getLeftBar().getShape() == Shape.THIN_BARLINE) ? REGULAR : HEAVY;

        case 2: {
            if (getLeftBar().getShape() == Shape.THIN_BARLINE) {
                return (getRightBar().getShape() == Shape.THIN_BARLINE) ? LIGHT_LIGHT : LIGHT_HEAVY;
            } else {
                return (getRightBar().getShape() == Shape.THIN_BARLINE) ? HEAVY_LIGHT : HEAVY_HEAVY;
            }
        }

        default:
            return null;
        }
    }

    //---------//
    // toStyle //
    //---------//
    private Style toStyle (Shape shape)
    {
        switch (shape) {
        case THIN_BARLINE:
            return Style.REGULAR;

        case THICK_BARLINE:
            return Style.HEAVY;

        case DOUBLE_BARLINE:
            return Style.LIGHT_LIGHT;

        case FINAL_BARLINE:
            return Style.LIGHT_HEAVY;

        case REVERSE_FINAL_BARLINE:
            return Style.HEAVY_LIGHT;

        case LEFT_REPEAT_SIGN:
            return Style.HEAVY_LIGHT; // + dots

        case RIGHT_REPEAT_SIGN:
            return Style.LIGHT_HEAVY; // + dots

        case BACK_TO_BACK_REPEAT_SIGN:
            return Style.LIGHT_HEAVY; // Bof! + dots on both sides

        default:
            return null;
        }
    }
}
