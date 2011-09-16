//----------------------------------------------------------------------------//
//                                                                            //
//                             L a g W e a v e r                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.Sections;

import omr.log.Logger;

import omr.run.Run;
import omr.run.RunsTable;

import omr.sheet.Sheet;

import omr.util.Predicate;
import omr.util.StopWatch;

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
    private static final Logger logger = Logger.getLogger(LagWeaver.class);

    /** Table dx/dy -> Heading */
    private static final Heading[][] headings = {
                                                    { null, Heading.NORTH, null },
                                                    {
                                                        Heading.WEST, null,
                                                        Heading.EAST
                                                    },
                                                    { null, Heading.SOUTH, null }
                                                };

    //~ Enumerations -----------------------------------------------------------

    private static enum Heading {
        //~ Enumeration constant initializers ----------------------------------

        NORTH,EAST, SOUTH,
        WEST;

        //~ Methods ------------------------------------------------------------

        public boolean insideCornerTo (Heading next)
        {
            switch (this) {
            case NORTH :
                return next == WEST;

            case EAST :
                return next == NORTH;

            case SOUTH :
                return next == EAST;

            case WEST :
                return next == SOUTH;
            }

            return false; // Unreachable stmt
        }
    }

    //~ Instance fields --------------------------------------------------------

    /** Vertical lag */
    private final GlyphLag vLag;

    /** Horizontal lag */
    private final GlyphLag hLag;

    /**
     * Actual points around current vLag section to check to hLag presence
     * (relevant only during crossRetrieval)
     */
    private final List<Point> points = new ArrayList<Point>();

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // LagWeaver //
    //-----------//
    /**
     * Creates a new LagWeaver object.
     * @param sheet the related sheet, which holds the v & h lags
     */
    public LagWeaver (Sheet sheet)
    {
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

        logger.info("vLag: " + vLag);
        logger.info("hLag: " + hLag);

        // Remove staff line stuff from hLag
        watch.start("purge hLag");
        removeStaffLines(hLag);
        logger.info("hLag: " + hLag);

        //
        watch.start("crossRetrieval");
        crossRetrieval();

        // The end
        watch.print();
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

    //----------//
    // addPoint //
    //----------//
    private void addPoint (int x,
                           int y)
    {
        //logger.fine("addPoint " + x + "," + y);
        points.add(new Point(x, y));
    }

    //-------------//
    // checkPoints //
    //-------------//
    private void checkPoints (GlyphSection vSect)
    {
        boolean added = false;

        for (Point pt : points) {
            Run run = hLag.getRunAt(pt.x, pt.y);

            if (run != null) {
                GlyphSection hSect = (GlyphSection) run.getSection();

                if (hSect != null) {
                    vSect.addCrossSection(hSect);
                    added = true;
                }
            }
        }

        if (added && logger.isFineEnabled()) {
            logger.info(
                "vSect#" + vSect.getId() + " checks:" + points.size() +
                Sections.toString(" hSects", vSect.getCrossSections()));
        }
    }

    //----------------//
    // crossRetrieval //
    //----------------//
    private void crossRetrieval ()
    {
        // Process each vertical section in turn
        for (GlyphSection vSect : vLag.getSections()) {
            //GlyphSection vSect = vLag.getVertexById(1924);
            if (logger.isFineEnabled()) {
                logger.fine("vSect: " + vSect);
            }

            final int       sectTop = vSect.getStart();
            final int       sectLeft = vSect.getFirstPos();
            final int       sectRight = vSect.getLastPos();
            final double[]  coords = new double[2];
            final boolean[] occupied = new boolean[vSect.getLength()];
            Point           prevPt = null;
            Point           pt = null;
            Heading         prevHeading = null;
            Heading         heading = null;
            points.clear();

            for (PathIterator it = vSect.getPathIterator(); !it.isDone();) {
                int kind = it.currentSegment(coords);
                pt = new Point((int) coords[0], (int) coords[1]);

                if (kind == SEG_LINETO) {
                    heading = getHeading(prevPt, pt);

                    switch (heading) {
                    case NORTH : {
                        int dir = -1;
                        // Check pixels on left column
                        //logger.info(prevPt + " " + heading + " " + pt);
                        Arrays.fill(occupied, false);

                        int x = pt.x - 1;
                        int yStart = prevPt.y - 1;

                        if (prevHeading == Heading.EAST) {
                            yStart += dir;
                        }

                        // Special case for section left run
                        if (pt.x == sectLeft) {
                            for (GlyphSection adj : vSect.getSources()) {
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
                                addPoint(x, y);
                            }
                        }
                    }

                    break;

                    case WEST :

                        // No pixel above
                        //logger.fine(prevPt + " " + heading + " " + pt);
                        if (prevHeading == Heading.NORTH) {
                            removePoint(prevPt.x - 1, prevPt.y);
                        }

                        break;

                    case SOUTH : {
                        int dir = +1;
                        // Check pixels on right column
                        Arrays.fill(occupied, false);

                        //logger.info(prevPt + " " + heading + " " + pt);
                        int x = pt.x;
                        int yStart = prevPt.y;

                        if (prevHeading == Heading.WEST) {
                            yStart += dir;
                        }

                        int yBreak = pt.y;

                        // Special case for section right run
                        if ((pt.x - 1) == sectRight) {
                            for (GlyphSection adj : vSect.getTargets()) {
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
                                addPoint(x, y);
                            }
                        }
                    }

                    break;

                    case EAST :

                        // No pixel below
                        //logger.fine(prevPt + " " + heading + " " + pt);
                        if (prevHeading == Heading.SOUTH) {
                            removePoint(prevPt.x, prevPt.y - 1);
                        }

                        break;
                    }
                }

                prevHeading = heading;
                prevPt = pt;
                it.next();
            }

            checkPoints(vSect);
        }
    }

    //-------------//
    // removePoint //
    //-------------//
    private void removePoint (int x,
                              int y)
    {
        //logger.info("Removing corner at x:" + x + " y:" + y);
        if (!points.isEmpty()) {
            ListIterator<Point> iter = points.listIterator(points.size());
            Point               lastCorner = iter.previous();

            if ((lastCorner.x == x) && (lastCorner.y == y)) {
                iter.remove();
            }
        }
    }

    //------------------//
    // removeStaffLines //
    //------------------//
    private void removeStaffLines (GlyphLag hLag)
    {
        hLag.purgeSections(
            new Predicate<GlyphSection>() {
                    public boolean check (GlyphSection section)
                    {
                        Glyph glyph = section.getGlyph();

                        return (glyph != null) &&
                               (glyph.getShape() == Shape.STAFF_LINE);
                    }
                });
    }
}
