//----------------------------------------------------------------------------//
//                                                                            //
//                             S t a f f I n f o                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.facets.Glyph;
import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.math.GeoPath;
import omr.math.LineUtil;
import omr.math.ReversePathIterator;

import omr.run.Orientation;

import omr.score.entity.Staff;

import omr.sheet.NotePosition;
import omr.sheet.Scale;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code StaffInfo} handles the physical informations of a staff
 * with its lines.
 * Note: All methods are meant to provide correct results, regardless of the
 * actual number of lines in the staff instance.
 *
 * @author Hervé Bitteur
 */
public class StaffInfo
        implements AttachmentHolder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(StaffInfo.class);

    /** To sort by staff id. */
    public static final Comparator<StaffInfo> byId = new Comparator<StaffInfo>()
    {
        @Override
        public int compare (StaffInfo o1,
                            StaffInfo o2)
        {
            return Integer.compare(o1.id, o2.id);
        }
    };

    //~ Instance fields --------------------------------------------------------
    //
    /** Sequence of the staff lines. (from top to bottom) */
    private final List<LineInfo> lines;

    /**
     * Scale specific to this staff. [not used actually]
     * (since different staves in a page may exhibit different scales)
     */
    private Scale specificScale;

    /** Top limit of staff related area. (left to right) */
    private GeoPath topLimit = null;

    /** Bottom limit of staff related area. (left to right) */
    private GeoPath bottomLimit = null;

    /** Staff id. counted from 1 within the sheet */
    private final int id;

    /** Information about left bar line. */
    private BarInfo leftBar;

    /** Left extrema. */
    private double left;

    /** Information about right bar line. */
    private BarInfo rightBar;

    /** Right extrema. */
    private double right;

    /** The area around the staff, lazily computed. */
    private GeoPath area;

    /** Map of ledgers nearby. */
    private final Map<Integer, SortedSet<Glyph>> ledgerMap = new TreeMap<>();

    /** Corresponding staff entity in the score hierarchy. */
    private Staff scoreStaff;

    /** Potential attachments. */
    private AttachmentHolder attachments = new BasicAttachmentHolder();

    //~ Constructors -----------------------------------------------------------
    //
    //-----------//
    // StaffInfo //
    //-----------//
    /**
     * Create info about a staff, with its contained staff lines.
     *
     * @param id            the id of the staff
     * @param left          abscissa of the left side
     * @param right         abscissa of the right side
     * @param specificScale specific scale detected for this staff
     * @param lines         the sequence of contained staff lines
     */
    public StaffInfo (int id,
                      double left,
                      double right,
                      Scale specificScale,
                      List<LineInfo> lines)
    {
        this.id = id;
        this.left = (int) Math.rint(left);
        this.right = (int) Math.rint(right);
        this.specificScale = specificScale;
        this.lines = lines;
    }

    //~ Methods ----------------------------------------------------------------
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
     * Add a ledger to the collection (which is lazily created)
     *
     * @param ledger the ledger to add
     * @param index  the staff-based index for ledger line
     */
    public void addLedger (Glyph ledger,
                           int index)
    {
        if (ledger == null) {
            throw new IllegalArgumentException("Cannot register a null ledger");
        }

        SortedSet<Glyph> ledgerSet = ledgerMap.get(index);

        if (ledgerSet == null) {
            ledgerSet = new TreeSet<>(Glyph.byAbscissa);
            ledgerMap.put(index, ledgerSet);
        }

        ledgerSet.add(ledger);
    }

    //-----------//
    // addLedger //
    //-----------//
    /**
     * Add a ledger to the collection, computing line index from
     * glyph pitch position.
     *
     * @param ledger the ledger to add
     */
    public void addLedger (Glyph ledger)
    {
        if (ledger == null) {
            throw new IllegalArgumentException("Cannot register a null ledger");
        }

        addLedger(ledger,
                getLedgerLineIndex(pitchPositionOf(ledger.getCentroid())));
    }

    //--------------//
    // removeLedger //
    //--------------//
    /**
     * Remove a legder from staff collection.
     *
     * @param ledger the ledger to remove
     * @return true if actually removed, false if not found
     */
    public boolean removeLedger (Glyph ledger)
    {
        if (ledger == null) {
            throw new IllegalArgumentException("Cannot remove a null ledger");
        }

        // Browse all staff ledger indices
        for (SortedSet<Glyph> ledgerSet : ledgerMap.values()) {
            if (ledgerSet.remove(ledger)) {
                return true;
            }
        }

        // Not found
        logger.debug("Could not find ledger {}", ledger.idString());
        return false;
    }

    //------//
    // dump //
    //------//
    /**
     * A utility meant for debugging.
     */
    public void dump ()
    {
        System.out.println(
                "StaffInfo" + getId() + " left=" + left + " right=" + right);

        int i = 0;

        for (LineInfo line : lines) {
            System.out.println(" LineInfo" + i++ + " " + line.toString());
        }
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
    public double getAbscissa (HorizontalSide side)
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
     * Report the lazily computed area defined by the staff limits.
     *
     * @return the whole staff area
     */
    public GeoPath getArea ()
    {
        if (area == null) {
            area = new GeoPath();
            area.append(topLimit, false);
            area.append(
                    ReversePathIterator.getReversePathIterator(bottomLimit),
                    true);
            area.closePath();
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

    //--------//
    // getBar //
    //--------//
    /**
     * Report the barline, if any, on the provided side
     *
     * @param side proper horizontal side
     * @return the bar on the provided side, if any
     */
    public BarInfo getBar (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftBar;
        } else {
            return rightBar;
        }
    }

    //------------------//
    // getClosestLedger //
    //------------------//
    /**
     * Report the closest ledger (if any) between provided point and
     * this staff.
     *
     * @param point the provided point
     * @return the closest ledger found, or null
     */
    public IndexedLedger getClosestLedger (Point2D point)
    {
        IndexedLedger bestLedger = null;
        double top = getFirstLine().yAt(point.getX());
        double bottom = getLastLine().yAt(point.getX());
        double rawPitch = (4.0d * ((2 * point.getY()) - bottom - top)) / (bottom
                                                                          - top);

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
            searchBox = new Rectangle2D.Double(
                    point.getX(),
                    bottom,
                    0,
                    point.getY() - bottom + 1);
        }

        //searchBox.grow(interline, interline);
        searchBox.setRect(
                searchBox.getX() - interline,
                searchBox.getY() - interline,
                searchBox.getWidth() + (2 * interline),
                searchBox.getHeight() + (2 * interline));

        // Browse all staff ledgers
        Set<IndexedLedger> foundLedgers = new HashSet<>();
        for (Map.Entry<Integer, SortedSet<Glyph>> entry : ledgerMap.entrySet()) {
            for (Glyph ledger : entry.getValue()) {
                if (ledger.getBounds().intersects(searchBox)) {
                    foundLedgers.add(new IndexedLedger(ledger, entry.getKey()));
                }
            }
        }

        if (!foundLedgers.isEmpty()) {
            // Use the closest ledger
            double bestDist = Double.MAX_VALUE;

            for (IndexedLedger iLedger : foundLedgers) {
                Point2D center = iLedger.glyph.getAreaCenter();
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

    //----------//
    // getGapTo //
    //----------//
    /**
     * Report the vertical gap between staff and the provided glyph.
     *
     * @param glyph the provided glyph
     * @return 0 if the glyph intersects the staff, otherwise the vertical
     *         distance from staff to closest edge of the glyph
     */
    public int getGapTo (Glyph glyph)
    {
        Point center = glyph.getAreaCenter();
        int staffTop = getFirstLine().yAt(center.x);
        int staffBot = getLastLine().yAt(center.x);
        int glyphTop = glyph.getBounds().y;
        int glyphBot = glyphTop + glyph.getBounds().height - 1;

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
            FilamentLine line = (FilamentLine) l;
            slopes.add(line.getSlope(side));
        }

        Collections.sort(
                slopes,
                new Comparator<Double>()
        {
            @Override
            public int compare (Double o1,
                                Double o2)
            {
                return Double.compare(Math.abs(o1), Math.abs(o2));
            }
        });

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
     * Report the staff id, counted from 1 in the sheet.
     *
     * @return the staff id
     */
    public int getId ()
    {
        return id;
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

    //--------------//
    // getLedgerMap //
    //--------------//
    public Map<Integer, SortedSet<Glyph>> getLedgerMap ()
    {
        return ledgerMap;
    }

    //------------//
    // getLedgers //
    //------------//
    /**
     * Report the ordered set of ledgers, if any, for a given pitch value.
     *
     * @param lineIndex the precise line index that specifies algebraic
     *                  distance from staff
     * @return the proper set of ledgers, or null
     */
    public SortedSet<Glyph> getLedgers (int lineIndex)
    {
        return ledgerMap.get(lineIndex);
    }

    //-------------//
    // getLimitAtX //
    //-------------//
    /**
     * Report the precise ordinate of staff area limit, on the provided
     * vertical side.
     *
     * @param side the provided vertical side
     * @param x    the provided abscissa
     * @return the ordinate of staff limit
     */
    public double getLimitAtX (VerticalSide side,
                               double x)
    {
        GeoPath limit = (side == TOP) ? topLimit : bottomLimit;

        return limit.yAtX(x);
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
                linesRight = Math.max(
                        linesRight,
                        line.getEndPoint(RIGHT).getX());
            }

            return linesRight;
        }
    }

    //----------------//
    // getMidOrdinate //
    //----------------//
    /**
     * Report an approximate ordinate of staff ending, on the provided
     * horizontal side.
     *
     * @param side provided side
     * @return the middle ordinate of staff ending
     */
    public double getMidOrdinate (HorizontalSide side)
    {
        return (getFirstLine().getEndPoint(side).getY() + getLastLine().
                getEndPoint(side).getY()) / 2;
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
                Point2D center = bestLedger.glyph.getAreaCenter();
                int ledgerPitch = getLedgerPitchPosition(bestLedger.index);
                double deltaPitch = (2d * (point.getY() - center.getY())) / specificScale.
                        getInterline();
                pitch = ledgerPitch + deltaPitch;
            }
        }

        return new NotePosition(this, pitch, bestLedger);
    }

    //------------------------//
    // getLedgerPitchPosition //
    //------------------------//
    /**
     * Report the pitch position of a ledger WRT the related staff
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

    //---------------//
    // getScoreStaff //
    //---------------//
    /**
     * Report the related score staff entity.
     *
     * @return the corresponding scoreStaff
     */
    public Staff getScoreStaff ()
    {
        return scoreStaff;
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

    //--------------//
    // intersection //
    //--------------//
    /**
     * Report the approximate point where a provided vertical stick
     * crosses this staff.
     *
     * @param stick the rather vertical stick
     * @return the crossing point
     */
    public Point2D intersection (Glyph stick)
    {
        LineInfo midLine = lines.get(lines.size() / 2);

        return LineUtil.intersection(
                midLine.getEndPoint(LEFT),
                midLine.getEndPoint(RIGHT),
                stick.getStartPoint(Orientation.VERTICAL),
                stick.getStopPoint(Orientation.VERTICAL));
    }

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    /**
     * Compute an approximation of the pitch position of a pixel point,
     * since it is based only on distance to staff, with no
     * consideration for ledgers.
     *
     * @param pt the pixel point
     * @return the pitch position
     */
    public double pitchPositionOf (Point2D pt)
    {
        double top = getFirstLine().yAt(pt.getX());
        double bottom = getLastLine().yAt(pt.getX());

        return ((lines.size() - 1) * ((2 * pt.getY()) - bottom - top)) / (bottom
                                                                          - top);
    }

    //-------------------//
    // removeAttachments //
    //-------------------//
    @Override
    public int removeAttachments (String prefix)
    {
        return attachments.removeAttachments(prefix);
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

        if ((firstLine != null) && (lastLine != null)) {
            if (g.getClipBounds().intersects(getAreaBounds())) {
                // Draw the left and right vertical lines
                for (HorizontalSide side : HorizontalSide.values()) {
                    Point2D first = firstLine.getEndPoint(side);
                    Point2D last = lastLine.getEndPoint(side);
                    g.draw(new Line2D.Double(first, last));
                }

                // Draw each horizontal line in the set
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
                             double val)
    {
        if (side == HorizontalSide.LEFT) {
            left = val;
        } else {
            right = val;
        }
    }

    //--------//
    // setBar //
    //--------//
    /**
     * Set a barline on the provided side
     *
     * @param side proper horizontal side
     * @param bar  the bar to set
     */
    public void setBar (HorizontalSide side,
                        BarInfo bar)
    {
        if (side == HorizontalSide.LEFT) {
            this.leftBar = bar;
        } else {
            this.rightBar = bar;
        }
    }

    //----------//
    // setLimit //
    //----------//
    /**
     * Define the limit of the staff area, on the provided vertical side.
     *
     * @param side  proper vertical side
     * @param limit assigned limit
     */
    public void setLimit (VerticalSide side,
                          GeoPath limit)
    {
        logger.debug("staff#{} setLimit {} {}", id, side, limit);

        if (side == TOP) {
            topLimit = limit;
        } else {
            bottomLimit = limit;
        }

        // Invalidate area, so that it gets recomputed when needed
        area = null;
    }

    //---------------//
    // setScoreStaff //
    //---------------//
    /**
     * Remember the related score staff entity.
     *
     * @param scoreStaff the corresponding scoreStaff to set
     */
    public void setScoreStaff (Staff scoreStaff)
    {
        this.scoreStaff = scoreStaff;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{StaffInfo");

        sb.append(" id=").append(getId());
        sb.append(" left=").append((float) left);
        sb.append(" right=").append((float) right);

        if (specificScale != null) {
            sb.append(" specificScale=").append(specificScale.getInterline());
        }

        if (leftBar != null) {
            sb.append(" leftBar:").append(leftBar);
        }

        if (rightBar != null) {
            sb.append(" rightBar:").append(rightBar);
        }

        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // IndexedLedger //
    //---------------//
    public static class IndexedLedger
    {

        /** The ledger glyph. */
        public final Glyph glyph;

        /** Staff-based line index. (-1, -2, ... above, +1, +2, ... below) */
        public final int index;

        public IndexedLedger (Glyph ledger,
                              int index)
        {
            this.glyph = ledger;
            this.index = index;
        }
    }
}
