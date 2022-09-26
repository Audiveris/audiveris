//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 O c t a v e S h i f t I n t e r                                //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.ui.ObjectEditor.Handle;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.OctaveShiftChordRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OctaveShiftSymbol;
import static org.audiveris.omr.ui.symbol.OctaveShiftSymbol.DEFAULT_HOOK_LENGTH;
import static org.audiveris.omr.ui.symbol.OctaveShiftSymbol.DEFAULT_LINE_LENGTH;
import static org.audiveris.omr.ui.symbol.OctaveShiftSymbol.DEFAULT_THICKNESS;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;

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
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>OctaveShiftInter</code> represents an octave shift.
 * <p>
 * It appears as a horizontal sequence of:
 * <ol>
 * <li>A number value (8, 15 or 22),
 * <li>A horizontal dashed line,
 * <li>An optional vertical ending hook pointing up or down to the related staff.
 * </ol>
 * The location of this octave shift element relative to the related staff indicates its kind
 * (ALTA if above staff, BASSA if below staff).
 * <p>
 * Two instances of <code>OctaveShiftChordRelation</code> are expected to link this item
 * to the first and to the last embraced chords in related staff.
 *
 * @see OctaveShiftChordRelation
 * @see OctaveShiftSymbol
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "octave-shift")
public class OctaveShiftInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(OctaveShiftInter.class);

    //~ Enumerations -------------------------------------------------------------------------------
    public static enum Kind
    {
        ALTA, // Notes to be performed higher than written
        BASSA; // Notes to be performed lower than written
    }

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** The kind of this shift. */
    @XmlAttribute(name = "kind")
    private Kind kind;

    /** Horizontal line, defined from left to right. */
    @XmlElement(name = "line")
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D line;

    /** Optional hook end. */
    @XmlElement(name = "hook")
    @XmlJavaTypeAdapter(Jaxb.Point2DAdapter.class)
    private Point2D hookEnd;

    // Transient data
    //---------------
    //
    /** Width of number value symbol. */
    private double valueWidth;

    /** Height of number value symbol. */
    private double valueHeight;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>OctaveShiftInter</code> object.
     *
     * @param glyph underlying glyph if any
     * @param shape shift shape (OTTAVA, QUINDICESIMA or VENTIDUESIMA)
     * @param kind  shift kind (ALTA or BASSA)
     * @param grade quality
     */
    public OctaveShiftInter (Glyph glyph,
                             Shape shape,
                             Kind kind,
                             Double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
        this.kind = kind;
    }

    /**
     * Creates a new <code>OctaveShiftInter</code> object, meant for manual use.
     *
     * @param shape shift shape
     * @param grade quality
     */
    public OctaveShiftInter (Shape shape,
                             Double grade)
    {
        this(null, shape, null, grade);
    }

    public OctaveShiftInter (Shape shape,
                             Kind kind,
                             Line2D line,
                             Point2D hookEnd)
    {
        this.kind = kind;
        this.line = line;
        this.hookEnd = hookEnd;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private OctaveShiftInter ()
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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No chord linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if octave shift  is connected on its left side
        setAbnormal(getChord(LEFT) == null);

        return isAbnormal();
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

        // Check value box
        if (getValueBox().contains(point)) {
            return true;
        }

        // Check ottava line
        if (line.ptLineDistSq(point) <= DEFAULT_THICKNESS * DEFAULT_THICKNESS) {
            return true;
        }

        if (hookEnd != null) {
            // Check ottava ending hook
            final Line2D hook = new Line2D.Double(line.getP2(), hookEnd);

            if (hook.ptLineDistSq(point) <= DEFAULT_THICKNESS * DEFAULT_THICKNESS) {
                return true;
            }
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
                               Point dropLocation)
    {
        final OctaveShiftSymbol shiftSymbol = (OctaveShiftSymbol) symbol;
        final Model model = shiftSymbol.getModel(font, dropLocation);

        if (line == null) {
            final TextLayout layout = font.layout(symbol.getString());
            final Rectangle2D symBounds = layout.getBounds();
            valueWidth = symBounds.getWidth();
            valueHeight = symBounds.getHeight();
        }

        line = new Line2D.Double(model.valueCenter, model.lineRight);
        hookEnd = new Point2D.Double(model.hookEnd.getX(), model.hookEnd.getY());

        if (staff != null) {
            determineKindAndHook(line, staff);
        }

        setBounds(null); // To reset cached value

        return true;
    }

    //----------------------//
    // determineKindAndHook //
    //----------------------//
    private void determineKindAndHook (Line2D line,
                                       Staff staff)
    {
        // Kind and hook orientation depend on vertical position relative to the staff
        final double x = line.getX1();

        if ((x >= staff.getAbscissa(HorizontalSide.LEFT)
                     && (x <= staff.getAbscissa(HorizontalSide.RIGHT)))) {
            // Vertical direction from ottava sign to staff
            final double y = line.getY1();
            final int toStaff = Integer.signum((int) (staff.getMidLine().yAt(y) - y));
            kind = (toStaff > 0) ? Kind.ALTA : Kind.BASSA;

            final Scale scale = staff.getSystem().getSheet().getScale();
            final int hookLg = scale.toPixels(DEFAULT_HOOK_LENGTH);
            hookEnd = new Point2D.Double(line.getX2(), line.getY2() + toStaff * hookLg);
        } else {
            hookEnd = null;
        }
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        final Rectangle box = line.getBounds();

        if (hookEnd != null) {
            box.add(hookEnd);
        }

        box.add(getValueBox());

        return new Rectangle(bounds = box);
    }

    //----------//
    // getChord //
    //----------//
    /**
     * Report the chord, if any, embraced on the specified side.
     *
     * @param side the desired side
     * @return the connected chord on this side, if any
     */
    public AbstractChordInter getChord (HorizontalSide side)
    {
        final OctaveShiftChordRelation rel = getChordRelation(side);

        if (rel != null) {
            return (AbstractChordInter) sig.getOppositeInter(this, rel);
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
    public OctaveShiftChordRelation getChordRelation (HorizontalSide side)
    {
        for (Relation rel : sig.getRelations(this, OctaveShiftChordRelation.class)) {
            OctaveShiftChordRelation oscRel = (OctaveShiftChordRelation) rel;

            if (oscRel.getSide() == side) {
                return oscRel;
            }
        }

        return null;
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (kind != null) {
            sb.append(" ").append(kind);
        }

        return sb.toString();
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //-------------//
    // getHookCopy //
    //-------------//
    /**
     * Report a COPY of the hook end.
     *
     * @return COPY of hook end
     */
    public Point2D getHookCopy ()
    {
        return hookEnd != null ? new Point2D.Double(hookEnd.getX(), hookEnd.getY()) : null;
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if ((line == null) && (glyph != null) && (bounds != null)) {
            // Compute a not too bad line chunk
            final Sheet sheet = StubsController.getInstance().getSelectedStub().getSheet();
            final Scale scale = sheet.getScale();
            final MusicFont font = MusicFont.getBaseFont(scale.getInterline());
            final ShapeSymbol symbol = Symbols.getSymbol(shape, false);
            final TextLayout layout = font.layout(symbol.getString());
            final Rectangle2D symBounds = layout.getBounds();
            valueWidth = symBounds.getWidth();
            valueHeight = symBounds.getHeight();
            final Point p1 = getCenterLeft();
            p1.x += (int) Math.rint(valueWidth / 2.0);
            final int lg = scale.toPixels(DEFAULT_LINE_LENGTH);
            final Point2D p2 = new Point2D.Double(p1.x + lg, p1.y);
            line = new Line2D.Double(p1, p2);
        }
    }

    //-------------//
    // setHookCopy //
    //-------------//
    /**
     * Assign a COPY to the hook end.
     *
     * @param hookEnd the new hook end, perhaps null
     */
    public void setHookCopy (Point2D hookEnd)
    {
        this.hookEnd = hookEnd != null ? new Point2D.Double(hookEnd.getX(), hookEnd.getY()) : null;
    }

    //---------//
    // getKind //
    //---------//
    public Kind getKind ()
    {
        return kind;
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Report the octave shift horizontal line.
     *
     * @return the (live) line
     */
    public Line2D getLine ()
    {
        return line;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter ()
    {
        return line.getP1();
    }

    //----------//
    // getShift //
    //----------//
    /**
     * Report the effective shift, stated in an algebraic number of octaves.
     *
     * @return +/- 1,2,3
     */
    public Integer getShift ()
    {
        final int sign = (kind == Kind.ALTA) ? 1 : -1;

        return switch (shape) {
            case OTTAVA ->
                sign * 1;
            case QUINDICESIMA ->
                sign * 2;
            case VENTIDUESIMA ->
                sign * 3;
            default ->
                null; // Should not occur!
        };
    }

    //----------//
    // getValue //
    //----------//
    public Integer getValue ()
    {
        return switch (shape) {
            case OTTAVA ->
                8;
            case QUINDICESIMA ->
                15;
            case VENTIDUESIMA ->
                22;
            default ->
                null; // Should not occur!
        };
    }

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create an instance of OctaveShiftInter.
     *
     * @param glyph        the underlying glyph (just the value part, with no dash part)
     * @param shape        OTTAVA, QUINDICESIMA or VENTIDUESIMA
     * @param grade        interpretation quality
     * @param closestStaff closest staff, which may not be the right one!
     * @return the created instance or null
     */
    public static Inter create (Glyph glyph,
                                Shape shape,
                                double grade,
                                Staff closestStaff)
    {
        // TODO: Search a more suitable staff than just the vertically closest one
        final Staff staff = closestStaff;
        //
        // Item cannot be located within staff height
        final Point center = glyph.getCenter();

        if (staff.distanceTo(center) <= 0) {
            return null;
        }

        //TODO: Can we check for presence of a long dashed line???
        //
        final OctaveShiftInter os = new OctaveShiftInter(glyph, shape, null, grade);
        final Scale scale = staff.getSystem().getSheet().getScale();
        final int lineLg = scale.toPixels(DEFAULT_LINE_LENGTH);
        os.line = new Line2D.Double(center, new Point(center.x + lineLg, center.y));
        os.setStaff(staff);

        return os;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        if (staff == null) {
            return Collections.emptyList();
        }

        final List<AbstractChordInter> staffChords = new ArrayList<>();

        for (Inter inter : system.getSig().inters(AbstractChordInter.class)) {
            final AbstractChordInter chord = (AbstractChordInter) inter;

            // TODO: Should we discard SmallChordInter instances?
            if (chord.getStaves().contains(staff)) {
                final Point center = chord.getCenter();

                if (center.x >= line.getX1() && center.x <= line.getX2()) {
                    staffChords.add(chord);
                }
            }
        }

        Collections.sort(staffChords, Inters.byAbscissa);

        return lookupLinks(staffChords);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, OctaveShiftChordRelation.class);
    }

    //----------//
    // setStaff //
    //----------//
    @Override
    public void setStaff (Staff staff)
    {
        super.setStaff(staff);

        // Kind?
        if ((kind == null) && (staff != null) && (bounds != null) && (shape != null)) {
            determineKindAndHook(line, staff);
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + kind;
    }

    //-------------//
    // getValueBox //
    //-------------//
    private Rectangle2D getValueBox ()
    {
        return new Rectangle2D.Double(line.getX1() - valueWidth / 2.0,
                                      line.getY1() - valueHeight / 2.0,
                                      valueWidth,
                                      valueHeight);
    }

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Try to detect link between this OctaveShift instance and first chord on left side
     * plus last chord on right side in the related staff.
     *
     * @param staffChords ordered collection of chords in related staff
     * @return the pair of links found, perhaps empty
     */
    private List<Link> lookupLinks (List<AbstractChordInter> staffChords)
    {
        if (staffChords.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Link> links = new ArrayList<>();
        final int last = staffChords.size() - 1;
        links.add(new Link(staffChords.get(0), new OctaveShiftChordRelation(LEFT), true));
        links.add(new Link(staffChords.get(last), new OctaveShiftChordRelation(RIGHT), true));

        return links;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Model //
    //-------//
    public static class Model
            implements ObjectUIModel
    {

        // OTTAVA, QUINDICESIMA or VENTIDUESIMA
        public Shape shape;

        // ALTA or BASSA
        public Kind kind;

        // Area center for the 8/15/22 symbol
        public Point2D valueCenter;

        // Right ending point of ottava line
        public Point2D lineRight;

        // (Optional) Hook ending point (above or below lineRight)
        public Point2D hookEnd;

        public Model (Shape shape,
                      Kind kind,
                      Point2D valueCenter,
                      Point2D lineRight,
                      Point2D hookEnd)
        {
            this.shape = shape;
            this.kind = kind;
            this.valueCenter = valueCenter;
            this.lineRight = lineRight;
            this.hookEnd = hookEnd;
        }

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(valueCenter, dx, dy);
            PointUtil.add(lineRight, dx, dy);

            if (hookEnd != null) {
                PointUtil.add(hookEnd, dx, dy);
            }
        }
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for an octave shift.
     * <p>
     * There are 3 handles:
     * <ul>
     * <li>Left handle on valueCenter, moving this point in any direction (extending line)
     * <li>Middle handle, moving the whole inter in any direction
     * <li>Right handle on lineRight, moving this point in any direction (extending line)
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {

        private final Model originalModel;

        private final Model model;

        // For middle line handle
        private final Point2D middle;

        public Editor (final OctaveShiftInter os)
        {
            super(os);

            originalModel = new Model(os.shape, os.kind, os.line.getP1(), os.line.getP2(),
                                      os.getHookCopy());
            model = new Model(os.shape, os.kind, os.line.getP1(), os.line.getP2(),
                              os.getHookCopy());

            middle = PointUtil.middle(os.line);

            // Global move: translate all points
            handles.add(selectedHandle = new Handle(middle)
            {
                @Override
                public boolean move (Point vector)
                {
                    // Data
                    model.translate(vector.x, vector.y);

                    // Middle handle
                    PointUtil.add(middle, vector);

                    return true;
                }
            });

            // Left handle: move valueCenter in any direction
            handles.add(new InterEditor.Handle(model.valueCenter)
            {
                @Override
                public boolean move (Point vector)
                {
                    // Handles
                    PointUtil.add(model.valueCenter, vector);
                    PointUtil.add(middle, vector.x / 2.0, vector.y / 2.0);

                    return true;
                }
            });

            // Right handle: move lineRight (and hookEnd if any) in any direction
            handles.add(new InterEditor.Handle(model.lineRight)
            {
                @Override
                public boolean move (Point vector)
                {
                    // Data
                    if (model.hookEnd != null) {
                        PointUtil.add(model.hookEnd, vector);
                    }

                    // Handles
                    PointUtil.add(model.lineRight, vector);
                    PointUtil.add(middle, vector.x / 2.0, vector.y / 2.0);

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            final OctaveShiftInter os = (OctaveShiftInter) getInter();
            os.line.setLine(model.valueCenter, model.lineRight);
            os.setHookCopy(model.hookEnd);

            os.setBounds(null);
            super.doit(); // No more glyph, hence the following hack to keep value as glyph
            final Inter inter = (Inter) object;
            inter.setGlyph(originalGlyph);
        }

        @Override
        public void undo ()
        {
            final OctaveShiftInter os = (OctaveShiftInter) getInter();
            os.line.setLine(originalModel.valueCenter, originalModel.lineRight);
            os.setHookCopy(originalModel.hookEnd);

            os.setBounds(null);
            super.undo();
        }
    }
}
