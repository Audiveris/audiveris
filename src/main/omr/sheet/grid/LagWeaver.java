//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L a g W e a v e r                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.Sections;

import omr.run.Orientation;
import omr.run.Run;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Predicate;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

/**
 * Class {@code LagWeaver} is just a prototype. TODO.
 *
 * @author Hervé Bitteur
 */
public class LagWeaver
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LagWeaver.class);

    /** Table dx/dy -> Heading. */
    private static final Heading[][] headings = {
        {null, Heading.NORTH, null},
        {Heading.WEST, null, Heading.EAST},
        {null, Heading.SOUTH, null}
    };

    //~ Enumerations -------------------------------------------------------------------------------
    private static enum Heading
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        NORTH,
        EAST,
        SOUTH,
        WEST;

        //~ Methods --------------------------------------------------------------------------------
        public boolean insideCornerTo (Heading next)
        {
            switch (this) {
            case NORTH:
                return next == WEST;

            case EAST:
                return next == NORTH;

            case SOUTH:
                return next == EAST;

            case WEST:
                return next == SOUTH;
            }

            return false; // Unreachable stmt
        }
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Vertical lag. */
    private final Lag vLag;

    /** Horizontal lag. */
    private final Lag hLag;

    /**
     * Actual points around current vLag section to check to hLag presence.
     * (relevant only during horiWithVert)
     */
    private final List<Point> pointsAside = new ArrayList<Point>();

    /** Points to check for source sections above in hLag. */
    private final List<Point> pointsAbove = new ArrayList<Point>();

    /** Points to check for target sections below in hLag. */
    private final List<Point> pointsBelow = new ArrayList<Point>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LagWeaver object.
     *
     * @param sheet the related sheet, which holds the v and h lags
     */
    public LagWeaver (Sheet sheet)
    {
        this.sheet = sheet;

        vLag = sheet.getLag(Lags.VLAG);
        hLag = sheet.getLag(Lags.HLAG);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // buildInfo //
    //-----------//
    public void buildInfo ()
    {
        StopWatch watch = new StopWatch("LagWeaver");

        // Remove staff line stuff from hLag
        watch.start("purge hLag");

        List<Section> staffLinesSections = removeStaffLineSections(hLag);
        logger.debug(
                "{}StaffLine sections removed: {}",
                sheet.getLogPrefix(),
                staffLinesSections.size());

        // Populate systems
        watch.start("populate systems");
        sheet.getSystemManager().populateSystems();

        watch.start("Hori <-> Hori");
        horiWithHori();

        watch.start("Hori <-> Vert");
        horiWithVert();

        // The end
        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //---------------//
    // addPointAbove //
    //---------------//
    private void addPointAbove (int x,
                                int y)
    {
        logger.debug("addPointAbove {},{}", x, y);
        pointsAbove.add(new Point(x, y));
    }

    //---------------//
    // addPointAside //
    //---------------//
    private void addPointAside (int x,
                                int y)
    {
        //logger.debug("addPointAside " + x + "," + y);
        pointsAside.add(new Point(x, y));
    }

    //---------------//
    // addPointBelow //
    //---------------//
    private void addPointBelow (int x,
                                int y)
    {
        logger.debug("addPointBelow {},{}", x, y);
        pointsBelow.add(new Point(x, y));
    }

    //------------------//
    // checkPointsAbove //
    //------------------//
    private void checkPointsAbove (Section lSect)
    {
        boolean added = false;
        Section cachedSection = null;

        // Determine relevant systems
        Rectangle bounds = lSect.getBounds();
        bounds.grow(0, 1);

        List<SystemInfo> relevants = sheet.getSystemManager().containingSystems(bounds, null);

        for (Point pt : pointsAbove) {
            if ((hLag.getRunAt(pt.x, pt.y) == null)
                || ((cachedSection != null) && cachedSection.contains(pt.x, pt.y))) {
                continue;
            }

            for (SystemInfo system : relevants) {
                Collection<Section> hSections = system.getHorizontalSections();
                Section hSect = Sections.containingSection(pt.x, pt.y, hSections);
                cachedSection = hSect;

                if (hSect != null) {
                    hSect.addTarget(lSect);
                    added = true;

                    break;
                }
            }
        }

        if (added && logger.isDebugEnabled()) {
            logger.info(
                    "lSect#{} checks:{}{}",
                    lSect.getId(),
                    pointsAbove.size(),
                    Sections.toString(" sources", lSect.getSources()));
        }
    }

    //------------------//
    // checkPointsAside //
    //------------------//
    private void checkPointsAside (Section vSect)
    {
        boolean added = false;
        Section cachedSection = null;

        // Determine relevant systems
        Rectangle bounds = vSect.getBounds();
        bounds.grow(1, 0);

        List<SystemInfo> relevants = sheet.getSystemManager().containingSystems(bounds, null);

        for (Point pt : pointsAside) {
            if ((hLag.getRunAt(pt.x, pt.y) == null)
                || ((cachedSection != null) && cachedSection.contains(pt.x, pt.y))) {
                continue;
            }

            for (SystemInfo system : relevants) {
                Collection<Section> hSections = system.getHorizontalSections();
                Section hSect = Sections.containingSection(pt.x, pt.y, hSections);
                cachedSection = hSect;

                if (hSect != null) {
                    vSect.addOppositeSection(hSect);
                    hSect.addOppositeSection(vSect);
                    added = true;

                    break;
                }
            }
        }

        if (added && logger.isDebugEnabled()) {
            logger.info(
                    "vSect#{} checks:{}{}",
                    vSect.getId(),
                    pointsAside.size(),
                    Sections.toString(" hSects", vSect.getOppositeSections()));
        }
    }

    //------------------//
    // checkPointsBelow //
    //------------------//
    private void checkPointsBelow (Section lSect)
    {
        boolean added = false;
        Section cachedSection = null;

        // Determine relevant systems
        Rectangle bounds = lSect.getBounds();
        bounds.grow(0, 1);

        List<SystemInfo> relevants = sheet.getSystemManager().containingSystems(bounds, null);

        for (Point pt : pointsBelow) {
            if ((hLag.getRunAt(pt.x, pt.y) == null)
                || ((cachedSection != null) && cachedSection.contains(pt.x, pt.y))) {
                continue;
            }

            for (SystemInfo system : relevants) {
                Collection<Section> hSections = system.getHorizontalSections();
                Section hSect = Sections.containingSection(pt.x, pt.y, hSections);
                cachedSection = hSect;

                if (hSect != null) {
                    lSect.addTarget(hSect);
                    added = true;

                    break;
                }
            }
        }

        if (added && logger.isDebugEnabled()) {
            logger.info(
                    "lSect#{} checks:{}{}",
                    lSect.getId(),
                    pointsBelow.size(),
                    Sections.toString(" targets", lSect.getTargets()));
        }
    }

    //------------//
    // getHeading //
    //------------//
    private Heading getHeading (Point prevPt,
                                Point pt)
    {
        int dx = Integer.signum(pt.x - prevPt.x);
        int dy = Integer.signum(pt.y - prevPt.y);

        return headings[1 + dy][1 + dx];
    }

    //--------------//
    // horiWithHori //
    //--------------//
    /**
     * Connect, when appropriate, the long horizontal sections (built from long runs)
     * with short horizontal sections (built later from shorter runs).
     * Without such connections, glyph building would suffer over-segmentation.
     * <p>
     * We take each long section in turn and check for connection, above and below, with short
     * sections. If positive, we cross-connect them.
     */
    private void horiWithHori ()
    {
        int maxLongId = sheet.getLongSectionMaxId();

        // Process each long section in turn
        for (Section lSect : hLag.getSections()) {
            if (lSect.getId() > maxLongId) {
                continue;
            }

            final int sectTop = lSect.getFirstPos();
            final int sectLeft = lSect.getStartCoord();
            final int sectBottom = lSect.getLastPos();
            final double[] coords = new double[2];
            final boolean[] occupied = new boolean[lSect.getLength(Orientation.HORIZONTAL)];
            Point prevPt = null;
            Point pt;
            Heading prevHeading = null;
            Heading heading = null;
            pointsAbove.clear();
            pointsBelow.clear();

            for (PathIterator it = lSect.getPathIterator(); !it.isDone();) {
                int kind = it.currentSegment(coords);
                pt = new Point((int) coords[0], (int) coords[1]);

                if (kind == SEG_LINETO) {
                    heading = getHeading(prevPt, pt);
                    logger.debug("{} {} {}", prevPt, heading, pt);

                    switch (heading) {
                    case NORTH:

                        // No pixel on right
                        if (prevHeading == Heading.WEST) {
                            removePointAbove(prevPt.x, prevPt.y - 1);
                        }

                        break;

                    case WEST: {
                        int dir = -1;

                        // Check pixels on row above
                        Arrays.fill(occupied, false);

                        int y = pt.y - 1;
                        int xStart = prevPt.x - 1;

                        if (prevHeading == Heading.SOUTH) {
                            xStart += dir;
                        }

                        // Special case for first run, check adjacent section
                        if (pt.y == sectTop) {
                            for (Section adj : lSect.getSources()) {
                                Run run = adj.getLastRun();
                                int left = Math.max(run.getStart() - 1, pt.x);
                                int right = Math.min(run.getStop() + 1, xStart);

                                for (int x = left; x <= right; x++) {
                                    occupied[x - sectLeft] = true;
                                }
                            }
                        }

                        int xBreak = pt.x - 1;

                        for (int x = xStart; x != xBreak; x += dir) {
                            if (!occupied[x - sectLeft]) {
                                addPointAbove(x, y);
                            }
                        }

                        break;
                    }

                    case SOUTH:

                        // No pixel on left
                        if (prevHeading == Heading.EAST) {
                            removePointBelow(prevPt.x - 1, prevPt.y);
                        }

                        break;

                    case EAST: {
                        int dir = +1;

                        // Check pixels on row below
                        Arrays.fill(occupied, false);

                        int y = pt.y;
                        int xStart = prevPt.x;

                        if (prevHeading == Heading.NORTH) {
                            xStart += dir;
                        }

                        int xBreak = pt.x;

                        // Special case for last run, check adjacent section
                        if ((pt.y - 1) == sectBottom) {
                            for (Section adj : lSect.getTargets()) {
                                Run run = adj.getFirstRun();
                                int left = Math.max(run.getStart() - 1, xStart);
                                int right = Math.min(run.getStop() + 1, xBreak - 1);

                                for (int x = left; x <= right; x++) {
                                    occupied[x - sectLeft] = true;
                                }
                            }
                        }

                        for (int x = xStart; x != xBreak; x += dir) {
                            if (!occupied[x - sectLeft]) {
                                addPointBelow(x, y);
                            }
                        }

                        break;
                    }
                    }
                }

                prevHeading = heading;
                prevPt = pt;
                it.next();
            }

            checkPointsAbove(lSect);
            checkPointsBelow(lSect);
        }
    }

    //--------------//
    // horiWithVert //
    //--------------//
    private void horiWithVert ()
    {
        // Process each vertical section in turn
        for (Section vSect : vLag.getSections()) {
            final int sectTop = vSect.getStartCoord();
            final int sectLeft = vSect.getFirstPos();
            final int sectRight = vSect.getLastPos();
            final double[] coords = new double[2];
            final boolean[] occupied = new boolean[vSect.getLength(Orientation.VERTICAL)];
            Point prevPt = null;
            Point pt;
            Heading prevHeading = null;
            Heading heading = null;
            pointsAside.clear();

            for (PathIterator it = vSect.getPathIterator(); !it.isDone();) {
                int kind = it.currentSegment(coords);
                pt = new Point((int) coords[0], (int) coords[1]);

                if (kind == SEG_LINETO) {
                    heading = getHeading(prevPt, pt);

                    //logger.info(prevPt + " " + heading + " " + pt);
                    switch (heading) {
                    case NORTH: {
                        int dir = -1;
                        // Check pixels on left column
                        Arrays.fill(occupied, false);

                        int x = pt.x - 1;
                        int yStart = prevPt.y - 1;

                        if (prevHeading == Heading.EAST) {
                            yStart += dir;
                        }

                        // Special case for section left run
                        if (pt.x == sectLeft) {
                            for (Section adj : vSect.getSources()) {
                                Run run = adj.getLastRun();
                                int top = Math.max(run.getStart() - 1, pt.y);
                                int bot = Math.min(run.getStop() + 1, yStart);

                                for (int y = top; y <= bot; y++) {
                                    occupied[y - sectTop] = true;
                                }
                            }
                        }

                        int yBreak = pt.y - 1;

                        for (int y = yStart; y != yBreak; y += dir) {
                            if (!occupied[y - sectTop]) {
                                addPointAside(x, y);
                            }
                        }
                    }

                    break;

                    case WEST:

                        // No pixel above
                        if (prevHeading == Heading.NORTH) {
                            removePointAside(prevPt.x - 1, prevPt.y);
                        }

                        break;

                    case SOUTH: {
                        int dir = +1;
                        // Check pixels on right column
                        Arrays.fill(occupied, false);

                        int x = pt.x;
                        int yStart = prevPt.y;

                        if (prevHeading == Heading.WEST) {
                            yStart += dir;
                        }

                        int yBreak = pt.y;

                        // Special case for section right run
                        if ((pt.x - 1) == sectRight) {
                            for (Section adj : vSect.getTargets()) {
                                Run run = adj.getFirstRun();
                                int top = Math.max(run.getStart() - 1, yStart);
                                int bot = Math.min(run.getStop() + 1, yBreak - 1);

                                for (int y = top; y <= bot; y++) {
                                    occupied[y - sectTop] = true;
                                }
                            }
                        }

                        for (int y = yStart; y != yBreak; y += dir) {
                            if (!occupied[y - sectTop]) {
                                addPointAside(x, y);
                            }
                        }
                    }

                    break;

                    case EAST:

                        // No pixel below
                        if (prevHeading == Heading.SOUTH) {
                            removePointAside(prevPt.x, prevPt.y - 1);
                        }

                        break;
                    }
                }

                prevHeading = heading;
                prevPt = pt;
                it.next();
            }

            checkPointsAside(vSect);
        }
    }

    //-------------//
    // removePoint //
    //-------------//
    private void removePoint (List<Point> points,
                              int x,
                              int y)
    {
        if (!points.isEmpty()) {
            ListIterator<Point> iter = points.listIterator(points.size());
            Point lastCorner = iter.previous();

            if ((lastCorner.x == x) && (lastCorner.y == y)) {
                iter.remove();
            }
        }
    }

    //------------------//
    // removePointAbove //
    //------------------//
    private void removePointAbove (int x,
                                   int y)
    {
        logger.debug("Removing corner above x:{} y:{}", x, y);
        removePoint(pointsAbove, x, y);
    }

    //------------------//
    // removePointAside //
    //------------------//
    private void removePointAside (int x,
                                   int y)
    {
        removePoint(pointsAside, x, y);
    }

    //------------------//
    // removePointBelow //
    //------------------//
    private void removePointBelow (int x,
                                   int y)
    {
        logger.debug("Removing corner below x:{} y:{}", x, y);
        removePoint(pointsBelow, x, y);
    }

    //-------------------------//
    // removeStaffLineSections //
    //-------------------------//
    private List<Section> removeStaffLineSections (Lag hLag)
    {
        List<Section> removedSections = hLag.purgeSections(
                new Predicate<Section>()
                {
                    @Override
                    public boolean check (Section section)
                    {
                        Glyph glyph = section.getGlyph();

                        if ((glyph != null) && (glyph.getShape() == Shape.STAFF_LINE)) {
                            /**
                             * Narrow horizontal section can be kept to avoid
                             * over-segmentation between vertical sections.
                             * TODO: keep this?
                             * TODO: use constant (instead of 1-pixel width)?
                             */
                            if ((section.getLength(Orientation.HORIZONTAL) == 1)
                                && (section.getLength(Orientation.VERTICAL) > 1)) {
                                if (section.isVip() || logger.isDebugEnabled()) {
                                    logger.info("Keeping staffline section {}", section);
                                }

                                section.setGlyph(null);

                                return false;
                            } else {
                                return true;
                            }
                        } else {
                            return false;
                        }
                    }
                });

        // Remove the underlying runs from runtable
        for (Section section : removedSections) {
        }

        return removedSections;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
