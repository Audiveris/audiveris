//----------------------------------------------------------------------------//
//                                                                            //
//                             S t a f f I n f o                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.GeoPath;
import omr.math.LineUtilities;
import omr.math.ReversePathIterator;

import omr.sheet.Dash;
import omr.sheet.Ledger;
import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

import java.awt.Graphics2D;
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
 * Class {@code StaffInfo} handles the physical informations of a staff with
 * its lines.
 *
 * @author Hervé Bitteur
 */
public class StaffInfo
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(StaffInfo.class);

    //~ Instance fields --------------------------------------------------------

    /** Sequence of the staff lines, from top to bottom */
    private final List<LineInfo> lines;

    /**
     * Scale specific to this staff, since different staves in a page may
     * exhibit different scales.
     */
    private Scale specificScale;

    /** Top limit of staff related area (left to right) */
    private GeoPath topLimit = null;

    /** Bottom limit of staff related area (left to right) */
    private GeoPath bottomLimit = null;

    /** For debug only */
    private final int id;

    /** Information about left bar line */
    private BarInfo leftBar;

    /** Left extrema */
    private double left;

    /** Information about right bar line */
    private BarInfo rightBar;

    /** Right extrema */
    private double right;

    /** The staff area */
    private GeoPath area;

    /** Ledgers nearby */
    private Map<Integer, SortedSet<Ledger>> ledgers;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // StaffInfo //
    //-----------//
    /**
     * Create info about a staff, with its contained staff lines
     *
     * @param id the id of the staff
     * @param left abscissa of the left side
     * @param right abscissa of the right side
     * @param specificScale specific scale detected for this staff
     * @param lines the sequence of contained staff lines
     */
    public StaffInfo (int            id,
                      double         left,
                      double         right,
                      Scale          specificScale,
                      List<LineInfo> lines)
    {
        this.id = id;
        this.left = (int) Math.rint(left);
        this.right = (int) Math.rint(right);
        this.specificScale = specificScale;
        this.lines = lines;
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // setAbscissa //
    //-------------//
    public void setAbscissa (HorizontalSide side,
                             double         val)
    {
        if (side == HorizontalSide.LEFT) {
            left = val;
        } else {
            right = val;
        }
    }

    //-------------//
    // getAbscissa //
    //-------------//
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
     * Report the lazily computed area defined by the staff limits
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
        }

        return area;
    }

    //---------------//
    // getAreaBounds //
    //---------------//
    /**
     * Report the bounding box of the staff area
     * @return the lazily computed bounding box
     */
    public Rectangle2D getAreaBounds ()
    {
        return getArea()
                   .getBounds2D();
    }

    //--------//
    // setBar //
    //--------//
    /**
     * @param side proper horizontal side
     * @param bar the bar to set
     */
    public void setBar (HorizontalSide side,
                        BarInfo        bar)
    {
        if (side == HorizontalSide.LEFT) {
            this.leftBar = bar;
        } else {
            this.rightBar = bar;
        }
    }

    //--------//
    // getBar //
    //--------//
    /**
     * @param side proper horizontal side
     * @return the leftBar
     */
    public BarInfo getBar (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftBar;
        } else {
            return rightBar;
        }
    }

    //----------------//
    // getEndingSlope //
    //----------------//
    /**
     * Discard highest and lowest absolute slopes
     * And return the average values for the remaining ones
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

        Collections.sort(
            slopes,
            new Comparator<Double>() {
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
     * Report the first line in the series
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
     * Report the mean height of the staff, between first and last line
     *
     * @return the mean staff height
     */
    public int getHeight ()
    {
        return getSpecificScale()
                   .interline() * (lines.size() - 1);
    }

    //-------//
    // getId //
    //-------//
    /**
     * @return the id
     */
    public int getId ()
    {
        return id;
    }

    //-------------//
    // getLastLine //
    //-------------//
    /**
     * Report the last line in the series
     *
     * @return the last line
     */
    public LineInfo getLastLine ()
    {
        return lines.get(lines.size() - 1);
    }

    //------------//
    // getLedgers //
    //------------//
    /**
     * Report the ordered set of ledgers, if any, for a given pitch value
     * @param lineIndex the precise line index that specifies algebraic
     * distance from staff
     * @return the proper set of ledgers, or null
     */
    public SortedSet<Ledger> getLedgers (int lineIndex)
    {
        if (ledgers != null) {
            return ledgers.get(lineIndex);
        } else {
            return null;
        }
    }

    //-------------------//
    // getLedgersToStaff //
    //-------------------//
    /**
     * Report the set of ledgers between the provided point and this staff.
     * TODO: Reimplement this.
     * @param pt the provided point
     * @param system the containing system
     * @return the set (perhaps empty) of ledgers found
     */
    public Set<Glyph> getLedgersToStaff (Point2D    pt,
                                         SystemInfo system)
    {
        Set<Glyph> ledgers = new HashSet<Glyph>();
        double     top = getFirstLine()
                             .yAt(pt.getX());
        double     bottom = getLastLine()
                                .yAt(pt.getX());
        double     rawPitch = (4.0d * ((2 * pt.getY()) - bottom - top)) / (bottom -
                                                                          top);

        if (Math.abs(rawPitch) <= 6) {
            return ledgers; // Empty so far
        }

        int         interline = specificScale.interline();
        Rectangle2D searchBox;

        if (rawPitch < 0) {
            searchBox = new Rectangle2D.Double(
                pt.getX(),
                pt.getY(),
                0,
                top - pt.getY() + 1);
        } else {
            searchBox = new Rectangle2D.Double(
                pt.getX(),
                bottom,
                0,
                pt.getY() - bottom + 1);
        }

        //searchBox.grow(interline / 2, interline / 2);
        searchBox.setRect(
            searchBox.getX() - (interline / 2),
            searchBox.getY() - (interline / 2),
            searchBox.getWidth() + interline,
            searchBox.getHeight() + interline);

        for (Glyph ledger : system.getLedgers()) {
            if (ledger.getContourBox()
                      .intersects(searchBox)) {
                ledgers.add(ledger);
            }
        }

        return ledgers;
    }

    //----------//
    // setLimit //
    //----------//
    /**
     * Define the limit at the bottom of the staff area
     *
     * @param side proper vertical side
     * @param limit assigned limit
     */
    public void setLimit (VerticalSide side,
                          GeoPath      limit)
    {
        if (side == TOP) {
            topLimit = limit;
        } else {
            bottomLimit = limit;
        }
    }

    //----------//
    // getLines //
    //----------//
    /**
     * Report the sequence of lines
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
     * Report the ending abscissa of the staff lines
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
    public double getMidOrdinate (HorizontalSide side)
    {
        return (getFirstLine()
                    .getEndPoint(side)
                    .getY() + getLastLine()
                                  .getEndPoint(side)
                                  .getY()) / 2;
    }

    //------------------//
    // getSpecificScale //
    //------------------//
    /**
     * Report the <b>specific</b> staff scale, which may have a different
     * interline value than the page average.
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
            logger.warning("No specific scale available");

            return null;
        }
    }

    //-----------//
    // addLedger //
    //-----------//
    /**
     * Add a ledger to the staff collection (which is lazily created)
     * @param ledger the ledger to add
     */
    public void addLedger (Ledger ledger)
    {
        if (ledger == null) {
            throw new IllegalArgumentException("Cannot register a null ledger");
        }

        if (ledgers == null) {
            ledgers = new TreeMap<Integer, SortedSet<Ledger>>();
        }

        int               pitch = ledger.getLineIndex();
        SortedSet<Ledger> ledgerSet = ledgers.get(pitch);

        if (ledgerSet == null) {
            ledgerSet = new TreeSet(Dash.abscissaComparator);
            ledgers.put(pitch, ledgerSet);
        }

        ledgerSet.add(ledger);
    }

    //---------//
    // cleanup //
    //---------//
    /**
     * Forward the cleaning order to each of the staff lines
     */
    public void cleanup ()
    {
        for (LineInfo line : lines) {
            line.cleanup();
        }
    }

    //------//
    // dump //
    //------//
    /**
     * A utility meant for debugging
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

    //--------------//
    // intersection //
    //--------------//
    /**
     * Report the approximate point where a provided vertical stick crosses this
     * staff
     * @param stick the rather vertical stick
     * @return the crossing point
     */
    public Point2D intersection (Glyph stick)
    {
        LineInfo midLine = lines.get(lines.size() / 2);

        return LineUtilities.intersection(
            midLine.getEndPoint(LEFT),
            midLine.getEndPoint(RIGHT),
            stick.getStartPoint(),
            stick.getStopPoint());
    }

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    /**
     * Compute an approximation of the pitch position of a pixel point, since it
     * is based only on distance to staff, with no consideration for ledgers.
     *
     * @param pt the pixel point
     * @return the pitch position
     */
    public double pitchPositionOf (Point2D pt)
    {
        double top = getFirstLine()
                         .yAt(pt.getX());
        double bottom = getLastLine()
                            .yAt(pt.getX());

        return (4.0d * ((2 * pt.getY()) - bottom - top)) / (bottom - top);
    }

    //------------------------//
    // precisePitchPositionOf //
    //------------------------//
    /**
     * Compute the precise integral pitch position of a pixel point, taking
     * ledgers into account for positions far from staff lines
     * TODO: Reimplement this feature, using the ledger map.
     *
     * @param pt the pixel point
     * @param system the containing system
     * @return the pitch position
     */
    public double precisePitchPositionOf (Point2D    pt,
                                          SystemInfo system)
    {
        double top = getFirstLine()
                         .yAt(pt.getX());
        double bottom = getLastLine()
                            .yAt(pt.getX());

        double raw = (4.0d * ((2 * pt.getY()) - bottom - top)) / (bottom - top);

        if (Math.abs(raw) <= 6) {
            return raw;
        }

        // Fallback to use of ledgers & interline value
        // Retrieve the closest ledger
        Set<Glyph> ledgers = getLedgersToStaff(pt, system);

        double     bestDist = Double.MAX_VALUE;
        Glyph      bestLedger = null;

        for (Glyph ledger : system.getLedgers()) {
            Point2D center = ledger.getAreaCenter();
            double  dist = Math.abs(center.getY() - pt.getY());

            if (dist < bestDist) {
                bestDist = dist;
                bestLedger = ledger;
            }
        }

        if (bestLedger == null) {
            return raw;
        }

        // Force an even position for the ledger
        Point2D center = bestLedger.getAreaCenter();
        int     ledgerPitch = 2 * (int) Math.rint(pitchPositionOf(center) / 2);
        int     deltaPitch = (int) Math.rint(
            (2d * (pt.getY() - center.getY())) / specificScale.interline());
        int     pitch = ledgerPitch + deltaPitch;

        if (logger.isFineEnabled()) {
            logger.fine(
                "Ledger#" + bestLedger.getId() + " deltaPitch:" + deltaPitch +
                " Precise pitch: " + pitch);
        }

        return pitch;
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the staff lines.
     *
     * @param g the graphics context
     * @return true if something has been drawn
     */
    public boolean render (Graphics2D g)
    {
        LineInfo firstLine = getFirstLine();
        LineInfo lastLine = getLastLine();

        if ((firstLine != null) && (lastLine != null)) {
            if (g.getClipBounds()
                 .intersects(getAreaBounds())) {
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

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on main parameters
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{StaffInfo")
          .append(" id=")
          .append(getId())
          .append(" left=")
          .append((float) left)
          .append(" right=")
          .append((float) right);

        if (specificScale != null) {
            sb.append(" specificScale=")
              .append(specificScale.interline());
        }

        if (leftBar != null) {
            sb.append(" leftBar:")
              .append(leftBar);
        }

        if (rightBar != null) {
            sb.append(" rightBar:")
              .append(rightBar);
        }

        sb.append("}");

        return sb.toString();
    }
}
