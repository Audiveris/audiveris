//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S l u r s B u i l d e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.grid.FilamentLine;
import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.image.ImageUtil;
import static omr.image.PixelSource.BACKGROUND;
import static omr.image.PixelSource.FOREGROUND;

import omr.math.BasicLine;
import omr.math.Circle;
import omr.math.GeoPath;
import omr.math.PointUtil;

import omr.score.ui.PageEraser;
import static omr.sheet.SlurInfo.Arc;
import static omr.sheet.SlurInfo.Arc.ArcShape;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;

import omr.sig.Exclusion.Cause;
import omr.sig.GradeImpacts;
import omr.sig.SIGraph;
import omr.sig.SlurInter;
import omr.sig.SlurInter.Impacts;

import omr.ui.BoardsPane;
import omr.ui.util.ItemRenderer;
import omr.ui.util.UIUtil;

import omr.util.Navigable;
import omr.util.StopWatch;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code SlursBuilder} retrieves slurs in a sheet by walking
 * along the arcs of image skeleton.
 * <p>
 * We have to visit each pixel of the buffer, detect junction points and arcs
 * departing or arriving at junction points.
 * A point if a junction point if it has more than 2 immediate neighbors in the
 * 8 peripheral cells of the 3x3 square centered on the point.
 * We may need a way to detect if a given point has already been visited.
 *
 * @author Hervé Bitteur
 */
public class SlursBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Point[] breakPoints = new Point[]{ ///new Point(234, 871)
    }; // BINGO

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlursBuilder.class);

    /** Color for a foreground pixel discarded. */
    private static final int HIDDEN = 230;

    /** Color for a foreground pixel already processed. */
    private static final int PROCESSED = 180;

    /** Color for a foreground pixel part of arc. */
    private static final int ARC = 140;

    /** Color for a foreground junction pixel. */
    private static final int JUNCTION = 80;

    /** Color for a foreground junction pixel already processed. */
    private static final int JUNCTION_PROCESSED = 90;

    /**
     * Directions.
     * <pre>
     * +-----+-----+-----+
     * |     |     |     |
     * |  7  |  8  |  1  |
     * |     |     |     |
     * +-----+-----+-----+
     * |     |     |     |
     * |  6  |  0  |  2  |
     * |     |     |     |
     * +-----+-----+-----+
     * |     |     |     |
     * |  5  |  4  |  3  |
     * |     |     |     |
     * +-----+-----+-----+
     * </pre>
     */
    /** Delta abscissa, per direction. ... 0. 1. 2. 3. 4 . 5 . 6 . 7. 8 */
    private static final int[] dxs = new int[]{0, 1, 1, 1, 0, -1, -1, -1, 0};

    /** Delta ordinate, per direction. ... 0 . 1. 2. 3. 4. 5. 6 . 7. 8 */
    private static final int[] dys = new int[]{0, -1, 0, 1, 1, 1, 0, -1, -1};

    /** Directions to scan, according to last direction. */
    private static final int[][] scans = new int[][]{
        {2, 4, 6, 8, 1, 3, 5, 7}, // 0
        {2, 8, 1, 3, 7}, // 1
        {2, 4, 8, 1, 3}, // 2
        {2, 4, 1, 3, 5}, // 3
        {2, 4, 6, 3, 5}, // 4
        {4, 6, 3, 5, 7}, // 5
        {4, 6, 8, 5, 7}, // 6
        {6, 8, 1, 5, 7}, // 7
        {2, 6, 8, 1, 7} //  8
    };

    /** Map (Dx,Dy) -> Direction. */
    private static final int[][] deltaToDir = new int[][]{
        {7, 6, 5}, // x:-1, y: -1, 0, +1
        {8, 0, 4}, // x: 0, y: -1, 0, +1
        {1, 2, 3} //  x:+1, y: -1, 0, +1
    };

    /** Vertical directions: south & north. */
    private static final int[] vertDirs = new int[]{4, 8};

    /** Horizontal directions: east & west. */
    private static final int[] horiDirs = new int[]{2, 6};

    /** Side directions: verticals + horizontals. */
    private static final int[] sideDirs = new int[]{2, 4, 6, 8};

    /** Diagonal directions: ne, se, sw, nw. */
    private static final int[] diagDirs = new int[]{1, 3, 5, 7};

    /** All directions. */
    private static final int[] allDirs = new int[]{2, 4, 6, 8, 1, 3, 5, 7};

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * Status for current move along arc.
     */
    private static enum Status
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** One more point on arc. */
        CONTINUE,
        /** Arrived at a new junction point. */
        SWITCH,
        /** No more move possible. */
        END;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Global sheet skew. */
    private final Skew skew;

    /** Sheet staves. */
    private final StaffManager staffManager;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Companion for slur-notes connections. */
    private final SlursLinker slursLinker;

    /** The skeleton buffer. */
    private final ByteProcessor buf;

    /** All slur infos created. */
    private final List<SlurInfo> pageInfos = new ArrayList<SlurInfo>();

    /** All slurs retrieved. */
    private final List<SlurInter> pageSlurs = new ArrayList<SlurInter>();

    /** Map of relevant arcs. (end points -> arc) */
    private final Map<Point, Arc> arcsMap = new LinkedHashMap<Point, Arc>();

    /** List of arcs end points, with no junction, by abscissa. */
    private final List<Point> arcsEnds = new ArrayList<Point>();

    /** For unique slur IDs. (page wise) */
    private int globalSlurId = 0;

    /** View on skeleton, if any. */
    private MyView view;

    //~ Constructors -------------------------------------------------------------------------------
    //--------------//
    // SlursBuilder //
    //--------------//
    /**
     * Creates a new SlursBuilder object.
     *
     * @param sheet the related sheet
     */
    public SlursBuilder (Sheet sheet)
    {
        this.sheet = sheet;
        skew = sheet.getSkew();
        staffManager = sheet.getStaffManager();
        params = new Parameters(sheet.getScale());
        slursLinker = new SlursLinker(sheet);

        buf = buildSkeleton();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildSlurs //
    //------------//
    /**
     * Build slurs out of the image skeleton, by appending arcs.
     */
    public void buildSlurs ()
    {
        logger.debug("{}buildSlurs", sheet.getLogPrefix());

        StopWatch watch = new StopWatch("Slurs");

        // Retrieve junctions.
        watch.start("retrieveJunctions");
        new JunctionRetriever().scanImage();

        // Scan arcs between junctions
        watch.start("retrieveArcs");
        new ArcRetriever().scanImage();

        // Retrieve slurs from arcs
        watch.start("retrieveSlurs");
        new SlurRetriever().retrieveSlurs();

        watch.print();
    }

    //--------//
    // getDir //
    //--------//
    /**
     * Report the precise direction that goes from 'from' to 'to'.
     *
     * @param from p1
     * @param to   p2
     * @return direction p1 -> p2
     */
    private static int getDir (Point from,
                               Point to)
    {
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        return deltaToDir[1 + dx][1 + dy];
    }

    //-------//
    // sinSq //
    //-------//
    /** Sin**2 of angle between (p0,p1) & (p0,p2). */
    private static double sinSq (int x0,
                                 int y0,
                                 int x1,
                                 int y1,
                                 int x2,
                                 int y2)
    {
        x1 -= x0;
        y1 -= y0;
        x2 -= x0;
        y2 -= y0;

        double vect = (x1 * y2) - (x2 * y1);
        double l1Sq = (x1 * x1) + (y1 * y1);
        double l2Sq = (x2 * x2) + (y2 * y2);

        return (vect * vect) / (l1Sq * l2Sq);
    }

    //------------//
    // areSimilar //
    //------------//
    /**
     * Check whether the provided circles have similar radius
     *
     * @param c1 a circle
     * @param c2 another circle
     * @return true if similar
     */
    private boolean areSimilar (Circle c1,
                                Circle c2)
    {
        double r1 = c1.getRadius();
        double r2 = c2.getRadius();
        double difRatio = abs(r1 - r2) / (max(r1, r2));

        return difRatio <= params.similarRadiusRatio;
    }

    //---------------//
    // buildSkeleton //
    //---------------//
    /**
     * Generate the skeleton from page binary image.
     *
     * @return the skeleton buffer
     */
    private ByteProcessor buildSkeleton ()
    {
        // First, get a skeleton of binary image
        Picture picture = sheet.getPicture();

        ///ByteProcessor buffer = picture.getSource(Picture.SourceKey.BINARY);
        ByteProcessor buffer = picture.getSource(Picture.SourceKey.STAFF_LINE_FREE);
        buffer = (ByteProcessor) buffer.duplicate();
        buffer.skeletonize();

        BufferedImage img = buffer.getBufferedImage();

        // Erase good shapes of each system
        Graphics2D g = img.createGraphics();
        PageEraser eraser = new PageEraser(g, sheet);
        eraser.erase(
                Arrays.asList(
                        Shape.THICK_BARLINE,
                        Shape.THIN_BARLINE,
                        Shape.THIN_CONNECTION,
                        Shape.THICK_CONNECTION,
                        Shape.STEM,
                        Shape.WHOLE_NOTE,
                        Shape.WHOLE_NOTE_SMALL,
                        Shape.NOTEHEAD_BLACK,
                        Shape.NOTEHEAD_BLACK_SMALL,
                        Shape.NOTEHEAD_VOID,
                        Shape.NOTEHEAD_VOID_SMALL,
                        Shape.BEAM,
                        Shape.BEAM_HOOK,
                        Shape.BEAM_SMALL,
                        Shape.BEAM_HOOK_SMALL));

        // Draw a background rectangle around the image
        g.setColor(Color.WHITE);
        g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
        g.dispose();

        // Build buffer
        buffer = new ByteProcessor(img);
        buffer.threshold(127);

        // Keep a copy on disk?
        if (constants.keepSkeleton.isSet()) {
            ImageUtil.saveOnDisk(img, sheet.getPage().getId() + ".skl");
        }

        // Display skeleton
        if (Main.getGui() != null) {
            view = new MyView(img);
            sheet.getAssembly().addViewTab(
                    "Skeleton",
                    new ScrollImageView(sheet, view),
                    new BoardsPane(new PixelBoard(sheet)));
        }

        return buffer;
    }

    //------------//
    // checkBreak //
    //------------//
    /**
     * Debug method to break on a specific arc.
     *
     * @param arc
     */
    private void checkBreak (Arc arc)
    {
        if (arc == null) {
            return;
        }

        for (Point pt : breakPoints) {
            if (pt.equals(arc.getEnd(false)) || pt.equals(arc.getEnd(true))) {
                view.selectPoint(arc.getEnd(true));
                logger.warn("BINGO break on {}", arc);

                break;
            }
        }
    }

    //-----------//
    // getCircle //
    //-----------//
    /**
     * Check whether the provided collection of points can represent
     * a slur (or a portion of a slur).
     *
     * @param points the provided points
     * @return the circle arc if OK, null if not
     */
    private Circle getCircle (List<Point> points)
    {
        Point p0 = points.get(0);
        Point p1 = points.get(points.size() / 2);
        Point p2 = points.get(points.size() - 1);

        // Compute rough circle values for quick tests
        Circle rough = new Circle(p0, p1, p2);

        // Minimum circle radius
        double radius = rough.getRadius();

        if (radius < params.minCircleRadius) {
            logger.debug("Arc radius too small {} at {}", radius, p0);

            return null;
        }

        // Max arc angle value
        double arcAngle = rough.getLastAngle() - rough.getFirstAngle();

        if (arcAngle > params.maxArcAngleHigh) {
            logger.debug("Arc angle too large {} at {}", arcAngle, p0);

            return null;
        }

        // Now compute "precise" circle
        try {
            Circle fitted = new Circle(points);

            // Check circle radius is rather similar to rough radius
            // If not, keep the rough one.
            final Circle circle;
            final double dist;

            if (areSimilar(fitted, rough)) {
                circle = fitted;
                dist = circle.getDistance();
            } else {
                circle = rough;
                dist = rough.computeDistance(points);
                rough.setDistance(dist);
            }

            if (dist > params.maxArcsDistance) {
                logger.debug("Bad circle fit {} at {}", dist, p0);

                return null;
            }

            logger.debug("{} to {} Circle {}", p0, p2, rough);

            return rough;
        } catch (Exception ex) {
            logger.debug("Could not compute circle {} at {}", p0);

            return null;
        }
    }

    //------------//
    // isJunction //
    //------------//
    /**
     * Tell whether the pixel value indicates a junction point.
     *
     * @param pix pixel gray value
     * @return true if junction
     */
    private boolean isJunction (int pix)
    {
        return (pix >= JUNCTION) && (pix <= (JUNCTION + 10));
    }

    //---------------------//
    // isJunctionProcessed //
    //---------------------//
    /**
     * Tell whether the pixel value indicates a junction point
     * already processed.
     *
     * @param pix pixel gray value
     * @return true if junction already processed
     */
    private boolean isJunctionProcessed (int pix)
    {
        return pix == JUNCTION_PROCESSED;
    }

    //-------------//
    // isProcessed //
    //-------------//
    /**
     * Tell whether the pixel value indicates an end point of an arc
     * already processed.
     *
     * @param pix pixel gray value
     * @return true if arc already processed
     */
    private boolean isProcessed (int pix)
    {
        return (pix >= PROCESSED) && (pix < (PROCESSED + 10));
    }

    //--------//
    // isSide //
    //--------//
    /**
     * Tell whether the provided direction is a side one (H or V).
     *
     * @param dir provided direction
     * @return true if horizontal or vertical
     */
    private boolean isSide (int dir)
    {
        return (dir % 2) == 0;
    }

    //------------//
    // isStaffArc //
    //------------//
    /**
     * Check whether this arc is simply a part of a staff line.
     *
     * @return true if positive
     */
    private boolean isStaffArc (Arc arc)
    {
        List<Point> points = arc.points;

        if (points.size() < params.minStaffArcLength) {
            return false;
        }

        Point p0 = points.get(0);
        StaffInfo staff = staffManager.getStaffAt(p0);
        FilamentLine line = staff.getClosestLine(p0);
        double maxDist = 0;
        double maxDy = Double.MIN_VALUE;
        double minDy = Double.MAX_VALUE;

        for (int i : new int[]{0, points.size() / 2, points.size() - 1}) {
            Point p = points.get(i);
            double dist = p.y - line.yAt(p.x);
            maxDist = max(maxDist, abs(dist));
            maxDy = max(maxDy, dist);
            minDy = min(minDy, dist);
        }

        return (maxDist < params.minStaffLineDistance)
               && ((maxDy - minDy) < params.minStaffLineDistance);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------------//
    // ArcRetriever //
    //--------------//
    /**
     * Retrieve all arcs and remember the interesting ones in arcsMap.
     * Each arc has its two ending points flagged with a specific gray value to
     * remember the arc shape.
     * <pre>
     * - scanImage()              // Scan the whole image for arc starts
     *   + scanJunction()         // Scan arcs leaving a junction point
     *   |   + scanArc()
     *   + scanArc()              // Scan one arc
     *       + walkAlong()        // Walk till arc end (forward or backward)
     *       |   + move()         // Move just one pixel
     *       + determineShape()   // Determine the global arc shape
     *       + storeShape()       // Store arc shape in its ending pixels
     * </pre>
     */
    private class ArcRetriever
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Current point abscissa. */
        int cx;

        /** Current point ordinate. */
        int cy;

        /** Last direction. */
        int lastDir;

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Scan the whole image.
         */
        public void scanImage ()
        {
            for (int x = 1, w = buf.getWidth(); x < w; x++) {
                for (int y = 1, h = buf.getHeight(); y < h; y++) {
                    int pix = buf.get(x, y);

                    if (pix == ARC) {
                        // Basic arc pixel, not yet processed, scan full arc
                        scanArc(x, y, null, 0);
                    } else if (isJunction(pix)) {
                        // Junction pixel, scan arcs linked to this junction point
                        if (!isJunctionProcessed(pix)) {
                            scanJunction(x, y);
                        }
                    }
                }
            }

            // Sort arcsEnds by abscissa
            Collections.sort(
                    arcsEnds,
                    new Comparator<Point>()
            {
                @Override
                public int compare (Point p1,
                                    Point p2)
                {
                    return Integer.compare(p1.x, p2.x);
                }
                    });
        }

        /**
         * Record one more point into arc sequence
         *
         * @param reverse scan orientation
         */
        private void addPoint (Arc arc,
                               int cx,
                               int cy,
                               boolean reverse)
        {
            List<Point> points = arc.points;

            if (reverse) {
                points.add(0, new Point(cx, cy));
            } else {
                points.add(new Point(cx, cy));
            }

            buf.set(cx, cy, PROCESSED);
        }

        /**
         * Determine shape for this arc.
         *
         * @param arc arc to evaluate
         * @return the shape classification
         */
        private ArcShape determineShape (Arc arc)
        {
            ///checkBreak(arc);
            List<Point> points = arc.points;

            // Too short?
            if (points.size() < params.arcMinQuorum) {
                ///logger.info("Too short: {}", points.size());
                return ArcShape.SHORT;
            }

            // Check arc is not just a long portion of staff line
            if (isStaffArc(arc)) {
                //logger.info("Staff Line");
                if (arc.getLength() > params.maxStaffArcLength) {
                    return ArcShape.IRRELEVANT;
                } else {
                    return ArcShape.STAFF_ARC;
                }
            }

            // Straight line?
            // Check mid point for colinearity
            Point p0 = points.get(0);
            Point p1 = points.get(points.size() / 2);
            Point p2 = points.get(points.size() - 1);
            double sinSq = sinSq(p0.x, p0.y, p1.x, p1.y, p2.x, p2.y);

            if (sinSq <= params.maxSinSq) {
                ///logger.info("3 colinear points");

                // This cannot be a slur, but perhaps a straight line.
                // Check mean distance to straight line
                BasicLine line = new BasicLine(points);
                double dist = line.getMeanDistance();

                if (dist <= params.maxLineDistance) {
                    // Check this is not a portion of staff line or bar line
                    double invSlope = line.getInvertedSlope();

                    if (abs(invSlope + skew.getSlope()) <= params.minSlope) {
                        //logger.info("Vertical line");
                        return ArcShape.IRRELEVANT;
                    }

                    double slope = line.getSlope();

                    if (abs(slope - skew.getSlope()) <= params.minSlope) {
                        //logger.info("Horizontal  line");
                    }

                    ///logger.info("Straight line");
                    return ArcShape.LINE;
                }
            }

            // Circle?
            Circle fittedCircle = getCircle(points);

            if (fittedCircle != null) {
                arc.circle = fittedCircle;

                return ArcShape.SLUR;
            }

            // Nothing interesting
            return ArcShape.IRRELEVANT;
        }

        /**
         * Update display to show the arc points as "discarded".
         */
        private void hide (Arc arc)
        {
            List<Point> points = arc.points;

            for (int i = 1; i < (points.size() - 1); i++) {
                Point p = points.get(i);

                buf.set(p.x, p.y, HIDDEN);
            }
        }

        /**
         * Try to move to the next point of the arc.
         *
         * @param x       abscissa of current point
         * @param y       ordinate of current point
         * @param reverse current orientation
         * @return code describing the move performed if any.
         *         The new position is stored in (cx, cy).
         */
        private Status move (Arc arc,
                             int x,
                             int y,
                             boolean reverse)
        {
            // First, check for junctions within reach to stop
            for (int dir : scans[lastDir]) {
                cx = x + dxs[dir];
                cy = y + dys[dir];

                int pix = buf.get(cx, cy);

                if (isJunction(pix)) {
                    // End of scan for this orientation
                    Point junctionPt = new Point(cx, cy);
                    arc.setJunction(junctionPt, reverse);
                    lastDir = dir;

                    return Status.SWITCH;
                }
            }

            // No junction to stop, so move along the arc
            for (int dir : scans[lastDir]) {
                cx = x + dxs[dir];
                cy = y + dys[dir];

                int pix = buf.get(cx, cy);

                if (pix == ARC) {
                    lastDir = dir;

                    return Status.CONTINUE;
                }
            }

            // The end (dead end or back to start)
            return Status.END;
        }

        /**
         * Scan an arc both ways, starting from a point of the arc,
         * not necessarily an end point.
         *
         * @param x             starting abscissa
         * @param y             starting ordinate
         * @param startJunction start junction point if any
         * @param lastDir       last direction (0 if none)
         * @return the arc fully scanned
         */
        private Arc scanArc (int x,
                             int y,
                             Point startJunction,
                             int lastDir)
        {
            // Remember starting point
            Arc arc = new Arc(startJunction);
            addPoint(arc, x, y, false);

            // Scan arc on normal side -> stopJunction
            walkAlong(arc, x, y, false, lastDir);

            // Scan arc on reverse side -> startJunction if needed
            // If we scanned from a junction, startJunction is already set
            if (startJunction == null) {
                // Set lastDir as the opposite of initial starting dir
                if (arc.points.size() > 1) {
                    lastDir = getDir(arc.points.get(1), arc.points.get(0));
                } else if (arc.getJunction(false) != null) {
                    lastDir = getDir(arc.getJunction(false), arc.points.get(0));
                }

                walkAlong(arc, x, y, true, lastDir);
            }

            // Check arc shape
            ArcShape shape = determineShape(arc);
            storeShape(arc, shape);

            if (shape.isSlurRelevant()) {
                Point first = arc.points.get(0);
                arcsMap.put(first, arc);
                arcsEnds.add(first);

                Point last = arc.points.get(arc.points.size() - 1);
                arcsMap.put(last, arc);
                arcsEnds.add(last);

                return arc;
            } else {
                hide(arc);

                return null;
            }
        }

        /**
         * Scan all arcs connected to this junction point
         *
         * @param x junction point abscissa
         * @param y junction point ordinate
         */
        private void scanJunction (int x,
                                   int y)
        {
            Point startJunction = new Point(x, y);
            buf.set(x, y, JUNCTION_PROCESSED);

            // Scan all arcs that depart from this junction point
            for (int dir : allDirs) {
                int nx = x + dxs[dir];
                int ny = y + dys[dir];
                int pix = buf.get(nx, ny);

                if (pix == ARC) {
                    scanArc(nx, ny, startJunction, dir);
                } else if (isJunction(pix)) {
                    if (!isJunctionProcessed(pix)) {
                        // We have a junction point, touching this one
                        // Use a no-point arg
                        Point stopJunction = new Point(nx, ny);
                        Arc arc = new Arc(startJunction, stopJunction);
                        arcsMap.put(startJunction, arc);
                        arcsMap.put(stopJunction, arc);
                    }
                }
            }
        }

        /**
         * "Store" the arc shape in its ending points, so that scanning
         * from a junction point can immediately know whether the arc
         * is relevant without having to rescan the full arc.
         *
         * @param shape the arc shape
         */
        private void storeShape (Arc arc,
                                 ArcShape shape)
        {
            arc.shape = shape;

            List<Point> points = arc.points;

            Point first = points.get(0);
            Point last = points.get(points.size() - 1);
            buf.set(first.x, first.y, PROCESSED + shape.ordinal());
            buf.set(last.x, last.y, PROCESSED + shape.ordinal());
        }

        /**
         * Walk along the arc in the desired orientation, starting at
         * (x,y) point, until no more incremental move is possible.
         * Detect the end of a straight line (either horizontal or vertical)
         * and insert an artificial junction point.
         *
         * @param xStart  starting abscissa
         * @param yStart  starting ordinate
         * @param reverse normal (-> stop) or reverse orientation (-> start)
         * @param lastDir arrival at (x,y) if any, 0 if none
         */
        private void walkAlong (Arc arc,
                                int xStart,
                                int yStart,
                                boolean reverse,
                                int lastDir)
        {
            this.lastDir = lastDir;
            cx = xStart;
            cy = yStart;

            Status status;

            while (Status.CONTINUE == (status = move(arc, cx, cy, reverse))) {
                addPoint(arc, cx, cy, reverse);
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean keepSkeleton = new Constant.Boolean(
                false,
                "Should we store skeleton images on disk?");

        final Constant.Ratio similarRadiusRatio = new Constant.Ratio(
                0.25,
                "Maximum difference ratio between radius of similar circles");

        final Constant.Double maxAlpha = new Constant.Double(
                "degree",
                2.5,
                "Maximum angle (in degrees) for 3 points colinearity");

        final Constant.Double maxIncidence = new Constant.Double(
                "degree",
                45,
                "Maximum incidence angle (in degrees) for staff tangency");

        final Scale.Fraction arcMinQuorum = new Scale.Fraction(
                1.5,
                "Minimum arc length for quorum");

        final Scale.Fraction arcMinSeedLength = new Scale.Fraction(
                1.0,
                "Minimum arc length for starting a slur build");

        final Scale.Fraction maxStaffLineDy = new Scale.Fraction(
                0.2,
                "Vertical distance to closest staff line to detect tangency");

        final Scale.Fraction maxLineDistance = new Scale.Fraction(
                0.1,
                "Maximum distance from straight line");

        final Scale.Fraction maxSlurDistance = new Scale.Fraction(
                0.07,
                "Maximum circle distance for final slur");

        final Scale.Fraction maxExtDistance = new Scale.Fraction(
                0.35,
                "Maximum circle distance for extension arc");

        final Scale.Fraction maxArcsDistance = new Scale.Fraction(
                0.15,
                "Maximum circle distance for intermediate arcs");

        final Scale.Fraction arcCheckLength = new Scale.Fraction(
                3,
                "Number of points checked for extension arc");

        final Scale.Fraction sideCircleLength = new Scale.Fraction(
                7,
                "Number of points for side osculatory circle");

        final Scale.Fraction minCircleRadius = new Scale.Fraction(
                0.5,
                "Minimum circle radius for a slur");

        final Scale.Fraction maxCircleRadius = new Scale.Fraction(
                50,
                "Maximum circle radius for a slur");

        final Scale.Fraction minSlurWidthLow = new Scale.Fraction(
                0.8,
                "Low minimum width for a slur");

        final Scale.Fraction minSlurWidthHigh = new Scale.Fraction(
                1.5,
                "High minimum width for a slur");

        final Scale.Fraction minSlurHeightLow = new Scale.Fraction(
                0.2,
                "Low minimum height for a slur");

        final Scale.Fraction minSlurHeightHigh = new Scale.Fraction(
                1.0,
                "High minimum height for a slur");

        final Scale.Fraction minStaffLineDistance = new Scale.Fraction(
                0.15,
                "Minimum distance from staff line");

        final Scale.Fraction minStaffArcLength = new Scale.Fraction(
                0.5,
                "Minimum length for a staff arc");

        final Scale.Fraction maxStaffArcLength = new Scale.Fraction(
                5.0,
                "Maximum length for a staff arc");

        final Scale.Fraction gapBoxLength = new Scale.Fraction(0.5, "Length for gap box");

        final Scale.Fraction gapBoxDeltaIn = new Scale.Fraction(
                0.15,
                "Delta for gap box on slur side");

        final Scale.Fraction gapBoxDeltaOut = new Scale.Fraction(
                0.3,
                "Delta for gap box on extension side");

        final Scale.Fraction lineBoxLength = new Scale.Fraction(
                1.75,
                "Length for box across staff line");

        final Scale.Fraction lineBoxIn = new Scale.Fraction(
                0.2,
                "Overlap for line box on slur side");

        final Scale.Fraction lineBoxDeltaIn = new Scale.Fraction(
                0.2,
                "Delta for line box on slur side");

        final Scale.Fraction lineBoxDeltaOut = new Scale.Fraction(
                0.3,
                "Delta for line box on extension side");

        final Constant.Double maxArcAngleHigh = new Constant.Double(
                "degree",
                190.0,
                "High maximum angle (in degrees) of slur arc");

        final Constant.Double maxArcAngleLow = new Constant.Double(
                "degree",
                170.0,
                "Low maximum angle (in degrees) of slur arc");

        final Constant.Double minAngleFromVerticalLow = new Constant.Double(
                "degree",
                20.0,
                "Low minimum angle (in degrees) between slur and vertical");

        final Constant.Double minAngleFromVerticalHigh = new Constant.Double(
                "degree",
                25.0,
                "High minimum angle (in degrees) between slur and vertical");

        final Constant.Double minSlope = new Constant.Double(
                "(co)tangent",
                0.03,
                "Minimum (inverted) slope, to detect vertical and horizontal lines");

        final Constant.Ratio quorumRatio = new Constant.Ratio(
                0.75,
                "Minimum length expressed as ratio of longest in clump");
    }

    //-------------------//
    // JunctionRetriever //
    //-------------------//
    /**
     * Scan all image pixels to retrieve junction pixels and flag
     * them as such with a specific color.
     */
    private class JunctionRetriever
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Vicinity of current pixel. */
        private final Vicinity vicinity = new Vicinity();

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Scan the whole image.
         */
        public void scanImage ()
        {
            for (int x = 1, w = buf.getWidth(); x < w; x++) {
                for (int y = 1, h = buf.getHeight(); y < h; y++) {
                    int pix = buf.get(x, y);

                    if ((pix == FOREGROUND) // Basic pixel, not yet processed
                        || isJunction(pix)) { // Junction, perhaps not the best
                        checkJunction(x, y);
                    }
                }
            }
        }

        /**
         * Check whether the point at (x, y) is a junction.
         * The point needs to have more than 2 neighbors, and no other junction
         * point side-connected with higher grade.
         *
         * @param x point abscissa
         * @param y point ordinate
         */
        private void checkJunction (int x,
                                    int y)
        {
            // Neighbors
            int n = vicinityOf(x, y);

            if (n > 2) {
                int grade = vicinity.getGrade();
                buf.set(x, y, JUNCTION + grade);

                int sideGrade = sideGrade(x, y);

                if (sideGrade > grade) {
                    buf.set(x, y, ARC);
                }
            } else {
                buf.set(x, y, ARC);
            }
        }

        /**
         * Count the immediate neighbors in the provided directions.
         *
         * @param x    center abscissa
         * @param y    center ordinate
         * @param dirs which directions to scan
         * @return the number of non-foreground pixels found
         */
        private int dirNeighbors (int x,
                                  int y,
                                  int[] dirs)
        {
            int n = 0;

            for (int dir : dirs) {
                int pix = buf.get(x + dxs[dir], y + dys[dir]);

                if (pix != BACKGROUND) {
                    n++;
                }
            }

            return n;
        }

        /**
         * Look for side-connected junction pixels and return their
         * highest junction grade.
         *
         * @param x center abscissa
         * @param y center ordinate
         * @return the highest junction grade found
         */
        private int sideGrade (int x,
                               int y)
        {
            int bestGrade = 0;

            for (int dir : sideDirs) {
                int nx = x + dxs[dir];
                int ny = y + dys[dir];
                int pix = buf.get(nx, ny);

                if (isJunction(pix)) {
                    // Point already evaluated
                    bestGrade = max(bestGrade, pix - JUNCTION);
                } else {
                    if (pix == FOREGROUND) {
                        int n = vicinityOf(nx, ny);

                        if (n > 2) {
                            int grade = vicinity.getGrade();
                            buf.set(nx, ny, JUNCTION + grade);
                            bestGrade = max(bestGrade, grade);
                        } else {
                            buf.set(nx, ny, ARC);
                        }
                    }
                }
            }

            return bestGrade;
        }

        /**
         * Count the immediate neighbors in all directions.
         * Details are written in structure "vicinity".
         *
         * @param x center abscissa
         * @param y center ordinate
         * @return the count of all neighbors.
         */
        private int vicinityOf (int x,
                                int y)
        {
            vicinity.verts = dirNeighbors(x, y, vertDirs);
            vicinity.horis = dirNeighbors(x, y, horiDirs);
            vicinity.diags = dirNeighbors(x, y, diagDirs);

            return vicinity.getCount();
        }
    }

    //--------//
    // MyView //
    //--------//
    /**
     * View dedicated to skeleton arcs.
     */
    private class MyView
            extends ImageView
    {
        //~ Constructors ---------------------------------------------------------------------------

        public MyView (BufferedImage image)
        {
            super(image);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void renderItems (Graphics2D g)
        {
            final Rectangle clip = g.getClipBounds();

            // Additional renderers if any
            for (ItemRenderer renderer : sheet.getItemRenderers()) {
                renderer.renderItems(g);
            }

            // Render seeds
            for (Arc arc : arcsMap.values()) {
                setColor(arc, g);

                for (Point p : arc.points) {
                    g.fillRect(p.x, p.y, 1, 1);
                }
            }

            // Render info attachments
            Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

            for (SlurInfo info : pageInfos) {
                info.renderAttachments(g);

                //                Circle circle = info.getCircle();
                //
                //                if (circle != null) {
                //                    double r = circle.getRadius();
                //                    Point2D c = circle.getCenter();
                //                    g.drawOval(
                //                            (int) rint(c.getX() - r),
                //                            (int) rint(c.getY() - r),
                //                            (int) rint(2 * r),
                //                            (int) rint(2 * r));
                //                }
            }

            g.setStroke(oldStroke);

            // Render slurs points
            g.setColor(new Color(255, 0, 0, 50));

            for (SlurInter slur : pageSlurs) {
                for (Arc arc : slur.getInfo().getArcs()) {
                    for (Point p : arc.points) {
                        g.fillRect(p.x, p.y, 1, 1);
                    }
                }
            }

            // Render slurs curves
            g.setColor(new Color(0, 255, 0, 100));

            Stroke lineStroke = new BasicStroke(
                    (float) sheet.getScale().getMainFore(),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND);
            g.setStroke(lineStroke);

            for (SlurInter slur : pageSlurs) {
                SlurInfo info = slur.getInfo();
                CubicCurve2D curve = slur.getInfo().getCurve();

                if (curve != null) {
                    if ((clip == null) || clip.intersects(curve.getBounds())) {
                        g.draw(curve);
                    }
                }

                // Draw osculatory portions, if any
                if (info.getSideCircle(true) != info.getSideCircle(false)) {
                    Color oldColor = g.getColor();
                    g.setColor(new Color(255, 255, 0, 100));

                    for (boolean rev : new boolean[]{true, false}) {
                        Circle sideCircle = info.getSideCircle(rev);

                        if (sideCircle != null) {
                            g.draw(sideCircle.getCurve());
                        }
                    }

                    g.setColor(oldColor);
                }
            }
        }

        private void setColor (Arc arc,
                               Graphics2D g)
        {
            if (arc.shape == ArcShape.SLUR) {
                g.setColor(Color.RED);
            } else if (arc.shape == ArcShape.LINE) {
                g.setColor(Color.BLUE);
            } else {
                g.setColor(Color.LIGHT_GRAY);
            }
        }
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * All pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double similarRadiusRatio;

        final int sideCircleLength;

        final int arcCheckLength;

        final double maxSinSq;

        final int arcMinQuorum;

        final int arcMinSeedLength;

        final double maxStaffLineDy;

        final double maxIncidence;

        final double maxLineDistance;

        final double maxSlurDistance;

        final double maxExtDistance;

        final double maxArcsDistance;

        final double minCircleRadius;

        final double maxCircleRadius;

        final double minSlurWidthLow;

        final double minSlurWidthHigh;

        final double minSlurHeightLow;

        final double minSlurHeightHigh;

        final double maxArcAngleLow;

        final double maxArcAngleHigh;

        final double minAngleFromVerticalLow;

        final double minAngleFromVerticalHigh;

        final double minSlope;

        final double minStaffLineDistance;

        final int minStaffArcLength;

        final int maxStaffArcLength;

        final double gapBoxLength;

        final double gapBoxDeltaIn;

        final double gapBoxDeltaOut;

        final double lineBoxLength;

        final double lineBoxIn;

        final double lineBoxDeltaIn;

        final double lineBoxDeltaOut;

        final double quorumRatio;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            double maxSin = sin(toRadians(constants.maxAlpha.getValue()));

            similarRadiusRatio = constants.similarRadiusRatio.getValue();
            sideCircleLength = scale.toPixels(constants.sideCircleLength);
            arcCheckLength = scale.toPixels(constants.arcCheckLength);
            maxSinSq = maxSin * maxSin;
            arcMinQuorum = scale.toPixels(constants.arcMinQuorum);
            arcMinSeedLength = scale.toPixels(constants.arcMinSeedLength);
            maxStaffLineDy = scale.toPixelsDouble(constants.maxStaffLineDy);
            maxIncidence = toRadians(constants.maxIncidence.getValue());
            maxLineDistance = scale.toPixelsDouble(constants.maxLineDistance);
            maxSlurDistance = scale.toPixelsDouble(constants.maxSlurDistance);
            maxExtDistance = scale.toPixelsDouble(constants.maxExtDistance);
            maxArcsDistance = scale.toPixelsDouble(constants.maxArcsDistance);
            minCircleRadius = scale.toPixelsDouble(constants.minCircleRadius);
            maxCircleRadius = scale.toPixelsDouble(constants.maxCircleRadius);
            minSlurWidthLow = scale.toPixelsDouble(constants.minSlurWidthLow);
            minSlurWidthHigh = scale.toPixelsDouble(constants.minSlurWidthHigh);
            minSlurHeightLow = scale.toPixelsDouble(constants.minSlurHeightLow);
            minSlurHeightHigh = scale.toPixelsDouble(constants.minSlurHeightHigh);
            maxArcAngleHigh = toRadians(constants.maxArcAngleHigh.getValue());
            maxArcAngleLow = toRadians(constants.maxArcAngleLow.getValue());
            minAngleFromVerticalLow = toRadians(constants.minAngleFromVerticalLow.getValue());
            minAngleFromVerticalHigh = toRadians(constants.minAngleFromVerticalHigh.getValue());
            minSlope = constants.minSlope.getValue();
            minStaffLineDistance = scale.toPixelsDouble(constants.minStaffLineDistance);
            minStaffArcLength = scale.toPixels(constants.minStaffArcLength);
            maxStaffArcLength = scale.toPixels(constants.maxStaffArcLength);
            gapBoxLength = scale.toPixels(constants.gapBoxLength);
            gapBoxDeltaIn = scale.toPixels(constants.gapBoxDeltaIn);
            gapBoxDeltaOut = scale.toPixels(constants.gapBoxDeltaOut);
            lineBoxLength = scale.toPixels(constants.lineBoxLength);
            lineBoxIn = scale.toPixels(constants.lineBoxIn);
            lineBoxDeltaIn = scale.toPixels(constants.lineBoxDeltaIn);
            lineBoxDeltaOut = scale.toPixels(constants.lineBoxDeltaOut);
            quorumRatio = constants.quorumRatio.getValue();

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }

    //---------------//
    // SlurRetriever //
    //---------------//
    /**
     * Scan all arcs to retrieve slurs.
     */
    private class SlurRetriever
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Current clump of aggregated slurs candidates. */
        private final Set<SlurInfo> clump = new LinkedHashSet<SlurInfo>();

        /** Current orientation for walking along a slur. */
        private boolean reverse;

        /** Current maximum length for arcs to be tried. */
        private Integer maxLength = null;

        //~ Methods --------------------------------------------------------------------------------
        //------------------//
        // discardShortests //
        //------------------//
        /**
         * Discard slurs candidates for which length is shorter than
         * clump quorum.
         *
         * @param clump the clump to purge
         */
        public void discardShortests (Set<SlurInter> clump)
        {
            int maxLength = 0;

            for (SlurInter inter : clump) {
                maxLength = Math.max(maxLength, inter.getInfo().getLength());
            }

            int quorum = (int) Math.ceil(params.quorumRatio * maxLength);

            for (Iterator<SlurInter> it = clump.iterator(); it.hasNext();) {
                if (quorum > it.next().getInfo().getLength()) {
                    it.remove();
                }
            }
        }

        public void retrieveSlurs ()
        {
            // Extend slur seeds as much as possible through junction points
            List<Arc> relevants = getSeedArcs();

            // Build slurs from initial SLUR seed
            maxLength = null;

            for (Arc arc : relevants) {
                if (!arc.assigned && (arc.shape == ArcShape.SLUR)) {
                    buildSlur(arc);
                }
            }

            // Build slurs from no initial SLUR seed
            // Since arcs are sorted by decreasing length, extension should
            // never try to include an arc longer than the initial one.
            for (Arc arc : relevants) {
                if (!arc.assigned && (arc.shape != ArcShape.SLUR)) {
                    maxLength = arc.getLength();
                    buildSlur(arc);
                }
            }

            logger.info("Slurs: {}", pageSlurs.size());
        }

        /**
         * Try to append one arc to an existing slur and thus create
         * a new slur instance.
         *
         * @param arc
         * @param slur
         * @param browsed
         * @return the newly created slur, if successful
         */
        private SlurInfo addArc (Arc arc,
                                 SlurInfo slur,
                                 Set<Arc> browsed)
        {
            if ((arc == null) || arc.assigned || browsed.contains(arc)) {
                return null;
            }

            if ((maxLength != null) && (arc.getLength() > maxLength)) {
                return null;
            }

            slur.checkArcOrientation(arc, reverse);

            // Check extension is compatible with slur (side) circle
            // Use slur side circle to check position of arc WRT circle
            // If OK, allocate a new slur
            Circle sideCircle = slur.getSideCircle(reverse);

            if (sideCircle == null) {
                return null;
            }

            double dist = arcDistance(sideCircle, arc);

            if (dist <= params.maxExtDistance) {
                // Check new side circle
                Circle newSideCircle = null;
                List<Arc> arcs = slur.getAllArcs(arc, reverse);
                List<Point> pts = slur.pointsOf(arcs);

                if (pts.size() >= params.sideCircleLength) {
                    // Side CCW cannot change with respect to slur CCW
                    newSideCircle = slur.computeSideCircle(pts, reverse);

                    if (newSideCircle == null) {
                        return null;
                    }

                    if (slur.getCircle() != null) {
                        if (newSideCircle.ccw() != slur.getCircle().ccw()) {
                            return null;
                        }
                    }
                }

                int id = ++globalSlurId;
                SlurInfo s = new SlurInfo(id, arcs, null, params.sideCircleLength);
                pageInfos.add(s);

                //                logger.info(
                //                    "Slur#{} extended as {} dist:{}",
                //                    slur.getId(),
                //                    s,
                //                    String.format("%.3f", dist));
                //
                if (newSideCircle != null) {
                    s.setSideCircle(newSideCircle, reverse);
                }

                if (slur.hasSideCircle(!reverse)) {
                    s.setSideCircle(slur.getSideCircle(!reverse), !reverse);
                }

                Circle sc = getCircle(pts);
                s.setCircle(sc);

                return s;
            } else {
                //                logger.info(
                //                    "Slur#{} could not add {} dist:{}",
                //                    slur.getId(),
                //                    arc,
                //                    String.format("%.3f", dist));
                //
                return null;
            }
        }

        /**
         * Measure the mean distance from additional arc to provided
         * circle (a slur side circle).
         * <p>
         * Not all arc points are checked, only the ones close to slur end.
         *
         * @param circle the reference circle
         * @param arc    the additional arc to be checked for compatibility
         * @return the average distance of arc points to circle
         */
        private double arcDistance (Circle circle,
                                    Arc arc)
        {
            double radius = circle.getRadius();
            Point2D.Double center = circle.getCenter();
            int np = 0;
            double sum = 0;

            // Include junction between slur & arc, if any
            Point junction = arc.getJunction(!reverse);

            if (junction != null) {
                double dx = junction.x - center.x;
                double dy = junction.y - center.y;
                sum += ((dx * dx) + (dy * dy));
                np++;
            }

            // Check initial arc points
            //TODO: what about the rest of arc points? use a global fit?
            for (Point p : arc.getSidePoints(params.arcCheckLength, !reverse)) {
                double dx = p.x - center.x;
                double dy = p.y - center.y;
                sum += ((dx * dx) + (dy * dy));
                np++;
            }

            double meanRadius = sqrt(sum / np);

            return Math.abs(meanRadius - radius);
        }

        /**
         * Build possible slurs, starting from the provided arc.
         * Successful slurs are inserted in global collection pageSlurs.
         *
         * @param arc the starting arc
         */
        private void buildSlur (Arc arc)
        {
            checkBreak(arc); // Debug

            clump.clear();

            int id = ++globalSlurId;
            SlurInfo seed = new SlurInfo(
                    id,
                    Arrays.asList(arc),
                    arc.circle,
                    params.sideCircleLength);
            pageInfos.add(seed);

            if (arc.circle == null) {
                seed.setCircle(getCircle(seed.pointsOf(Arrays.asList(arc))));
            }

            //            logger.info("----------------------------\nbuildSlur {}", seed);
            // Extensions in "normal" orientation of the seed
            reverse = false;
            extend(seed, seed.getArcs());

            // Extensions in "reverse" orientation for each candidate in clump
            reverse = true;

            for (SlurInfo slur : new LinkedHashSet<SlurInfo>(clump)) {
                slur.setCrossedLine(null);
                extend(slur, slur.getArcs());
            }

            // Now, evaluate the final candidates and keep some of them
            Set<SlurInter> inters = new HashSet<SlurInter>();

            for (SlurInfo slur : clump) {
                GradeImpacts impacts = computeImpacts(slur);

                if (impacts != null) {
                    SlurInter inter = new SlurInter(slur, impacts);
                    inters.add(inter);
                }
            }

            if (inters.isEmpty()) {
                return;
            }

            // Try selection based on quorum length and grade
            weed(inters);

            ///register(inters); // For debug
            // Check embraced notes
            // Delegate final selection to SlursLinker
            SlurInter selected = slursLinker.prune(inters);

            if (selected != null) {
                selected.getInfo().assign(); // Assign arcs
                pageSlurs.add(selected);
            }

            ///register(inters);
        }

        /**
         * Compute impacts for slur candidate.
         *
         * @param slur slur information
         * @return grade impacts
         */
        private Impacts computeImpacts (SlurInfo slur)
        {
            // Distance to circle
            Circle circle = needCircle(slur);

            if (circle == null) {
                return null;
            }

            if (circle.getDistance() > params.maxSlurDistance) {
                return null;
            }

            double distImpact = 1 - (circle.getDistance() / params.maxSlurDistance);

            // Max arc angle value
            double arcAngle = circle.getArcAngle();

            if (arcAngle > params.maxArcAngleHigh) {
                logger.debug("Slur too curved {} {}", arcAngle, this);

                return null;
            }

            double angleImpact = (params.maxArcAngleHigh - arcAngle) / (params.maxArcAngleHigh
                                                                        - params.maxArcAngleLow);

            // No vertical slur (mid angle close to 0 or PI)
            double midAngle = circle.getMidAngle();

            if (midAngle < 0) {
                midAngle += (2 * PI);
            }

            midAngle = midAngle % PI;

            double fromVertical = min(abs(midAngle), abs(PI - midAngle));

            if (fromVertical < params.minAngleFromVerticalLow) {
                logger.debug("Slur too vertical {} {}", midAngle, circle);

                return null;
            }

            double vertImpact = (fromVertical - params.minAngleFromVerticalLow) / (params.minAngleFromVerticalHigh
                                                                                   - params.minAngleFromVerticalLow);

            List<Point> points = new ArrayList<Point>();

            for (Arc a : slur.getArcs()) {
                points.addAll(a.points);
            }

            Point p0 = points.get(0);
            Point p1 = points.get(points.size() / 2);
            Point p2 = points.get(points.size() - 1);

            // Slur wide enough
            int width = abs(p2.x - p0.x);

            if (width < params.minSlurWidthLow) {
                logger.debug("Slur too narrow {} at {}", width, p0);

                return null;
            }

            double widthImpact = (width - params.minSlurWidthLow) / (params.minSlurWidthHigh
                                                                     - params.minSlurWidthLow);

            // Slur high enough (bent enough)
            double height = Line2D.ptLineDist(p0.x, p0.y, p2.x, p2.y, p1.x, p1.y);

            if (height < params.minSlurHeightLow) {
                logger.debug("Slur too flat {} at {}", height, p0);

                return null;
            }

            double heightImpact = (height - params.minSlurHeightLow) / (params.minSlurHeightHigh
                                                                        - params.minSlurHeightLow);

            return new Impacts(distImpact, widthImpact, heightImpact, angleImpact, vertImpact);
        }

        /**
         * Define extension area on desired side of the slur, in order
         * to catch candidate slur extensions.
         * (side is defined by 'reverse' current value)
         * <p>
         * Shape and size of lookup area depend highly on the potential
         * crossing of a staff line.
         *
         * @param inter  the slur to extend
         * @param tgLine slur tangent staff line, if any
         * @return the extension lookup area
         */
        private Area defineExtArea (SlurInfo info,
                                    FilamentLine tgLine)
        {
            Point se = info.getEnd(reverse);
            Point2D uv = getEndVector(info);
            GeoPath path;

            if (tgLine != null) {
                int xDir = (uv.getX() > 0) ? 1 : (-1);
                double lg = xDir * params.lineBoxLength;
                double dx = xDir * params.lineBoxIn;
                int yDir = (uv.getY() > 0) ? 1 : (-1);
                double dl1 = yDir * params.lineBoxDeltaIn;
                double dl2 = yDir * params.lineBoxDeltaOut;
                double yLine = tgLine.yAt(se.x);
                path = new GeoPath(
                        new Line2D.Double(
                                new Point2D.Double(se.x - dx, yLine),
                                new Point2D.Double(se.x - dx, yLine + dl1)));
                yLine = tgLine.yAt(se.x + lg);
                path.append(
                        new Line2D.Double(
                                new Point2D.Double(se.x + lg, yLine + dl2),
                                new Point2D.Double(se.x + lg, yLine)),
                        true);
            } else {
                if (uv == null) {
                    return null;
                }

                double dl1 = params.gapBoxDeltaIn;
                Point2D dlVect = new Point2D.Double(-dl1 * uv.getY(), dl1 * uv.getX());
                path = new GeoPath(
                        new Line2D.Double(
                                PointUtil.addition(se, dlVect),
                                PointUtil.subtraction(se, dlVect)));

                double lg = params.gapBoxLength;
                Point2D lgVect = new Point2D.Double(lg * uv.getX(), lg * uv.getY());
                Point2D se2 = PointUtil.addition(se, lgVect);
                double dl2 = params.gapBoxDeltaOut;
                dlVect = new Point2D.Double(-dl2 * uv.getY(), dl2 * uv.getX());
                path.append(
                        new Line2D.Double(
                                PointUtil.subtraction(se2, dlVect),
                                PointUtil.addition(se2, dlVect)),
                        true);
            }

            path.closePath();

            Area area = new Area(path);
            info.setExtArea(area, reverse);
            info.addAttachment(reverse ? "t" : "f", area);

            return area;
        }

        /**
         * Try to recursively extend a slur in the desired orientation
         * (normal or reverse).
         *
         * @param slur     the slur to extend
         * @param pastArcs collection of arcs already browsed
         */
        private void extend (SlurInfo slur,
                             Collection<Arc> pastArcs)
        {
            Set<Arc> browsed = new LinkedHashSet<Arc>(pastArcs);
            clump.add(slur);
            slur.retrieveJunctions();

            List<SlurInfo> newSlurs = new ArrayList<SlurInfo>();

            // Check whether this slur end is getting tangent to a staff line
            FilamentLine tgLine = getTangentLine(slur);

            if (tgLine != null) {
                // Check beyond staff line: scan arcs ending in extension window
                scanGap(slur, browsed, newSlurs, tgLine);
            } else {
                Point pivot = slur.getJunction(reverse);

                if (pivot != null) {
                    // Check beyond pivot: scan arcs ending at pivot
                    scanPivot(slur, pivot, browsed, newSlurs);
                } else {
                    // Check beyond gap: scan arcs ending in extension window
                    scanGap(slur, browsed, newSlurs, null);
                }
            }

            if (!newSlurs.isEmpty()) {
                for (SlurInfo s : newSlurs) {
                    browsed.add(s.getEndArc(reverse));

                    if ((s.getCrossedLine() == null) && (slur.getCrossedLine() != null)) {
                        s.setCrossedLine(slur.getCrossedLine());
                    }
                }

                for (SlurInfo s : newSlurs) {
                    extend(s, browsed); // Increment further
                }
            }
        }

        private Rectangle getBounds (Set<SlurInter> slurs)
        {
            Rectangle box = null;

            for (SlurInter slur : slurs) {
                Rectangle b = slur.getInfo().getBounds();

                if (box == null) {
                    box = b;
                } else {
                    box.add(b);
                }
            }

            return box;
        }

        /**
         * Report the unit vector at slur end, based on global slur
         * circle.
         *
         * @param info slur
         * @return the vector which extends the slur end
         */
        private Point2D getEndVector (SlurInfo info)
        {
            Circle circle = needCircle(info);

            if (circle == null) {
                return null;
            }

            int dir = reverse ? circle.ccw() : (-circle.ccw());
            double angle = circle.getAngle(reverse);

            // Unit vector that extends slur end
            return new Point2D.Double(-dir * sin(angle), dir * cos(angle));
        }

        /**
         * Build the arcs that can be used to start slur building.
         * They contain only arcs with relevant shape and of sufficient length.
         *
         * @return the collection sorted by decreasing length
         */
        private List<Arc> getSeedArcs ()
        {
            Set<Arc> set = new HashSet<Arc>();

            for (Arc arc : arcsMap.values()) {
                if (arc.getLength() >= params.arcMinSeedLength) {
                    arc.checkOrientation();
                    set.add(arc);
                }
            }

            List<Arc> list = new ArrayList<Arc>(set);
            Collections.sort(
                    list,
                    new Comparator<Arc>()
            {
                @Override
                public int compare (Arc a1,
                                    Arc a2)
                {
                    return Integer.compare(a2.getLength(), a1.getLength());
                }
                    });

            return list;
        }

        /**
         * Check whether the slur end is getting tangent to staff line.
         * Use vertical distance from slur end to closest staff line.
         * Use global slur circle to confirm crossing.
         *
         * @param slur the slur to check (on current side)
         * @return the tangent staff line
         */
        private FilamentLine getTangentLine (SlurInfo slur)
        {
            // Distance from slur end to closest staff line
            Point se = slur.getEnd(reverse);
            StaffInfo staff = staffManager.getStaffAt(se);
            FilamentLine line = staff.getClosestLine(se);

            if (line == slur.getCrossedLine()) {
                return null; // Already crossed
            }

            double dy = line.yAt(se.x) - se.y;

            if (abs(dy) > params.maxStaffLineDy) {
                logger.debug("End ({},{}] far from staff line", se.x, se.y);

                return null;
            }

            // General direction of slur end WRT staff line
            Point2D uv = getEndVector(slur);

            if (uv == null) {
                return null;
            }

            try {
                // There may be not computable midPoint
                Point2D midPoint = slur.getMidPoint();
                double backDy = line.yAt(midPoint.getX()) - midPoint.getY();
                boolean crossing = (uv.getY() * backDy) > 0;
                Circle gc = slur.getCircle();
                double incidence = gc.getAngle(reverse) - ((gc.ccw() * PI) / 2);

                if (crossing && (abs(incidence) <= params.maxIncidence)) {
                    //                logger.info(
                    //                        "End ({},{}] line tangent degrees: {}",
                    //                        se.x,
                    //                        se.y,
                    //                        String.format("%.0f", toDegrees(incidence)));
                    //                view.selectPoint(se);
                    return line;
                }
            } catch (Exception ex) {
            }

            return null;
        }

        //------------//
        // needCircle //
        //------------//
        private Circle needCircle (SlurInfo slur)
        {
            Circle circle = slur.getCircle();

            if (circle == null) {
                circle = getCircle(slur.pointsOf(slur.getArcs()));
                slur.setCircle(circle);
            }

            return circle;
        }

        /**
         * Register the slurs of clump into their containing systems.
         *
         * @param clump the clump of slurs
         */
        private void register (Set<SlurInter> clump)
        {
            for (SlurInter slur : clump) {
                slur.getInfo().assign();
                pageSlurs.add(slur);
            }

            // Dispatch slurs
            Rectangle clumpBounds = getBounds(clump);
            SystemManager mgr = sheet.getSystemManager();

            List<SlurInter> list = new ArrayList<SlurInter>(clump);

            for (SystemInfo system : mgr.getSystemsOf(clumpBounds, null)) {
                SIGraph sig = system.getSig();

                for (SlurInter slur : clump) {
                    sig.addVertex(slur);
                }

                // Mutual exclusion of all slurs within clump
                for (int i = 0; i < list.size(); i++) {
                    SlurInter s1 = list.get(i);

                    if (sig.containsVertex(s1)) {
                        for (SlurInter s2 : list.subList(i + 1, clump.size())) {
                            if (sig.containsVertex(s2)) {
                                sig.insertExclusion(s1, s2, Cause.OVERLAP);
                            }
                        }
                    }
                }
            }
        }

        //---------//
        // scanGap //
        //---------//
        /**
         * Build all possible slur extensions, just one arc past the
         * ending gap.
         *
         * @param slur     the slur to extend
         * @param browsed  arcs already browsed (kept or not)
         * @param newSlurs (output) to be populated with new slurs found
         * @param tgLine   tangent staff line if any
         */
        private void scanGap (SlurInfo slur,
                              Set<Arc> browsed,
                              List<SlurInfo> newSlurs,
                              FilamentLine tgLine)
        {
            // Look for arcs ends in extension window
            Area area = defineExtArea(slur, tgLine);

            if (area != null) {
                Rectangle box = area.getBounds();
                int xMax = (box.x + box.width) - 1;

                for (Point end : arcsEnds) {
                    if (area.contains(end)) {
                        Arc arc = arcsMap.get(end);

                        if (!arc.assigned && !browsed.contains(arc)) {
                            checkBreak(arc);

                            SlurInfo sl = addArc(arc, slur, browsed);

                            if (sl != null) {
                                newSlurs.add(sl);

                                if (tgLine != null) {
                                    sl.setCrossedLine(tgLine);
                                }
                            }
                        }
                    } else if (end.x > xMax) {
                        break; // Since list arcsEnds is sorted
                    }
                }
            }
        }

        /**
         * Build all possible slur extensions, just one arc past the
         * ending pivot (junction point).
         *
         * @param slur     the slur to extend
         * @param pivot    the ending junction
         * @param browsed  arcs already browsed (kept or not)
         * @param newSlurs (output) to be populated with new slurs found
         */
        private void scanPivot (SlurInfo slur,
                                Point pivot,
                                Set<Arc> browsed,
                                List<SlurInfo> newSlurs)
        {
            // What was the last direction?
            final Arc endArc = slur.getEndArc(reverse);
            final Point prevPoint = (!endArc.points.isEmpty()) ? endArc.getEnd(reverse)
                    : endArc.getJunction(reverse);
            final int lastDir = getDir(prevPoint, pivot);

            // Try to go past this pivot, keeping only the acceptable possibilities
            final Point np = new Point();
            boolean sideJunctionMet = false;

            for (int dir : scans[lastDir]) {
                // If junction has already been met on side dir, stop here
                if (!isSide(dir) && sideJunctionMet) {
                    return;
                }

                np.move(pivot.x + dxs[dir], pivot.y + dys[dir]);

                int pix = buf.get(np.x, np.y);

                if (pix == BACKGROUND) {
                    continue;
                }

                Arc arc = null;

                if (isProcessed(pix)) {
                    // Check arc shape
                    ArcShape shape = ArcShape.values()[pix - PROCESSED];

                    if (shape.isSlurRelevant()) {
                        arc = arcsMap.get(np); // Retrieve arc data
                        checkBreak(arc);
                    }
                } else if (isJunction(pix)) {
                    if (!np.equals(pivot)) {
                        arc = arcsMap.get(pivot);
                    }
                }

                if (arc != null) {
                    SlurInfo sl = addArc(arc, slur, browsed);

                    if (sl != null) {
                        newSlurs.add(sl);

                        if (isSide(dir) && isJunction(pix)) {
                            sideJunctionMet = true;
                        }
                    }
                }
            }
        }

        //------//
        // weed //
        //------//
        private void weed (Set<SlurInter> clump)
        {
            // Discard too short ones
            int upperLength = 0;
            SlurInter longest = null;

            for (SlurInter slur : clump) {
                int length = slur.getInfo().getLength();

                if (upperLength < length) {
                    upperLength = length;
                    longest = slur;
                }
            }

            int quorum = (int) Math.ceil(params.quorumRatio * upperLength);

            for (Iterator<SlurInter> it = clump.iterator(); it.hasNext();) {
                if (quorum > it.next().getInfo().getLength()) {
                    it.remove();
                }
            }

            // Discard those with grade lower than grade of longest
            double longestGrade = longest.getGrade();

            for (Iterator<SlurInter> it = clump.iterator(); it.hasNext();) {
                if (it.next().getGrade() < longestGrade) {
                    it.remove();
                }
            }

            // Make sure all slurs in clump are left-right oriented
            SlurInfo info = longest.getInfo();

            if (info.getEnd(true).x > info.getEnd(false).x) {
                // Reverse ALL arcs & slurs of the clump
                Set<Arc> allArcs = new HashSet<Arc>();

                for (SlurInter inter : clump) {
                    inter.getInfo().reverse();
                    allArcs.addAll(inter.getInfo().getArcs());
                }

                for (Arc arc : allArcs) {
                    arc.reverse();
                }
            }
        }
    }

    //----------//
    // Vicinity //
    //----------//
    /**
     * Gathers the number of immediate neighbors of a pixel and
     * characterizes the links.
     */
    private static class Vicinity
    {
        //~ Instance fields ------------------------------------------------------------------------

        int verts; // Number of neighbors vertically connected

        int horis; // Number of neighbors horizontally connected

        int diags; // Number of neighbors diagonally connected

        //~ Methods --------------------------------------------------------------------------------
        public int getCount ()
        {
            return verts + horis + diags;
        }

        public int getGrade ()
        {
            return (2 * verts) + (2 * horis) + (((verts > 0) && (horis > 0)) ? 1 : 0);
        }
    }
}
