//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S t a f f B a r l i n e I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.BACK_TO_BACK_REPEAT_SIGN;
import static org.audiveris.omr.glyph.Shape.LEFT_REPEAT_SIGN;
import static org.audiveris.omr.glyph.Shape.RIGHT_REPEAT_SIGN;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.PartBarline.Style;
import static org.audiveris.omr.sheet.PartBarline.Style.HEAVY;
import static org.audiveris.omr.sheet.PartBarline.Style.HEAVY_HEAVY;
import static org.audiveris.omr.sheet.PartBarline.Style.HEAVY_LIGHT;
import static org.audiveris.omr.sheet.PartBarline.Style.LIGHT_HEAVY;
import static org.audiveris.omr.sheet.PartBarline.Style.LIGHT_HEAVY_LIGHT;
import static org.audiveris.omr.sheet.PartBarline.Style.LIGHT_LIGHT;
import static org.audiveris.omr.sheet.PartBarline.Style.NONE;
import static org.audiveris.omr.sheet.PartBarline.Style.REGULAR;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.EndingBarRelation;
import org.audiveris.omr.sig.relation.FermataBarRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.RepeatDotBarRelation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
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
 * Class <code>StaffBarlineInter</code> represents a logical barline for one staff only.
 * <p>
 * A <code>StaffBarlineInter</code> is a horizontal sequence of one or several {@link BarlineInter}
 * instances, for example a final StaffBarlineInter is composed on a thin BarlineInter followed by a
 * thick BarlineInter.
 * <p>
 * A {@link PartBarline} is a logical barline for one part, a vertical sequence composed of one
 * <code>StaffBarlineInter</code> for each staff in the part.
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
public final class StaffBarlineInter
        extends AbstractInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StaffBarlineInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Transient data
    //---------------

    private Style style;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor needed for JAXB.
     */
    private StaffBarlineInter ()
    {
    }

    /**
     * Creates a new <code>StaffBarlineInter</code> object from its Barline members.
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
     * Creates a new <code>StaffBarlineInter</code> object from a shape.
     *
     * @param shape THIN_BARLINE, THICK_BARLINE, DOUBLE_BARLINE, FINAL_BARLINE,
     *              REVERSE_FINAL_BARLINE, LEFT_REPEAT_SIGN,
     *              RIGHT_REPEAT_SIGN, BACK_TO_BACK_REPEAT_SIGN
     * @param grade quality
     */
    public StaffBarlineInter (Shape shape,
                              Double grade)
    {
        this.shape = shape;
        setGrade(grade);

        if (shape != null) {
            style = toStyle(shape);
        }
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

    //-----------//
    // cancelAdd //
    //-----------//
    /**
     * Cancel the proposed StaffBarlineInter addition.
     *
     * @param cancel the boolean to set
     * @param sheet  the containing sheet
     * @return an empty task list
     */
    private List<? extends UITask> cancelAdd (WrappedBoolean cancel,
                                              Sheet sheet)
    {
        cancel.set(true);

        sheet.getInterIndex().publish(null);
        sheet.getLocationService().publish(
                new LocationEvent(this, SelectionHint.LOCATION_INIT, MouseMovement.PRESSING, null));

        return Collections.emptyList();
    }

    //--------------//
    // computeStyle //
    //--------------//
    /**
     * Compute the style for this StaffBarline, based on its member barlines.
     * <p>
     * The separation between thin and thick barlines is not reliable enough, therefore we have
     * to base the computation on the number of barlines rather than a strict difference between
     * thin/thick roles.
     *
     * @return the style inferred from composition
     */
    private Style computeStyle ()
    {
        switch (getMembers().size()) {
        case 0:
            return NONE;

        case 1:
            return (getLeftBar().getShape() == Shape.THIN_BARLINE) ? REGULAR : HEAVY;

        case 2:
            if (getLeftBar().getShape() == Shape.THIN_BARLINE) {
                return (getRightBar().getShape() == Shape.THIN_BARLINE) ? LIGHT_LIGHT : LIGHT_HEAVY;
            } else {
                return (getRightBar().getShape() == Shape.THIN_BARLINE) ? HEAVY_LIGHT : HEAVY_HEAVY;
            }

        case 3:
            return LIGHT_HEAVY_LIGHT;

        default:
            logger.warn("Unknown style for {}", this);

            return null;
        }
    }

    //----------//
    // contains //
    //----------//
    /**
     * Tell whether this StaffBarlineInter contains the provided barline.
     *
     * @param barline provided barline
     * @return true if so
     */
    public boolean contains (BarlineInter barline)
    {
        return sig.getRelation(this, barline, Containment.class) != null;
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        return deriveOnStaffMiddleLine(this, staff, symbol, sheet, font, dropLocation);
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

    //--------------------//
    // getClosureToRemove //
    //--------------------//
    /**
     * Retrieve the system-high closure of StaffBarline instance this one is part of,
     * and prompt user for removal confirmation.
     *
     * @param cancel (output) if not null, ability to cancel processing by setting it to true
     * @return the closure to remove, empty if user did not confirm.
     */
    List<Inter> getClosureToRemove (WrappedBoolean cancel)
    {
        final List<Inter> closure = new ArrayList<>();

        for (PartBarline pb : getSystemBarline()) {
            closure.addAll(pb.getStaffBarlines());
        }

        // Display closure staff barlines to user
        final Sheet sheet = sig.getSystem().getSheet();
        sheet.getInterIndex().getEntityService().publish(
                new EntityListEvent<>(
                        this,
                        SelectionHint.ENTITY_INIT,
                        MouseMovement.PRESSING,
                        closure));

        if ((cancel == null) || OMR.gui.displayConfirmation(
                "Do you confirm whole system-height removal?",
                "Removal of " + closure.size() + " barline(s)")) {
            return closure;
        } else {
            cancel.set(true);

            return Collections.emptyList();
        }
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
                    fermatas = new LinkedHashSet<>();
                }

                fermatas.add((FermataInter) sig.getOppositeInter(bar, rel));
            }
        }

        if (fermatas == null) {
            // Use of direct relation
            for (Relation rel : sig.getRelations(this, FermataBarRelation.class)) {
                if (fermatas == null) {
                    fermatas = new LinkedHashSet<>();
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
    /**
     * Report the starting Barline in this StaffBarlineInter.
     *
     * @return first barline
     */
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
    /**
     * Report the containing measure.
     *
     * @return containing measure
     */
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

    //--------------//
    // getMiddleBar //
    //--------------//
    /**
     * Report the middle barline if any.
     *
     * @return middle barline or null
     */
    public BarlineInter getMiddleBar ()
    {
        final List<Inter> bars = getMembers();

        if (bars.size() != 3) {
            return null;
        }

        return (BarlineInter) bars.get(1);
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
    public Point2D getReferenceCenter ()
    {
        BarlineInter rightBar = getRightBar();

        if (rightBar != null) {
            return rightBar.getCenter2D();
        }

        // No bar members, use shape and bounds
        if ((shape != null) && (bounds != null)) {
            final int y = bounds.y + (bounds.height / 2);

            switch (shape) {
            case THIN_BARLINE:
            case THICK_BARLINE:
                return GeoUtil.center2D(bounds);

            case DOUBLE_BARLINE:
            case REVERSE_FINAL_BARLINE:
                return new Point2D.Double(bounds.x + (0.9 * bounds.width), y);

            case FINAL_BARLINE:
                return new Point2D.Double(bounds.x + (0.7 * bounds.width), y);

            case LEFT_REPEAT_SIGN:
            case RIGHT_REPEAT_SIGN:
            case BACK_TO_BACK_REPEAT_SIGN:
                return new Point2D.Double(bounds.x + (0.5 * bounds.width), y);

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
                    related = new ArrayList<>();
                }

                related.add(sig.getOppositeInter(bar, rel));
            }
        }

        if (bars.isEmpty()) {
            for (Relation rel : sig.getRelations(this, relationClass)) {
                if (related == null) {
                    related = new ArrayList<>();
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
    /**
     * report the stopping barline
     *
     * @return last barline in StaffBarline
     */
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

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        PartBarline.Style style = getStyle();

        if (style != null) {
            return style.toString();
        }

        return null;
    }

    //----------//
    // getStyle //
    //----------//
    /**
     * Report the StaffBarline style.
     *
     * @return style (like LIGHT_HEAVY, ...) in line with MusicXML definitions
     */
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
        final List<PartBarline> systemBarline = new ArrayList<>();
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
    /**
     * Tell whether there are repeat dots on the left side of this bar.
     *
     * @return true if so
     */
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
    /**
     * Tell whether there are repeat dots on the right side of this bar.
     *
     * @return true if so
     */
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

    //-------------------------//
    // imposeWithinStaffLimits //
    //-------------------------//
    @Override
    public boolean imposeWithinStaffLimits ()
    {
        return true;
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

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        bounds = null;
        style = null;

        // Recompute ensemble grade
        setGrade(EnsembleHelper.computeMeanContextualGrade(this));
    }

    //--------------//
    // isBackToBack //
    //--------------//
    /**
     * Report whether this instance corresponds to a back to back configuration of barlines.
     *
     * @return true if so
     */
    public boolean isBackToBack ()
    {
        if (shape == BACK_TO_BACK_REPEAT_SIGN) {
            return true;
        }

        return getMembers().size() >= 3;
    }

    //--------------//
    // isLeftRepeat //
    //--------------//
    /**
     * Tell whether this barline is a measure left repeat (dots on right of barline).
     * <p>
     * Here again, we can't be too strict on thin/thick difference. *
     *
     * @return true if so
     */
    public boolean isLeftRepeat ()
    {
        if ((shape == LEFT_REPEAT_SIGN) || (shape == BACK_TO_BACK_REPEAT_SIGN)) {
            return true;
        }

        if (!hasDotsOnRight()) {
            return false;
        }

        final List<Inter> bars = getMembers();
        final int size = bars.size();
        getStyle();

        switch (size) {
        case 2:
            // Style should be HEAVY_LIGHT
            if (style != HEAVY_LIGHT) {
                BarlineInter b1 = (BarlineInter) bars.get(0);
                double w1 = b1.getWidth();
                BarlineInter b2 = (BarlineInter) bars.get(1);
                double w2 = b2.getWidth();

                if (w2 < w1) {
                    style = HEAVY_LIGHT;
                    b1.setShape(Shape.THICK_BARLINE);
                    b2.setShape(Shape.THIN_BARLINE);
                }
            }

            return true;

        case 3:
            // Style should be LIGHT_HEAVY_LIGHT
            return true;

        default:
            return false;
        }
    }

    //---------------//
    // isRightRepeat //
    //---------------//
    /**
     * Tell whether this barline is a measure right repeat (dots on left of barline).
     * <p>
     * Here again, we can't be too strict on thin/thick difference.
     *
     * @return true if so
     */
    public boolean isRightRepeat ()
    {
        if ((shape == RIGHT_REPEAT_SIGN) || (shape == BACK_TO_BACK_REPEAT_SIGN)) {
            return true;
        }

        if (!hasDotsOnLeft()) {
            return false;
        }

        final List<Inter> bars = getMembers();
        final int size = bars.size();
        getStyle();

        switch (size) {
        case 2:
            // Style should be LIGHT_HEAVY
            if (style != LIGHT_HEAVY) {
                BarlineInter b1 = (BarlineInter) bars.get(0);
                double w1 = b1.getWidth();
                BarlineInter b2 = (BarlineInter) bars.get(1);
                double w2 = b2.getWidth();

                if (w2 > w1) {
                    style = LIGHT_HEAVY;
                    b1.setShape(Shape.THIN_BARLINE);
                    b2.setShape(Shape.THICK_BARLINE);
                }
            }

            return true;

        case 3:
            // Style should be LIGHT_HEAVY_LIGHT
            return true;

        default:
            return false;
        }
    }

    //------------//
    // isStaffEnd //
    //------------//
    /**
     * Report whether this StaffBarlineInter is on provided side of the staff.
     *
     * @param hSide provided horizontal side
     * @return true if so
     */
    public boolean isStaffEnd (HorizontalSide hSide)
    {
        final int x = getCenter().x;
        final int extrema = staff.getAbscissa(hSide);
        final int dx = Math.abs(extrema - x);
        final Scale scale = staff.getSystem().getSheet().getScale();
        final int maxShift = scale.toPixels(StaffBarlineInter.getMaxStaffBarlineShift());

        return dx <= maxShift;
    }

    //--------//
    // preAdd //
    //--------//
    /**
     * {@inheritDoc}
     * <p>
     * Specifically, when manually adding a StaffBarline, we make sure to add one StaffBarline
     * for every staff in system.
     */
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        // Standard addition task for this staffBarline
        final List<UITask> tasks = new ArrayList<>(super.preAdd(cancel, toPublish));
        final SystemInfo system = staff.getSystem();
        final Sheet sheet = system.getSheet();

        // Include a staffBarline per system staff, properly positioned in abscissa
        final Scale scale = sheet.getScale();
        final int lineThickness = scale.getFore();
        final SIGraph theSig = system.getSig();
        final Skew skew = sheet.getSkew();
        final Point center = getCenter();
        final double slope = skew.getSlope();

        final List<Inter> bars = new ArrayList<>(); // Bars to insert
        final List<Staff> extStaves = new ArrayList<>(); // Staves that would need a side extension
        final int maxDx = scale.toPixels(getMaxStaffBarlineShift());

        for (Staff st : system.getStaves()) {
            final double x; // Abscissa for the to-be-inserted StaffBarlineInter

            if (st == staff) {
                x = center.getX();
                bars.add(this);
            } else {
                final double y1 = st.getFirstLine().yAt(center.getX());
                final double y2 = st.getLastLine().yAt(center.getX());
                final double y = (y1 + y2) / 2;
                x = center.x - ((y - center.y) * slope);
                final Rectangle box = new Rectangle((int) Math.rint(x), (int) Math.rint(y), 0, 0);
                box.grow(bounds.width / 2, (int) Math.rint((lineThickness + y2 - y1) / 2));

                final StaffBarlineInter sb = new StaffBarlineInter(shape, 1.0);
                sb.setManual(true);
                sb.setStaff(st);
                sb.setBounds(box);
                bars.add(sb);
                tasks.add(new AdditionTask(theSig, sb, box, sb.searchLinks(system)));
            }

            // Staff extension?
            if ((x > st.getAbscissa(HorizontalSide.RIGHT) + maxDx) || (x < st.getAbscissa(
                    HorizontalSide.LEFT) - maxDx)) {
                extStaves.add(st);
            }
        }

        // Display closure staff barlines to user
        if (bars.size() > 1) {
            sheet.getInterIndex().getEntityService().publish(
                    new EntityListEvent<>(
                            this,
                            SelectionHint.ENTITY_INIT,
                            MouseMovement.PRESSING,
                            bars));

            if (!OMR.gui.displayConfirmation(
                    "Do you confirm whole system-height addition?",
                    "Insertion of " + bars.size() + " barlines")) {
                return cancelAdd(cancel, sheet);
            }
        }

        // Check for staff lines extension
        if (!extStaves.isEmpty()) {
            final StringBuilder ids = new StringBuilder();
            extStaves.forEach(st -> ids.append(" #").append(st.getId()));

            if (!OMR.gui.displayConfirmation(
                    "Do you confirm extension of staff lines?",
                    "Extension of staves" + ids)) {
                return cancelAdd(cancel, sheet);
            }

            // Extend staff lines BEFORE inserting barlines
        }

        return tasks;
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

        if (sheet.getStub().getLatestStep().compareTo(OmrStep.MEASURES) < 0) {
            return inters;
        }

        // Now that measures exist, it's whole system height or nothing
        final List<Inter> closure = getClosureToRemove(cancel);

        if ((cancel == null) || !cancel.isSet()) {
            inters.addAll(closure);
        }

        return inters;
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
            logger.warn("No style for barline shape {}", shape);

            return null;
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

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

    //-------------------------//
    // getMaxStaffBarlineShift //
    //-------------------------//
    public static Scale.Fraction getMaxStaffBarlineShift ()
    {
        return constants.maxStaffBarlineShift;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxStaffBarlineShift = new Scale.Fraction(
                1.0,
                "Maximum deskewed abscissa difference within a column");
    }
}
