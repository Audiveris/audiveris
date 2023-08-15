//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y s t e m M a n a g e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.GeoPath;
import org.audiveris.omr.math.ReversePathIterator;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.score.SystemRef;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.BOTTOM;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>SystemManager</code> handles physical information about all the systems of a
 * given sheet.
 * <p>
 * Note that systems of the same sheet may belong to separate pages (this is the case when a new
 * movement is detected, thanks to system indentation).
 * <p>
 * A key question is to dispatch entities (sections, glyph instances) to relevant system(s).
 * It is important to restrict the amount of entities to be searched when processing a given system.
 * This also speeds up the program, since it allows to process all systems in parallel.
 * <p>
 * For systems laid side by side, the middle vertical line should be enough to separate entities
 * (mainly composed of braces or similar things in that case).
 * <p>
 * For systems laid one under the other, unfortunately, there is no way to always find out a precise
 * border. There are even examples where such separating border simply cannot be found.
 * So the strategy is to dispatch such "shareable" entities to both systems when relevant, and let
 * each system process these entities as needed.
 * Basically the shareable entities are those found between the last line of upper system and the
 * first line of lower system.
 *
 * @author Hervé Bitteur
 */
public class SystemManager
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SystemManager.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Related sheet. */
    @Navigable(false)
    private Sheet sheet;

    /** Sheet retrieved systems. */
    private final List<SystemInfo> systems = new ArrayList<>();

    /**
     * No-arg constructor needed for JAXB.
     */
    private SystemManager ()
    {
        this.sheet = null;
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SystemManager object.
     *
     * @param sheet the related sheet
     */
    public SystemManager (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // allocatePages //
    //---------------//
    /**
     * Allocate page(s) for the sheet, as well as contained systems and parts.
     * <p>
     * Detect if an indented system starts a new movement (and thus a new score).
     * <p>
     * This behavior is driven by the "indentations" processing switch.
     *
     * @see <a href="https://github.com/Audiveris/audiveris/pull/350">PR of Vladimir Buyanov</a>
     */
    public void allocatePages ()
    {
        final SheetStub stub = sheet.getStub();
        final ProcessingSwitches switches = stub.getProcessingSwitches();
        final boolean useIndentation = switches.getValue(ProcessingSwitch.indentations);
        Page page = null;
        PageRef pageRef = null;

        if (useIndentation) {
            // Look at left indentation of (deskewed) systems
            checkIndentations();
        }

        // Allocate systems and pages
        for (SystemInfo system : systems) {
            final int pageId = 1 + sheet.getPages().size();

            if (system.isIndented()) {
                final int systId = system.getId();
                logger.info("Indentation detected for system#{}", systId);

                // We have a movement start
                if (page != null) {
                    // Sheet middle => Score break, finish current page
                    page.setLastSystemId(systId - 1);
                    page.setSystemsFrom(systems);
                }

                // Start a new page
                stub.addPageRef(pageRef = new PageRef(stub, pageId, true));
                sheet.addPage(page = new Page(sheet, pageId, (systId == 1) ? null : systId));
            } else if (page == null) {
                // Start first page in sheet
                stub.addPageRef(pageRef = new PageRef(stub, pageId, false));
                sheet.addPage(page = new Page(sheet, pageId, null));
            }

            system.setPage(page);
            pageRef.addSystem(system.buildRef());
        }

        if (page != null) {
            page.setSystemsFrom(systems);
        }
    }

    //-------------------//
    // checkIndentations //
    //-------------------//
    /**
     * Check all (de-skewed) systems for left-indentation that would signal a new movement.
     */
    private void checkIndentations ()
    {
        final Skew skew = sheet.getSkew();
        final double minIndentation = sheet.getScale().toPixels(constants.minIndentation);

        for (SystemInfo system : systems) {
            // For side by side systems, only the leftmost one is concerned
            if (system.getAreaEnd(LEFT) != 0) {
                continue;
            }

            final Point2D ul = skew.deskewed(new Point(system.getLeft(), system.getTop()));

            for (VerticalSide side : VerticalSide.values()) {
                final List<SystemInfo> others = verticalNeighbors(system, side);

                if (!others.isEmpty()) {
                    SystemInfo other = others.get(0);
                    Point2D ulOther = skew.deskewed(new Point(other.getLeft(), other.getTop()));

                    if ((ul.getX() - ulOther.getX()) >= minIndentation) {
                        system.setIndented(true);

                        break;
                    }
                }
            }
        }
    }

    //-------------------//
    // computeSystemArea //
    //-------------------//
    /**
     * Compute the system related area.
     * <p>
     * For vertical borders, use plain vertical lines.
     * For horizontal borders, use first line encountered in next system.
     * <p>
     * If we have no system neighbor on left or right, compute the area with north and south paths.
     * If we do have neighbor(s) on left or right, compute the global area and intersect with
     * rectangular slice of the system
     *
     * @param system the system to process
     */
    public void computeSystemArea (SystemInfo system)
    {
        final int sheetWidth = sheet.getWidth();
        final int sheetHeight = sheet.getHeight();
        final List<SystemInfo> aboves = verticalNeighbors(system, TOP);
        final List<SystemInfo> belows = verticalNeighbors(system, BOTTOM);

        // Vertical abscissae on system left & right
        final SystemInfo leftNeighbor = horizontalNeighbor(system, LEFT);
        final int left = (leftNeighbor != null) ? ((leftNeighbor.getRight() + system.getLeft()) / 2)
                : 0;
        system.setAreaEnd(LEFT, left);

        final SystemInfo rightNeighbor = horizontalNeighbor(system, RIGHT);
        final int right = (rightNeighbor != null) ? ((system.getRight() + rightNeighbor.getLeft())
                / 2) : sheetWidth;
        system.setAreaEnd(RIGHT, right);

        PathIterator north = aboves.isEmpty() ? new GeoPath(new Line2D.Double(left, 0, right, 0))
                .getPathIterator(null) : getGlobalLine(aboves, BOTTOM);

        PathIterator south = belows.isEmpty() ? new GeoPath(
                new Line2D.Double(left, sheetHeight, right, sheetHeight)).getPathIterator(null)
                : getGlobalLine(belows, TOP);

        // Define sheet-wide area
        GeoPath wholePath = new GeoPath();
        wholePath.append(north, false);
        wholePath.append(new ReversePathIterator(south), true);

        final Area area = new Area(wholePath);

        // If we have neighbor(s) on left or right, intersect with proper slice
        if ((left != 0) || (right != sheetWidth)) {
            Rectangle slice = new Rectangle(left, 0, right - left, sheetHeight);
            area.intersect(new Area(slice));
        }

        system.setArea(area);
    }

    //-------------------//
    // containingSystems //
    //-------------------//
    /**
     * Report the systems that contain the provided rectangle
     *
     * @param rect  the provided rectangle
     * @param found (output) list to be populated (allocated if null)
     * @return the containing systems info, perhaps empty but not null
     */
    public List<SystemInfo> containingSystems (Rectangle2D rect,
                                               List<SystemInfo> found)
    {
        if (found != null) {
            found.clear();
        } else {
            found = new ArrayList<>();
        }

        for (SystemInfo system : systems) {
            Area area = system.getArea();

            if ((area != null) && area.contains(rect)) {
                found.add(system);
            }
        }

        return found;
    }

    //----------------------------//
    // dispatchHorizontalSections //
    //----------------------------//
    /**
     * Dispatch the various horizontal sections among systems.
     */
    public void dispatchHorizontalSections ()
    {
        // Clear systems containers
        for (SystemInfo system : systems) {
            system.getMutableHorizontalSections().clear();
        }

        // Now dispatch the lag sections among relevant systems
        List<SystemInfo> relevants = new ArrayList<>();

        for (Section section : sheet.getLagManager().getLag(Lags.HLAG).getEntities()) {
            getSystemsOf(section.getCentroid(), relevants);

            for (SystemInfo system : relevants) {
                // Link system <>-> section
                system.getMutableHorizontalSections().add(section);
            }
        }
    }

    //--------------------------//
    // dispatchVerticalSections //
    //--------------------------//
    /**
     * Dispatch the various vertical sections among systems.
     */
    public void dispatchVerticalSections ()
    {
        // Clear systems containers
        for (SystemInfo system : systems) {
            system.getMutableVerticalSections().clear();
        }

        // Now dispatch the lag sections among relevant systems
        List<SystemInfo> relevants = new ArrayList<>();

        for (Section section : sheet.getLagManager().getLag(Lags.VLAG).getEntities()) {
            getSystemsOf(section.getCentroid(), relevants);

            for (SystemInfo system : relevants) {
                // Link system <>-> section
                system.getMutableVerticalSections().add(section);
            }
        }
    }

    //------------------//
    // getClosestSystem //
    //------------------//
    /**
     * Report the system which is (vertically) closest to the provided point.
     *
     * @param point provided point
     * @return closest system or null if external to sheet bounds
     */
    public SystemInfo getClosestSystem (Point2D point)
    {
        final List<SystemInfo> found = getSystemsOf(point);

        if (found.isEmpty()) {
            return null;
        }

        int bestIndex = 0;

        if (found.size() > 1) {
            int minDist = Integer.MAX_VALUE;

            for (int i = 0; i < found.size(); i++) {
                SystemInfo syst = found.get(i);
                int vDist = Math.min(
                        syst.getFirstStaff().distanceTo(point),
                        syst.getLastStaff().distanceTo(point));

                if (vDist < minDist) {
                    minDist = vDist;
                    bestIndex = i;
                }
            }
        }

        return found.get(bestIndex);
    }

    //---------------//
    // getGlobalLine //
    //---------------//
    /**
     * Report a line which concatenates the corresponding
     * (first or last) staff lines of all provided systems
     * (assumed to be side by side).
     *
     * @param list the horizontal sequence of systems
     * @param side the desired vertical side
     * @return iterator on the global line
     */
    private PathIterator getGlobalLine (List<SystemInfo> list,
                                        VerticalSide side)
    {
        if (list.isEmpty()) {
            return null;
        }

        List<Staff> staffList = new ArrayList<>();

        for (SystemInfo system : list) {
            staffList.add((side == TOP) ? system.getFirstStaff() : system.getLastStaff());
        }

        return sheet.getStaffManager().getGlobalLine(staffList, side);
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * Report an unmodifiable view on current systems.
     *
     * @return a view on systems list
     */
    public List<SystemInfo> getSystems ()
    {
        return Collections.unmodifiableList(systems);
    }

    //--------------//
    // getSystemsOf //
    //--------------//
    /**
     * Report the systems that contain the provided glyph
     *
     * @param glyph the provided glyph
     * @return the containing systems info, perhaps empty but not null
     */
    public List<SystemInfo> getSystemsOf (Glyph glyph)
    {
        return getSystemsOf(glyph.getCentroid(), null);
    }

    //--------------//
    // getSystemsOf //
    //--------------//
    /**
     * Report the systems that contain the provided point
     *
     * @param point the provided pixel point
     * @return the containing systems info, perhaps empty but not null
     */
    public List<SystemInfo> getSystemsOf (Point2D point)
    {
        return getSystemsOf(point, null);
    }

    //--------------//
    // getSystemsOf //
    //--------------//
    /**
     * Report the systems that contain the provided point
     *
     * @param point the provided pixel point
     * @param found (output) list to be populated (allocated if null)
     * @return the containing systems info, perhaps empty but not null
     */
    public List<SystemInfo> getSystemsOf (Point2D point,
                                          List<SystemInfo> found)
    {
        if (found != null) {
            found.clear();
        } else {
            found = new ArrayList<>();
        }

        if (point != null) {
            for (SystemInfo system : systems) {
                Area area = system.getArea();

                if ((area != null) && area.contains(point)) {
                    found.add(system);
                }
            }
        }

        return found;
    }

    //--------------//
    // getSystemsOf //
    //--------------//
    /**
     * Report the systems that intersect the provided rectangle
     *
     * @param rect  the provided rectangle
     * @param found (output) list to be populated (allocated if null)
     * @return the containing systems info, perhaps empty but not null
     */
    public List<SystemInfo> getSystemsOf (Rectangle2D rect,
                                          List<SystemInfo> found)
    {
        if (found != null) {
            found.clear();
        } else {
            found = new ArrayList<>();
        }

        for (SystemInfo system : systems) {
            Area area = system.getArea();

            if ((area != null) && area.intersects(rect)) {
                found.add(system);
            }
        }

        return found;
    }

    //------------------//
    // getSystemsString //
    //------------------//
    /**
     * Report the string of sheet systems with their staves
     *
     * @return string of systems
     */
    public String getSystemsString ()
    {
        StringBuilder sb = new StringBuilder();

        for (SystemInfo system : systems) {
            if (sb.length() > 0) {
                sb.append(" ");
            }

            sb.append("#").append(system.getId()).append("[");

            List<Staff> staves = system.getStaves();

            for (int i = 0; i < staves.size(); i++) {
                if (i != 0) {
                    sb.append(", ");
                }

                sb.append(staves.get(i).getId());
            }

            sb.append("]");
        }

        if (sb.length() > 0) {
            return sb.toString();
        }

        return "none found!";
    }

    //--------------------//
    // horizontalNeighbor //
    //--------------------//
    /**
     * Report the system, if any, located on the desired horizontal
     * side of the current one.
     *
     * @param current current system
     * @param hSide   desired horizontal side
     * @return the neighboring system or null
     */
    public SystemInfo horizontalNeighbor (SystemInfo current,
                                          HorizontalSide hSide)
    {
        final int idx = systems.indexOf(current);
        final int dir = (hSide == LEFT) ? (-1) : 1;
        final int iBreak = (hSide == LEFT) ? (-1) : systems.size();

        // Pickup the one immediately on desired horizontal side
        for (int i = idx + dir; (dir * (iBreak - i)) > 0; i += dir) {
            SystemInfo s = systems.get(i);

            if (current.yOverlaps(s)) {
                return s;
            }
        }

        return null;
    }

    //----------------//
    // initTransients //
    //----------------//
    void initTransients (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //-----------------//
    // populateSystems //
    //-----------------//
    /**
     * Populate the systems with relevant sections and glyphs.
     */
    public void populateSystems ()
    {
        // Compute systems areas
        for (SystemInfo system : sheet.getSystems()) {
            system.updateCoordinates();
            computeSystemArea(system);
        }

        // Compute staves areas
        StaffManager staffManager = sheet.getStaffManager();

        for (SystemInfo system : sheet.getSystems()) {
            for (Staff staff : system.getStaves()) {
                staffManager.computeStaffArea(staff); // Force update now that system width is known
            }
        }

        // Dispatch sections to relevant systems
        dispatchHorizontalSections();
        dispatchVerticalSections();

        // Allocate one (or several) page instances for the sheet
        allocatePages();

        // Report layout results
        reportResults();
    }

    //--------------//
    // removeSystem //
    //--------------//
    /**
     * Remove the provided system (perhaps because of a merge with system above).
     *
     * @param system the system to remove
     * @return the PageRef removed if any
     */
    public PageRef removeSystem (SystemInfo system)
    {
        final int index = systems.indexOf(system);

        if (index == -1) {
            logger.error("Cannot remove unknown {}", system);
        } else {
            final SystemRef systemRef = system.getRef();
            systems.remove(system);

            final Page page = system.getPage();
            page.removeSystem(system);
            final PageRef pageRef = page.getRef();
            pageRef.removeSystem(systemRef);

            // Update ID for each following system
            for (int i = index; i < systems.size(); i++) {
                SystemInfo s = systems.get(i);
                s.setId(i + 1);
            }

            // Remove page?
            if (page.getSystems().isEmpty()) {
                sheet.removePage(page);
                sheet.getStub().removePageRef(pageRef);

                return pageRef;
            }
        }

        return null;
    }

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        int pageNb = sheet.getPages().size();

        if (pageNb > 1) {
            logger.info("{} pages found in sheet", pageNb);
        }

        for (Page page : sheet.getPages()) {
            StringBuilder sb = new StringBuilder();

            if (pageNb > 1) {
                sb.append("Page #").append(1 + sheet.getPages().indexOf(page)).append(": ");
            }

            int partNb = 0;
            int tabNb = 0;

            for (SystemInfo system : page.getSystems()) {
                partNb = Math.max(partNb, system.getParts().size());

                int systemTabs = 0;

                for (Part part : system.getParts()) {
                    systemTabs += part.getTablatures().size();
                }

                tabNb = Math.max(tabNb, systemTabs);
            }

            if (partNb > 0) {
                sb.append(partNb).append(" part");

                if (partNb > 1) {
                    sb.append("s");
                }
            } else {
                sb.append("no part found");
            }

            int sysNb = page.getSystems().size();

            if (sysNb > 0) {
                sb.append(" along ").append(sysNb).append(" system");

                if (sysNb > 1) {
                    sb.append("s");
                }
            } else {
                sb.append(", no system found");
            }

            // Tablatures in page?
            if (tabNb > 0) {
                sb.append(", ").append(tabNb).append(" tablature");

                if (tabNb > 1) {
                    sb.append("s");
                }
            }

            logger.info("{}", sb);
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Empty the whole collection of systems.
     */
    public void reset ()
    {
        systems.clear();
    }

    //------------//
    // setSystemsFrom //
    //------------//
    /**
     * Assign the whole sequence of systems
     *
     * @param systems the (new) systems
     */
    public void setSystems (Collection<SystemInfo> systems)
    {
        if (this.systems != systems) {
            this.systems.clear();
            this.systems.addAll(systems);
        }
    }

    //----------------//
    // unremoveSystem //
    //----------------//
    /**
     * Un-remove the provided system (canceling a remove).
     *
     * @param system  the system to remove
     * @param pageRef the removed PageRef if any
     */
    public void unremoveSystem (SystemInfo system,
                                PageRef pageRef)
    {
        // Re-insert system in sheet
        final int indexInSheet = system.getId() - 1;
        systems.add(indexInSheet, system);

        // Re-insert page?
        final Page page = system.getPage();

        if (pageRef != null) {
            sheet.addPage(page.getId() - 1, page);

            final SheetStub stub = sheet.getStub();
            stub.addPageRef(pageRef.getId() - 1, pageRef);
        }

        // Re-insert system into containing page
        page.unremoveSystem(system);
        final int indexInPage = system.getIndexInPage();
        page.getRef().unremoveSystem(indexInPage, system.getRef());

        // Update ID for each following system
        for (int i = indexInSheet + 1; i < systems.size(); i++) {
            SystemInfo s = systems.get(i);
            s.setId(i + 1);
        }
    }

    //-------------------//
    // verticalNeighbors //
    //-------------------//
    /**
     * Report the systems, if any, which are located immediately on the desired vertical
     * side of the provided one.
     *
     * @param current current system
     * @param vSide   desired vertical side
     * @return the neighboring systems if any, otherwise an empty list
     */
    public List<SystemInfo> verticalNeighbors (SystemInfo current,
                                               VerticalSide vSide)
    {
        final List<SystemInfo> neighbors = new ArrayList<>();
        final int idx = systems.indexOf(current);
        final int dir = (vSide == TOP) ? (-1) : 1;
        final int iBreak = (vSide == TOP) ? (-1) : systems.size();
        SystemInfo other = null;

        // Pickup the one immediately on desired vertical side
        for (int i = idx + dir; (dir * (iBreak - i)) > 0; i += dir) {
            SystemInfo s = systems.get(i);

            if (current.xOverlaps(s)) {
                other = s;

                break;
            }
        }

        if (other != null) {
            // Pick up this first one, and its horizontal neighbors
            neighbors.add(other);

            for (HorizontalSide hSide : HorizontalSide.values()) {
                SystemInfo next = other;

                do {
                    next = horizontalNeighbor(next, hSide);

                    if (next != null) {
                        neighbors.add(next);
                    } else {
                        break;
                    }
                } while (true);
            }
        }

        Collections.sort(neighbors, SystemInfo.byId);

        return neighbors;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction minIndentation = new Scale.Fraction(
                2.0,
                "Minimum shift to detect a system indentation");
    }
}
