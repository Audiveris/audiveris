//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S t a f f M a n a g e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.GeoPath;
import omr.math.NaturalSpline;
import omr.math.ReversePathIterator;

import omr.sheet.grid.LineInfo;

import omr.ui.Colors;
import omr.ui.util.ItemRenderer;
import omr.ui.util.UIUtil;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.Navigable;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code StaffManager} handles physical information about all the real staves of
 * a sheet.
 * <p>
 * It knows nothing about the dummy staves (created in dummy parts).
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
        implements ItemRenderer
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
    private final List<Staff> staves = new ArrayList<Staff>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StaffManager object.
     *
     * @param sheet the related sheet
     */
    public StaffManager (Sheet sheet)
    {
        this.sheet = sheet;

        for (SystemInfo system : sheet.getSystems()) {
            staves.addAll(system.getStaves());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getClosestStaff //
    //-----------------//
    /**
     * Report the closest staff from the provided point, among the specified staves.
     *
     * @param point     the provided point
     * @param theStaves the list of staves to browse
     * @return the nearest staff, or null if none found
     */
    public static Staff getClosestStaff (Point2D point,
                                         List<Staff> theStaves)
    {
        // All staves whose area contains the provided point
        final List<Staff> found = getStavesOf(point, theStaves);

        switch (found.size()) {
        case 0:
            return null;

        case 1:
            return found.get(0);

        default:

            Staff bestStaff = null;
            double bestDist = Double.MAX_VALUE;

            for (Staff staff : found) {
                double dist = staff.distanceTo(point);

                if (dist < bestDist) {
                    bestDist = dist;
                    bestStaff = staff;
                }
            }

            return bestStaff;
        }
    }

    //-------------//
    // getCoreArea //
    //-------------//
    /**
     * Compute a staff core area limited to staff lines (staff height and staff width)
     * with additional margins in horizontal and vertical directions.
     *
     * @param staff   the staff to process
     * @param hMargin margin added on left and right of staff (in pixels)
     * @param vMargin margin added on top and bottom of staff (in pixels)
     * @return the staff core area
     */
    public static Area getCoreArea (Staff staff,
                                    int hMargin,
                                    int vMargin)
    {
        GeoPath path = new GeoPath();

        {
            // Top limit
            NaturalSpline spline = staff.getFirstLine().getSpline();
            Point2D tl = spline.getFirstPoint();
            path.moveTo(tl.getX() - hMargin, tl.getY() - vMargin);

            AffineTransform at = AffineTransform.getTranslateInstance(0, -vMargin);
            path.append(spline.getPathIterator(at), true);

            Point2D tr = spline.getLastPoint();
            path.lineTo(tr.getX() + hMargin, tr.getY() - vMargin);
        }

        {
            // Bottom limit
            NaturalSpline spline = staff.getLastLine().getSpline();
            Point2D br = spline.getLastPoint();
            path.lineTo(br.getX() + hMargin, br.getY() + vMargin);

            AffineTransform at = AffineTransform.getTranslateInstance(0, +vMargin);
            path.append(new ReversePathIterator(spline.getPathIterator(at)), true);

            Point2D bl = spline.getFirstPoint();
            path.lineTo(bl.getX() - hMargin, bl.getY() + vMargin);
        }

        path.closePath();

        return new Area(path);
    }

    //------------//
    // getStaffAt //
    //------------//
    /**
     * Report the staff, among the sequence provided, whose area contains the provided
     * point.
     * Unfortunately, a point located in the gutter between two staves, is contained by both staves.
     *
     * @param point     the provided point
     * @param theStaves the staves sequence to search
     * @return the containing staff, or null if none found
     */
    @Deprecated
    public static Staff getStaffAt (Point2D point,
                                    List<Staff> theStaves)
    {
        final double x = point.getX();
        final double y = point.getY();

        for (Staff staff : theStaves) {
            // If the point is ON the area boundary, it is NOT contained.
            // So we use a rectangle of 1x1 pixels
            if (staff.getArea().intersects(x, y, 1, 1)) {
                return staff;
            }
        }

        return null;
    }

    //-------------//
    // getStavesOf //
    //-------------//
    /**
     * Report the staves whose area contains the provided point.
     *
     * @param point     the provided pixel point
     * @param theStaves the list of staves to check
     * @return the containing staves
     */
    public static List<Staff> getStavesOf (Point2D point,
                                           List<Staff> theStaves)
    {
        List<Staff> found = new ArrayList<Staff>();

        for (Staff staff : theStaves) {
            Area area = staff.getArea();

            if ((area != null) && area.contains(point)) {
                found.add(staff);
            }
        }

        return found;
    }

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
    public void addStaff (Staff staff)
    {
        staves.add(staff);
    }

    //------------------//
    // computeStaffArea //
    //------------------//
    /**
     * Compute the staff area, whether systems are one under the other or side by side.
     * <p>
     * Use horizontal staff slice and intersect it with area of containing system.
     *
     * @param staff the staff to process
     */
    public void computeStaffArea (Staff staff)
    {
        final int sheetWidth = sheet.getWidth();
        final int sheetHeight = sheet.getHeight();

        final List<Staff> aboves = vertNeighbors(staff, TOP);
        final PathIterator north = aboves.isEmpty()
                ? new GeoPath(new Line2D.Double(0, 0, sheetWidth, 0)).getPathIterator(
                        null) : getGlobalLine(aboves, BOTTOM);

        final List<Staff> belows = vertNeighbors(staff, BOTTOM);
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
        int left = system.getAreaEnd(LEFT); // May not be known yet -> 0
        int right = system.getAreaEnd(RIGHT); // May not be known yet -> 0

        if ((left != 0) || (right != 0 && right != sheetWidth)) {
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
        for (Staff staff : staves) {
            if ((horiNeighbor(staff, LEFT) != null) || (horiNeighbor(staff, RIGHT) != null)) {
                staff.setShort();
            }
        }
    }

    //-----------------//
    // getClosestStaff //
    //-----------------//
    /**
     * Report the closest staff from the provided point.
     *
     * @param point the provided point
     * @return the nearest staff, or null if none found
     */
    public Staff getClosestStaff (Point2D point)
    {
        return getClosestStaff(point, staves);
    }

    //---------------//
    // getGlobalLine //
    //---------------//
    /**
     * Report a line which concatenates the corresponding (first or last) lines of all
     * provided staves (assumed to be side by side), slightly translated vertically by
     * verticalMargin.
     *
     * @param staffList the horizontal sequence of staves
     * @param side      the desired vertical side
     * @return iterator on the global line
     */
    public PathIterator getGlobalLine (List<Staff> staffList,
                                       VerticalSide side)
    {
        if (staffList.isEmpty()) {
            return null;
        }

        final GeoPath globalLine = new GeoPath();

        // Point on left
        Staff leftStaff = staffList.get(0);
        LineInfo leftLine = (side == TOP) ? leftStaff.getFirstLine() : leftStaff.getLastLine();
        NaturalSpline leftSpline = leftLine.getSpline();
        globalLine.moveTo(0, leftSpline.getFirstPoint().getY());

        // Proper line of each staff
        for (Staff staff : staffList) {
            LineInfo fLine = (side == TOP) ? staff.getFirstLine() : staff.getLastLine();
            globalLine.append(fLine.getSpline(), true);
        }

        // Point on right
        Staff rightStaff = staffList.get(staffList.size() - 1);
        LineInfo rightLine = (side == TOP) ? rightStaff.getFirstLine() : rightStaff.getLastLine();
        NaturalSpline rightSpline = rightLine.getSpline();
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
    public int getIndexOf (Staff staff)
    {
        return staves.indexOf(staff);
    }

    //----------//
    // getRange //
    //----------//
    /**
     * Report a view on the range of staves from first to last (both inclusive).
     *
     * @param first the first staff of the range
     * @param last  the last staff of the range
     * @return a view on this range
     */
    public List<Staff> getRange (Staff first,
                                 Staff last)
    {
        return staves.subList(getIndexOf(first), getIndexOf(last) + 1);
    }

    //----------//
    // getStaff //
    //----------//
    public Staff getStaff (int index)
    {
        return staves.get(index);
    }

    //---------------//
    // getStaffCount //
    //---------------//
    /**
     * Report the total number of staves, whatever their containing systems.
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
     * Report an unmodifiable view (perhaps empty) of list of current staves.
     *
     * @return a view on staves
     */
    public List<Staff> getStaves ()
    {
        return Collections.unmodifiableList(staves);
    }

    //-------------//
    // getStavesOf //
    //-------------//
    /**
     * Report the staves that contain the provided point
     *
     * @param point the provided pixel point
     * @return the containing staves info, perhaps empty but not null
     */
    public List<Staff> getStavesOf (Point2D point)
    {
        return getStavesOf(point, staves);
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
    public Staff horiNeighbor (Staff current,
                               HorizontalSide side)
    {
        final int idx = getIndexOf(current);
        final int dir = (side == LEFT) ? (-1) : 1;
        final int iBreak = (side == LEFT) ? (-1) : staves.size();

        // Pickup the one immediately on left (or right)
        for (int i = idx + dir; (dir * (iBreak - i)) > 0; i += dir) {
            Staff s = getStaff(i);

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
     * Paint staff attachments.
     *
     * @param g the graphics context
     */
    public void render (Graphics2D g)
    {
        for (Staff staff : staves) {
            staff.renderAttachments(g);
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render just the staff lines.
     *
     * @param g graphics context
     */
    @Override
    public void renderItems (Graphics2D g)
    {
        if (constants.showStaffLines.isSet()) {
            final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
            final Color oldColor = g.getColor();
            g.setColor(Colors.ENTITY_MINOR);

            for (Staff staff : staves) {
                staff.render(g);
            }

            g.setStroke(oldStroke);
            g.setColor(oldColor);
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
     * Report the staves, if any, which are located immediately on the desired vertical
     * side of the current staff.
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
     * - neighborsOf(1, TOP)    == []
     * - neighborsOf(1, BOTTOM) == [2]
     * - neighborsOf(2, BOTTOM) == [3,5]
     * - neighborsOf(5, TOP)    == [2]
     * - neighborsOf(6, TOP)    == [3,5] (not just 5)
     * - neighborsOf(3, BOTTOM) == [4,6] (not just 4)
     * - neighborsOf(4, BOTTOM) == [7]
     * - neighborsOf(7, TOP)    == [4,6]
     * - neighborsOf(7, BOTTOM) == [8]
     * - neighborsOf(8, BOTTOM) == []
     * </pre>
     *
     * @param current current staff
     * @param side    desired vertical side
     * @return the neighboring staves if any, otherwise an empty list
     */
    public List<Staff> vertNeighbors (Staff current,
                                      VerticalSide side)
    {
        final List<Staff> neighbors = new ArrayList<Staff>();
        final int idx = getIndexOf(current);
        final int dir = (side == TOP) ? (-1) : 1;
        final int iBreak = (side == TOP) ? (-1) : staves.size();
        Staff other = null;

        // Pickup the one immediately above (or below)
        for (int i = idx + dir; (dir * (iBreak - i)) > 0; i += dir) {
            Staff s = getStaff(i);

            if (current.xOverlaps(s)) {
                other = s;

                break;
            }
        }

        if (other != null) {
            // Pick up this first one, and its horizontal neighbors
            neighbors.add(other);

            for (HorizontalSide hSide : HorizontalSide.values()) {
                Staff next = other;

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

        Collections.sort(neighbors, Staff.byId);

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

        private final Constant.Boolean showStaffLines = new Constant.Boolean(
                true,
                "Should we show the staff lines on all views?");

        private final Scale.Fraction verticalAreaMargin = new Scale.Fraction(
                1.0,
                "Vertical margin on staff areas");
    }
}
