//----------------------------------------------------------------------------//
//                                                                            //
//                           S l u r s B u i l d e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
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
import omr.math.GeoUtil;

import omr.score.ui.PageEraser;
import static omr.sheet.SlurInfo.Arc;
import static omr.sheet.SlurInfo.Arc.ArcShape;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;

import omr.sig.GradeImpacts;
import omr.sig.SIGraph;
import omr.sig.SlurInter;
import omr.sig.SlurInter.Impacts;

import omr.ui.BoardsPane;
import omr.ui.util.ItemRenderer;

import omr.util.Navigable;
import omr.util.StopWatch;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SlursBuilder.class);

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

    //~ Enumerations -----------------------------------------------------------
    /**
     * Status for current move along arc.
     */
    private static enum Status
    {
        //~ Enumeration constant initializers ----------------------------------

        /** One more point on arc. */
        CONTINUE,
        /** Arrived at a new junction point. */
        SWITCH,
        /** No more move possible. */
        END;
    }

    //~ Instance fields --------------------------------------------------------
    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Global sheet skew. */
    private final Skew skew;

    /** Sheet staves. */
    private final StaffManager staffManager;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** The skeleton buffer. */
    private final ByteProcessor buf;

    /** All slurs retrieved. */
    private final List<SlurInter> allSlurs = new ArrayList<SlurInter>();

    /** Map of relevant arcs. (end points -> arc) */
    private final Map<Point, Arc> arcsMap = new LinkedHashMap<Point, Arc>();

    /** For unique slur IDs. (page wise) */
    private int globalSlurId = 0;

    //~ Constructors -----------------------------------------------------------
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

        buf = buildSkeleton();
    }

    //~ Methods ----------------------------------------------------------------
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
        ByteProcessor buffer = picture.getSource(Picture.SourceKey.BINARY);
        buffer = (ByteProcessor) buffer.duplicate();
        buffer.skeletonize();

        BufferedImage img = buffer.getBufferedImage();

        // Erase good shapes of each system
        Graphics2D g = img.createGraphics();
        PageEraser eraser = new PageEraser(g, sheet);
        eraser.erase(
                Arrays.asList(
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
            sheet.getAssembly()
                    .addViewTab(
                            "Skeleton",
                            new ScrollImageView(sheet, new MyView(img)),
                            new BoardsPane(new PixelBoard(sheet)));
        }

        return buffer;
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
        double arcAngle = rough.getStopAngle() - rough.getStartAngle();

        if (arcAngle > params.maxArcAngleHigh) {
            logger.debug("Arc angle too large {} at {}", arcAngle, p0);

            return null;
        }

        // Now compute precise circle
        try {
            Circle circle = new Circle(points);
            double dist = circle.getDistance();

            if (dist > params.maxCircleDistance) {
                logger.debug("Bad circle fit {} at {}", dist, p0);

                return null;
            }

            logger.debug("{} to {} Circle {}", p0, p2, circle);

            return circle;
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

    //----------//
    // setPixel //
    //----------//
    /**
     * Modify buffer pixel value to remember information.
     */
    private void setPixel (int x,
                           int y,
                           int val)
    {
        buf.set(x, y, val);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Boolean keepSkeleton = new Constant.Boolean(
                false,
                "Should we store skeleton images on disk?");

        final Constant.Double maxAlpha = new Constant.Double(
                "degree",
                3,
                "Maximum angle (in degrees) for 3 points colinearity");

        final Scale.Fraction arcMinQuorum = new Scale.Fraction(
                1.5,
                "Minimum arc length for quorum");

        final Scale.Fraction arcMinSeedLength = new Scale.Fraction(
                0.5,
                "Minimum arc length for starting a slur build");

        final Scale.Fraction maxLineDistance = new Scale.Fraction(
                0.1,
                "Maximum distance from straight line");

        final Scale.Fraction maxCircleDistance = new Scale.Fraction(
                0.006,
                "Maximum distance to approximating circle");

        Scale.Fraction minCircleRadius = new Scale.Fraction(
                0.7,
                "Minimum circle radius for a slur");

        Scale.Fraction maxCircleRadius = new Scale.Fraction(
                50,
                "Maximum circle radius for a slur");

        Scale.Fraction minSlurWidthLow = new Scale.Fraction(
                1.0,
                "Low minimum width for a slur");

        Scale.Fraction minSlurWidthHigh = new Scale.Fraction(
                1.5,
                "High minimum width for a slur");

        Scale.Fraction minSlurHeightLow = new Scale.Fraction(
                0.5,
                "Low minimum height for a slur");

        Scale.Fraction minSlurHeightHigh = new Scale.Fraction(
                1.0,
                "High minimum height for a slur");

        Scale.Fraction minStaffLineDistance = new Scale.Fraction(
                0.25,
                "Minimum distance from staff line");

        Constant.Double maxArcAngleHigh = new Constant.Double(
                "degree",
                190.0,
                "High maximum angle (in degrees) of slur arc");

        Constant.Double maxArcAngleLow = new Constant.Double(
                "degree",
                170.0,
                "Low maximum angle (in degrees) of slur arc");

        Constant.Double minAngleFromVerticalLow = new Constant.Double(
                "degree",
                20.0,
                "Low minimum angle (in degrees) between slur and vertical");

        Constant.Double minAngleFromVerticalHigh = new Constant.Double(
                "degree",
                25.0,
                "High minimum angle (in degrees) between slur and vertical");

        Constant.Double minSlope = new Constant.Double(
                "(co)tangent",
                0.03,
                "Minimum (inverted) slope, to detect vertical and horizontal lines");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * All pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final double maxSinSq;

        final int arcMinQuorum;

        final int arcMinSeedLength;

        final double maxLineDistance;

        final double maxCircleDistance;

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

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            double maxSin = sin(toRadians(constants.maxAlpha.getValue()));

            maxSinSq = maxSin * maxSin;
            arcMinQuorum = scale.toPixels(constants.arcMinQuorum);
            arcMinSeedLength = scale.toPixels(constants.arcMinSeedLength);
            maxLineDistance = scale.toPixelsDouble(constants.maxLineDistance);
            maxCircleDistance = scale.toPixelsDouble(
                    constants.maxCircleDistance);
            minCircleRadius = scale.toPixelsDouble(constants.minCircleRadius);
            maxCircleRadius = scale.toPixelsDouble(constants.maxCircleRadius);
            minSlurWidthLow = scale.toPixelsDouble(constants.minSlurWidthLow);
            minSlurWidthHigh = scale.toPixelsDouble(constants.minSlurWidthHigh);
            minSlurHeightLow = scale.toPixelsDouble(constants.minSlurHeightLow);
            minSlurHeightHigh = scale.toPixelsDouble(
                    constants.minSlurHeightHigh);
            maxArcAngleHigh = toRadians(constants.maxArcAngleHigh.getValue());
            maxArcAngleLow = toRadians(constants.maxArcAngleLow.getValue());
            minAngleFromVerticalLow = toRadians(
                    constants.minAngleFromVerticalLow.getValue());
            minAngleFromVerticalHigh = toRadians(
                    constants.minAngleFromVerticalHigh.getValue());
            minSlope = constants.minSlope.getValue();
            minStaffLineDistance = scale.toPixelsDouble(
                    constants.minStaffLineDistance);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
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
        //~ Instance fields ----------------------------------------------------

        int verts; // Number of neighbors vertically connected

        int horis; // Number of neighbors horizontally connected

        int diags; // Number of neighbors diagonally connected

        //~ Methods ------------------------------------------------------------
        public int getCount ()
        {
            return verts + horis + diags;
        }

        public int getGrade ()
        {
            return (2 * verts) + (2 * horis)
                   + (((verts > 0) && (horis > 0)) ? 1 : 0);
        }
    }

    //--------------//
    // ArcRetriever //
    //--------------//
    /**
     * Retrieve all arcs and remember the interesting ones in arcsMap.
     * Each arc has its two ending points flagged with a specific color to
     * remember the arc shape.
     */
    private class ArcRetriever
    {
        //~ Instance fields ----------------------------------------------------

        /** Current point abscissa. */
        int cx;

        /** Current point ordinate. */
        int cy;

        /** Last direction. */
        int lastDir;

        //~ Methods ------------------------------------------------------------
        /**
         * Determine shape for this arc.
         *
         * @param arc
         * @return
         */
        public ArcShape determineShape (Arc arc)
        {
            List<Point> points = arc.points;

            // Too short?
            if (points.size() < params.arcMinQuorum) {
                ///logger.info("Too short: {}", points.size());
                return ArcShape.SHORT;
            }

            // Check arc is not just a long portion of staff line
            if (isStaffLine(arc)) {
                //logger.info("Staff Line");
                return ArcShape.STAFF_LINE;
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
         * Walk along the arc in the desired orientation, starting at
         * (x,y) point, until no more incremental move is possible.
         *
         * @param x       starting abscissa
         * @param y       starting ordinate
         * @param reverse normal (-> stop) or reverse orientation (-> start)
         * @param lastDir arrival at (x,y) if any, 0 if none
         */
        public void walk (Arc arc,
                          int x,
                          int y,
                          boolean reverse,
                          int lastDir)
        {
            this.lastDir = lastDir;
            cx = x;
            cy = y;

            Status status;

            while (Status.CONTINUE == (status = move(arc, cx, cy, reverse))) {
                addPoint(arc, cx, cy, reverse);
            }
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

            setPixel(cx, cy, PROCESSED);
        }

        /**
         * Update display to show the arc points as "discarded".
         */
        private void hide (Arc arc)
        {
            List<Point> points = arc.points;

            for (int i = 1; i < (points.size() - 1); i++) {
                Point p = points.get(i);

                setPixel(p.x, p.y, HIDDEN);
            }
        }

        /**
         * Check whether this arc is simply a part of a staff line.
         *
         * @return true if positive
         */
        private boolean isStaffLine (Arc arc)
        {
            List<Point> points = arc.points;
            Point p0 = points.get(0);
            StaffInfo staff = staffManager.getStaffAt(p0);
            FilamentLine line = staff.getClosestLine(p0);
            double maxDist = 0;

            for (int i : new int[]{0, points.size() / 2, points.size() - 1}) {
                Point p = points.get(i);
                double dist = abs(line.yAt(p.x) - p.y);
                maxDist = max(maxDist, dist);
            }

            return maxDist < params.minStaffLineDistance;
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

                    if (reverse) {
                        arc.setStartJunction(junctionPt);
                    } else {
                        arc.setStopJunction(junctionPt);
                    }

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
            walk(arc, x, y, false, lastDir);

            // Scan arc on reverse side -> startJunction if needed
            // If we scanned from a junction, startJunction is already set
            if (startJunction == null) {
                // Set lastDir as the opposite of initial starting dir
                if (arc.points.size() > 1) {
                    lastDir = getDir(arc.points.get(1), arc.points.get(0));
                } else if (arc.getStopJunction() != null) {
                    lastDir = getDir(arc.getStopJunction(), arc.points.get(0));
                }

                walk(arc, x, y, true, lastDir);
            }

            // Check arc shape
            ArcShape shape = determineShape(arc);
            storeShape(arc, shape);

            if (shape.isSlurRelevant()) {
                arcsMap.put(arc.points.get(0), arc);
                arcsMap.put(arc.points.get(arc.points.size() - 1), arc);

                return arc;
            } else {
                hide(arc);

                return null;
            }
        }

        /**
         * Scan the whole image.
         */
        private void scanImage ()
        {
            for (int x = 1, w = buf.getWidth(); x < w; x++) {
                for (int y = 1, h = buf.getHeight(); y < h; y++) {
                    int pix = buf.get(x, y);

                    if (pix == ARC) {
                        // Basic arc pixel, not yet processed.
                        // Scan full arc
                        ///logger.info("Initial arc at x:{}, y:{}", x, y);
                        scanArc(x, y, null, 0);
                    } else if (isJunction(pix)) {
                        // Junction pixel, scan arcs linked to this junction point
                        if (!isJunctionProcessed(pix)) {
                            scanJunction(x, y);
                        }
                    }
                }
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
            setPixel(x, y, JUNCTION_PROCESSED);

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
            setPixel(first.x, first.y, PROCESSED + shape.ordinal());
            setPixel(last.x, last.y, PROCESSED + shape.ordinal());
        }
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
        //~ Instance fields ----------------------------------------------------

        /** Vicinity of current pixel. */
        private final Vicinity vicinity = new Vicinity();

        //~ Methods ------------------------------------------------------------
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
                setPixel(x, y, JUNCTION + grade);

                int sideGrade = sideGrade(x, y);

                if (sideGrade > grade) {
                    setPixel(x, y, ARC);
                }
            } else {
                setPixel(x, y, ARC);
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
                            setPixel(nx, ny, JUNCTION + grade);
                            bestGrade = max(bestGrade, grade);
                        } else {
                            setPixel(nx, ny, ARC);
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
    private class MyView
            extends ImageView
    {
        //~ Constructors -------------------------------------------------------

        public MyView (BufferedImage image)
        {
            super(image);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected void renderItems (Graphics2D g)
        {
            // Additional renderers if any
            for (ItemRenderer renderer : sheet.getItemRenderers()) {
                renderer.renderItems(g);
            }

            // Render seeds
            for (Arc arc : arcsMap.values()) {
                switch (arc.shape) {
                case SLUR:
                    g.setColor(Color.RED);

                    break;

                case LINE:
                    g.setColor(Color.BLUE);

                    break;

                default:

                    continue;
                }

                for (Point p : arc.points) {
                    g.fillRect(p.x, p.y, 1, 1);
                }
            }

            // Render slurs points
            g.setColor(new Color(255, 0, 0, 50));

            for (SlurInter slur : allSlurs) {
                for (Arc arc : slur.getInfo()
                        .getArcs()) {
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

            for (SlurInter slur : allSlurs) {
                CubicCurve2D curve = slur.getInfo()
                        .getCircle()
                        .getCurve();

                if (curve != null) {
                    g.draw(curve);
                } else {
                    logger.info("No curve for {}", slur);
                }
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
        //~ Methods ------------------------------------------------------------

        public void retrieveSlurs ()
        {
            // Extend slur seeds as much as possible through junction points
            List<Arc> relevants = getSeedArcs();

            // Build slurs with initial seed
            for (Arc arc : relevants) {
                if (!arc.assigned && (arc.shape == ArcShape.SLUR)) {
                    buildSlur(arc);
                }
            }

            // Build slurs with no initial seed
            for (Arc arc : relevants) {
                if (!arc.assigned && (arc.shape != ArcShape.SLUR)) {
                    buildSlur(arc);
                }
            }

            // Extend slurs past gap, with other slurs or compatible arcs
            // TODO
            //
            // Dispatch slurs to their related system(s)
            dispatchSlurs();
        }

        /**
         * Build the best possible slur, starting from the provided arc.
         * If successful, the slur is inserted in global collection allSlurs.
         *
         * @param arc the starting arc
         */
        private void buildSlur (Arc arc)
        {
            Set<SlurInfo> clump = new LinkedHashSet<SlurInfo>();
            int id = ++globalSlurId;
            SlurInfo slur = new SlurInfo(
                    id,
                    Arrays.asList(arc),
                    arc.circle);
            logger.debug("trying slur {}", slur);
            clump.add(slur);

            extend(slur, slur.getArcs(), clump, false);
            slur = weed(clump);

            extend(slur, slur.getArcs(), clump, true);
            slur = weed(clump);

            GradeImpacts impacts = computeImpacts(slur);

            if (impacts != null) {
                slur.assign();

                SlurInter inter = new SlurInter(slur, impacts);
                allSlurs.add(inter);
            }
        }

        /**
         * Compute impacts for slur candidate.
         *
         * @param slur slur information
         * @return grade impacts
         */
        private Impacts computeImpacts (SlurInfo slur)
        {
            Circle circle = slur.getCircle();

            if (circle == null) {
                return null;
            }

            double distImpact = 1
                                - (circle.getDistance() / params.maxCircleDistance);

            // Max arc angle value
            double arcAngle = circle.getStopAngle() - circle.getStartAngle();

            if (arcAngle > params.maxArcAngleHigh) {
                logger.debug("Slur too curved {} {}", arcAngle, this);

                return null;
            }

            double angleImpact = (params.maxArcAngleHigh - arcAngle) / (params.maxArcAngleHigh
                                                                        - params.maxArcAngleLow);

            // No vertical slur (mid angle close to 0 or PI)
            double midAngle = (circle.getStopAngle() + circle.getStartAngle()) / 2;

            if (midAngle < 0) {
                midAngle += (2 * PI);
            }

            midAngle = midAngle % PI;

            double fromVertical = min(abs(midAngle), abs(PI - midAngle));

            if (fromVertical < params.minAngleFromVerticalLow) {
                logger.debug("Slur too vertical {} {}", midAngle, circle);

                return null;
            }

            double vertImpact = (fromVertical
                                 - params.minAngleFromVerticalLow) / (params.minAngleFromVerticalHigh
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
            double height = Line2D.ptLineDist(
                    p0.x,
                    p0.y,
                    p2.x,
                    p2.y,
                    p1.x,
                    p1.y);

            if (height < params.minSlurHeightLow) {
                logger.debug("Slur too flat {} at {}", height, p0);

                return null;
            }

            double heightImpact = (height - params.minSlurHeightLow) / (params.minSlurHeightHigh
                                                                        - params.minSlurHeightLow);

            return new Impacts(
                    distImpact,
                    widthImpact,
                    heightImpact,
                    angleImpact,
                    vertImpact);
        }

        //---------------//
        // dispatchSlurs //
        //---------------//
        /**
         * Dispatch page slurs to their containing system(s).
         */
        private void dispatchSlurs ()
        {
            List<SystemInfo> relevants = new ArrayList<SystemInfo>();
            SystemManager systemManager = sheet.getSystemManager();

            for (SlurInter slur : allSlurs) {
                logger.info("{} {}", slur, slur.getDetails());

                Point center = GeoUtil.centerOf(slur.getBounds());
                systemManager.getSystemsOf(center, relevants);

                for (SystemInfo system : relevants) {
                    SIGraph sig = system.getSig();
                    sig.addVertex(slur);
                }
            }

            logger.info(
                    "{}Slurs retrieved: {}",
                    sheet.getLogPrefix(),
                    allSlurs.size());
        }

        /**
         * Try to recursively extend a slur in the desired orientation
         * (normal or reverse).
         *
         * @param slur     the slur to extend
         * @param pastArcs collection of arcs already browsed
         * @param clump    the clump of slurs this one is part of
         * @param reverse  true for reverse orientation, false for normal
         */
        private void extend (SlurInfo slur,
                             Collection<Arc> pastArcs,
                             Set<SlurInfo> clump,
                             boolean reverse)
        {
            Set<Arc> browsed = new LinkedHashSet<Arc>(pastArcs);
            slur.getSlurJunctions();

            Point pivot = reverse ? slur.getStartJunction()
                    : slur.getStopJunction();

            if (pivot != null) {
                List<SlurInfo> newSlurs = increment(
                        slur,
                        pivot,
                        browsed,
                        reverse);

                if (!newSlurs.isEmpty()) {
                    // Increments found
                    for (SlurInfo s : newSlurs) {
                        browsed.add(s.getEndArc(reverse));
                    }

                    // Increment further
                    for (SlurInfo s : newSlurs) {
                        extend(s, browsed, clump, reverse);
                    }
                } else {
                    // No compatible arc beyond the junction
                    clump.add(slur);
                }
            } else {
                // It's a free leaf, with no junction
                clump.add(slur);
            }
        }

        /**
         * Build the arcs that can be used to start slur building.
         * They contain only arcs with relevant shape and of sufficient length.
         *
         * @return the collection sorted by decreasing length
         */
        private List<Arc> getSeedArcs ()
        {
            List<Arc> arcs = new ArrayList<Arc>();

            for (Arc arc : arcsMap.values()) {
                if (arc.getLength() >= params.arcMinSeedLength) {
                    arcs.add(arc);
                }
            }

            Collections.sort(
                    arcs,
                    new Comparator<Arc>()
            {
                @Override
                public int compare (Arc a1,
                                    Arc a2)
                {
                    return Integer.compare(
                            a2.getLength(),
                            a1.getLength());
                }
                    });

            return arcs;
        }

        /**
         * Retrieve the approximating circle portion for this slur
         *
         * @param allArcs the sequence of arcs
         * @return approximating circle portion, if successful, null otherwise
         */
        private Circle getSlurCircle (List<Arc> allArcs)
        {
            List<Point> allPoints = new ArrayList<Point>();

            for (int i = 0, na = allArcs.size(); i < na; i++) {
                Arc arc = allArcs.get(i);
                allPoints.addAll(arc.points);

                if ((i < (na - 1)) && (arc.getStopJunction() != null)) {
                    allPoints.add(arc.getStopJunction());
                }
            }

            return getCircle(allPoints);
        }

        /**
         * Build all possible slur extensions, just one arc past the
         * ending junction.
         *
         * @param slur    the slur to extend
         * @param pivot   the ending junction
         * @param browsed arcs already browsed (kept or not)
         * @param reverse desired orientation
         * @return all the acceptable extended slurs
         */
        private List<SlurInfo> increment (SlurInfo slur,
                                          Point pivot,
                                          Set<Arc> browsed,
                                          boolean reverse)
        {
            // What was the last direction?
            final Arc endArc = slur.getEndArc(reverse);
            final Point prevPoint = (!endArc.points.isEmpty())
                    ? endArc.getEnd(reverse)
                    : (reverse
                    ? endArc.getStopJunction()
                    : endArc.getStartJunction());
            final int lastDir = getDir(prevPoint, pivot);

            // Try to go past this pivot, keeping only the acceptable possibilities
            final List<SlurInfo> newSlurs = new ArrayList<SlurInfo>();
            final Point np = new Point();

            for (int dir : scans[lastDir]) {
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
                    }
                } else if (isJunction(pix)) {
                    if (!np.equals(pivot)) {
                        arc = arcsMap.get(pivot);
                    }
                }

                if ((arc != null) && !arc.assigned && !browsed.contains(arc)) {
                    slur.checkArcOrientation(arc, reverse);

                    // Check compatibility with slur
                    List<Arc> allArcs = slur.getAllArcs(arc, reverse);
                    Circle extendedCircle = getSlurCircle(allArcs);

                    if (extendedCircle != null) {
                        int id = ++globalSlurId;
                        SlurInfo s = new SlurInfo(id, allArcs, extendedCircle);
                        logger.debug("slur#{} extended as {}", id, s);
                        newSlurs.add(s);
                    }
                }
            }

            return newSlurs;
        }

        /**
         * Keep only the best slur in the clump of slur candidates.
         *
         * @param clump the clump to weed out
         * @return the best slur in the clump
         */
        private SlurInfo weed (Set<SlurInfo> clump)
        {
            if (clump.size() == 1) {
                return clump.iterator()
                        .next();
            }

            // Keep the best one, typically the longest
            int bestNp = 0;
            SlurInfo bestSlur = null;

            for (SlurInfo slur : clump) {
                int np = 0;

                for (Arc a : slur.getArcs()) {
                    np += a.points.size();
                }

                if (bestNp < np) {
                    bestNp = np;
                    bestSlur = slur;
                }
            }

            clump.clear();
            clump.add(bestSlur);

            return bestSlur;
        }
    }
}
