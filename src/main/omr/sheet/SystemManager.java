//----------------------------------------------------------------------------//
//                                                                            //
//                          S y s t e m M a n a g e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.lag.Lags;
import omr.lag.Section;

import omr.math.GeoPath;
import omr.math.ReversePathIterator;

import omr.step.StepException;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.*;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import omr.glyph.GlyphLayer;

/**
 * Class {@code SystemManager} handles physical information about all
 * the systems of a given sheet.
 * <p>
 * A key question is to dispatch entities (sections, glyph instances) to
 * relevant system(s).
 * It is important to restrict the amount of entities to be searched when
 * processing a given system.
 * This also speeds up the program, since it allows to process all systems in
 * parallel.
 * <p>
 * For systems laid side by side, the middle vertical line should be enough to
 * separate entities (mainly composed of braces or similar things in that case).
 * <p>
 * For systems laid one under the other, unfortunately, there is no way to
 * always find out a precise border.
 * There are even examples where such separating border simply cannot be found.
 * So the strategy is to dispatch such "shareable" entities to both systems
 * when relevant, and let each system process these entities as needed.
 * Basically the shareable entities are those found between the last line of
 * upper system and the first line of lower system.
 *
 * @author Hervé Bitteur
 */
public class SystemManager
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SystemManager.class);

    //~ Instance fields --------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Sheet retrieved systems. */
    private final List<SystemInfo> systems;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // SystemManager //
    //---------------//
    /**
     * Creates a new SystemManager object.
     *
     * @param sheet the related sheet
     */
    public SystemManager (Sheet sheet)
    {
        this.sheet = sheet;

        systems = sheet.getSystems();
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // dispatchGlyphs //
    //----------------//
    /**
     * Dispatch the sheet glyphs among systems
     *
     * @param glyphs the collection of glyphs to dispatch among sheet systems.
     *               If null, the nest glyphs are used.
     */
    public void dispatchGlyphs (Collection<Glyph> glyphs)
    {
        if (glyphs == null) {
            for (GlyphLayer layer : GlyphLayer.concreteValues()) {
                dispatchGlyphs(sheet.getNest().getGlyphs(layer));
            }
        } else {
            // Assign the glyphs to the proper system glyphs collection
            List<SystemInfo> relevants = new ArrayList<SystemInfo>();

            for (Glyph glyph : glyphs) {
                getSystemsOf(glyph.getCentroid(), relevants);

                for (SystemInfo system : relevants) {
                    system.registerGlyph(glyph);
                }
            }
        }
    }

    //--------------------------------//
    // dispatchHorizontalHugeSections //
    //--------------------------------//
    /**
     * Dispatch the various horizontal huge sections among systems.
     */
    public void dispatchHorizontalHugeSections ()
    {
        // Clear systems containers
        for (SystemInfo system : systems) {
            system.getMutableHorizontalFullSections()
                    .clear();
        }

        // Now dispatch the lag huge sections among the systems
        List<SystemInfo> relevants = new ArrayList<SystemInfo>();

        for (Section section : sheet.getLag(Lags.FULL_HLAG)
                .getSections()) {
            getSystemsOf(section.getCentroid(), relevants);

            for (SystemInfo system : relevants) {
                // Link system <>-> section
                system.getMutableHorizontalFullSections()
                        .add(section);
            }
        }
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
    public List<SystemInfo> getSystemsOf (Point point)
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
    public List<SystemInfo> getSystemsOf (Point point,
                                          List<SystemInfo> found)
    {
        if (found != null) {
            found.clear();
        } else {
            found = new ArrayList<SystemInfo>();
        }

        for (SystemInfo system : systems) {
            Area area = system.getArea();

            if ((area != null) && area.contains(point)) {
                found.add(system);
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
     * @param rect the provided rectangle
     * @param found (output) list to be populated (allocated if null)
     * @return the containing systems info, perhaps empty but not null
     */
    public List<SystemInfo> getSystemsOf (Rectangle2D rect,
                                          List<SystemInfo> found)
    {
        if (found != null) {
            found.clear();
        } else {
            found = new ArrayList<SystemInfo>();
        }

        for (SystemInfo system : systems) {
            Area area = system.getArea();

            if ((area != null) && area.intersects(rect)) {
                found.add(system);
            }
        }

        return found;
    }

    //-----------------//
    // populateSystems //
    //-----------------//
    /**
     * Populate the systems with relevant sections and glyphs.
     *
     * @throws omr.step.StepException
     */
    public void populateSystems ()
            throws StepException
    {
        // Create score counterparts of systems & parts
        allocateScoreStructure();

        // Report number of systems retrieved
        reportResults();

        // Compute systems areas
        for (SystemInfo system : sheet.getSystems()) {
            system.updateCoordinates();
            computeSystemArea(system);
        }

        // Compute staves areas
        StaffManager staffManager = sheet.getStaffManager();

        for (SystemInfo system : sheet.getSystems()) {
            for (StaffInfo staff : system.getStaves()) {
                //TODO: is this useful?
                staffManager.computeStaffArea(staff);
            }
        }

        // Dispatch sections & glyphs per system
        dispatchSystemEntities();
    }

    //------------------------//
    // allocateScoreStructure //
    //------------------------//
    /**
     * For each SystemInfo, build the corresponding System entity
     * with all its depending Parts and Staves.
     */
    private void allocateScoreStructure ()
            throws StepException
    {
        // Clear Score -> Systems
        sheet.getPage()
                .resetSystems();

        for (SystemInfo system : systems) {
            system.allocateScoreStructure(); // ScoreSystem, Parts & Staves
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
     * If we have no system neighbor on left or right, compute the area with
     * north and south paths.
     * If we do have neighbor(s) on left or right, compute the global area and
     * intersect with rectangular slice of the system
     */
    private void computeSystemArea (SystemInfo system)
    {
        final int sheetWidth = sheet.getWidth();
        final int sheetHeight = sheet.getHeight();
        final List<SystemInfo> aboves = vertNeighbors(system, TOP);
        final List<SystemInfo> belows = vertNeighbors(system, BOTTOM);

        // Vertical abscissae on system left & right
        final SystemInfo leftNeighbor = horiNeighbor(system, LEFT);
        final int left = (leftNeighbor != null)
                ? ((leftNeighbor.getRight() + system.getLeft()) / 2)
                : 0;
        system.setAreaEnd(LEFT, left);

        final SystemInfo rightNeighbor = horiNeighbor(system, RIGHT);
        final int right = (rightNeighbor != null)
                ? ((system.getRight()
                    + rightNeighbor.getLeft()) / 2) : sheetWidth;
        system.setAreaEnd(RIGHT, right);

        PathIterator north = aboves.isEmpty()
                ? new GeoPath(
                        new Line2D.Double(left, 0, right, 0)).getPathIterator(null)
                : getGlobalLine(aboves, BOTTOM);

        PathIterator south = belows.isEmpty()
                ? new GeoPath(
                        new Line2D.Double(left, sheetHeight, right, sheetHeight)).getPathIterator(
                        null) : getGlobalLine(belows, TOP);

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

    //----------------------------//
    // dispatchHorizontalSections //
    //----------------------------//
    /**
     * Dispatch the various horizontal sections among systems.
     */
    private void dispatchHorizontalSections ()
    {
        // Clear systems containers
        for (SystemInfo system : systems) {
            system.getMutableHorizontalSections()
                    .clear();
        }

        // Now dispatch the lag sections among relevant systems
        List<SystemInfo> relevants = new ArrayList<SystemInfo>();

        for (Section section : sheet.getLag(Lags.HLAG)
                .getSections()) {
            getSystemsOf(section.getCentroid(), relevants);

            for (SystemInfo system : relevants) {
                // Link system <>-> section
                system.getMutableHorizontalSections()
                        .add(section);
            }
        }
    }

    //------------------------//
    // dispatchSystemEntities //
    //------------------------//
    /**
     * Split horizontal sections, vertical sections, glyph instances
     * per system.
     */
    private void dispatchSystemEntities ()
    {
        // Dispatch sections and glyphs to relevant systems
        dispatchHorizontalSections();
        dispatchVerticalSections();
        dispatchGlyphs(null);
    }

    //--------------------------//
    // dispatchVerticalSections //
    //--------------------------//
    /**
     * Dispatch the various vertical sections among systems
     */
    private void dispatchVerticalSections ()
    {
        // Clear systems containers
        for (SystemInfo system : systems) {
            system.getMutableVerticalSections()
                    .clear();
        }

        // Now dispatch the lag sections among relevant systems
        List<SystemInfo> relevants = new ArrayList<SystemInfo>();

        for (Section section : sheet.getLag(Lags.VLAG)
                .getSections()) {
            getSystemsOf(section.getCentroid(), relevants);

            for (SystemInfo system : relevants) {
                // Link system <>-> section
                system.getMutableVerticalSections()
                        .add(section);
            }
        }
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

        List<StaffInfo> staffList = new ArrayList<StaffInfo>();

        for (SystemInfo system : list) {
            staffList.add(
                    (side == TOP) ? system.getFirstStaff() : system.getLastStaff());
        }

        return sheet.getStaffManager()
                .getGlobalLine(staffList, side);
    }

    //--------------//
    // horiNeighbor //
    //--------------//
    /**
     * Report the system, if any, located on the desired horizontal
     * side of the current one.
     *
     * @param current current system
     * @param side    desired horizontal side
     * @return the neighboring system or null
     */
    private SystemInfo horiNeighbor (SystemInfo current,
                                     HorizontalSide side)
    {
        final int idx = systems.indexOf(current);
        final int dir = (side == LEFT) ? (-1) : 1;
        final int iBreak = (side == LEFT) ? (-1) : systems.size();

        // Pickup the one immediately on left (or right)
        for (int i = idx + dir; (dir * (iBreak - i)) > 0; i += dir) {
            SystemInfo s = systems.get(i);

            if (current.yOverlaps(s)) {
                return s;
            }
        }

        return null;
    }

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        StringBuilder sb = new StringBuilder();
        int partNb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            partNb = Math.max(partNb, system.getParts().size());
        }

        int sysNb = systems.size();

        if (partNb > 0) {
            sb.append(partNb)
                    .append(" part");

            if (partNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no part found");
        }

        sheet.getBench()
                .recordPartCount(partNb);

        if (sysNb > 0) {
            sb.append(" along ")
                    .append(sysNb)
                    .append(" system");

            if (sysNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append(", no system found");
        }

        sheet.getBench()
                .recordSystemCount(sysNb);

        logger.info("{}{}", sheet.getLogPrefix(), sb);
    }

    //---------------//
    // vertNeighbors //
    //---------------//
    /**
     * Report the systems, if any, which are located immediately on the
     * desired vertical side of the current one.
     *
     * @param current current system
     * @param side    desired vertical side
     * @return the neighboring systems if any, otherwise an empty list
     */
    private List<SystemInfo> vertNeighbors (SystemInfo current,
                                            VerticalSide side)
    {
        final List<SystemInfo> neighbors = new ArrayList<SystemInfo>();
        final int idx = systems.indexOf(current);
        final int dir = (side == TOP) ? (-1) : 1;
        final int iBreak = (side == TOP) ? (-1) : systems.size();
        SystemInfo other = null;

        // Pickup the one immediately above (or below)
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
                    next = horiNeighbor(next, hSide);

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

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction yellowZoneHalfHeight = new Scale.Fraction(
                0.1,
                "Half height of inter-system yellow zone");

    }
}
