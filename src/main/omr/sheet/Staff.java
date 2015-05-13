//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S t a f f                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.math.GeoUtil;
import omr.math.Population;

import omr.sheet.grid.FilamentLine;
import omr.sheet.grid.LineInfo;
import omr.sheet.grid.StaffPeak;
import omr.sheet.header.StaffHeader;
import omr.sheet.note.NotePosition;

import omr.sig.inter.BarlineInter;
import omr.sig.inter.Inter;
import omr.sig.inter.LedgerInter;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code Staff} handles physical information of a staff with its lines.
 * <p>
 * Note: All methods are meant to provide correct results, regardless of the actual number of lines
 * in the staff instance.
 *
 * @author Hervé Bitteur
 */
public class Staff
        implements AttachmentHolder
{
    //~ Static fields/initializers -----------------------------------------------------------------

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
    /** To flag a dummy staff. */
    private boolean dummy;

    /**
     * Area around the staff.
     * The same area strategy applies for staves and for systems:
     * The purpose is to contain relevant entities (sections, glyphs) for the staff at hand but a
     * given entity may be contained by several staff areas when it is located in the inter-staff
     * gutter.
     * There is no need to be very precise, but a staff line cannot belong to several staff areas.
     * Horizontally, the area is extended half way to the next staff if any, otherwise to the limit
     * of the page.
     * Vertically, the area is extended to the first line (exclusive) of next staff if any,
     * otherwise to the limit of the page.
     */
    private Area area;

    /** Staff id. (counted from 1 within the sheet) */
    private final int id;

    /** Sequence of staff lines. (from top to bottom) */
    private final List<FilamentLine> lines;

    /**
     * Scale specific to this staff. [not used for the time being]
     * (since different staves in a page may exhibit different scales)
     */
    private final Scale specificScale;

    /** Left extrema. (beginning of lines) */
    private int left;

    /** Staff Header information. */
    private StaffHeader header;

    /** Right extrema. (end of lines) */
    private int right;

    /** Flag for short staff. (With a neighbor staff on left or right side) */
    private boolean isShort;

    /** Map of ledgers nearby. */
    private final SortedMap<Integer, SortedSet<LedgerInter>> ledgerMap = new TreeMap<Integer, SortedSet<LedgerInter>>();

    /** Sequence of brace / bracket / bar lines peaks kept. */
    private List<StaffPeak> peaks;

    /** Sequence of bar lines. */
    private List<BarlineInter> bars;

    /** Side bars, if any. */
    private final Map<HorizontalSide, BarlineInter> sideBars = new EnumMap<HorizontalSide, BarlineInter>(
            HorizontalSide.class);

    /** Containing system. */
    @Navigable(false)
    private SystemInfo system;

    /** Potential attachments. */
    private final AttachmentHolder attachments = new BasicAttachmentHolder();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create info about a staff, with its contained staff lines.
     *
     * @param id            the id of the staff
     * @param left          abscissa of the left side
     * @param right         abscissa of the right side
     * @param specificScale specific scale detected for this staff
     * @param lines         the sequence of contained staff lines
     */
    public Staff (int id,
                  double left,
                  double right,
                  Scale specificScale,
                  List<FilamentLine> lines)
    {
        this.id = id;
        this.left = (int) Math.rint(left);
        this.right = (int) Math.rint(right);
        this.specificScale = specificScale;
        this.lines = lines;
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        assert ledger != null : "Cannot add a null ledger";

        SortedSet<LedgerInter> ledgerSet = ledgerMap.get(index);

        if (ledgerSet == null) {
            ledgerSet = new TreeSet<LedgerInter>(Inter.byFullAbscissa);
            ledgerMap.put(index, ledgerSet);
        }

        ledgerSet.add(ledger);
    }

    //-----------//
    // addLedger //
    //-----------//
    /**
     * Add a ledger glyph to the collection, computing line index from
     * glyph pitch position.
     *
     * @param ledger the ledger glyph to add
     */
    public void addLedger (LedgerInter ledger)
    {
        assert ledger != null : "Cannot add a null ledger";

        addLedger(ledger, getLedgerLineIndex(pitchPositionOf(ledger.getGlyph().getCentroid())));
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
        return area.getBounds2D();
    }

    //----------------//
    // getAttachments //
    //----------------//
    @Override
    public Map<String, Shape> getAttachments ()
    {
        return attachments.getAttachments();
    }

    //---------//
    // getBars //
    //---------//
    /**
     * @return the bars
     */
    public List<BarlineInter> getBars ()
    {
        return Collections.unmodifiableList(bars);
    }

    /**
     * @return the clefStop
     */
    public Integer getClefStop ()
    {
        if (header.clefRange.valid) {
            return header.clefRange.stop;
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

        int interline = specificScale.getInterline();
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
                searchBox.getX() - interline,
                searchBox.getY() - interline,
                searchBox.getWidth() + (2 * interline),
                searchBox.getHeight() + (2 * interline));

        // Browse all staff ledgers
        Set<IndexedLedger> foundLedgers = new HashSet<IndexedLedger>();

        for (Map.Entry<Integer, SortedSet<LedgerInter>> entry : ledgerMap.entrySet()) {
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
                Point2D center = iLedger.ledger.getGlyph().getAreaCenter();
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
    public FilamentLine getClosestLine (Point2D point)
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
            FilamentLine line = (FilamentLine) l;
            slopes.add(line.getSlope(side));
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
    public FilamentLine getFirstLine ()
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
     * @return the dmzStart
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
        return getSpecificScale().getInterline() * (lines.size() - 1);
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

    /**
     * @return the keyStop
     */
    public Integer getKeyStop ()
    {
        if (header.keyRange.valid) {
            return header.keyRange.stop;
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
    public FilamentLine getLastLine ()
    {
        return lines.get(lines.size() - 1);
    }

    //--------------//
    // getLedgerMap //
    //--------------//
    public SortedMap<Integer, SortedSet<LedgerInter>> getLedgerMap ()
    {
        return ledgerMap;
    }

    //------------//
    // getLedgers //
    //------------//
    /**
     * Report the ordered set of ledgers, if any, for a given index.
     *
     * @param lineIndex the precise line index that specifies algebraic
     *                  distance from staff
     * @return the proper abscissa-ordered set of ledgers, or null
     */
    public SortedSet<LedgerInter> getLedgers (int lineIndex)
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
    public FilamentLine getLine (VerticalSide side)
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
    public List<FilamentLine> getLines ()
    {
        return lines;
    }

    //-------------//
    // getLinesEnd //
    //-------------//
    /**
     * Report the ending abscissa of the staff lines.
     *
     * @param side desired horizontal side
     * @return the abscissa corresponding to lines extrema
     */
    public double getLinesEnd (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            double linesLeft = Integer.MAX_VALUE;

            for (LineInfo line : lines) {
                linesLeft = Math.min(linesLeft, line.getEndPoint(LEFT).getX());
            }

            return linesLeft;
        } else {
            double linesRight = Integer.MIN_VALUE;

            for (LineInfo line : lines) {
                linesRight = Math.max(linesRight, line.getEndPoint(RIGHT).getX());
            }

            return linesRight;
        }
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

        int dx = specificScale.getInterline();
        int xMin = getAbscissa(LEFT);
        int xMax = getAbscissa(RIGHT);

        for (double x = xMin; x <= xMax; x += dx) {
            double prevY = -1;

            for (FilamentLine line : lines) {
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
                Point2D center = bestLedger.ledger.getGlyph().getAreaCenter();
                int ledgerPitch = getLedgerPitchPosition(bestLedger.index);
                double deltaPitch = (2d * (point.getY() - center.getY())) / specificScale.getInterline();
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
        return system.getPartOf(this);
    }

    //----------//
    // getPeaks //
    //----------//
    /**
     * @return the peaks
     */
    public List<StaffPeak> getPeaks ()
    {
        return Collections.unmodifiableList(peaks);
    }

    //------------//
    // getSideBar //
    //------------//
    public BarlineInter getSideBar (HorizontalSide side)
    {
        return sideBars.get(side);
    }

    //------------------//
    // getSpecificScale //
    //------------------//
    /**
     * Report the <b>specific</b> staff scale, which may have a
     * different interline value than the page average.
     *
     * @return the staff scale
     */
    public Scale getSpecificScale ()
    {
        if (specificScale != null) {
            // Return the specific scale of this staff
            return specificScale;
        } else {
            // Return the scale of the sheet
            logger.warn("No specific scale available");

            return null;
        }
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * @return the system
     */
    public SystemInfo getSystem ()
    {
        return system;
    }

    /**
     * @return the timeStop
     */
    public Integer getTimeStop ()
    {
        if (header.timeRange.valid) {
            return header.timeRange.stop;
        }

        return null;
    }

    //-----------------//
    // insertBracePeak //
    //-----------------//
    public void insertBracePeak (StaffPeak.Brace bracePeak)
    {
        peaks.add(0, bracePeak);
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

    //-----------//
    // removeBar //
    //-----------//
    /**
     * Remove the provided instance of Barline from internal staff collection.
     *
     * @param bar the provided bar to remove
     * @return true if actually removed
     */
    public boolean removeBar (BarlineInter bar)
    {
        // Purge sideBars if needed
        for (Iterator<Entry<HorizontalSide, BarlineInter>> it = sideBars.entrySet().iterator();
                it.hasNext();) {
            Entry<HorizontalSide, BarlineInter> entry = it.next();

            if (entry.getValue() == bar) {
                it.remove();
            }
        }

        // Purge bars
        return bars.remove(bar);
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
        assert ledger != null : "Cannot remove a null ledger";

        // Browse all staff ledger indices
        for (SortedSet<LedgerInter> ledgerSet : ledgerMap.values()) {
            if (ledgerSet.remove(ledger)) {
                return true;
            }
        }

        // Not found
        logger.debug("Could not find ledger {}", ledger);

        return false;
    }

    //-------------//
    // removePeaks //
    //-------------//
    public void removePeaks (Collection<? extends StaffPeak> toRemove)
    {
        peaks.removeAll(toRemove);
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the staff lines.
     *
     * @param g the graphics context
     * @return true if something has been actually drawn
     */
    public boolean render (Graphics2D g)
    {
        LineInfo firstLine = getFirstLine();
        LineInfo lastLine = getLastLine();

        if ((firstLine != null) && (lastLine != null) && (area != null)) {
            Rectangle clip = g.getClipBounds();

            if ((clip == null) || clip.intersects(getAreaBounds())) {
                // Draw each staff line
                for (LineInfo line : lines) {
                    line.render(g);
                }

                return true;
            }
        }

        return false;
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
        Staff replicate = new Staff(0, left, right, null, null);

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
    }

    //-------------//
    // setBarPeaks //
    //-------------//
    /**
     * @param peaks the peaks to set
     */
    public void setBarPeaks (List<StaffPeak> peaks)
    {
        this.peaks = peaks;
    }

    //---------//
    // setBars //
    //---------//
    /**
     * @param bars the bars to set
     */
    public void setBars (List<BarlineInter> bars)
    {
        this.bars = bars;
    }

    /**
     * @param clefStop the clefStop to set
     */
    public void setClefStop (int clefStop)
    {
        header.clefRange.stop = clefStop;
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

    /**
     * @param keyStop the keyStop to set
     */
    public void setKeyStop (Integer keyStop)
    {
        header.keyRange.stop = keyStop;
        header.keyRange.valid = true;
    }

    //------------//
    // setSideBar //
    //------------//
    public void setSideBar (HorizontalSide side,
                            BarlineInter inter)
    {
        sideBars.put(side, inter);
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

    /**
     * @param timeStop the timeStop to set
     */
    public void setTimeStop (Integer timeStop)
    {
        header.timeRange.stop = timeStop;
        header.timeRange.valid = true;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Staff");

        sb.append(" id=").append(getId());

        if (isShort) {
            sb.append(" SHORT");
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
        final double thisTop = this.getFirstLine().getLeftPoint().getY();
        final double thatTop = that.getFirstLine().getLeftPoint().getY();
        final double commonTop = Math.max(thisTop, thatTop);

        final double thisBottom = this.getLastLine().getLeftPoint().getY();
        final double thatBottom = that.getLastLine().getLeftPoint().getY();
        final double commonBottom = Math.min(thisBottom, thatBottom);

        return commonBottom > commonTop;
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
    void setShort ()
    {
        isShort = true;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
}
