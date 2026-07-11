//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       W e d g e I n t e r                                      //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.ChordWedgeRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.BOTTOM;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>WedgeInter</code> represents a wedge (crescendo or diminuendo).
 * <p>
 * <img alt="Wedge image"
 * src=
 * "http://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Music_hairpins.svg/296px-Music_hairpins.svg.png">
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "wedge")
public class WedgeInter
        extends AbstractDirectionInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(WedgeInter.class);

    /** Default thickness of a wedge line. */
    public static final double DEFAULT_THICKNESS = constants.defaultThickness.getValue();

    /** Location of handle below, with respect to wedge line length. */
    private static final double HANDLE_RATIO = constants.handleRatio.getValue();

    //~ Instance fields ----------------------------------------------------------------------------

    /** Top line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D l1;

    /** Bottom line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D l2;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private WedgeInter ()
    {
        super(null, null, null, 0.0);
    }

    /**
     * Creates a new <code>WedgeInter</code> object, meant for manual assignment.
     *
     * @param glyph underlying glyph, if any
     * @param shape CRESCENDO or DIMINUENDO
     * @param grade assigned grade
     */
    public WedgeInter (Glyph glyph,
                       Shape shape,
                       Double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);

        if (glyph != null) {
            setGlyph(glyph);
        }
    }

    /**
     * Creates a new WedgeInter object.
     *
     * @param l1      precise first line
     * @param l2      precise second line
     * @param bounds  bounding box
     * @param shape   CRESCENDO or DIMINUENDO
     * @param impacts assignments details
     */
    public WedgeInter (Line2D l1,
                       Line2D l2,
                       Rectangle bounds,
                       Shape shape,
                       GradeImpacts impacts)
    {
        super(null, bounds, shape, impacts);
        this.l1 = l1;
        this.l2 = l2;
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

        if (l1.ptLineDistSq(point) <= ((DEFAULT_THICKNESS * DEFAULT_THICKNESS) / 4)) {
            return true;
        }

        if (l2.ptLineDistSq(point) <= ((DEFAULT_THICKNESS * DEFAULT_THICKNESS) / 4)) {
            return true;
        }

        return false;
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point loc)
    {
        final TextLayout layout = font.layoutShapeByCode(shape);
        final Rectangle2D wr = layout.getBounds();
        final double halfWidth = wr.getWidth() / 2;
        final double halfHeight = wr.getHeight() / 2;

        if (shape == Shape.CRESCENDO) {
            l1 = new Line2D.Double(loc.x - halfWidth, loc.y, loc.x + halfWidth, loc.y - halfHeight);
            l2 = new Line2D.Double(loc.x - halfWidth, loc.y, loc.x + halfWidth, loc.y + halfHeight);
        } else {
            l1 = new Line2D.Double(loc.x - halfWidth, loc.y - halfHeight, loc.x + halfWidth, loc.y);
            l2 = new Line2D.Double(loc.x - halfWidth, loc.y + halfHeight, loc.x + halfWidth, loc.y);
        }

        setBounds(null);

        return true;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        Rectangle box = super.getBounds();

        if (box != null) {
            return new Rectangle(box);
        }

        box = l1.getBounds().union(l2.getBounds());
        box.grow(0, (int) Math.ceil(DEFAULT_THICKNESS / 2.0));

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

    //----------//
    // getLine1 //
    //----------//
    /**
     * Report wedge top line.
     *
     * @return top line
     */
    public Line2D getLine1 ()
    {
        return l1;
    }

    //----------//
    // getLine2 //
    //----------//
    /**
     * Report wedge bottom line.
     *
     * @return bottom line
     */
    public Line2D getLine2 ()
    {
        return l2;
    }

    //-----------//
    // getSpread //
    //-----------//
    /**
     * Report vertical gap between ending points on the provided horizontal side.
     *
     * @param side provided horizontal side
     * @return vertical gap in pixels
     */
    public double getSpread (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return l2.getY1() - l1.getY1();
        } else {
            return l2.getY2() - l1.getY2();
        }
    }

    //----------//
    // getChord //
    //----------//
    /**
     * Report the chord linked to this wedge on the provided horizontal side.
     *
     * @param side the provided side
     * @return the linked chord, if any, otherwise null
     */
    public AbstractChordInter getChord (HorizontalSide side)
    {
        if (sig != null) {
            for (Relation rel : sig.edgesOf(this)) {
                if (rel instanceof ChordWedgeRelation chordWedgeRelation) {
                    if (chordWedgeRelation.getSide() == side) {
                        return (AbstractChordInter) sig.getOppositeInter(this, rel);
                    }
                }
            }
        }

        return null;
    }

    //-----------------//
    //getChordRelation //
    //-----------------//
    /**
     * Report the relation to chord, if any, on the specified side.
     *
     * @param side the desired side
     * @return the relation found or null
     */
    public ChordWedgeRelation getChordRelation (HorizontalSide side)
    {
        for (Relation rel : sig.getRelations(this, ChordWedgeRelation.class)) {
            final ChordWedgeRelation cwRel = (ChordWedgeRelation) rel;

            if (cwRel.getSide() == side) {
                return cwRel;
            }
        }

        return null;
    }

    //--------//
    // lookup //
    //--------//
    /**
     * Look for a link to a suitable chord.
     *
     * @param system the containing system
     * @param hSide  the left or the right side of the wedge to consider
     * @param vSide  looking up or down from the wedge
     * @return the suitable link found, null otherwise
     */
    private Link lookup (SystemInfo system,
                         HorizontalSide hSide,
                         VerticalSide vSide)
    {
        final Line2D line = (vSide == TOP) ? l1 : l2;
        final Point2D end = (hSide == LEFT) ? line.getP1() : line.getP2();
        final Scale scale = system.getSheet().getScale();
        MeasureStack stack = system.getStackAt(end);

        if (stack == null) {
            // Perhaps a bit beyond staff limit abscissa?
            final double xMargin = scale.toPixels(constants.stackAbscissaMargin);
            final Point2D end2 = new Point2D.Double(
                    end.getX() + ((hSide == LEFT) ? xMargin : -xMargin),
                    end.getY());
            stack = system.getStackAt(end2);
        }

        if (stack != null) {
            // Lookup window for abscissa inclusion (and for unused ordinate exclusion)
            final Rectangle box = new Rectangle(
                    (int) Math.rint(end.getX()),
                    (int) Math.rint(end.getY()),
                    0,
                    0);
            box.grow(scale.toPixels(constants.maxXGap), 0);

            final AbstractChordInter chord = (vSide == TOP) //
                    ? stack.getStandardChordAbove(end, box)
                    : stack.getStandardChordBelow(end, box);

            if (chord != null) {
                return new Link(chord, new ChordWedgeRelation(hSide), false);
            }
        }

        return null;
    }

    //-------------//
    // searchLinks //
    //-------------//
    /**
     * We look for chords at left and right sides, both above or both below the wedge.
     * Note: Perhaps we should look for non-rest chords only.
     *
     * @param system the containing system
     * @return the links found, perhaps empty
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final TreeMap<HorizontalSide, Link> map = new TreeMap<>();

        if (staff != null) {
            // Look for chord within that staff only
            final Point wedgeCenter = getCenter();
            final double yStaff = staff.getMidLine().yAt(wedgeCenter.x);

            if (yStaff <= wedgeCenter.y) {
                // Look above
                for (HorizontalSide hSide : HorizontalSide.values()) {
                    map.put(hSide, lookup(system, hSide, TOP));
                }
            } else {
                // Look below
                for (HorizontalSide hSide : HorizontalSide.values()) {
                    map.put(hSide, lookup(system, hSide, BOTTOM));
                }
            }
        } else {
            // Look above
            for (HorizontalSide hSide : HorizontalSide.values()) {
                map.put(hSide, lookup(system, hSide, TOP));
            }

            if (map.get(LEFT) == null || map.get(RIGHT) == null) {
                map.clear();

                // Then look below if needed
                for (HorizontalSide hSide : HorizontalSide.values()) {
                    map.put(hSide, lookup(system, hSide, BOTTOM));
                }
            }
        }

        // Collect the links
        final List<Link> links = new ArrayList<>();

        if (map.get(LEFT) != null) {
            links.add(map.get(LEFT));
        }

        if (map.get(RIGHT) != null) {
            links.add(map.get(RIGHT));
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
        return searchObsoletelinks(links, ChordWedgeRelation.class);
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle b)
    {
        super.setBounds(b);

        if (b != null) {
            final double dy = DEFAULT_THICKNESS / 2.0;

            if (shape == Shape.CRESCENDO) {
                l1 = new Line2D.Double(b.x, b.y + b.height / 2.0, b.x + b.width, b.y + 0.5 + dy);
            } else {
                l1 = new Line2D.Double(b.x, b.y + 0.5 + dy, b.x + b.width, b.y + b.height / 2.0);
            }

            if (shape == Shape.CRESCENDO) {
                l2 = new Line2D.Double(
                        b.x,
                        b.y + b.height / 2.0,
                        b.x + b.width,
                        b.y + b.height - 0.5 - dy);
            } else {
                l2 = new Line2D.Double(
                        b.x,
                        b.y + b.height - 0.5 - dy,
                        b.x + b.width,
                        b.y + b.height / 2.0);
            }
        }
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if (glyph != null) {
            setBounds(glyph.getBounds());
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction stackAbscissaMargin = new Scale.Fraction(
                1.0,
                "Margin beyond stack abscissa limits");

        private final Scale.Fraction maxXGap = new Scale.Fraction(
                2.0,
                "Maximum horizontal gap between wedge side and related chord");

        private final Constant.Double defaultThickness = new Constant.Double(
                "pixels",
                3.0,
                "Default wedge line thickness");

        private final Constant.Ratio handleRatio = new Constant.Ratio(
                0.1,
                "Ratio of handle below with respect to line length");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a wedge.
     * <p>
     * For a wedge, there are 4 handles:
     * <ul>
     * <li>Left handle, moving in any direction (extending both wedge lines)
     * <li>Middle handle, moving the whole inter in any direction
     * <li>Right handle, moving in any direction (extending both wedge lines)
     * <li>Below handle, moving only vertically (opening both wedge lines)
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {
        private final List<Staff> staves;

        private final Model originalModel;

        private final Model model;

        // Handles
        private final Point2D mid1;

        private final Point2D middle;

        private final Point2D mid2;

        private final Point2D below;

        public Editor (final WedgeInter wedge)
        {
            super(wedge);

            staves = wedge.getSig().getSystem().getStaves();

            originalModel = new Model(wedge.getLine1(), wedge.getLine2(), wedge.getStaff());
            model = new Model(wedge.l1, wedge.l2, wedge.getStaff());

            mid1 = PointUtil.middle(model.top1, model.bot1);
            mid2 = PointUtil.middle(model.top2, model.bot2);
            middle = PointUtil.middle(mid1, mid2);
            below = getBelow(wedge.shape);

            // Global move: move all points
            handles.add(selectedHandle = new Handle(middle)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Data
                    model.translate(dx, dy);

                    // Handles
                    for (InterEditor.Handle handle : handles) {
                        PointUtil.add(handle.getPoint(), dx, dy);
                    }

                    // Within a staff?
                    for (Staff staff : staves) {
                        if (staff.contains(middle)) {
                            model.staff = staff;
                            break;
                        }
                    }

                    return true;
                }
            });

            // Left handle: move in any direction
            handles.add(new InterEditor.Handle(mid1)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Data
                    PointUtil.add(model.top1, dx, dy);
                    PointUtil.add(model.bot1, dx, dy);

                    // Handles
                    PointUtil.add(mid1, dx, dy);
                    PointUtil.add(middle, dx / 2.0, dy / 2.0);
                    below.setLocation(getBelow(wedge.shape));

                    return true;
                }
            });

            // Right handle: move in any direction
            handles.add(new InterEditor.Handle(mid2)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Data
                    PointUtil.add(model.top2, dx, dy);
                    PointUtil.add(model.bot2, dx, dy);

                    // Handles
                    PointUtil.add(mid2, dx, dy);
                    PointUtil.add(middle, dx / 2.0, dy / 2.0);
                    below.setLocation(getBelow(wedge.shape));

                    return true;
                }
            });

            // Below handle: move vertically only
            handles.add(new InterEditor.Handle(below)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    if (dy == 0) {
                        return false;
                    }

                    // Data
                    final Shape shape = wedge.shape;
                    final Point2D top = (shape == Shape.DIMINUENDO) ? model.top1 : model.top2;
                    final Point2D bot = (shape == Shape.DIMINUENDO) ? model.bot1 : model.bot2;
                    final double dyp = dy / (1 - HANDLE_RATIO);
                    PointUtil.add(top, 0, -dyp);
                    PointUtil.add(bot, 0, +dyp);

                    // Handle
                    PointUtil.add(below, 0, dy);

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            final Inter inter = getInter();
            final WedgeInter wedge = (WedgeInter) inter;
            wedge.l1.setLine(model.top1, model.top2);
            wedge.l2.setLine(model.bot1, model.bot2);
            wedge.setStaff(model.staff);

            inter.setBounds(null);
            super.doit(); // No more glyph
        }

        /**
         * Compute location of the "below handle" on bottom line.
         *
         * @param shape DIMINUENDO or CRESCENDO
         * @return location for the specific handle
         */
        private Point2D getBelow (Shape shape)
        {
            final Point2D p1 = (shape == Shape.DIMINUENDO) ? model.bot1 : model.bot2;
            final Point2D p2 = (shape == Shape.DIMINUENDO) ? model.bot2 : model.bot1;

            return new Point2D.Double(
                    p1.getX() + HANDLE_RATIO * (p2.getX() - p1.getX()),
                    p1.getY() + HANDLE_RATIO * (p2.getY() - p1.getY()));
        }

        @Override
        public void undo ()
        {
            final Inter inter = getInter();
            final WedgeInter wedge = (WedgeInter) object;

            wedge.l1.setLine(originalModel.top1, originalModel.top2);
            wedge.l2.setLine(originalModel.bot1, originalModel.bot2);
            wedge.setStaff(originalModel.staff);

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
        private static final String[] NAMES = new String[] { "s1", "s2", "closedDy", "openDy",
                "openBias", "width" };

        private static final double[] WEIGHTS = new double[] { 1, 1, 1, 1, 1, 1 };

        public Impacts (double s1,
                        double s2,
                        double closedDy,
                        double openDy,
                        double openBias,
                        double width)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, s1);
            setImpact(1, s2);
            setImpact(2, closedDy);
            setImpact(3, openDy);
            setImpact(4, openBias);
            setImpact(5, width);
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements ObjectUIModel
    {
        public final Point2D top1;

        public final Point2D top2;

        public final Point2D bot1;

        public final Point2D bot2;

        public Staff staff;

        public Model (Line2D l1,
                      Line2D l2,
                      Staff staff)
        {
            top1 = l1.getP1();
            top2 = l1.getP2();
            bot1 = l2.getP1();
            bot2 = l2.getP2();
            this.staff = staff;
        }

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(top1, dx, dy);
            PointUtil.add(top2, dx, dy);
            PointUtil.add(bot1, dx, dy);
            PointUtil.add(bot2, dx, dy);
        }
    }
}
