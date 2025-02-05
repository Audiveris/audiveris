//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 O c t a v e S h i f t I n t e r                                //
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

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.ui.ObjectEditor.Handle;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.OctaveShiftChordRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.EditingTask;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterService;
import org.audiveris.omr.sig.ui.RemovalTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.symbol.FontSymbol;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OctaveShiftSymbol;
import static org.audiveris.omr.ui.symbol.OctaveShiftSymbol.DEFAULT_HOOK_LENGTH;
import static org.audiveris.omr.ui.symbol.OctaveShiftSymbol.DEFAULT_LINE_LENGTH;
import static org.audiveris.omr.ui.symbol.OctaveShiftSymbol.DEFAULT_THICKNESS;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Jaxb;
import static org.audiveris.omr.util.VerticalSide.*;
import org.audiveris.omr.util.WrappedBoolean;

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
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.SwingUtilities;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>OctaveShiftInter</code> represents a (physical) octave shift.
 * <p>
 * It appears as a horizontal sequence of:
 * <ol>
 * <li>A number value (8, 15 or 22),
 * <li>A horizontal dashed line,
 * <li>An optional vertical ending hook pointing up or down to the related staff.
 * </ol>
 * <img src="doc-files/OctaveShiftInter.png" alt="octave shift inter">
 * <p>
 * The vertical location of this octave shift element with respect to the related staff indicates
 * its kind (ALTA if above staff, BASSA if below staff).
 * <p>
 * Two instances of <code>OctaveShiftChordRelation</code> are expected to link this item
 * to the first and to the last embraced chords in related staff.
 * <p>
 * Since an octave shift can logically continue on succeeding stave(s), such "logical" octave shift
 * is implemented as a sequence of several "physical" OctaveShiftInter instances, one in each system
 * involved, connected via left and right extensions.
 * <br>
 * This required a specific {@link Editor} class.
 *
 * @see OctaveShiftChordRelation
 * @see OctaveShiftSymbol
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "octave-shift")
public class OctaveShiftInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(OctaveShiftInter.class);

    /** For comparing interpretations by increasing staff id. */
    public static final Comparator<Inter> byStaffId = (Inter i1,
                                                       Inter i2) -> Integer.compare(
                                                               i1.getStaff().getId(),
                                                               i2.getStaff().getId());

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** The kind of this shift. */
    @XmlAttribute(name = "kind")
    private Kind kind;

    /** Horizontal line defined from value center to right end. */
    @XmlElement(name = "line")
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    private Line2D line;

    /** Optional hook end. */
    @XmlElement(name = "hook")
    @XmlJavaTypeAdapter(Jaxb.Point2DAdapter.class)
    private Point2D hookEnd;

    /** Extension OctaveShift on left, if any (within the same sheet). */
    @XmlIDREF
    @XmlAttribute(name = "left-extension")
    private OctaveShiftInter leftExtension;

    /** Extension OctaveShift on right, if any (within the same sheet). */
    @XmlIDREF
    @XmlAttribute(name = "right-extension")
    private OctaveShiftInter rightExtension;

    // Transient data
    //---------------

    /** Width of number value symbol. */
    private double valueWidth;

    /** Height of number value symbol. */
    private double valueHeight;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed by JAXB.
     */
    @SuppressWarnings("unchecked")
    private OctaveShiftInter ()
    {
    }

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

    /**
     * Creates a new <code>OctaveShiftInter</code> object, with all geometry parameters.
     *
     * @param shape   shift shape (OTTAVA, QUINDICESIMA or VENTIDUESIMA)
     * @param kind    shift kind (ALTA or BASSA)
     * @param line    shift horizontal line
     * @param hookEnd optional hook end point, perhaps null
     */
    protected OctaveShiftInter (Shape shape,
                                Kind kind,
                                Line2D line,
                                Point2D hookEnd)
    {
        super(null, null, shape, 1.0);
        this.kind = kind;
        this.line = line;
        this.hookEnd = hookEnd;
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

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     *
     * @param system the containing system
     */
    public void afterReload (SystemInfo system)
    {
        try {
            computeValueDimensions(system.getSheet());
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
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

    //------------------------//
    // computeValueDimensions //
    //------------------------//
    /**
     * Determine dimensions of the value box, according to OctaveShiftInter shape
     * and to sheet scale.
     *
     * @param sheet the containing sheet
     */
    private void computeValueDimensions (Sheet sheet)
    {
        final Scale scale = sheet.getScale();
        final MusicFamily family = sheet.getStub().getMusicFamily();
        final FontSymbol fs = shape.getFontSymbolByInterline(family, scale.getInterline());
        final TextLayout layout = fs.getLayout();
        final Rectangle2D symBounds = layout.getBounds();
        valueWidth = symBounds.getWidth();
        valueHeight = symBounds.getHeight();
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
        final OctaveShiftSymbol octaveShiftSymbol = (OctaveShiftSymbol) symbol;
        final Model model = octaveShiftSymbol.getModel(font, dropLocation);

        if (line == null) {
            final TextLayout layout = font.layoutShapeByCode(octaveShiftSymbol.getShape());
            final Rectangle2D symBounds = layout.getBounds();
            valueWidth = symBounds.getWidth();
            valueHeight = symBounds.getHeight();
        }

        line = new Line2D.Double(model.p1, model.p2);
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
    /**
     * Set OctaveShiftInter kind and hookEnd, depending on position relative to the staff.
     *
     * @param line  instance line
     * @param staff related staff
     */
    private void determineKindAndHook (Line2D line,
                                       Staff staff)
    {
        final double x = line.getX1();

        if ((x >= staff.getAbscissa(HorizontalSide.LEFT) && (x <= staff.getAbscissa(
                HorizontalSide.RIGHT)))) {
            // Vertical direction from ottava sign to staff
            final double y = line.getY1();
            final int toStaff = Integer.signum((int) (staff.getMidLine().yAt(y) - y));
            kind = (toStaff > 0) ? Kind.ALTA : Kind.BASSA;
            hookEnd = new Point2D.Double(line.getX2(), line.getY2() + toStaff * getHookLg());
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

    //------------------//
    // getChordRelation //
    //------------------//
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
        final StringBuilder sb = new StringBuilder(super.getDetails());

        for (HorizontalSide side : HorizontalSide.values()) {
            final OctaveShiftInter ext = getExtension(side);

            if (ext != null) {
                sb.append((sb.length() != 0) ? " " : "");
                sb.append(side).append("-extension: #").append(ext.getId());
            }
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

    //--------------//
    // getExtension //
    //--------------//
    /**
     * Report extension if any on the provided side.
     *
     * @param side LEFT or RIGHT
     * @return the extension, perhaps null
     */
    public OctaveShiftInter getExtension (HorizontalSide side)
    {
        return switch (side) {
            case null -> throw new NullPointerException(
                    "No side provided for OctaveShift getExtension");
            case LEFT -> leftExtension;
            case RIGHT -> rightExtension;
        };
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
        return (hookEnd != null) ? new Point2D.Double(hookEnd.getX(), hookEnd.getY()) : null;
    }

    //-----------//
    // getHookLg //
    //-----------//
    /**
     * Report the length of an ending hook.
     *
     * @return hook length in pixels
     */
    private int getHookLg ()
    {
        final Scale scale = staff.getSystem().getSheet().getScale();
        return scale.toPixels(DEFAULT_HOOK_LENGTH);
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

    //-------------------//
    // getRelationCenter //
    //-------------------//
    /**
     * We use a different relation center, depending upon the octave shift provided side.
     *
     * @param relation the relation instance
     * @return left or right end of line
     */
    @Override
    public Point2D getRelationCenter (Relation relation)
    {
        if (relation instanceof OctaveShiftChordRelation osr) {
            return switch (osr.getSide()) {
                case LEFT -> line.getP1();
                case RIGHT -> line.getP2();
            };
        } else {
            return getRelationCenter();
        }
    }

    //-------------//
    // getSequence //
    //-------------//
    /**
     * Build the whole sequence of (physical) OctaveShiftInter instances that compose
     * a (logical) octave shift.
     * <p>
     * This is done by using the existing leftExtension and rightExtension pointers.
     *
     * @return the sequence of OctaveShiftInter's
     */
    private List<OctaveShiftInter> getSequence ()
    {
        final List<OctaveShiftInter> list = new ArrayList<>();
        list.add(this);

        for (HorizontalSide side : HorizontalSide.values()) {
            OctaveShiftInter other = getExtension(side);

            while (other != null) {
                if (side == LEFT) {
                    list.add(0, other);
                } else {
                    list.add(other);
                }

                other = other.getExtension(side);
            }
        }

        return list;
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
            case OTTAVA -> sign * 1;
            case QUINDICESIMA -> sign * 2;
            case VENTIDUESIMA -> sign * 3;
            default -> null; // Should not occur!
        };
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the number that appears in value box.
     *
     * @return numeric value
     */
    public Integer getValue ()
    {
        return switch (shape) {
            case OTTAVA -> 8;
            case QUINDICESIMA -> 15;
            case VENTIDUESIMA -> 22;
            default -> null; // Should not occur!
        };
    }

    //-------------//
    // getValueBox //
    //-------------//
    /**
     * Report the bounding box around the value symbol of the OctaveShiftInter at hand.
     *
     * @return bounds of the 8/15/22 number
     */
    private Rectangle2D getValueBox ()
    {
        return new Rectangle2D.Double(
                line.getX1() - valueWidth / 2.0,
                line.getY1() - valueHeight / 2.0,
                valueWidth,
                valueHeight);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        final StringBuilder sb = new StringBuilder(super.internals());

        if (kind != null) {
            sb.append(" ").append(kind);
        }

        return sb.toString();
    }

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Try to detect the links between this OctaveShift instance and first chord on left side
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

    //---------//
    // preEdit //
    //---------//
    @Override
    public List<? extends UITask> preEdit (InterEditor interEditor)
    {
        final Editor editor = (Editor) interEditor;
        final List<UITask> tasks = new ArrayList<>();

        // Editings
        final Set<OctaveShiftInter> editedKeys = new LinkedHashSet<>(editor.models.keySet());
        editedKeys.retainAll(editor.originalSeq);
        for (OctaveShiftInter os : editedKeys) {
            final SystemInfo system = os.staff.getSystem();
            final Collection<Link> links = os.searchLinks(system);
            final Collection<Link> unlinks = os.searchUnlinks(system, links);
            tasks.add(new EditingTask(editor.getMiniEditor(os), links, unlinks));
        }

        // Removals
        final Set<OctaveShiftInter> removedKeys = new LinkedHashSet<>(editor.originalSeq);
        removedKeys.removeAll(editor.models.keySet());
        for (OctaveShiftInter os : removedKeys) {
            tasks.add(new RemovalTask(os, editor.links.get(os)));
            editor.links.put(os, null); // It's safer to clean this immediately after use!
        }

        // Additions
        final Set<OctaveShiftInter> addedKeys = new LinkedHashSet<>(editor.models.keySet());
        addedKeys.removeAll(editor.originalSeq);
        for (OctaveShiftInter os : addedKeys) {
            final SystemInfo system = os.staff.getSystem();
            final Collection<Link> links = os.searchLinks(system);
            tasks.add(new AdditionTask(system.getSig(), os, os.getBounds(), links));
        }

        return tasks;
    }

    //-----------//
    // preRemove //
    //-----------//
    /**
     * Removing this (physical) octave shift means removing the whole (logical) sequence.
     *
     * @param cancel not used
     * @return the whole sequence
     */
    @Override
    public Set<? extends Inter> preRemove (WrappedBoolean cancel)
    {
        return new LinkedHashSet<>(getSequence());
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

        final List<AbstractChordInter> staffChords = staff.getChords();
        for (Iterator<AbstractChordInter> it = staffChords.iterator(); it.hasNext();) {
            final AbstractChordInter chord = it.next();
            final Point center = chord.getCenter();

            if (center.x < line.getX1() || center.x > line.getX2()) {
                it.remove();
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

    //--------------//
    // setExtension //
    //--------------//
    /**
     * Set extension on the provided side.
     *
     * @param side  LEFT or RIGHT
     * @param other extension, perhaps null
     */
    public void setExtension (HorizontalSide side,
                              OctaveShiftInter other)
    {
        switch (side) {
            case null -> throw new NullPointerException(
                    "No side provided for OctaveShift setExtension");
            case LEFT -> leftExtension = other;
            case RIGHT -> rightExtension = other;
        }
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
            computeValueDimensions(sheet);

            final Point p1 = getCenterLeft();
            p1.x += (int) Math.rint(valueWidth / 2.0);
            final int lg = sheet.getScale().toPixels(DEFAULT_LINE_LENGTH);
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

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create an instance of OctaveShiftInter.
     * <p>
     * TODO: this is a VERY PRELIMINARY implementation!
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

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction minGapFromStaff = new Scale.Fraction(
                2.0,
                "Minimum vertical gap from upper or lower staff");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for an octave shift.
     * <p>
     * A logical octave shift can span several systems, starting in one system and stopping
     * in another system.
     * <p>
     * User editing of such logical octave shift includes the ability to extend or shrink the
     * logical octave shift, which implies to work on several physical instances.
     * <p>
     * The chosen implementation for this "possibly-multi-system" editor is to work on the whole
     * vertical sequence of physical instances.
     * This sequence can dynamically grow or shrink according to user actions.
     * Note that this behavior departs from the general design of InterEditor class working on
     * a single dedicated Inter instance.
     * <p>
     * Each physical OctaveShiftInter instance has a triplet of editing handles:
     * <ul>
     * <li>{@link LeftHandle}
     * <li>{@link MiddleHandle}
     * <li>{@link RightHandle}
     * </ul>
     * The triplet always contains a middle handle.
     * But a left handle and/or a right handle can be dynamically allocated or removed, according to
     * the current position (and thus role) of the physical instance within the vertical sequence.
     */
    private static class Editor
            extends InterEditor
    {
        private final List<OctaveShiftInter> seq; // Current sequence of physical shifts

        private final List<OctaveShiftInter> originalSeq;// Original sequence of physical shifts

        private final Map<OctaveShiftInter, Model> models = new TreeMap<>(byStaffId);

        private final Map<OctaveShiftInter, Model> originalModels = new TreeMap<>(byStaffId);

        private final Map<OctaveShiftInter, Triplet> triplets = new TreeMap<>(byStaffId);

        private final Map<OctaveShiftInter, Collection<Link>> links = new TreeMap<>(byStaffId);

        private final Sheet sheet;

        private final Scale scale;

        private final StaffManager stfMgr;

        private final InterService interService;

        public Editor (OctaveShiftInter os)
        {
            super(os);

            if (os.getGlyph() != null) {
                originalGlyph = os.getGlyph();
            }

            sheet = os.getStaff().getSystem().getSheet();
            scale = sheet.getScale();
            stfMgr = sheet.getStaffManager();
            interService = (InterService) sheet.getInterIndex().getEntityService();

            seq = os.getSequence();
            originalSeq = new ArrayList<>(seq);

            // Initial handles creation
            for (OctaveShiftInter osi : seq) {
                createHandles(osi);
            }

            // Initial handle selection
            final Triplet triplet = triplets.get(os);
            selectedHandle = (triplet.right != null) ? triplet.right
                    : (triplet.left != null) ? triplet.left : triplet.middle;
        }

        /**
         * Report a perhaps modified dy value, so that OctaveShift remains vertically in the
         * gutter between its current upper and lower staves.
         *
         * @param dy the user vertical translation
         * @param os the underlying OctaveShiftInter
         * @return the adjusted dy value
         */
        private int adjustDy (int dy,
                              OctaveShiftInter os)
        {
            final Staff staff = os.getStaff();
            final Point middle = PointUtil.rounded(PointUtil.middle(os.getLine()));
            final int mx = middle.x; // Middle point abscissa
            final int targetMy = middle.y + dy; // Middle point target ordinate
            final int minYGap = scale.toPixels(constants.minGapFromStaff);
            int yGap;

            switch (os.getKind()) {
                case ALTA -> {
                    // Stay above current staff
                    yGap = (staff.getFirstLine().yAt(mx) - minYGap) - targetMy;
                    if (yGap < 0) {
                        dy += yGap;
                    } else {
                        // Stay below upper staff if any
                        final Staff upper = getLimitingStaff(stfMgr.vertNeighbors(staff, TOP), os);
                        if (upper != null) {
                            yGap = targetMy - (upper.getLastLine().yAt(mx) + minYGap);
                            if (yGap < 0) {
                                dy -= yGap;
                            }
                        } else {
                            // Stay below top of sheet
                            if (targetMy < 0) {
                                dy -= targetMy;
                            }
                        }
                    }
                }
                case BASSA -> {
                    // Stay below current staff
                    yGap = targetMy - (staff.getLastLine().yAt(mx) + minYGap);
                    if (yGap < 0) {
                        dy -= yGap;
                    } else {
                        // Stay above lower staff if any
                        final Staff lower = getLimitingStaff(
                                stfMgr.vertNeighbors(staff, BOTTOM),
                                os);
                        if (lower != null) {
                            yGap = (lower.getFirstLine().yAt(mx) - minYGap) - targetMy;
                            if (yGap < 0) {
                                dy += yGap;
                            }
                        } else {
                            // Stay above bottom of sheet
                            yGap = sheet.getPicture().getHeight() - targetMy;
                            if (yGap < 0) {
                                dy += yGap;
                            }
                        }
                    }
                }
            }

            return dy;
        }

        /**
         * Report whether the provided Inter is concerned by this editor.
         * <p>
         * For this "multi-inter" editor we consider the whole sequence of inters.
         *
         * @param inter provided inter
         * @return true if provided inter is involved in 'seq'
         */
        @Override
        public boolean concerns (Inter inter)
        {
            return seq.contains(inter);
        }

        /**
         * Create the triplet of OctaveShift handles, according to instance position within the
         * global sequence.
         *
         * @param os the OctaveShiftInter at hand
         */
        private void createHandles (OctaveShiftInter os)
        {
            final int idx = seq.indexOf(os);
            final Triplet triplet = new Triplet();
            triplets.put(os, triplet);
            models.put(os, new Model(os));
            originalModels.put(os, new Model(os));

            // Left handle on top line only
            if (idx == 0) {
                handles.add(triplet.left = new LeftHandle(os, models.get(os).p1));
            }

            // Middle handle on every line
            handles.add(triplet.middle = new MiddleHandle(os, PointUtil.middle(os.getLine())));

            // Right handle on bottom line only
            if (idx == seq.size() - 1) {
                handles.add(triplet.right = new RightHandle(os, models.get(os).p2));
            }
        }

        @Override
        protected void doit ()
        {
            for (OctaveShiftInter os : seq) {
                final Model model = models.get(os);
                os.line.setLine(model.p1, model.p2);
                os.setHookCopy(model.hookEnd);
                os.setBounds(null);
            }
        }

        /**
         * Report the staff, if any, that would represent the vertical limit for octave shift.
         *
         * @param others provided list of vertical staff neighbors, perhaps empty
         * @param os     the underlying OctaveShiftInter
         * @return the limiting staff found or null
         */
        private Staff getLimitingStaff (List<Staff> others,
                                        OctaveShiftInter os)
        {
            final Point middle = PointUtil.rounded(PointUtil.middle(os.getLine()));
            final int mx = (int) middle.getX(); // Middle point abscissa
            Staff best = null;
            int bestDx = Integer.MAX_VALUE;

            for (Staff s : others) {
                // Choose the abscissawise closest staff
                final int sLeft = s.getAbscissa(LEFT);
                final int sRight = s.getAbscissa(RIGHT);

                if ((mx >= sLeft) && (mx <= sRight)) {
                    return s;
                }

                final int dx = Math.min(Math.abs(sLeft - mx), Math.abs(sRight - mx));
                if (bestDx > dx) {
                    bestDx = dx;
                    best = s;
                }
            }

            return best;
        }

        private InterEditor getMiniEditor (OctaveShiftInter os)
        {
            return new MiniEditor(os);
        }

        /**
         * Specific handle management that does not forbid to drag beyond system limits.
         *
         * @param dx user location translation in abscissa
         * @param dy user location translation in ordinate
         */
        @Override
        protected void moveHandle (int dx,
                                   int dy)
        {
            if (dx == 0 && dy == 0) {
                return;
            }

            if (selectedHandle.move(dx, dy)) {
                hasMoved = true;
                lastPt = new Point(lastPt.x + dx, lastPt.y + dy);
                doit();
            }
        }

        @Override
        public void publish ()
        {
            if (selectedHandle != null) {
                final Inter inter = ((OsHandle) selectedHandle).os; // The current os
                tracker.setInter(inter); // This triggers the dynamic display of os links
                tracker.setSystem(inter.getStaff().getSystem());
            }
        }

        /**
         * Publish the sequence of OctaveShiftInter instances.
         */
        private void publishSequence ()
        {
            // To make tracker focus on selected OctaveShiftInter
            publish();

            // To make all OctaveShiftInter in 'seq' displayed,
            // even those not yet registered in a SIG
            SwingUtilities.invokeLater(
                    () -> interService.publish(
                            new EntityListEvent<>(
                                    this,
                                    SelectionHint.ENTITY_TRANSIENT,
                                    MouseMovement.DRAGGING,
                                    seq)));
        }

        /**
         * Remove the provided OctaveShiftInter instance, with its model and handle triplet.
         *
         * @param os the instance to remove
         */
        private void removeOctaveShift (OctaveShiftInter os)
        {
            seq.remove(os);
            models.remove(os);

            final Triplet triplet = triplets.get(os);
            triplets.remove(os);

            if (triplet.left != null) {
                handles.remove(triplet.left);
            }

            if (triplet.middle != null) {
                handles.remove(triplet.middle);
            }

            if (triplet.right != null) {
                handles.remove(triplet.right);
            }

            if (os.getSig() != null) {
                // We store links now, before os get removed from sig
                links.put(os, os.getLinks());
            }
            os.remove();
        }

        /**
         * Shrink the provided OctaveShiftInter at current abscissa.
         *
         * @param os octave shift being shrunk
         * @param x  current end abscissa
         */
        private void shrinkOctaveShift (OctaveShiftInter os,
                                        int x)
        {
            final Model model = models.get(os);
            final Triplet triplet = triplets.get(os);
            model.p2.setLocation(x, model.p2.getY());
            model.hookEnd = new Point2D.Double(x, model.p2.getY() + os.getHookLg());
            triplet.middle.getPoint().setLocation(
                    (model.p1.getX() + model.p2.getX()) / 2.0,
                    model.p2.getY());
            handles.add(triplet.right = new RightHandle(os, model.p2));

            selectedHandle = triplet.right;

            publishSequence();
        }

        @Override
        public void undo ()
        {
            for (OctaveShiftInter os : seq) {
                final Model originalModel = originalModels.get(os);
                os.line.setLine(originalModel.p1, originalModel.p2);
                os.setHookCopy(originalModel.hookEnd);
                os.setBounds(null);
                super.undo();
            }
        }

        /**
         * Update the left and right extensions for every OctaveShiftInter in 'seq'.
         *
         * @param takeAll true for including all 'seq' inters, even those flagged as REMOVED
         */
        private void updateExtensions (boolean takeAll)
        {
            final List<OctaveShiftInter> fSeq = new ArrayList<>();
            for (OctaveShiftInter os : seq) {
                if (takeAll || !os.isRemoved()) {
                    fSeq.add(os);
                }
            }

            for (OctaveShiftInter os : fSeq) {
                final int idx = fSeq.indexOf(os);

                if (idx == 0) { // first
                    os.setExtension(LEFT, null);
                    os.setExtension(RIGHT, (fSeq.size() > 1) ? fSeq.get(1) : null);
                } else if (idx < fSeq.size() - 1) { // medium
                    os.setExtension(LEFT, fSeq.get(idx - 1));
                    fSeq.get(idx - 1).setExtension(RIGHT, os);

                    os.setExtension(RIGHT, fSeq.get(idx + 1));
                    fSeq.get(idx + 1).setExtension(LEFT, os);
                } else { // last
                    os.setExtension(LEFT, fSeq.get(idx - 1));
                    os.setExtension(RIGHT, null);
                }
            }
        }

        //------------//
        // LeftHandle //
        //------------//
        /**
         * A left handle moves the whole line vertically and the left point horizontally
         * while staying within staff gutter.
         * <p>
         * It can however be dragged beyond current system to extend to previous staff or to
         * shrink with next staff.
         */
        private class LeftHandle
                extends OsHandle
        {
            public LeftHandle (OctaveShiftInter os,
                               Point2D center)
            {
                super(os, center);
            }

            @Override
            public boolean move (int dx,
                                 int dy)
            {
                final Point newPt = new Point(lastPt.x + dx, lastPt.y + dy);

                // Stay away from staff left
                final int xTarget = (int) center.getX() + dx;
                int xGap = xTarget - os.getStaff().getAbscissa(LEFT);

                if (xGap < 0) {
                    dx -= xGap;
                } else {
                    // Stay away from line right
                    if (triplet.right != null) {
                        xGap = (int) triplet.right.getPoint().getX() - xTarget;
                    } else {
                        xGap = (int) os.getLine().getX2() - xTarget;
                    }

                    if (xGap < 0) {
                        dx += xGap;
                    }
                }

                dy = adjustDy(dy, os); // Stay within current gutter

                // Data
                if (model.hookEnd != null) {
                    PointUtil.add(model.hookEnd, 0, dy);
                }

                // Handles
                PointUtil.add(model.p1, dx, dy);

                if (triplet.middle != null) {
                    PointUtil.add(triplet.middle.getPoint(), dx / 2.0, dy);
                }

                PointUtil.add(model.p2, 0, dy);

                // Look beyond current system
                final Part curPart = os.staff.getPart();
                final int curStaffIdx = os.staff.getIndexInPart();

                // Extension to previous staff?
                final Part prevPart = curPart.getPrecedingInPage();
                if (prevPart != null) {
                    final Staff prevStaff = prevPart.getStaves().get(curStaffIdx);
                    if ((prevPart != null) && prevStaff.contains(newPt)) {
                        // Left extend current octave shift to start of its staff
                        final List<AbstractChordInter> chords = os.staff.getChords();
                        final AbstractChordInter chLeft = chords.get(0);
                        model.p1.setLocation(chLeft.getCenterLeft().x, model.p2.getY());
                        triplet.middle.getPoint().setLocation(
                                (model.p1.getX() + model.p2.getX()) / 2.0,
                                model.p2.getY());
                        handles.remove(triplet.left);
                        triplet.left = null;

                        // Create a new octave shift from current abscissa to end of prevStaff
                        final List<AbstractChordInter> otherChords = prevStaff.getChords();
                        final AbstractChordInter chRight = otherChords.get(otherChords.size() - 1);
                        final OctaveShiftInter nos = new OctaveShiftInter(
                                os.shape,
                                os.kind,
                                new Line2D.Double(
                                        newPt.x,
                                        newPt.y,
                                        chRight.getCenterRight().x,
                                        newPt.y),
                                null);
                        nos.setManual(true);
                        nos.setStaff(prevStaff);
                        nos.computeValueDimensions(sheet);
                        logger.debug("Created {}", nos);
                        seq.add(0, nos);
                        createHandles(nos);
                        selectedHandle = triplets.get(nos).left;

                        publishSequence();
                    }
                }

                // Shrinking with next staff at current abscissa?
                if (seq.indexOf(os) < seq.size() - 1) {
                    final Part nextPart = curPart.getNextInPage();
                    if (nextPart != null) {
                        final Staff nextStaff = nextPart.getStaves().get(curStaffIdx);
                        if ((nextStaff != null) && nextStaff.contains(newPt)) {
                            removeOctaveShift(os);
                            shrinkOctaveShift(seq.get(0), newPt.x);
                        }
                    }
                }

                return true;
            }
        }

        //--------------//
        // MiddleHandle //
        //--------------//
        /**
         * A middle handle translates all points the same, but vertically and horizontally
         * stays within staff gutter.
         * <p>
         * It cannot be dragged beyond current system.
         */
        private class MiddleHandle
                extends OsHandle
        {
            public MiddleHandle (OctaveShiftInter os,
                                 Point2D center)
            {
                super(os, center);
            }

            @Override
            public boolean move (int dx,
                                 int dy)
            {
                if (triplet.left != null && triplet.right != null) {
                    // Stay within left and right staff sides
                    final int left = os.getStaff().getAbscissa(LEFT);
                    int xGap = (int) triplet.left.getPoint().getX() + dx - left;
                    if (xGap < 0) {
                        dx -= xGap;
                    }

                    final int right = os.getStaff().getAbscissa(RIGHT);
                    xGap = right - (int) triplet.right.getPoint().getX() + dx;
                    if (xGap < 0) {
                        dx += xGap;
                    }
                } else {
                    dx = 0; // No horizontal move
                }

                dy = adjustDy(dy, os);

                // Data
                model.translate(dx, dy);

                // Handles
                PointUtil.add(center, dx, dy);

                return true;
            }
        }

        //------------//
        // MiniEditor //
        //------------//
        /**
         * This class is a trick to fill an EditingTask focused on a specific OctaveShiftInter.
         */
        private class MiniEditor
                extends InterEditor
        {
            public MiniEditor (OctaveShiftInter os)
            {
                super(os);
            }

            @Override
            public void finalDoit ()
            {
                Editor.this.doit();
                updateExtensions(true); // Take all inters in 'seq'
            }

            @Override
            public void undo ()
            {
                Editor.this.undo();
                updateExtensions(false); // Take in 'seq' only the inters non flagged as REMOVED
            }
        }

        //----------//
        // OsHandle //
        //----------//
        /**
         * A UI handle, with its underlying OctaveShiftInter instance.
         * <p>
         * Reference to related OctaveShiftInter is needed, since this editor can handle several
         * physical OctaveShiftInter instances making a long logical octave shift on several staves.
         */
        private abstract class OsHandle
                extends Handle
        {
            protected final OctaveShiftInter os; // Underlying OS instance

            protected final Model model; // Model for OS

            protected final Triplet triplet; // Triplet of left / middle / right handles

            public OsHandle (OctaveShiftInter os,
                             Point2D center)
            {
                super(center);
                this.os = os;
                model = models.get(os);
                triplet = triplets.get(os);
            }
        }

        //-------------//
        // RightHandle //
        //-------------//
        /**
         * A right handle moves the whole line vertically and the right point horizontally
         * while staying within staff gutter.
         * <p>
         * It can however be dragged beyond current system to shrink with previous staff or to
         * extend with next staff.
         */
        private class RightHandle
                extends OsHandle
        {
            public RightHandle (OctaveShiftInter os,
                                Point2D center)
            {
                super(os, center);
            }

            @Override
            public boolean move (int dx,
                                 int dy)
            {
                final Point newPt = new Point(lastPt.x + dx, lastPt.y + dy);

                // Stay away from staff right
                final int xTarget = (int) center.getX() + dx;
                int xGap = os.getStaff().getAbscissa(RIGHT) - xTarget;

                if (xGap < 0) {
                    dx += xGap;
                } else {
                    // Stay away from line left
                    if (triplet.left != null) {
                        xGap = xTarget - (int) triplet.left.getPoint().getX();
                    } else {
                        xGap = xTarget - (int) os.getLine().getX1();
                    }

                    if (xGap < 0) {
                        dx -= xGap;
                    }
                }

                dy = adjustDy(dy, os);

                // Data
                if (model.hookEnd != null) {
                    PointUtil.add(model.hookEnd, dx, dy);
                }

                // Handles
                PointUtil.add(model.p1, 0, dy);

                if (triplet.middle != null) {
                    PointUtil.add(triplet.middle.getPoint(), dx / 2.0, dy);
                }

                PointUtil.add(model.p2, dx, dy);

                // Look beyond current system
                final Part curPart = os.staff.getPart();
                final int curStaffIdx = os.staff.getIndexInPart();

                // Shrinking with previous staff at current abscissa?
                if (seq.indexOf(os) > 0) {
                    final Part prevPart = curPart.getPrecedingInPage();
                    if (prevPart != null) {
                        final Staff prevStaff = prevPart.getStaves().get(curStaffIdx);
                        if ((prevStaff != null) && prevStaff.contains(newPt)) {
                            removeOctaveShift(os);
                            shrinkOctaveShift(seq.get(seq.size() - 1), newPt.x);
                        }
                    }
                }

                // Extension to next staff?
                final Part nextPart = curPart.getNextInPage();
                if (nextPart != null) {
                    final Staff nextStaff = nextPart.getStaves().get(curStaffIdx);
                    if ((nextStaff != null) && nextStaff.contains(newPt)) {
                        // Right extend current octave shift to end of its staff
                        final List<AbstractChordInter> chords = os.staff.getChords();
                        final AbstractChordInter chRight = chords.get(chords.size() - 1);
                        model.p2.setLocation(chRight.getCenterRight().x, model.p2.getY());
                        triplet.middle.getPoint().setLocation(
                                (model.p1.getX() + model.p2.getX()) / 2.0,
                                model.p2.getY());
                        handles.remove(triplet.right);
                        triplet.right = null;
                        model.hookEnd = null;

                        // Create a new octave shift from start of nextStaff to current abscissa
                        final List<AbstractChordInter> otherChords = nextStaff.getChords();
                        final AbstractChordInter chLeft = otherChords.get(0);
                        final OctaveShiftInter nos = new OctaveShiftInter(
                                os.shape,
                                os.kind,
                                new Line2D.Double(
                                        chLeft.getCenterLeft().x,
                                        newPt.y,
                                        newPt.x,
                                        newPt.y),
                                new Point2D.Double(newPt.x, newPt.y + os.getHookLg()));
                        nos.setManual(true);
                        nos.setStaff(nextStaff);
                        nos.computeValueDimensions(sheet);
                        logger.debug("Created {}", nos);
                        seq.add(nos);
                        createHandles(nos);
                        selectedHandle = triplets.get(nos).right;

                        publishSequence();
                    }
                }

                return true;
            }
        }

        //---------//
        // Triplet //
        //---------//
        /**
         * The 3 possible handles for editing an OctaveShiftInter.
         */
        private static class Triplet
        {
            LeftHandle left;

            MiddleHandle middle;

            RightHandle right;
        }
    }

    //------//
    // Kind //
    //------//
    public static enum Kind
    {
        ALTA, // Notes to be performed higher than written
        BASSA; // Notes to be performed lower than written
    }

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

        // Line left ending point (area center for the 8/15/22 symbol)
        public Point2D p1;

        // Line right ending point
        public Point2D p2;

        // (Optional) Hook ending point (above or below p2)
        public Point2D hookEnd;

        public Model (OctaveShiftInter os)
        {
            this(os.shape, os.kind, os.line.getP1(), os.line.getP2(), os.hookEnd);
        }

        public Model (Shape shape,
                      Kind kind,
                      Point2D p1,
                      Point2D p2,
                      Point2D hookEnd)
        {
            this.shape = shape;
            this.kind = kind;
            this.p1 = new Point2D.Double(p1.getX(), p1.getY());
            this.p2 = new Point2D.Double(p2.getX(), p2.getY());
            this.hookEnd = (hookEnd == null) ? null
                    : new Point2D.Double(hookEnd.getX(), hookEnd.getY());
        }

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(p1, dx, dy);
            PointUtil.add(p2, dx, dy);

            if (hookEnd != null) {
                PointUtil.add(hookEnd, dx, dy);
            }
        }
    }
}
