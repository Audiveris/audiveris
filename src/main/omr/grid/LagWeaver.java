//----------------------------------------------------------------------------//
//                                                                            //
//                             L a g W e a v e r                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.GlyphsBuilder;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.Sections;

import omr.run.Orientation;
import omr.run.Run;

import omr.sheet.Sheet;

import omr.util.Predicate;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Class {@code LagWeaver} is just a prototype. TODO.
 *
 * @author Hervé Bitteur
 */
public class LagWeaver
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(LagWeaver.class);

    /** Table dx/dy -> Heading */
    private static final Heading[][] headings = {
        {null, Heading.NORTH, null},
        {
            Heading.WEST, null,
            Heading.EAST
        },
        {null, Heading.SOUTH, null}
    };

    //~ Enumerations -----------------------------------------------------------
    private static enum Heading
    {
        //~ Enumeration constant initializers ----------------------------------

        NORTH,
        EAST,
        SOUTH,
        WEST;

        //~ Methods ------------------------------------------------------------
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

    //~ Instance fields --------------------------------------------------------
    /** Related sheet */
    private final Sheet sheet;

    /** Vertical lag */
    private final Lag vLag;

    /** Horizontal lag */
    private final Lag hLag;

    /**
     * Actual points around current vLag section to check to hLag presence
     * (relevant only during horiWithVert)
     */
    private final List<Point> pointsAside = new ArrayList<>();

    /** Points to check for source sections above in hLag */
    private final List<Point> pointsAbove = new ArrayList<>();

    /** Points to check for target sections below in hLag */
    private final List<Point> pointsBelow = new ArrayList<>();

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // LagWeaver //
    //-----------//
    /**
     * Creates a new LagWeaver object.
     *
     * @param sheet the related sheet, which holds the v & h lags
     */
    public LagWeaver (Sheet sheet)
    {
        this.sheet = sheet;

        vLag = sheet.getVerticalLag();
        hLag = sheet.getHorizontalLag();
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // buildInfo //
    //-----------//
    public void buildInfo ()
    {
        StopWatch watch = new StopWatch("LagWeaver");

        // Remove staff line stuff from hLag
        watch.start("purge hLag");

        List<Section> staffLinesSections = removeStaffLines(hLag);

        logger.debug("{}StaffLine sections removed: {}",
                sheet.getLogPrefix(), staffLinesSections.size());

        watch.start("Hori <-> Hori");
        horiWithHori();

        watch.start("Hori <-> Vert");
        horiWithVert();

        watch.start("buildGlyphs");
        buildGlyphs();

        // The end
        ///watch.print();
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

    //-------------//
    // buildGlyphs //
    //-------------//
    private void buildGlyphs ()
    {
        // Group (unknown) sections into glyphs
        // Consider all unknown vertical & horizontal sections
        List<Section> allSections = new ArrayList<>();

        for (Section section : sheet.getVerticalLag().getSections()) {
            if (!section.isKnown()) {
                section.setProcessed(false);
                allSections.add(section);
            } else {
                section.setProcessed(true);
            }
        }

        for (Section section : sheet.getHorizontalLag().getSections()) {
            if (!section.isKnown()) {
                section.setProcessed(false);
                allSections.add(section);
            } else {
                section.setProcessed(true);
            }
        }

        GlyphsBuilder.retrieveGlyphs(
                allSections,
                sheet.getNest(),
                sheet.getScale());
    }

    //------------------//
    // checkPointsAbove //
    //------------------//
    private void checkPointsAbove (Section lSect)
    {
        boolean added = false;

        for (Point pt : pointsAbove) {
            Run run = hLag.getRunAt(pt.x, pt.y);

            if (run != null) {
                Section hSect = run.getSection();

                if (hSect != null) {
                    hSect.addTarget(lSect);
                    added = true;
                }
            }
        }

        if (added && logger.isDebugEnabled()) {
            logger.info("lSect#{} checks:{}{}",
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

        for (Point pt : pointsAside) {
            Run run = hLag.getRunAt(pt.x, pt.y);

            if (run != null) {
                Section hSect = run.getSection();

                if (hSect != null) {
                    vSect.addOppositeSection(hSect);
                    hSect.addOppositeSection(vSect);
                    added = true;
                }
            }
        }

        if (added && logger.isDebugEnabled()) {
            logger.info("vSect#{} checks:{}{}",
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

        for (Point pt : pointsBelow) {
            Run run = hLag.getRunAt(pt.x, pt.y);

            if (run != null) {
                Section hSect = run.getSection();

                if (hSect != null) {
                    lSect.addTarget(hSect);
                    added = true;
                }
            }
        }

        if (added && logger.isDebugEnabled()) {
            logger.info("lSect#{} checks:{}{}", lSect.getId(),
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
     * Connect, when appropriate, the long horizontal sections (built from long
     * runs) with short horizontal sections (built later from shorter runs).
     * Without such connections, glyph building would suffer over-segmentation.
     *
     * <p>We take each long section in turn and check for connection, above and
     * below, with short sections. If positive, we cross-connect them.
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
            final boolean[] occupied = new boolean[lSect.getLength(
                    Orientation.HORIZONTAL)];
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
                                int right = Math.min(
                                        run.getStop() + 1,
                                        xBreak - 1);

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
            final boolean[] occupied = new boolean[vSect.getLength(
                    Orientation.VERTICAL)];
            Point prevPt = null;
            Point pt = null;
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
                                int bot = Math.min(
                                        run.getStop() + 1,
                                        yBreak - 1);

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

    //------------------//
    // removeStaffLines //
    //------------------//
    private List<Section> removeStaffLines (Lag hLag)
    {
        return hLag.purgeSections(
                new Predicate<Section>()
        {
            @Override
            public boolean check (Section section)
            {
                Glyph glyph = section.getGlyph();

                if ((glyph != null)
                    && (glyph.getShape() == Shape.STAFF_LINE)) {
                    /**
                     * Narrow horizontal section can be kept to avoid
                     * over-segmentation between vertical sections
                     */
                    if ((section.getLength(Orientation.HORIZONTAL) == 1)
                        && (section.getLength(Orientation.VERTICAL) > 1)) {
                        if (section.isVip() || logger.isDebugEnabled()) {
                            logger.info("Keeping staffline section {}",
                                    section);
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
    }
}
