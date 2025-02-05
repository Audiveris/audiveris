//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S t a f f                                            //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import static org.audiveris.omr.glyph.Shape.PERCUSSION_CLEF;
import org.audiveris.omr.math.GeoPath;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.score.StaffConfig;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.grid.StaffFilament;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sheet.ui.ObjectEditor;
import org.audiveris.omr.sheet.ui.StaffEditor;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.relation.BarConnectionRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.ui.util.BasicAttachmentHolder;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.jgrapht.Graphs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>Staff</code> handles physical information of a staff with its lines.
 * <ul>
 * <li><b>Pitch position</b> applies to any item (such a head), is counted from 0 on staff mid line
 * and increases in top down direction.
 * <li><b>Ledger index</b> applies to ledger and ledgerLine, is counted from +/-1 for first ledger
 * and increases in top down direction.
 * </ul>
 * Example of pitch position values and ledger index values in a 5-line staff:
 *
 * <pre>
 *       pitch                                     ledger
 *     position:                                   index:
 *
 *         etc
 *         -8  -- second ledger above               (-2)
 *         -7
 *         -6  -- first ledger above                (-1)
 *         -5
 *     +-- -4 -------------  first staff line  -------------------------------+
 *     |   -3                                                                 |
 *     +-- -2 -------------                    -------------------------------+
 *     |   -1                                                                 |
 *     +--  0 -------------  mid staff line    -------------------------------+
 *     |    1                                                                 |
 *     +--  2 -------------                    -------------------------------+
 *     |    3                                                                 |
 *     +--  4 -------------  last staff line   -------------------------------+
 *          5
 *          6  -- first ledger below               ( 1)
 *          7
 *          8  -- second ledger below              ( 2)
 *         etc
 * </pre>
 * <p>
 * The interline value, which is defined as the vertical distance between two consecutive lines,
 * measured from center to center, is rather constant within first and last lines of a staff,
 * but may be different for ledgers, especially between the staff and the first ledger.
 * This must be taken into account when converting between absolute ordinates on one hand and
 * pitch position or ledger index in the other hand.
 * <p>
 * Note: All methods in Staff class are meant to provide correct results, regardless of the actual
 * number of lines in the staff instance.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "staff")
public class Staff
        implements AttachmentHolder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Staff.class);

    /** To sort by staff id. */
    public static final Comparator<Staff> byId = (Staff o1,
                                                  Staff o2) -> Integer.compare(
                                                          o1.getId(),
                                                          o2.getId());

    /** To sort by staff abscissa. */
    public static final Comparator<Staff> byAbscissa = (Staff o1,
                                                        Staff o2) -> Integer.compare(
                                                                o1.left,
                                                                o2.left);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * Staff id.
     * <p>
     * Counted top down, globally from 1 within the sheet.
     * <p>
     * The StringIntegerAdapter converts int type to String and allows to use this id as an XML ID.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = int.class, value = Jaxb.StringIntegerAdapter.class)
    protected int id;

    /**
     * Left extrema.
     * <p>
     * Absolute abscissa at beginning of lines
     */
    @XmlAttribute
    protected int left;

    /**
     * Right extrema.
     * <p>
     * Absolute abscissa at end of lines.
     */
    @XmlAttribute
    protected int right;

    /**
     * Flag for short staff.
     * <p>
     * With a neighboring staff on left or right side.
     */
    @XmlAttribute(name = "short")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    protected boolean isShort;

    /**
     * Flag for small staff.
     * <p>
     * Height of this staff is lower than others.
     */
    @XmlAttribute(name = "small")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    protected boolean isSmall;

    /**
     * Vertical sequence of staff lines.
     * <p>
     * Ordered from top to bottom.
     */
    @XmlElementWrapper(name = "lines")
    @XmlElement(name = "line")
    protected final List<LineInfo> lines;

    /** Staff Header information. */
    @XmlElement
    protected StaffHeader header;

    /** Horizontal sequence of bar lines. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "barlines")
    protected List<BarlineInter> barlines = new ArrayList<>();

    /**
     * Ledgers nearby, organized by ledger index with respect to the staff.
     * <p>
     * Temporary field used for persistency of transient <code>ledgerMap</code>.
     */
    @XmlElementWrapper(name = "ledgers")
    @XmlElement(name = "ledgers-entry")
    protected List<LedgersEntry> ledgersValue;

    /** Notes (heads and rests) assigned to this staff. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "notes")
    protected LinkedHashSet<AbstractNoteInter> notes;

    // Transient data
    //---------------

    /**
     * Ledgers around the staff, organized in a map.
     * Key: Ledger index value
     * Value: Horizontal sequence of ledgers at the same ledger index
     */
    protected final TreeMap<Integer, List<LedgerInter>> ledgerMap = new TreeMap<>();

    /**
     * To flag a dummy staff.
     * Used when exporting staff for a dummy part to MusicXML
     */
    protected boolean dummy;

    /** Side barlines, if any, on left and/or right sides. */
    protected final Map<HorizontalSide, BarlineInter> sideBars = new EnumMap<>(
            HorizontalSide.class);

    /**
     * Area around the staff.
     * The same area strategy applies for staves and for systems:
     * The purpose is to contain relevant entities (sections, glyphs) for the staff at hand but a
     * given entity may be contained by several staff areas when it is located in the inter-staff
     * gutter.
     * There is no need to be very precise, but a staff line cannot belong to several staff areas.
     * <ul>
     * <li>Horizontally, the area is extended half way to the neighboring side staff if any,
     * otherwise to the limit of the page.
     * <li>Vertically, the area is extended to the first encountered line (exclusive) of the next
     * staff if any, otherwise to the limit of the page.
     * </ul>
     */
    protected Area area;

    /**
     * Interline value specific to this staff.
     * (Different staves in a page may exhibit different interline values)
     */
    protected int specificInterline;

    /**
     * Map of ledger lines.
     * <p>
     * As opposed to a real staffLine (a standard staff contains 5 staffLines), we define a
     * 'ledgerLine' as a virtual line, going through 1 or several ledgers at a specified index,
     * and thus external to the staff but rather parallel to the staff lines.
     * <ul>
     * <li>Map key is the ledger index
     * <li>Map value is the virtual ledgerLine
     * </ul>
     */
    protected final TreeMap<Integer, GeoPath> ledgerLineMap = new TreeMap<>();

    /** Containing system. */
    @Navigable(false)
    protected SystemInfo system;

    /** Containing part. */
    @Navigable(false)
    protected Part part;

    /** Potential attachments. */
    protected final AttachmentHolder attachments = new BasicAttachmentHolder();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor needed for JAXB.
     */
    public Staff ()
    {
        this.id = 0;
        this.lines = null;
    }

    /**
     * Meant for a staff holder only.
     *
     * @param id the staff id to remember
     */
    private Staff (int id)
    {
        this.id = id;
        this.lines = null;
    }

    /**
     * Create info about a staff, with its contained staff lines.
     *
     * @param id                the id of the staff
     * @param left              abscissa of the left side
     * @param right             abscissa of the right side
     * @param specificInterline specific interline detected for this staff
     * @param lines             the sequence of contained staff lines
     */
    public Staff (int id,
                  double left,
                  double right,
                  int specificInterline,
                  List<LineInfo> lines)
    {
        this.id = id;
        this.left = (int) Math.rint(left);
        this.right = (int) Math.rint(right);
        this.specificInterline = specificInterline;
        this.lines = lines;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // addAttachment //
    //---------------//
    @Override
    public void addAttachment (String id,
                               Shape attachment)
    {
        attachments.addAttachment(id, attachment);
    }

    //------------//
    // addBarline //
    //------------//
    /**
     * Include a barline into staff structure.
     *
     * @param barline the barline to include
     */
    public void addBarline (BarlineInter barline)
    {
        if (!barlines.contains(barline)) {
            barlines.add(barline);
            Collections.sort(barlines, Inters.byCenterAbscissa);
            retrieveSideBars();
        }
    }

    //-----------//
    // addLedger //
    //-----------//
    /**
     * Add a ledger glyph to the collection, computing line index from pitch position.
     *
     * @param ledger the ledger glyph to add
     */
    public void addLedger (LedgerInter ledger)
    {
        Objects.requireNonNull(ledger, "Cannot add a null ledger");

        Glyph glyph = ledger.getGlyph();
        Point center = (glyph != null) ? glyph.getCentroid() : ledger.getCenter();
        addLedger(ledger, getLedgerLineIndex(pitchPositionOf(center)));
    }

    //-----------//
    // addLedger //
    //-----------//
    /**
     * Add a ledger glyph to the collection.
     *
     * @param ledger the ledger to add
     * @param index  the staff-based index for ledger line
     */
    public void addLedger (LedgerInter ledger,
                           int index)
    {
        Objects.requireNonNull(ledger, "Cannot add a null ledger");

        List<LedgerInter> ledgerList = ledgerMap.get(index);

        if (ledgerList == null) {
            ledgerMap.put(index, ledgerList = new ArrayList<>());
        }

        ledgerList.add(ledger);
    }

    //---------//
    // addNote //
    //---------//
    /**
     * Assign a note (head or rest) to this staff.
     *
     * @param note the note to add to staff collection
     */
    public void addNote (AbstractNoteInter note)
    {
        if (notes == null) {
            notes = new LinkedHashSet<>();
        }

        notes.add(note);
    }

    //--------------//
    // afterMarshal //
    //--------------//
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        ledgersValue = null;
    }

    //-------------//
    // afterReload //
    //-------------//
    /**
     * To be called right after unmarshalling.
     */
    public void afterReload ()
    {
        try {
            // Specific interline for this staff
            Scale scale = system.getSheet().getScale();
            specificInterline = isSmall() ? scale.getSmallInterline() : scale.getInterline();

            // Populate sideBars
            retrieveSideBars();

            // Populate ledgerMap from ledgersValue if any
            clearLedgers();

            if (ledgersValue != null) {
                for (LedgersEntry entry : ledgersValue) {
                    ledgerMap.put(entry.index, entry.ledgers);
                }

                ledgersValue = null;
            }

            buildAllLedgerLines();
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        if (!ledgerMap.isEmpty()) {
            // Populate ledgersValue from ledgerMap
            ledgersValue = new ArrayList<>();

            for (Entry<Integer, List<LedgerInter>> entry : ledgerMap.entrySet()) {
                ledgersValue.add(new LedgersEntry(entry.getKey(), entry.getValue()));
            }
        }
    }

    //---------------------//
    // buildAllLedgerLines //
    //---------------------//
    /**
     * Use staff ledgerMap to compute virtual external staff lines based on these ledgers.
     */
    public void buildAllLedgerLines ()
    {
        ledgerLineMap.clear();

        // Above
        buildLedgerLines(getFirstLine().getSpline(), -1);

        // Below
        buildLedgerLines(getLastLine().getSpline(), 1);
    }

    //------------------//
    // buildLedgerLines //
    //------------------//
    /**
     * Populate ledgerLineMap with the ledgers on one vertical side of the staff.
     *
     * @param line      first or last staff line, according to the vertical side of the staff
     * @param increment -1 when processing ledgers above the staff, +1 below the staff
     */
    private void buildLedgerLines (GeoPath line,
                                   int increment)
    {
        for (int i = increment;; i += increment) {
            final List<LedgerInter> ledgers = ledgerMap.get(i);
            if ((ledgers == null) || ledgers.isEmpty()) {
                return;
            }

            // Compute vertical distance of ledgers from previous line
            final Population pop = new Population();
            for (LedgerInter ledger : ledgers) {
                final Point2D middle = PointUtil.middle(ledger.getMedian());
                final double yLine = line.yAtX(middle.getX());
                pop.includeValue(middle.getY() - yLine);
            }

            final double dy = pop.getMeanValue();
            final AffineTransform at = AffineTransform.getTranslateInstance(0, dy);
            final GeoPath newLine = new GeoPath(line, at);
            ledgerLineMap.put(i, newLine);
            line = newLine;
        }
    }

    //--------------//
    // clearLedgers //
    //--------------//
    /**
     * Clear staff from its ledgers.
     */
    public void clearLedgers ()
    {
        ledgerMap.clear();
    }

    //----------//
    // contains //
    //----------//
    /**
     * Report whether the provided point lies within staff limits.
     *
     * @param point the location to check
     * @return true if within staff limits
     */
    public boolean contains (Point2D point)
    {
        if ((point.getX() < left) || (point.getX() > right)) {
            return false;
        }

        return distanceTo(point) <= 0;
    }

    //------------//
    // distanceTo //
    //------------//
    /**
     * Report the vertical (algebraic) distance between staff and the provided point.
     * Distance is negative if the point is within the staff height and positive if outside.
     *
     * @param point the provided point
     * @return algebraic distance between staff and point, specified in pixels
     */
    public int distanceTo (Point2D point)
    {
        return (int) doubleDistanceTo(point);
    }

    //------------------//
    // doubleDistanceTo //
    //------------------//
    /**
     * Report the vertical (algebraic) distance between staff and the provided point.
     * Distance is negative if the point is within the staff height and positive if outside.
     *
     * @param point the provided point
     * @return algebraic distance between staff and point, specified in pixels
     */
    public double doubleDistanceTo (Point2D point)
    {
        final double top = getFirstLine().yAt(point.getX());
        final double bottom = getLastLine().yAt(point.getX());

        return Math.max(top - point.getY(), point.getY() - bottom);
    }

    //------//
    // dump //
    //------//
    /**
     * A utility meant for debugging.
     */
    public void dump ()
    {
        System.out.println("StaffInfo" + getId() + " left=" + left + " right=" + right);

        int i = 0;

        for (LineInfo line : lines) {
            System.out.println(" LineInfo" + i++ + " " + line.toString());
        }
    }

    //-------//
    // gapTo //
    //-------//
    /**
     * Report the vertical gap between staff and the provided rectangle.
     *
     * @param rect the provided rectangle
     * @return 0 if the rectangle intersects the staff, otherwise the vertical
     *         distance from staff to closest edge of the rectangle
     */
    public double gapTo (Rectangle2D rect)
    {
        Point2D center = GeoUtil.center2D(rect);
        double staffTop = getFirstLine().yAt(center.getX());
        double staffBot = getLastLine().yAt(center.getX());
        double glyphTop = rect.getY();
        double glyphBot = glyphTop + rect.getHeight();

        // Check overlap
        double top = Math.max(glyphTop, staffTop);
        double bot = Math.min(glyphBot, staffBot);

        if (top <= bot) {
            return 0;
        }

        // No overlap, compute distance
        double dist = Double.MAX_VALUE;
        dist = Math.min(dist, Math.abs(staffTop - glyphTop));
        dist = Math.min(dist, Math.abs(staffTop - glyphBot));
        dist = Math.min(dist, Math.abs(staffBot - glyphTop));
        dist = Math.min(dist, Math.abs(staffBot - glyphBot));

        return dist;
    }

    //-------------//
    // getAbscissa //
    //-------------//
    /**
     * Report the staff abscissa, on the provided side.
     *
     * @param side provided side
     * @return the staff abscissa
     */
    public int getAbscissa (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return left;
        } else {
            return right;
        }
    }

    //---------//
    // getArea //
    //---------//
    /**
     * Report the area defined by the staff limits.
     *
     * @return the whole staff area
     */
    public Area getArea ()
    {
        if (area == null) {
            system.getSheet().getStaffManager().computeStaffArea(this);
        }

        return area;
    }

    //---------------//
    // getAreaBounds //
    //---------------//
    /**
     * Report the bounding box of the staff area.
     *
     * @return the lazily computed bounding box
     */
    public Rectangle2D getAreaBounds ()
    {
        return getArea().getBounds2D();
    }

    //----------------//
    // getAttachments //
    //----------------//
    @Override
    public Map<String, Shape> getAttachments ()
    {
        return attachments.getAttachments();
    }

    //-------------//
    // getBarlines //
    //-------------//
    /**
     * @return the barlines
     */
    public List<BarlineInter> getBarlines ()
    {
        return Collections.unmodifiableList(barlines);
    }

    //-------------//
    // getBestClef //
    //-------------//
    /**
     * Report the best clef that applies at provided abscissa.
     *
     * @param x provided abscissa
     * @return best clef found or null
     */
    public ClefInter getBestClef (int x)
    {
        List<ClefInter> clefs = getCompetingClefs(x);

        if (clefs.isEmpty()) {
            return null;
        }

        if (clefs.size() > 1) {
            SIGraph sig = getSystem().getSig();

            // Select best clef
            for (Inter clef : clefs) {
                sig.computeContextualGrade(clef);
            }

            Collections.sort(clefs, Inters.byReverseBestGrade);
        }

        return clefs.get(0);
    }

    //---------------//
    // getBrowseStop //
    //---------------//
    /**
     * Report the maximum abscissa before a really good barline is encountered.
     *
     * @param xMin minimum abscissa
     * @param xMax initial value of maximum abscissa
     * @return final maximum abscissa
     */
    public int getBrowseStop (int xMin,
                              int xMax)
    {
        final SIGraph sig = system.getSig();

        for (BarlineInter bar : barlines) {
            // Exclude poor barline
            if (!bar.isGood()) {
                continue;
            }

            // Exclude barline not connected to other staff
            if (!sig.hasRelation(bar, BarConnectionRelation.class)) {
                continue;
            }

            int barStart = bar.getBounds().x;

            if (barStart > xMax) {
                break;
            }

            if (barStart > xMin) {
                logger.debug("Staff#{} stopping search before {}", getId(), bar);
                xMax = barStart - 1;

                break;
            }
        }

        return xMax;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the abscissa-sorted sequence of chords found in this staff.
     *
     * @return list of staff chords
     */
    public List<AbstractChordInter> getChords ()
    {
        final List<AbstractChordInter> staffChords = new ArrayList<>();

        for (Inter inter : system.getSig().inters(AbstractChordInter.class)) {
            final AbstractChordInter chord = (AbstractChordInter) inter;

            // TODO: Should we discard SmallChordInter instances?
            if (chord.getStaves().contains(this)) {
                staffChords.add(chord);
            }
        }

        Collections.sort(staffChords, Inters.byAbscissa);

        return staffChords;
    }

    //-------------//
    // getClefStop //
    //-------------//
    /**
     * @return the clefStop
     */
    public Integer getClefStop ()
    {
        if (header.clef != null) {
            Rectangle bounds = header.clef.getBounds();

            return (bounds.x + bounds.width) - 1;
        }

        if ((header.clefRange != null) && header.clefRange.valid) {
            return header.clefRange.getStop();
        }

        return null;
    }

    //---------------------//
    // getClosestStaffLine //
    //---------------------//
    /**
     * Report the staff line which is closest to the provided point.
     *
     * @param point the provided point
     * @return the closest staff line found
     */
    public LineInfo getClosestStaffLine (Point2D point)
    {
        final double pitch = pitchPositionOf(point);
        int idx = (int) Math.rint((pitch + (lines.size() - 1)) / 2);

        if (idx < 0) {
            idx = 0;
        } else if (idx > (lines.size() - 1)) {
            idx = lines.size() - 1;
        }

        return lines.get(idx);
    }

    //-------------------//
    // getCompetingClefs //
    //-------------------//
    /**
     * Report the competing clef candidates active at provided abscissa.
     *
     * @param x provided abscissa
     * @return the collection of competing clefs
     */
    public List<ClefInter> getCompetingClefs (int x)
    {
        // Look for clef on left side in staff (together with its competing clefs)
        SIGraph sig = getSystem().getSig();
        List<Inter> staffClefs = sig.inters(this, ClefInter.class);
        Collections.sort(staffClefs, Inters.byAbscissa);

        Inter lastClef = null;

        for (Inter inter : staffClefs) {
            int xClef = inter.getBounds().x;

            if (xClef < x) {
                lastClef = inter;
            }
        }

        if (lastClef == null) {
            return Collections.emptyList();
        }

        // Pick up this clef together with all competing clefs
        Set<Relation> excs = sig.getExclusions(lastClef);
        List<ClefInter> clefs = new ArrayList<>();
        clefs.add((ClefInter) lastClef);

        for (Relation rel : excs) {
            Inter inter = Graphs.getOppositeVertex(sig, rel, lastClef);

            if (inter instanceof ClefInter clef) {
                if ((clef.getStaff() == this) && !clefs.contains(clef)) {
                    clefs.add(clef);
                }
            }
        }

        return clefs;
    }

    //-------------------------//
    // getConcreteLedgerNearby //
    //-------------------------//
    /**
     * Report the concrete ledger (if any) near the provided point.
     *
     * @param point the provided point
     * @return the closest ledger found, or null
     */
    public LedgerInter getConcreteLedgerNearby (Point2D point)
    {
        final double rawPitch = pitchPositionOf(point);
        final int linePitch = 2 * (int) Math.rint(rawPitch / 2);

        if (Math.abs(linePitch) <= lines.size()) {
            return null; // Within staff height, hence no ledger
        }

        final int dir = Integer.signum(linePitch);
        final int ledgerIndex = (linePitch - dir * (lines.size() - 1)) / 2;
        final List<LedgerInter> ledgers = ledgerMap.get(ledgerIndex);

        if (ledgers == null) {
            return null; // Not close to a ledger line
        }

        final double x = point.getX();
        final double y = point.getY();
        final Rectangle2D searchBox = new Rectangle2D.Double(
                x - specificInterline,
                y - specificInterline,
                2 * specificInterline,
                2 * specificInterline);
        LedgerInter bestLedger = null;
        double bestDist = Double.MAX_VALUE;

        // Browse the sequence of concrete ledgers
        for (LedgerInter ledger : ledgers) {
            if (ledger.getBounds().intersects(searchBox)) {
                final Point2D center = ledger.getCenter();
                final double dist = Math.abs(center.getY() - y);

                if (dist < bestDist) {
                    bestDist = dist;
                    bestLedger = ledger;
                }
            }
        }

        return bestLedger;
    }

    //-----------//
    // getEditor //
    //-----------//
    /**
     * Provide an editor on this staff.
     *
     * @param global true for a global mode editor, false for a lines mode editor
     * @return the chosen editor
     */
    public ObjectEditor getEditor (boolean global)
    {
        if (global) {
            return new StaffEditor.GlobalEditor(this);
        } else {
            return new StaffEditor.LinesEditor(this);
        }
    }

    //----------------//
    // getEndingSlope //
    //----------------//
    /**
     * Report mean ending slope, on the provided side.
     * We discard highest and lowest absolute slopes, and return the average
     * values for the remaining ones.
     *
     * @param side which side to select (left or right)
     * @return a "mean" value
     */
    public double getEndingSlope (HorizontalSide side)
    {
        List<Double> slopes = new ArrayList<>(lines.size());

        for (LineInfo l : lines) {
            StaffFilament line = (StaffFilament) l;
            slopes.add(line.getSlopeAt(line.getEndPoint(side).getX(), Orientation.HORIZONTAL));
        }

        if (slopes.size() < 3) {
            return slopes.get(0);
        }

        Collections.sort(slopes);

        double sum = 0;

        for (Double slope : slopes.subList(1, slopes.size() - 1)) {
            sum += slope;
        }

        return sum / (slopes.size() - 2);
    }

    //--------------//
    // getFirstLine //
    //--------------//
    /**
     * Report the first line in the series.
     *
     * @return the first line
     */
    public LineInfo getFirstLine ()
    {
        return lines.get(0);
    }

    //-----------//
    // getHeader //
    //-----------//
    /**
     * @return the StaffHeader information
     */
    public StaffHeader getHeader ()
    {
        return header;
    }

    //----------------//
    // getHeaderStart //
    //----------------//
    /**
     * @return the start of header area
     */
    public int getHeaderStart ()
    {
        return header.start;
    }

    //---------------//
    // getHeaderStop //
    //---------------//
    /**
     * Report the abscissa at end of staff StaffHeader area.
     * The StaffHeader is the zone at the beginning of the staff, dedicated to clef, plus key-sig
     * if any, plus time-sig if any. The StaffHeader cannot contain notes, stems, beams, etc.
     *
     * @return StaffHeader end abscissa
     */
    public int getHeaderStop ()
    {
        return header.stop;
    }

    //------------------//
    // getHeadPointSize //
    //------------------//
    /**
     * Report the proper point size for heads in this staff
     *
     * @return proper head point size
     */
    public int getHeadPointSize ()
    {
        return MusicFont.getHeadPointSize(system.getSheet().getScale(), specificInterline);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the mean height of the staff, between first and last line.
     *
     * @return the mean staff height
     */
    public int getHeight ()
    {
        return specificInterline * (lines.size() - 1);
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the staff id, counted from 1 in the sheet, regardless of containing system
     * and part.
     *
     * @return the staff id
     */
    public int getId ()
    {
        return id;
    }

    //----------------//
    // getIndexInPart //
    //----------------//
    /**
     * Report the index of this staff in the containing part.
     *
     * @return the index in containing part
     */
    public int getIndexInPart ()
    {
        Part part = getPart();
        List<Staff> staves = part.getStaves();

        return staves.indexOf(this);
    }

    //-------------//
    // getKeyStart //
    //-------------//
    /**
     * @return the keyStart
     */
    public Integer getKeyStart ()
    {
        if ((header.keyRange != null) && header.keyRange.valid) {
            return header.keyRange.getStart();
        }

        if (header.key != null) {
            return header.key.getBounds().x;
        }

        return null;
    }

    //------------//
    // getKeyStop //
    //------------//
    /**
     * @return the keyStop
     */
    public Integer getKeyStop ()
    {
        if (header.key != null) {
            Rectangle bounds = header.key.getBounds();

            return (bounds.x + bounds.width) - 1;
        }

        if ((header.keyRange != null) && header.keyRange.valid) {
            return header.keyRange.getStop();
        }

        return null;
    }

    //-------------//
    // getLastLine //
    //-------------//
    /**
     * Report the last line in the series.
     *
     * @return the last line
     */
    public LineInfo getLastLine ()
    {
        return lines.get(lines.size() - 1);
    }

    //-------------//
    // getLedgerAt //
    //-------------//
    /**
     * Report the ledger, if any, at provided line index and embracing provided abscissa.
     *
     * @param ledgerIndex index of ledger line
     * @param x           absolute abscissa
     * @return the ledger found or null
     */
    public LedgerInter getLedgerAt (int ledgerIndex,
                                    double x)
    {
        final List<LedgerInter> ledgerList = ledgerMap.get(ledgerIndex);

        if (ledgerList == null) {
            return null;
        }

        for (LedgerInter ledger : ledgerList) {
            if (GeoUtil.xEmbraces(ledger.getBounds(), x)) {
                return ledger;
            }
        }

        return null;
    }

    //----------------//
    // getLedgerIndex //
    //----------------//
    /**
     * Report the index in ledgerMap for the provided ledger
     *
     * @param ledger the provided ledger
     * @return the related index value or null if not found
     */
    public Integer getLedgerIndex (LedgerInter ledger)
    {
        for (Map.Entry<Integer, List<LedgerInter>> entry : ledgerMap.entrySet()) {
            if (entry.getValue().contains(ledger)) {
                return entry.getKey();
            }
        }

        return null;
    }

    //--------------//
    // getLedgerMap //
    //--------------//
    /**
     * Report the map of ledgers for this staff, indexed by relative vertical position.
     *
     * @return map of ledgers
     */
    public SortedMap<Integer, List<LedgerInter>> getLedgerMap ()
    {
        return ledgerMap;
    }

    //------------//
    // getLedgers //
    //------------//
    /**
     * Report the ordered set of ledgers, if any, for a given index.
     *
     * @param lineIndex the precise line index that specifies algebraic distance from staff
     * @return the proper set of ledgers, or null
     */
    public List<LedgerInter> getLedgers (int lineIndex)
    {
        return ledgerMap.get(lineIndex);
    }

    //----------//
    // getLeftY //
    //----------//
    /**
     * Report the ordinate at left side of staff for the desired vertical line
     *
     * @param verticalSide TOP or BOTTOM
     * @return the top of bottom ordinate on left side of staff
     */
    public int getLeftY (VerticalSide verticalSide)
    {
        return getLine(verticalSide).yAt(left);
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Report first or last staff line, according to desired vertical
     * side.
     *
     * @param side TOP for first, BOTTOM for last
     * @return the staff line
     */
    public LineInfo getLine (VerticalSide side)
    {
        if (side == TOP) {
            return lines.get(0);
        } else {
            return lines.get(lines.size() - 1);
        }
    }

    //--------------//
    // getLineCount //
    //--------------//
    /**
     * Report the number of lines in this staff.
     *
     * @return the number of lines (6, 5, 4, ...)
     */
    public int getLineCount ()
    {
        return lines.size();
    }

    //----------//
    // getLines //
    //----------//
    /**
     * Report the sequence of lines.
     *
     * @return the list of lines in this staff
     */
    public List<LineInfo> getLines ()
    {
        return lines;
    }

    //------------------//
    // getMeanInterline //
    //------------------//
    /**
     * Return the actual mean interline as observed on this staff.
     *
     * @return the precise mean interline value
     */
    public double getMeanInterline ()
    {
        if (lines.size() == 1) {
            return 0; // Case of one-line staff
        }

        Population dys = new Population();

        int dx = system.getSheet().getScale().getInterline(); // No need for precise sampling value
        int xMin = getAbscissa(LEFT);
        int xMax = getAbscissa(RIGHT);

        for (double x = xMin; x <= xMax; x += dx) {
            double prevY = -1;

            for (LineInfo line : lines) {
                double y = line.yAt(x);

                if (prevY != -1) {
                    double dy = y - prevY;
                    dys.includeValue(dy);
                }

                prevY = y;
            }
        }

        double mean = dys.getMeanValue();

        //        logger.info(
        //            String.format("Staff#%d dy:%.2f std:%.2f", id, mean, dys.getStandardDeviation()));
        //
        return mean;
    }

    //------------//
    // getMidLine //
    //------------//
    /**
     * Report the staff mid line.
     *
     * @return the center line. If line number is even, use lower index.
     */
    public LineInfo getMidLine ()
    {
        return lines.get((lines.size() - 1) / 2);
    }

    //---------//
    // getPart //
    //---------//
    /**
     * Report the part that contains this staff.
     *
     * @return the containing part
     */
    public Part getPart ()
    {
        if (part == null) {
            // This should not occur
            for (Part p : system.getParts()) {
                if (p.getStaves().contains(this)) {
                    return part = p;
                }
            }
        }

        return part;
    }

    //----------------//
    // getSideBarline //
    //----------------//
    /**
     * Report the barline, if any, at the desired horizontal side of this staff.
     *
     * @param side desired horizontal side
     * @return the side barline or null
     */
    public BarlineInter getSideBarline (HorizontalSide side)
    {
        return sideBars.get(side);
    }

    //----------------------//
    // getSpecificInterline //
    //----------------------//
    /**
     * Report the <b>specific</b> staff interline value, which may have a different
     * value than the sheet main interline value.
     *
     * @return the staff scale
     */
    public int getSpecificInterline ()
    {
        return specificInterline;
    }

    //------------------//
    // getStaffBarlines //
    //------------------//
    /**
     * Report the ordered sequence of StaffBarlineInter's in this staff.
     *
     * @return the sequence of StaffBarlineInter's found in staff
     */
    public List<StaffBarlineInter> getStaffBarlines ()
    {
        final SIGraph sig = getSystem().getSig();
        if (sig == null) {
            return Collections.emptyList();
        }

        final List<Inter> inters = sig.inters(StaffBarlineInter.class);
        final List<StaffBarlineInter> found = inters.stream() //
                .filter(inter -> inter.getStaff() == this) //
                .map(inter -> (StaffBarlineInter) inter) //
                .sorted(Inters.byCenterAbscissa) //
                .collect(Collectors.toList());

        return found;
    }

    //----------------//
    // getStaffConfig //
    //----------------//
    public StaffConfig getStaffConfig ()
    {
        return new StaffConfig(lines.size(), isSmall());
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * @return the containing system
     */
    public SystemInfo getSystem ()
    {
        return system;
    }

    //--------------//
    // getTimeStart //
    //--------------//
    /**
     * @return the timeStart
     */
    public Integer getTimeStart ()
    {
        if ((header.timeRange != null) && header.timeRange.valid) {
            return header.timeRange.getStart();
        }

        if (header.time != null) {
            return header.time.getBounds().x;
        }

        return null;
    }

    //-------------//
    // getTimeStop //
    //-------------//
    /**
     * @return the timeStop
     */
    public Integer getTimeStop ()
    {
        if (header.time != null) {
            Rectangle bounds = header.time.getBounds();

            return (bounds.x + bounds.width) - 1;
        }

        if ((header.timeRange != null) && header.timeRange.valid) {
            return header.timeRange.getStop();
        }

        return null;
    }

    //--------//
    // isDrum //
    //--------//
    /**
     * Report whether this staff is a 5-line or 1-line unpitched staff.
     *
     * @return true if so
     */
    public boolean isDrum ()
    {
        if (getLineCount() == 1) {
            return true;
        }

        if (header != null && header.clef != null) {
            return header.clef.getShape() == PERCUSSION_CLEF;
        }

        return part.isDrumPart();
    }

    //---------//
    // isDummy //
    //---------//
    /**
     * Report whether this staff is a dummy staff (created temporarily for export).
     *
     * @return true for dummy staff
     */
    public boolean isDummy ()
    {
        return dummy;
    }

    //----------------//
    // isOneLineStaff //
    //----------------//
    /**
     * Report whether the staff is a one-line staff.
     *
     * @return true for a OneLineStaff
     */
    public boolean isOneLineStaff ()
    {
        return false; // By default
    }

    //--------------//
    // isPointAbove //
    //--------------//
    /**
     * Report whether the provided point lies above the staff.
     *
     * @param pt provided point
     * @return true if above
     */
    public boolean isPointAbove (Point2D pt)
    {
        return pt.getY() < getFirstLine().yAt(pt.getX());
    }

    //--------------//
    // isPointBelow //
    //--------------//
    /**
     * Report whether the provided point lies below the staff.
     *
     * @param pt provided point
     * @return true if below
     */
    public boolean isPointBelow (Point2D pt)
    {
        return pt.getY() > getLastLine().yAt(pt.getX());
    }

    //---------//
    // isShort //
    //---------//
    /**
     * Report whether the staff is a short (partial) one, which means
     * that there is another staff on left or right side.
     *
     * @return the isShort
     */
    public boolean isShort ()
    {
        return isShort;
    }

    //---------//
    // isSmall //
    //---------//
    /**
     * Report whether the staff has a small height compared with others.
     *
     * @return the isSmall
     */
    public boolean isSmall ()
    {
        return isSmall;
    }

    //-------------//
    // isTablature //
    //-------------//
    /**
     * Report whether the staff is a tablature (rather than a standard staff).
     *
     * @return true for a tablature
     */
    public boolean isTablature ()
    {
        return false; // By default
    }

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    /**
     * Compute the pitch position of a pixel point, taking ledger lines if any into account,
     * otherwise extrapolating from vertical distance to staff lines.
     *
     * @param pt the pixel point
     * @return the pitch position (a double value)
     */
    public double pitchPositionOf (Point2D pt)
    {
        final double x = pt.getX();
        final double y = pt.getY();

        // Location with respect to staff
        final double top = getFirstLine().yAt(x);
        final double bottom = getLastLine().yAt(x);

        if (y >= top && y <= bottom) {
            // Inside staff:
            // Interpolate between staff lines
            return ((lines.size() - 1) * ((2 * y) - bottom - top)) / (bottom - top);
        }

        // Outside staff:
        // Check ledger lines
        final int dir = (y < top) ? -1 : 1;
        double prevY = (dir == -1) ? top : bottom;
        int prevPP = dir * (lines.size() - 1);

        for (int li = dir;; li += dir) {
            final GeoPath ledgerLine = ledgerLineMap.get(li);

            if (ledgerLine == null) {
                break;
            }

            final double ledgerY = ledgerLine.yAtXExt(x);

            if (Double.compare(ledgerY, y) * dir >= 0) {
                // Interpolate between ordinates of prev line and ledger line
                return prevPP + 2 * dir * (y - prevY) / (ledgerY - prevY);
            }

            prevY = ledgerY;
            prevPP = prevPP + 2 * dir;
        }

        // Beyond ledger lines:
        // Extrapolate from last known line (staff or ledger), using staff interline value
        return prevPP + 2 * (y - prevY) / specificInterline;
    }

    //-----------------//
    // pitchToOrdinate //
    //-----------------//
    /**
     * Report the absolute ordinate for a staff-related pitch at a given abscissa.
     *
     * @param x     provided absolute abscissa
     * @param pitch pitch position with respect to this staff
     * @return the corresponding absolute ordinate
     */
    public double pitchToOrdinate (double x,
                                   double pitch)
    {
        // Location with respect to staff
        final double top = getFirstLine().yAt(x);
        final double bottom = getLastLine().yAt(x);

        if (Math.abs(pitch) <= lines.size() - 1) {
            // Inside staff:
            // Interpolate between staff lines
            if (lines.size() - 1 == 0) {
                return top;
            }
            return 0.5 * (top + bottom + ((pitch * (bottom - top)) / (lines.size() - 1)));
        }

        // Outside staff:
        // Check ledger lines
        final int dir = (pitch >= 0) ? 1 : -1;
        double prevY = (dir == -1) ? top : bottom;
        int prevPP = dir * (lines.size() - 1);

        for (int li = dir;; li += dir) {
            final GeoPath ledgerLine = ledgerLineMap.get(li);

            if (ledgerLine == null) {
                break;
            }

            final int ledgerPP = prevPP + 2 * dir;
            final double ledgerY = ledgerLine.yAtXExt(x);

            if (Double.compare(ledgerPP, pitch) * dir >= 0) {
                // Interpolate between ordinates of prev line and ledger line
                return prevY + (ledgerY - prevY) * (pitch - prevPP) / (ledgerPP - prevPP);
            }

            prevY = ledgerY;
            prevPP = ledgerPP;
        }

        // Beyond ledger lines:
        // Extrapolate from last known line (staff or ledger), using staff interline value
        return prevY + 0.5 * (pitch - prevPP) * specificInterline;
    }

    //-------------------//
    // removeAttachments //
    //-------------------//
    @Override
    public int removeAttachments (String prefix)
    {
        return attachments.removeAttachments(prefix);
    }

    //---------------//
    // removeBarline //
    //---------------//
    /**
     * Remove the provided instance of Barline from internal staff collection.
     *
     * @param barline the barline to remove
     * @return true if actually removed
     */
    public boolean removeBarline (BarlineInter barline)
    {
        // Purge sideBars if needed
        for (Iterator<Entry<HorizontalSide, BarlineInter>> it = sideBars.entrySet().iterator(); it
                .hasNext();) {
            Entry<HorizontalSide, BarlineInter> entry = it.next();

            if (entry.getValue() == barline) {
                it.remove();
            }
        }

        // Purge barlines
        boolean res = barlines.remove(barline);

        retrieveSideBars();

        return res;
    }

    //--------------//
    // removeLedger //
    //--------------//
    /**
     * Remove a ledger from staff collection.
     *
     * @param ledger the ledger to remove
     * @return true if actually removed, false if not found
     */
    public boolean removeLedger (LedgerInter ledger)
    {
        Objects.requireNonNull(ledger, "Cannot remove a null ledger");

        if (ledger.isVip()) {
            logger.info("VIP removing {}", ledger);
        }

        // Browse all staff ledger indices
        for (Entry<Integer, List<LedgerInter>> entry : ledgerMap.entrySet()) {
            List<LedgerInter> ledgerList = entry.getValue();

            if (ledgerList.remove(ledger)) {
                if (ledgerList.isEmpty()) {
                    // No ledger is left on this line index, thus remove the map entry
                    ledgerMap.remove(entry.getKey());
                }

                return true;
            }
        }

        // Not found
        logger.debug("Could not find ledger {}", ledger);

        return false;
    }

    //------------//
    // removeNote //
    //------------//
    /**
     * Remove a note (head or rest) from staff collection.
     *
     * @param note the note to remove
     * @return true if actually removed, false if not found
     */
    public boolean removeNote (AbstractNoteInter note)
    {
        boolean result = false;

        if (notes != null) {
            result = notes.remove(note);

            if (notes.isEmpty()) {
                notes = null;
            }
        }

        return result;
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint each staff line, perhaps with its defining points.
     *
     * @param g the graphics context
     * @return true if something has been actually drawn
     */
    public boolean render (Graphics2D g)
    {
        //        if (area != null) {
        //            LineInfo firstLine = getFirstLine();
        //            LineInfo lastLine = getLastLine();
        //
        //            if ((firstLine != null) && (lastLine != null)) {
        //                Rectangle clip = g.getClipBounds();
        //
        //                if ((clip != null) && !clip.intersects(getAreaBounds())) {
        //                    return false;
        //                }
        //            }
        //        }
        //
        final boolean showPoints = ViewParameters.getInstance().isStaffPointsPainting();
        final Scale scale = system.getSheet().getScale();
        final double pointWidth = scale.toPixelsDouble(constants.definingPointSize);

        // Draw each staff line
        for (LineInfo line : lines) {
            line.renderLine(g, showPoints, pointWidth);
        }

        //        // Draw ledger lines
        //        final Color oldColor = g.getColor();
        //        g.setColor(Color.CYAN);
        //        for (GeoPath line : ledgerLineMap.values()) {
        //            g.draw(line);
        //        }
        //        g.setColor(oldColor);
        //
        return true;
    }

    //-------------------//
    // renderAttachments //
    //-------------------//
    @Override
    public void renderAttachments (Graphics2D g)
    {
        attachments.renderAttachments(g);
    }

    //-----------//
    // replicate //
    //-----------//
    /**
     * Build a "replicate" of this staff, to be used as a dummy staff.
     *
     * @return the dummy replicate
     */
    public Staff replicate ()
    {
        Staff replicate = new Staff(0, left, right, specificInterline, null);

        return replicate;
    }

    //------------------//
    // retrieveSideBars //
    //------------------//
    /**
     * Remember barlines on left and right sides, if any.
     */
    private void retrieveSideBars ()
    {
        sideBars.clear();

        if (!barlines.isEmpty()) {
            for (HorizontalSide side : HorizontalSide.values()) {
                final int end = getAbscissa(side);
                final BarlineInter bar = barlines.get((side == LEFT) ? 0 : (barlines.size() - 1));
                final Rectangle barBox = bar.getBounds();

                if ((barBox.x <= end) && (end <= ((barBox.x + barBox.width) - 1))) {
                    sideBars.put(side, bar);
                }
            }
        }

        logger.debug("Staff#{} sideBars:{}", id, sideBars);
    }

    //-------------//
    // setAbscissa //
    //-------------//
    /**
     * Set the staff abscissa of the provided side.
     *
     * @param side provided side
     * @param val  abscissa of staff end
     */
    public void setAbscissa (HorizontalSide side,
                             int val)
    {
        if (side == HorizontalSide.LEFT) {
            left = val;
        } else {
            right = val;
        }
    }

    //---------//
    // setArea //
    //---------//
    /**
     * Assign staff area.
     *
     * @param area the underlying area
     */
    public void setArea (Area area)
    {
        this.area = area;

        getArea(); // Force recomputation if area has been set to null

        addAttachment("staff-area-" + id, area);
    }

    //-------------//
    // setBarlines //
    //-------------//
    /**
     * Assign the sequence of barlines.
     *
     * @param barlines the barlines to set
     */
    public void setBarlines (List<BarlineInter> barlines)
    {
        this.barlines = barlines;
        retrieveSideBars();
    }

    //-------------//
    // setClefStop //
    //-------------//
    /**
     * Assign the ending abscissa of staff header clef.
     *
     * @param clefStop the clefStop to set
     */
    public void setClefStop (int clefStop)
    {
        header.clefRange.setStop(clefStop);
        header.clefRange.valid = true;
    }

    //----------//
    // setDummy //
    //----------//
    /**
     * Flag this staff as a dummy one (meant for MusicXML export of missing part).
     */
    public void setDummy ()
    {
        dummy = true;
    }

    //-----------//
    // setHeader //
    //-----------//
    /**
     * Assign staff header information.
     *
     * @param header the StaffHeader information
     */
    public void setHeader (StaffHeader header)
    {
        this.header = header;
    }

    //---------------//
    // setHeaderStop //
    //---------------//
    /**
     * Refine the abscissa of StaffHeader break.
     *
     * @param headerStop the refined StaffHeader end value
     */
    public void setHeaderStop (int headerStop)
    {
        header.stop = headerStop;
    }

    //-------//
    // setId //
    //-------//
    /**
     * Modify staff ID.
     *
     * @param id thee new ID
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //------------//
    // setKeyStop //
    //------------//
    /**
     * Assign the ending abscissa of staff header key.
     *
     * @param keyStop the keyStop to set
     */
    public void setKeyStop (Integer keyStop)
    {
        header.keyRange.setStop(keyStop);
        header.keyRange.valid = true;
    }

    //---------//
    // setPart //
    //---------//
    /**
     * Assign the containing part.
     *
     * @param part the part to set
     */
    public void setPart (Part part)
    {
        this.part = part;
    }

    //----------//
    // setShort //
    //----------//
    /**
     * Flag this staff as a "short" one, because it is displayed side
     * by side with another one.
     * This indicates these two staves belong to separate systems, displayed
     * side by side, rather than one under the other.
     */
    public void setShort ()
    {
        isShort = true;
    }

    //----------//
    // setSmall //
    //----------//
    /**
     * Flag this staff as a "small" one.
     */
    public void setSmall ()
    {
        isSmall = true;
    }

    //-----------//
    // setSystem //
    //-----------//
    /**
     * Assign the containing system.
     *
     * @param system the system to set
     */
    public void setSystem (SystemInfo system)
    {
        this.system = system;
    }

    //-------------//
    // setTimeStop //
    //-------------//
    /**
     * Assign the ending abscissa of the staff header time signature.
     *
     * @param timeStop the timeStop to set
     */
    public void setTimeStop (Integer timeStop)
    {
        header.timeRange.setStop(timeStop);
        header.timeRange.valid = true;
    }

    //---------------//
    // simplifyLines //
    //---------------//
    /**
     * Replace the transient StaffFilament instances by persistent StaffLine instances.
     *
     * @param sheet the sheet to process
     * @return the original StaffFilaments
     */
    public List<LineInfo> simplifyLines (Sheet sheet)
    {
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        final List<LineInfo> originals = new ArrayList<>(lines);
        lines.clear();

        for (LineInfo original : originals) {
            final StaffFilament staffFilament = (StaffFilament) original;
            final StaffLine staffLine = staffFilament.toStaffLine(glyphIndex); // Simplification
            lines.add(staffLine);
        }

        return originals;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{" + getClass().getSimpleName());

        sb.append(" id=").append(getId());

        if (isShort()) {
            sb.append(" SHORT");
        }

        if (isSmall()) {
            sb.append(" SMALL");
        }

        sb.append(" left:").append(left);
        sb.append(" right:").append(right);

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // xOverlaps //
    //-----------//
    /**
     * Report whether staff horizontally overlaps that other staff.
     *
     * @param that the other staff
     * @return true if overlap
     */
    public boolean xOverlaps (Staff that)
    {
        final double commonLeft = Math.max(left, that.left);
        final double commonRight = Math.min(right, that.right);

        return commonRight > commonLeft;
    }

    //-----------//
    // yOverlaps //
    //-----------//
    /**
     * Report whether staff vertically overlaps that other staff.
     *
     * @param that the other staff
     * @return true if overlap
     */
    public boolean yOverlaps (Staff that)
    {
        final double thisTop = this.getFirstLine().getEndPoint(LEFT).getY();
        final double thatTop = that.getFirstLine().getEndPoint(LEFT).getY();
        final double commonTop = Math.max(thisTop, thatTop);

        final double thisBottom = this.getLastLine().getEndPoint(LEFT).getY();
        final double thatBottom = that.getLastLine().getEndPoint(LEFT).getY();
        final double commonBottom = Math.min(thisBottom, thatBottom);

        return commonBottom > commonTop;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------------//
    // getDefiningPointSize //
    //----------------------//
    /**
     * Report the diameter to draw each staff defining point.
     *
     * @return circle diameter to draw a point
     */
    public static Scale.Fraction getDefiningPointSize ()
    {
        return constants.definingPointSize;
    }

    //--------------------//
    // getLedgerLineIndex //
    //--------------------//
    /**
     * Compute staff-based line index, based on provided pitch position
     *
     * @param pitchPosition the provided pitch position with respect to the staff
     * @return the computed ledger line index
     */
    public static int getLedgerLineIndex (double pitchPosition)
    {
        if (pitchPosition > 0) {
            return (int) Math.rint(pitchPosition / 2) - 2;
        } else {
            return (int) Math.rint(pitchPosition / 2) + 2;
        }
    }

    //------------------------//
    // getLedgerPitchPosition //
    //------------------------//
    /**
     * Report the pitch position of a ledger WRT the related staff.
     * <p>
     * TODO: This implementation assumes a 5-line staff.
     * But can we have ledgers on a staff with more (of less) than 5 lines?
     *
     * @param lineIndex the ledger line index
     * @return the ledger pitch position
     */
    public static int getLedgerPitchPosition (int lineIndex)
    {
        //        // Safer, for the time being...
        //        if (getStaff()
        //                .getLines()
        //                .size() != 5) {
        //            throw new RuntimeException("Only 5-line staves are supported");
        //        }
        return Integer.signum(lineIndex) * 4 + (2 * lineIndex);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction definingPointSize = new Scale.Fraction(
                0.05,
                "Display width of a defining point");
    }

    //--------------//
    // LedgersEntry //
    //--------------//
    /**
     * This temporary structure is needed to marshal / unmarshal the ledgerMap,
     * because of its use of IDREF's.
     */
    protected static class LedgersEntry
    {
        /**
         * Index of the ledger line, with respect to the staff.
         */
        @XmlAttribute
        private final int index;

        /**
         * Horizontal sequence of concrete ledgers in the ledger line.
         */
        @XmlList
        @XmlIDREF
        @XmlValue
        private final List<LedgerInter> ledgers;

        // Needed for JAXB
        LedgersEntry ()
        {
            this.index = 0;
            this.ledgers = null;
        }

        LedgersEntry (int index,
                      List<LedgerInter> ledgers)
        {
            this.index = index;
            this.ledgers = ledgers;
        }

        @Override
        public String toString ()
        {
            final StringBuilder sb = new StringBuilder("LedgersEntry{");
            sb.append("index:").append(index);

            if (ledgers != null) {
                sb.append(" ledgers:").append(ledgers.size());
            }

            sb.append('}');

            return sb.toString();
        }
    }

    //-------------//
    // StaffHolder //
    //-------------//
    /**
     * Meant to be a placeholder for Staff while performing JAXB unmarshalling.
     * It is a dummy staff, whose temporary purpose is just to record the staff ID.
     */
    public static class StaffHolder
            extends Staff
    {
        /** Predefined place holders. */
        private static ConcurrentHashMap<Integer, StaffHolder> holders = new ConcurrentHashMap<>();

        /**
         * Create a place holder
         *
         * @param id staff ID to remember
         */
        public StaffHolder (int id)
        {
            super(id);
        }

        /**
         * Check provided inter, to replace staff holder with proper staff instance.
         *
         * @param inter the inter instance to check
         * @param mgr   sheet staff manager
         */
        public static void checkStaffHolder (Inter inter,
                                             StaffManager mgr)
        {
            if (inter.hasStaff()) {
                Staff staff = inter.getStaff();

                if (staff instanceof StaffHolder) {
                    inter.setStaff(mgr.getStaff(staff.getId() - 1));
                }
            }
        }

        /** Delete all the predefined place holders. */
        public static void clearStaffHolders ()
        {
            holders.clear();
        }

        /**
         * Get a place holder with proper ID.
         *
         * @param id specific staff ID
         * @return a (predefined) place holder with proper ID
         */
        public static StaffHolder getStaffHolder (int id)
        {
            StaffHolder holder = StaffHolder.holders.get(id);

            if (holder == null) {
                holders.put(id, holder = new StaffHolder(id));
            }

            return holder;
        }
    }
}
