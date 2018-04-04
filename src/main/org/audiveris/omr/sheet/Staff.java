//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S t a f f                                            //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.grid.StaffFilament;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sheet.note.NotePosition;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.relation.BarConnectionRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.ui.util.BasicAttachmentHolder;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.*;

import org.jgrapht.Graphs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
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

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Staff} handles physical information of a staff with its lines.
 * <p>
 * Note: All methods are meant to provide correct results, regardless of the actual number of lines
 * in the staff instance.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Staff
        implements AttachmentHolder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            Staff.class);

    /** To sort by staff id. */
    public static final Comparator<Staff> byId = new Comparator<Staff>()
    {
        @Override
        public int compare (Staff o1,
                            Staff o2)
        {
            return Integer.compare(o1.getId(), o2.getId());
        }
    };

    /** To sort by staff abscissa. */
    public static final Comparator<Staff> byAbscissa = new Comparator<Staff>()
    {
        @Override
        public int compare (Staff o1,
                            Staff o2)
        {
            return Integer.compare(o1.left, o2.left);
        }
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Staff id. (counted globally from 1 within the sheet) */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = int.class, value = Jaxb.StringIntegerAdapter.class)
    private final int id;

    /** Left extrema. (abscissa at beginning of lines) */
    @XmlAttribute
    private int left;

    /** Right extrema. (abscissa at end of lines) */
    @XmlAttribute
    private int right;

    /** Flag for short staff. (With a neighbor staff on left or right side) */
    @XmlAttribute(name = "short")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean isShort;

    /** Flag for small staff. (height lower than others) */
    @XmlAttribute(name = "small")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean isSmall;

    /** Sequence of staff lines. (from top to bottom) */
    @XmlElementWrapper(name = "lines")
    @XmlElement(name = "line")
    private final List<LineInfo> lines;

    /** Staff Header information. */
    @XmlElement
    private StaffHeader header;

    /** Sequence of bar lines. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "barlines")
    private List<BarlineInter> barlines = new ArrayList<BarlineInter>();

    /** Ledgers nearby, organized by position index WRT staff. Temporary for persistency */
    @XmlElementWrapper(name = "ledgers")
    @XmlElement(name = "ledgers-entry")
    private List<LedgersEntry> ledgersValue;

    /** Notes (heads & rests) assigned to this staff. */
    @XmlList
    @XmlIDREF
    @XmlElement(name = "notes")
    private LinkedHashSet<AbstractNoteInter> notes;

    // Transient data
    //---------------
    //
    /** Ledgers nearby, organized in a map. */
    private final TreeMap<Integer, List<LedgerInter>> ledgerMap = new TreeMap<Integer, List<LedgerInter>>();

    /** To flag a dummy staff. */
    private boolean dummy;

    /** Side barlines, if any. */
    private final Map<HorizontalSide, BarlineInter> sideBars = new EnumMap<HorizontalSide, BarlineInter>(
            HorizontalSide.class);

    /**
     * Area around the staff.
     * The same area strategy applies for staves and for systems:
     * The purpose is to contain relevant entities (sections, glyphs) for the staff at hand but a
     * given entity may be contained by several staff areas when it is located in the inter-staff
     * gutter.
     * There is no need to be very precise, but a staff line cannot belong to several staff areas.
     * <ul>
     * <li>Horizontally, the area is extended half way to the side staff if any, otherwise to the
     * limit of the page.
     * <li>Vertically, the area is extended to the first encountered line (exclusive) of the next
     * staff if any, otherwise to the limit of the page.
     * </ul>
     */
    private Area area;

    /**
     * Interline specific to this staff.
     * (since different staves in a page may exhibit different interline values)
     */
    private int specificInterline;

    /** Containing system. */
    @Navigable(false)
    private SystemInfo system;

    /** Containing part. */
    @Navigable(false)
    private Part part;

    /** Potential attachments. */
    private final AttachmentHolder attachments = new BasicAttachmentHolder();

    //~ Constructors -------------------------------------------------------------------------------
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

    /**
     * No-arg constructor needed for JAXB.
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

    //~ Methods ------------------------------------------------------------------------------------
    //----------------------//
    // getDefiningPointSize //
    //----------------------//
    public static Scale.Fraction getDefiningPointSize ()
    {
        return constants.definingPointSize;
    }

    //
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
        barlines.add(barline);
        Collections.sort(barlines, Inters.byCenterAbscissa);
        retrieveSideBars();
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

        List<LedgerInter> ledgerSet = ledgerMap.get(index);

        if (ledgerSet == null) {
            ledgerSet = new ArrayList<LedgerInter>();
            ledgerMap.put(index, ledgerSet);
        }

        ledgerSet.add(ledger);
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
            notes = new LinkedHashSet<AbstractNoteInter>();
        }

        notes.add(note);
    }

    //-------------//
    // afterReload //
    //-------------//
    public void afterReload ()
    {
        try {
            // Specific interline for this staff
            Scale scale = system.getSheet().getScale();
            specificInterline = isSmall() ? scale.getSmallInterline() : scale.getInterline();

            // Populate sideBars
            retrieveSideBars();

            // Populate ledgerMap from ledgersValue if any
            ledgerMap.clear();

            if (ledgersValue != null) {
                for (LedgersEntry entry : ledgersValue) {
                    ledgerMap.put(entry.index, entry.ledgers);
                }

                ledgersValue = null;
            }
        } catch (Exception ex) {
            logger.warn("Error in " + getClass() + " afterReload() " + ex, ex);
        }
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
        final double top = getFirstLine().yAt(point.getX());
        final double bottom = getLastLine().yAt(point.getX());

        return (int) Math.max(top - point.getY(), point.getY() - bottom);
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
    public int gapTo (Rectangle rect)
    {
        Point center = GeoUtil.centerOf(rect);
        int staffTop = getFirstLine().yAt(center.x);
        int staffBot = getLastLine().yAt(center.x);
        int glyphTop = rect.y;
        int glyphBot = (glyphTop + rect.height) - 1;

        // Check overlap
        int top = Math.max(glyphTop, staffTop);
        int bot = Math.min(glyphBot, staffBot);

        if (top <= bot) {
            return 0;
        }

        // No overlap, compute distance
        int dist = Integer.MAX_VALUE;
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

    //-------------//
    // getClefStop //
    //-------------//
    /**
     * @return the clefStop
     */
    public Integer getClefStop ()
    {
        if ((header.clefRange != null) && header.clefRange.valid) {
            return header.clefRange.getStop();
        }

        if (header.clef != null) {
            Rectangle bounds = header.clef.getBounds();

            return (bounds.x + bounds.width) - 1;
        }

        return null;
    }

    //------------------//
    // getClosestLedger //
    //------------------//
    /**
     * Report the closest ledger (if any) to provided point, located between the point
     * and this staff.
     *
     * @param point the provided point
     * @return the closest ledger found, or null
     */
    public IndexedLedger getClosestLedger (Point2D point)
    {
        IndexedLedger bestLedger = null;
        double top = getFirstLine().yAt(point.getX());
        double bottom = getLastLine().yAt(point.getX());
        double rawPitch = (4.0d * ((2 * point.getY()) - bottom - top)) / (bottom - top);

        if (Math.abs(rawPitch) <= 5) {
            return null;
        }

        Rectangle2D searchBox;

        if (rawPitch < 0) {
            searchBox = new Rectangle2D.Double(
                    point.getX(),
                    point.getY(),
                    0,
                    top - point.getY() + 1);
        } else {
            searchBox = new Rectangle2D.Double(point.getX(), bottom, 0, point.getY() - bottom + 1);
        }

        //searchBox.grow(interline, interline);
        searchBox.setRect(
                searchBox.getX() - specificInterline,
                searchBox.getY() - specificInterline,
                searchBox.getWidth() + (2 * specificInterline),
                searchBox.getHeight() + (2 * specificInterline));

        // Browse all staff ledgers
        Set<IndexedLedger> foundLedgers = new LinkedHashSet<IndexedLedger>();

        for (Map.Entry<Integer, List<LedgerInter>> entry : ledgerMap.entrySet()) {
            for (LedgerInter ledger : entry.getValue()) {
                if (ledger.getBounds().intersects(searchBox)) {
                    foundLedgers.add(new IndexedLedger(ledger, entry.getKey()));
                }
            }
        }

        if (!foundLedgers.isEmpty()) {
            // Use the closest ledger
            double bestDist = Double.MAX_VALUE;

            for (IndexedLedger iLedger : foundLedgers) {
                Point2D center = iLedger.ledger.getGlyph().getCenter();
                double dist = Math.abs(center.getY() - point.getY());

                if (dist < bestDist) {
                    bestDist = dist;
                    bestLedger = iLedger;
                }
            }
        }

        return bestLedger;
    }

    //----------------//
    // getClosestLine //
    //----------------//
    /**
     * Report the staff line which is closest to the provided point.
     *
     * @param point the provided point
     * @return the closest line found
     */
    public LineInfo getClosestLine (Point2D point)
    {
        double pos = pitchPositionOf(point);
        int idx = (int) Math.rint((pos + (lines.size() - 1)) / 2);

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
        List<ClefInter> clefs = new ArrayList<ClefInter>();
        clefs.add((ClefInter) lastClef);

        for (Relation rel : excs) {
            Inter inter = Graphs.getOppositeVertex(sig, rel, lastClef);

            if (inter instanceof ClefInter) {
                ClefInter clef = (ClefInter) inter;

                if ((clef.getStaff() == this) && !clefs.contains(clef)) {
                    clefs.add(clef);
                }
            }
        }

        return clefs;
    }

    //--------------------//
    // getLedgerLineIndex //
    //--------------------//
    /**
     * Compute staff-based line index, based on provided pitch position
     *
     * @param pitchPosition the provided pitch position
     * @return the computed line index
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
        if (lineIndex > 0) {
            return 4 + (2 * lineIndex);
        } else {
            return -4 + (2 * lineIndex);
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
        List<Double> slopes = new ArrayList<Double>(lines.size());

        for (LineInfo l : lines) {
            StaffFilament line = (StaffFilament) l;
            slopes.add(line.getSlopeAt(line.getEndPoint(side).getX(), Orientation.HORIZONTAL));
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
        if ((header.keyRange != null) && header.keyRange.valid) {
            return header.keyRange.getStop();
        }

        if (header.key != null) {
            Rectangle bounds = header.key.getBounds();

            return (bounds.x + bounds.width) - 1;
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
     * @return the number of lines (6, 4, ...)
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

    //-----------------//
    // getNotePosition //
    //-----------------//
    /**
     * Report the precise position for a note-like entity with respect
     * to this staff, taking ledgers (if any) into account.
     *
     * @param point the absolute location of the provided note
     * @return the detailed note position
     */
    public NotePosition getNotePosition (Point2D point)
    {
        double pitch = pitchPositionOf(point);
        IndexedLedger bestLedger = null;

        // If we are rather far from the staff, try getting help from ledgers
        if (Math.abs(pitch) > lines.size()) {
            bestLedger = getClosestLedger(point);

            if (bestLedger != null) {
                Point2D center = bestLedger.ledger.getGlyph().getCenter();
                int ledgerPitch = getLedgerPitchPosition(bestLedger.index);
                double deltaPitch = (2.0 * (point.getY() - center.getY())) / specificInterline;
                pitch = ledgerPitch + deltaPitch;
            }
        }

        return new NotePosition(this, pitch, bestLedger);
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
     * Report the barline, if any, at the desired extremum side of this staff.
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
        if ((header.timeRange != null) && header.timeRange.valid) {
            return header.timeRange.getStop();
        }

        if (header.time != null) {
            Rectangle bounds = header.time.getBounds();

            return (bounds.x + bounds.width) - 1;
        }

        return null;
    }

    //---------//
    // isDummy //
    //---------//
    public boolean isDummy ()
    {
        return dummy;
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

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    /**
     * Compute an approximation of the pitch position of a pixel point, since it is
     * based only on distance to staff, with no consideration for ledgers.
     *
     * @param pt the pixel point
     * @return the pitch position
     */
    public double pitchPositionOf (Point2D pt)
    {
        double top = getFirstLine().yAt(pt.getX());
        double bottom = getLastLine().yAt(pt.getX());

        return ((lines.size() - 1) * ((2 * pt.getY()) - bottom - top)) / (bottom - top);
    }

    //-----------------//
    // pitchToOrdinate //
    //-----------------//
    /**
     * Report the absolute ordinate for a staff-related pitch at a given abscissa.
     *
     * @param x     provided absolute abscissa
     * @param pitch pitch value WRT this staff
     * @return the corresponding absolute ordinate
     */
    public double pitchToOrdinate (double x,
                                   double pitch)
    {
        double top = getFirstLine().yAt(x);
        double bottom = getLastLine().yAt(x);

        return 0.5 * (top + bottom + ((pitch * (bottom - top)) / (lines.size() - 1)));
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
        for (Iterator<Entry<HorizontalSide, BarlineInter>> it = sideBars.entrySet().iterator();
                it.hasNext();) {
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
        final boolean showPoints = constants.showDefiningPoints.isSet();
        final Scale scale = system.getSheet().getScale();
        final double pointWidth = scale.toPixelsDouble(constants.definingPointSize);

        // Draw each staff line
        for (LineInfo line : lines) {
            line.renderLine(g, showPoints, pointWidth);
        }

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
    public Staff replicate ()
    {
        Staff replicate = new Staff(0, left, right, specificInterline, null);

        return replicate;
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
    public void setArea (Area area)
    {
        this.area = area;

        ///addAttachment("staff-area-" + id, area);
    }

    //-------------//
    // setBarlines //
    //-------------//
    /**
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
    public void setDummy ()
    {
        dummy = true;
    }

    //-----------//
    // setHeader //
    //-----------//
    /**
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

    //------------//
    // setKeyStop //
    //------------//
    /**
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
     * @param timeStop the timeStop to set
     */
    public void setTimeStop (Integer timeStop)
    {
        header.timeRange.setStop(timeStop);
        header.timeRange.valid = true;
    }

    //--------------------//
    // showDefiningPoints //
    //--------------------//
    public static Boolean showDefiningPoints ()
    {
        return constants.showDefiningPoints.isSet();
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
        if (getFirstLine() instanceof StaffLine) {
            logger.error("Staff lines have already been simplified!");

            return null;
        }

        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        List<LineInfo> copies = new ArrayList<LineInfo>(lines);
        lines.clear();

        for (LineInfo line : copies) {
            StaffFilament staffFilament = (StaffFilament) line;
            StaffLine staffLine = staffFilament.toStaffLine(glyphIndex);
            lines.add(staffLine);
        }

        return copies;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Staff");

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

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        if (!ledgerMap.isEmpty()) {
            // Populate ledgersValue from ledgerMap
            ledgersValue = new ArrayList<LedgersEntry>();

            for (Entry<Integer, List<LedgerInter>> entry : ledgerMap.entrySet()) {
                ledgersValue.add(new LedgersEntry(entry.getKey(), entry.getValue()));
            }
        }
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static class Adapter
            extends XmlAdapter<Integer, Staff>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Integer marshal (Staff staff)
                throws Exception
        {
            return staff.getId();
        }

        @Override
        public Staff unmarshal (Integer id)
                throws Exception
        {
            return null; // Handled later
        }
    }

    //---------------//
    // IndexedLedger //
    //---------------//
    /**
     * This combines the ledger with the index relative to the
     * hosting staff.
     */
    public static class IndexedLedger
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The ledger. */
        public final LedgerInter ledger;

        /** Staff-based line index. (-1, -2, ... above, +1, +2, ... below) */
        public final int index;

        //~ Constructors ---------------------------------------------------------------------------
        public IndexedLedger (LedgerInter ledger,
                              int index)
        {
            this.ledger = ledger;
            this.index = index;
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
        //~ Static fields/initializers -------------------------------------------------------------

        /** Predefined place holders. */
        private static ConcurrentHashMap<Integer, StaffHolder> holders = new ConcurrentHashMap<Integer, StaffHolder>();

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create a place holder
         *
         * @param id staff ID to remember
         */
        public StaffHolder (int id)
        {
            super(id);
        }

        //~ Methods --------------------------------------------------------------------------------
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean showDefiningPoints = new Constant.Boolean(
                false,
                "Should we show defining points?");

        private final Scale.Fraction definingPointSize = new Scale.Fraction(
                0.05,
                "Display width of a defining point");
    }

    //--------------//
    // LedgersEntry //
    //--------------//
    /**
     * This temporary structure is needed to marshall / unmarshall the ledgerMap,
     * because of its use of IDREF's.
     */
    private static class LedgersEntry
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlAttribute
        private final int index;

        @XmlList
        @XmlIDREF
        @XmlValue
        private final List<LedgerInter> ledgers;

        //~ Constructors ---------------------------------------------------------------------------
        // Needed for JAXB
        public LedgersEntry ()
        {
            this.index = 0;
            this.ledgers = null;
        }

        public LedgersEntry (int index,
                             List<LedgerInter> ledgers)
        {
            this.index = index;
            this.ledgers = ledgers;
        }
    }
}
