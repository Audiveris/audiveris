//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t a f f M a n a g e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.constant.ConstantSet;

import omr.math.GeoPath;
import omr.math.NaturalSpline;
import omr.math.ReversePathIterator;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code StaffManager} handles physical information about all the staves of a
 * sheet.
 * <p>
 * It must be able to correctly handle the sequence of staves even in complex
 * configurations like the following one (referred to as "layout order"):
 * <pre>
 * +-------+
 * |   1   |
 * |   2   |
 * +---+---+
 * | 3 | 5 |
 * | 4 | 6 |
 * +---+---+
 * |   7   |
 * |   8   |
 * +-------+
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class StaffManager
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StaffManager.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** The sequence of staves, ordered by layout position. */
    private final List<StaffInfo> staves = new ArrayList<StaffInfo>();

    //~ Constructors -------------------------------------------------------------------------------
    //
    //--------------//
    // StaffManager //
    //--------------//
    /**
     * Creates a new StaffManager object.
     *
     * @param sheet the related sheet
     */
    public StaffManager (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //
    //----------//
    // addStaff //
    //----------//
    /**
     * Append one staff to the current collection.
     * Staves are assumed to be added in layout order.
     *
     * @param staff the staff to add
     */
    public void addStaff (StaffInfo staff)
    {
        staves.add(staff);
    }

    //------------------//
    // computeStaffArea //
    //------------------//
    /**
     * Compute the staff area, whether systems are one under the other
     * or side by side.
     * <p>
     * Use horizontal staff slice and intersect it with area of containing
     * system.
     *
     * @param staff the staff to process
     */
    public void computeStaffArea (StaffInfo staff)
    {
        final int sheetWidth = sheet.getWidth();
        final int sheetHeight = sheet.getHeight();

        final List<StaffInfo> aboves = vertNeighbors(staff, TOP);
        final PathIterator north = aboves.isEmpty()
                ? new GeoPath(new Line2D.Double(0, 0, sheetWidth, 0)).getPathIterator(
                        null) : getGlobalLine(aboves, BOTTOM);

        final List<StaffInfo> belows = vertNeighbors(staff, BOTTOM);
        final PathIterator south = belows.isEmpty()
                ? new GeoPath(
                        new Line2D.Double(0, sheetHeight, sheetWidth, sheetHeight)).getPathIterator(null)
                : getGlobalLine(belows, TOP);

        // Define sheet-wide area
        GeoPath wholePath = new GeoPath();
        wholePath.append(north, false);
        wholePath.append(new ReversePathIterator(south), true);

        final Area area = new Area(wholePath);

        // Intersect with system width
        SystemInfo system = staff.getSystem();
        int left = system.getAreaEnd(LEFT);
        int right = system.getAreaEnd(RIGHT);

        if ((left != 0) || (right != sheetWidth)) {
            Rectangle slice = new Rectangle(left, 0, right - left, sheetHeight);
            area.intersect(new Area(slice));
        }

        staff.setArea(area);
    }

    //-------------------//
    // detectShortStaves //
    //-------------------//
    /**
     * Detect which staves are short ones, displayed side by side.
     */
    public void detectShortStaves ()
    {
        for (StaffInfo staff : staves) {
            if ((horiNeighbor(staff, LEFT) != null) || (horiNeighbor(staff, RIGHT) != null)) {
                staff.setShort();
            }
        }
    }

    //---------------//
    // getGlobalLine //
    //---------------//
    /**
     * Report a line which concatenates the corresponding
     * (first or last) lines of all provided staves
     * (assumed to be side by side).
     *
     * @param staffList the horizontal sequence of staves
     * @param side      the desired vertical side
     * @return iterator on the global line
     */
    public PathIterator getGlobalLine (List<StaffInfo> staffList,
                                       VerticalSide side)
    {
        if (staffList.isEmpty()) {
            return null;
        }

        final GeoPath globalLine = new GeoPath();

        // Point on left
        StaffInfo leftStaff = staffList.get(0);
        FilamentLine leftLine = (side == TOP) ? leftStaff.getFirstLine() : leftStaff.getLastLine();
        NaturalSpline leftSpline = (NaturalSpline) leftLine.getFilament().getLine();
        globalLine.moveTo(0, leftSpline.getFirstPoint().getY());

        // Proper line of each staff
        for (StaffInfo staff : staffList) {
            FilamentLine fLine = (side == TOP) ? staff.getFirstLine() : staff.getLastLine();
            globalLine.append(fLine.getFilament().getLine().toPath(), true);
        }

        // Point on right
        StaffInfo rightStaff = staffList.get(staffList.size() - 1);
        FilamentLine rightLine = (side == TOP) ? rightStaff.getFirstLine() : rightStaff.getLastLine();
        NaturalSpline rightSpline = (NaturalSpline) rightLine.getFilament().getLine();
        globalLine.lineTo(sheet.getWidth(), rightSpline.getLastPoint().getY());

        final int verticalMargin = sheet.getScale().toPixels(constants.verticalAreaMargin);
        AffineTransform at = AffineTransform.getTranslateInstance(
                0,
                ((side == TOP) ? (-verticalMargin) : verticalMargin));

        return globalLine.getPathIterator(at);
    }

    //------------//
    // getIndexOf //
    //------------//
    public int getIndexOf (StaffInfo staff)
    {
        return staves.indexOf(staff);
    }

    //----------//
    // getRange //
    //----------//
    /**
     * Report a view on the range of staves from first to last
     * (both inclusive).
     *
     * @param first the first staff of the range
     * @param last  the last staff of the range
     * @return a view on this range
     */
    public List<StaffInfo> getRange (StaffInfo first,
                                     StaffInfo last)
    {
        return staves.subList(getIndexOf(first), getIndexOf(last) + 1);
    }

    //----------//
    // getStaff //
    //----------//
    public StaffInfo getStaff (int index)
    {
        return staves.get(index);
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Report the staff, among the sequence provided, whose area
     * contains the provided point.
     *
     * @param point     the provided point
     * @param theStaves the staves sequence to search
     * @return the containing staff, or null if none found
     */
    public static StaffInfo getStaffAt (Point2D point,
                                        List<StaffInfo> theStaves)
    {
        final double x = point.getX();
        final double y = point.getY();

        for (StaffInfo staff : theStaves) {
            // If the point is ON the area boundary, it is NOT contained.
            // So we use a rectangle of 1x1 pixels
            if (staff.getArea().intersects(x, y, 1, 1)) {
                return staff;
            }
        }

        return null;
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Report the staff whose area contains the provided point
     *
     * @param point the provided point
     * @return the nearest staff, or null if none found
     */
    public StaffInfo getStaffAt (Point2D point)
    {
        return getStaffAt(point, staves);
    }

    //---------------//
    // getStaffCount //
    //---------------//
    /**
     * Report the total number of staves, whatever their containing
     * systems.
     *
     * @return the count of staves
     */
    public int getStaffCount ()
    {
        return staves.size();
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report an unmodifiable view (perhaps empty) of list of current
     * staves.
     *
     * @return a view on staves
     */
    public List<StaffInfo> getStaves ()
    {
        return Collections.unmodifiableList(staves);
    }

    //--------------//
    // horiNeighbor //
    //--------------//
    /**
     * Report the staff, if any, which is located on the desired
     * horizontal side of the current one.
     * <p>
     * On the layout example:
     * <pre>
     * +-------+
     * |   1   |
     * |   2   |
     * +---+---+
     * | 3 | 5 |
     * | 4 | 6 |
     * +---+---+
     * |   7   |
     * |   8   |
     * +-------+
     * - neighborOf(1, RIGHT) == null
     * - neighborOf(3, RIGHT) == 5
     * - neighborOf(6, LEFT) == 4
     * - neighborOf(7, LEFT) == null
     * </pre>
     *
     * @param current current staff
     * @param side    desired horizontal side
     * @return the neighboring staff if any, otherwise null
     */
    public StaffInfo horiNeighbor (StaffInfo current,
                                   HorizontalSide side)
    {
        final int idx = getIndexOf(current);
        final int dir = (side == LEFT) ? (-1) : 1;
        final int iBreak = (side == LEFT) ? (-1) : staves.size();

        // Pickup the one immediately on left (or right)
        for (int i = idx + dir; (dir * (iBreak - i)) > 0; i += dir) {
            StaffInfo s = getStaff(i);

            if (current.yOverlaps(s)) {
                return s;
            }
        }

        return null;
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint all the staff lines
     *
     * @param g the graphics context (including current color and stroke)
     */
    public void render (Graphics2D g)
    {
        for (StaffInfo staff : staves) {
            staff.renderAttachments(g);
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Empty the whole collection of staves.
     */
    public void reset ()
    {
        staves.clear();
    }

    //---------------//
    // vertNeighbors //
    //---------------//
    /**
     * Report the staves, if any, which are located immediately on the
     * desired vertical side of the current one.
     * <p>
     * On the layout example:
     * <pre>
     * +-------+
     * |   1   |
     * |   2   |
     * +---+---+
     * | 3 | 5 |
     * | 4 | 6 |
     * +---+---+
     * |   7   |
     * |   8   |
     * +-------+
     * - neighborsOf(1, TOP) == []
     * - neighborsOf(1, BOTTOM) == [2]
     * - neighborsOf(2, BOTTOM) == [3,5]
     * - neighborsOf(5, TOP) == [2]
     * - neighborsOf(6, TOP) == [3,5]
     * - neighborsOf(4, BOTTOM) == [7]
     * - neighborsOf(7, TOP) == [4,6]
     * - neighborsOf(7, BOTTOM) == []
     * </pre>
     *
     * @param current current staff
     * @param side    desired vertical side
     * @return the neighboring staves if any, otherwise an empty list
     */
    public List<StaffInfo> vertNeighbors (StaffInfo current,
                                          VerticalSide side)
    {
        final List<StaffInfo> neighbors = new ArrayList<StaffInfo>();
        final int idx = getIndexOf(current);
        final int dir = (side == TOP) ? (-1) : 1;
        final int iBreak = (side == TOP) ? (-1) : staves.size();
        StaffInfo other = null;

        // Pickup the one immediately above (or below)
        for (int i = idx + dir; (dir * (iBreak - i)) > 0; i += dir) {
            StaffInfo s = getStaff(i);

            if (current.xOverlaps(s)) {
                other = s;

                break;
            }
        }

        if (other != null) {
            // Pick up this first one, and its horizontal neighbors
            neighbors.add(other);

            for (HorizontalSide hSide : HorizontalSide.values()) {
                StaffInfo next = other;

                do {
                    next = horiNeighbor(next, hSide);

                    if (next != null) {
                        neighbors.add(next);
                    } else {
                        break;
                    }
                } while (true);
            }
        }

        Collections.sort(neighbors, StaffInfo.byId);

        return neighbors;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Scale.Fraction samplingDx = new Scale.Fraction(
                4d,
                "Abscissa sampling to compute top & bottom limits of staff areas");

        Scale.Fraction verticalAreaMargin = new Scale.Fraction(
                1.0,
                "Vertical margin on system & staff areas");
    }
}
