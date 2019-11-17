//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E n d i n g I n t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.EndingBarRelation;
import org.audiveris.omr.sig.relation.EndingSentenceRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.EndingSymbol;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.sig.ui.InterUIModel;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code EndingInter} represents an ending.
 * <p>
 * MusicXML spec:
 * The number attribute reflects the numeric values of what is under the ending line.
 * Single endings such as "1" or comma-separated multiple endings such as "1,2" may be used.
 * The ending element text is used when the text displayed in the ending is different than what
 * appears in the number attribute.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ending")
public class EndingInter
        extends AbstractInter
{

    private static final Constants constants = new Constants();

    /** Default thickness of a wedge line. */
    public static final double DEFAULT_THICKNESS = constants.defaultThickness.getValue();

    // Persistent Data
    //----------------
    //
    /** Mandatory left leg. */
    @XmlElement(name = "left-leg")
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D leftLeg;

    /** Horizontal line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D line;

    /** Optional right leg, if any. */
    @XmlElement(name = "right-leg")
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D rightLeg;

    /**
     * Creates a new {@code EndingInter} object.
     *
     * @param line     precise line
     * @param leftLeg  mandatory left leg
     * @param rightLeg optional right leg
     * @param bounds   bounding box
     * @param impacts  assignments details
     */
    public EndingInter (Line2D line,
                        Line2D leftLeg,
                        Line2D rightLeg,
                        Rectangle bounds,
                        GradeImpacts impacts)
    {
        super(null, bounds, Shape.ENDING, impacts);
        this.line = line;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
    }

    /**
     * Creates a new {@code EndingInter} object, meant for user manual handling.
     *
     * @param grade interpretation quality
     */
    public EndingInter (double grade)
    {
        super(null, null, Shape.ENDING, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private EndingInter ()
    {
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        getBounds();

        if ((bounds != null) && !bounds.contains(point)) {
            return false;
        }

        if ((glyph != null) && glyph.contains(point)) {
            return true;
        }

        if (line.ptLineDistSq(point) <= DEFAULT_THICKNESS * DEFAULT_THICKNESS / 4) {
            return true;
        }

        if (leftLeg.ptLineDistSq(point) <= DEFAULT_THICKNESS * DEFAULT_THICKNESS / 4) {
            return true;
        }

        if (rightLeg != null) {
            if (rightLeg.ptLineDistSq(point) <= DEFAULT_THICKNESS * DEFAULT_THICKNESS / 4) {
                return true;
            }
        }

        return false;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        Rectangle box = super.getBounds();

        if (box != null) {
            return box;
        }

        box = line.getBounds().union(leftLeg.getBounds());

        if (rightLeg != null) {
            box = box.union(rightLeg.getBounds());
        }

        box.grow((int) Math.ceil(DEFAULT_THICKNESS / 2.0), (int) Math.ceil(DEFAULT_THICKNESS / 2.0));

        return new Rectangle(bounds = box);
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //-------------------//
    // getExportedNumber //
    //-------------------//
    /**
     * Filter the ending number string to comply with MusicXML constraint that it must
     * be formatted as "1" or "1,2".
     *
     * @return the formatted number string, if any
     */
    public String getExportedNumber ()
    {
        String raw = getNumber();

        if (raw == null) {
            return null;
        }

        String[] nums = raw.split("[^0-9]"); // Any non-digit character is a separator
        StringBuilder sb = new StringBuilder();

        for (String num : nums) {
            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(num);
        }

        return sb.toString();
    }

    //------------//
    // getLeftLeg //
    //------------//
    /**
     * @return the leftLeg
     */
    public Line2D getLeftLeg ()
    {
        return leftLeg;
    }

    //---------//
    // getLine //
    //---------//
    /**
     * @return the line
     */
    public Line2D getLine ()
    {
        return line;
    }

    //-----------//
    // getNumber //
    //-----------//
    /**
     * Report the ending number clause, if any.
     *
     * @return ending number clause or null
     */
    public String getNumber ()
    {
        for (Relation r : sig.getRelations(this, EndingSentenceRelation.class)) {
            SentenceInter sentence = (SentenceInter) sig.getOppositeInter(this, r);
            TextRole role = sentence.getRole();
            String value = sentence.getValue().trim();

            if ((role == TextRole.EndingNumber) || value.matches("[1-9].*")) {
                return value;
            }
        }

        return null;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter ()
    {
        return PointUtil.middle(line);
    }

    //-------------//
    // getRightLeg //
    //-------------//
    /**
     * @return the rightLeg
     */
    public Line2D getRightLeg ()
    {
        return rightLeg;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * The raw ending text, only if different from normalized number.
     * <p>
     * For instance, the actual text could be: "1., 2." and the normalized number: "1, 2"
     *
     * @return the raw ending text or null
     */
    public String getValue ()
    {
        final String number = getNumber();

        for (Relation r : sig.getRelations(this, EndingSentenceRelation.class)) {
            SentenceInter sentence = (SentenceInter) sig.getOppositeInter(this, r);
            String value = sentence.getValue().trim();

            if (!value.equals(number)) {
                return value;
            }
        }

        return null;
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public void deriveFrom (ShapeSymbol symbol,
                            MusicFont font,
                            Point dropLocation,
                            Alignment alignment)
    {
        final EndingSymbol endingSymbol = (EndingSymbol) symbol;
        final Model model = endingSymbol.getModel(font, dropLocation, alignment);

        line = new Line2D.Double(model.topLeft, model.topRight);
        leftLeg = new Line2D.Double(model.topLeft, model.bottomLeft);

        if (model.bottomRight != null) {
            rightLeg = new Line2D.Double(model.topRight, model.bottomRight);
        }

        setBounds(null);
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final List<Link> links = new ArrayList<>();
        final Scale scale = system.getSheet().getScale();

        // Consider the staff just below the segment
        staff = system.getStaffAtOrBelow(line.getP1());

        if (staff == null) {
            return links;
        }

        List<Inter> systemBars = system.getSig().inters(StaffBarlineInter.class);

        // Left bar (or header)
        StaffBarlineInter leftBar = lookupBar(LEFT, staff, systemBars);
        final EndingBarRelation leftRel = new EndingBarRelation(LEFT, 0.5);

        if (leftBar == null) {
            // Check the special case of a staff start (with header?, with no barline?)
            MeasureStack firstStack = system.getFirstStack();

            if (firstStack == null) {
                return links;
            }

            Measure firstMeasure = firstStack.getMeasureAt(staff);

            if (line.getX1() >= firstMeasure.getAbscissa(RIGHT, staff)) {
                // segment starts after end of first measure
                return links;
            }

            PartBarline partLine = staff.getPart().getLeftPartBarline();

            if (partLine != null) {
                leftBar = partLine.getStaffBarline(staff.getPart(), staff);
                leftRel.setOutGaps(0, 0, false);
            }
        } else {
            double leftDist = scale.pixelsToFrac(Math.abs(leftBar.getCenter().x - line.getX1()));
            leftRel.setOutGaps(leftDist, 0, false);
        }

        links.add(new Link(leftBar, leftRel, true));

        // Right bar
        StaffBarlineInter rightBar = lookupBar(RIGHT, staff, systemBars);

        if (rightBar != null) {
            double rightDist = scale.pixelsToFrac(Math.abs(rightBar.getCenter().x - line.getX2()));
            final EndingBarRelation rightRel = new EndingBarRelation(RIGHT, rightDist);
            rightRel.setOutGaps(rightDist, 0, false);

            links.add(new Link(rightBar, rightRel, true));
        }

        return links;
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, EndingBarRelation.class);
    }

    //-----------//
    // lookupBar //
    //-----------//
    /**
     * Look for a StaffBarline vertically aligned with the ending side.
     * <p>
     * It is not very important to select a precise barline within a group, since for left end we
     * choose the right-most bar and the opposite for right end.
     * We simply have to make sure that the lookup area is wide enough.
     * <p>
     * An ending which starts a staff may have its left side after the clef and key signature, which
     * means far after the starting barline (if any).
     * Perhaps we should consider the staff header in such case.
     *
     * @param staff      related staff
     * @param systemBars the collection of StaffBarlines in the containing system
     * @return the selected bar line, or null if none
     */
    private StaffBarlineInter lookupBar (HorizontalSide side,
                                         Staff staff,
                                         List<Inter> systemBars)
    {
        final SystemInfo system = staff.getSystem();
        final Scale scale = system.getSheet().getScale();
        final Point end = PointUtil.rounded(
                (side == HorizontalSide.LEFT) ? line.getP1() : line.getP2());
        final int maxBarShift = scale.toPixels(EndingBarRelation.getXGapMaximum(manual));
        Rectangle box = new Rectangle(end);
        box.grow(maxBarShift, 0);
        box.height = staff.getLastLine().yAt(end.x) - end.y;

        List<Inter> bars = Inters.intersectedInters(systemBars, GeoOrder.NONE, box);
        Collections.sort(bars, Inters.byAbscissa);

        if (bars.isEmpty()) {
            return null;
        }

        return (StaffBarlineInter) bars.get((side == HorizontalSide.LEFT) ? (bars.size() - 1) : 0);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double defaultThickness = new Constant.Double(
                "pixels",
                2.0,
                "Default ending lines thickness");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for an ending.
     * <p>
     * For an ending, there are 3 handles:
     * <ul>
     * <li>Left handle, moving only horizontally (with its related leg if any)
     * <li>Top middle handle, moving the whole inter in any direction
     * <li>Right handle, moving only horizontally (with its related leg if any)
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {

        private final Model originalModel = new Model();

        private final Model model = new Model();

        private final Point2D midTop;

        private final Point2D midLeft;

        private final Point2D midRight;

        public Editor (final EndingInter ending)
        {
            super(ending);

            originalModel.topLeft = ending.line.getP1();
            model.topLeft = ending.line.getP1();

            originalModel.topRight = ending.line.getP2();
            model.topRight = ending.line.getP2();

            originalModel.bottomLeft = ending.leftLeg.getP2();
            model.bottomLeft = ending.leftLeg.getP2();

            midTop = PointUtil.middle(ending.line);
            midLeft = PointUtil.middle(ending.leftLeg);

            if (ending.rightLeg != null) {
                originalModel.bottomRight = ending.rightLeg.getP2();
                model.bottomRight = ending.rightLeg.getP2();

                midRight = PointUtil.middle(ending.rightLeg);
            } else {
                midRight = null;
            }

            // Global move: move all points
            handles.add(selectedHandle = new Handle(midTop)
            {
                @Override
                public boolean applyMove (Point vector)
                {
                    PointUtil.add(model.topLeft, vector);
                    PointUtil.add(midTop, vector);
                    PointUtil.add(model.topRight, vector);

                    if (ending.leftLeg != null) {
                        PointUtil.add(midLeft, vector);
                        PointUtil.add(model.bottomLeft, vector);
                    }

                    if (ending.rightLeg != null) {
                        PointUtil.add(midRight, vector);
                        PointUtil.add(model.bottomRight, vector);
                    }

                    return true;
                }
            });

            // Left handle: move horizontally only
            if (ending.leftLeg != null) {
                handles.add(new InterEditor.Handle(midLeft)
                {
                    @Override
                    public boolean applyMove (Point vector)
                    {
                        final double dx = vector.getX();

                        if (dx == 0) {
                            return false;
                        }

                        PointUtil.add(model.topLeft, dx, 0);
                        PointUtil.add(midLeft, dx, 0);
                        PointUtil.add(model.bottomLeft, dx, 0);
                        PointUtil.add(midTop, dx / 2, 0);

                        return true;
                    }
                });
            } else {
                handles.add(new InterEditor.Handle(model.topLeft)
                {
                    @Override
                    public boolean applyMove (Point vector)
                    {
                        final double dx = vector.getX();

                        if (dx == 0) {
                            return false;
                        }

                        PointUtil.add(model.topLeft, dx, 0);
                        PointUtil.add(midTop, dx / 2, 0);

                        return true;
                    }
                });
            }

            // Right handle: move horizontally only
            if (ending.rightLeg != null) {
                handles.add(new InterEditor.Handle(midRight)
                {
                    @Override
                    public boolean applyMove (Point vector)
                    {
                        final double dx = vector.getX();

                        if (dx == 0) {
                            return false;
                        }

                        PointUtil.add(model.topRight, dx, 0);
                        PointUtil.add(midRight, dx, 0);
                        PointUtil.add(model.bottomRight, dx, 0);
                        PointUtil.add(midTop, dx / 2, 0);

                        return true;
                    }
                });
            } else {
                handles.add(new InterEditor.Handle(model.topRight)
                {
                    @Override
                    public boolean applyMove (Point vector)
                    {
                        final double dx = vector.getX();

                        if (dx == 0) {
                            return false;
                        }

                        PointUtil.add(model.topRight, dx, 0);
                        PointUtil.add(midTop, dx / 2, 0);

                        return true;
                    }
                });
            }
        }

        @Override
        protected void doit ()
        {
            final EndingInter ending = (EndingInter) inter;
            ending.line.setLine(model.topLeft, model.topRight);
            ending.leftLeg.setLine(model.topLeft, model.bottomLeft);

            if (ending.rightLeg != null) {
                ending.rightLeg.setLine(model.topRight, model.bottomRight);
            }

            inter.setBounds(null);
            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            final EndingInter ending = (EndingInter) inter;

            ending.line.setLine(originalModel.topLeft, originalModel.topRight);

            if (ending.leftLeg != null) {
                ending.leftLeg.setLine(originalModel.topLeft, originalModel.bottomLeft);
            }

            if (ending.rightLeg != null) {
                ending.rightLeg.setLine(originalModel.topRight, originalModel.bottomRight);
            }

            inter.setBounds(null);
            super.undo();
        }
    }

    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]{
            "straight",
            "slope",
            "length"};

        private static final double[] WEIGHTS = new double[]{1, 1, 1};

        public Impacts (double straight,
                        double slope,
                        double length)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, straight);
            setImpact(1, slope);
            setImpact(2, length);
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements InterUIModel
    {

        public Point2D topLeft;

        public Point2D topRight;

        public Point2D bottomLeft;

        public Point2D bottomRight; // Optional

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(topLeft, dx, dy);
            PointUtil.add(topRight, dx, dy);
            PointUtil.add(bottomLeft, dx, dy);

            if (bottomRight != null) {
                PointUtil.add(bottomRight, dx, dy);
            }
        }
    }
}
