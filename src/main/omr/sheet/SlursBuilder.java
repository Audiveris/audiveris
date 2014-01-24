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

import omr.grid.FilamentLine;
import omr.grid.StaffInfo;
import omr.grid.StaffManager;

import omr.image.PixelBuffer;
import static omr.image.PixelSource.BACKGROUND;
import static omr.image.PixelSource.FOREGROUND;

import omr.math.BasicLine;
import omr.math.Circle;

import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;

import omr.ui.BoardsPane;
import omr.ui.util.ItemRenderer;

import omr.util.Navigable;
import omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.CubicCurve2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
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

    /** Vertical directions: north & south. */
    private static final List<Point> vertDirs = Arrays.asList(
            new Point(0, -1),
            new Point(0, 1));

    /** Horizontal directions: east & west. */
    private static final List<Point> horiDirs = Arrays.asList(
            new Point(1, 0),
            new Point(-1, 0));

    /** Side directions: verticals + horizontals. */
    private static final List<Point> sideDirs = new ArrayList<Point>();

    static {
        sideDirs.addAll(horiDirs);
        sideDirs.addAll(vertDirs);
    }

    /** Diagonal directions: nw, ne, se, sw. */
    private static final List<Point> diagDirs = Arrays.asList(
            new Point(1, -1),
            new Point(1, 1),
            new Point(-1, -1),
            new Point(-1, 1));

    /** All directions. */
    private static final List<Point> allDirs = new ArrayList<Point>();

    static {
        allDirs.addAll(sideDirs);
        allDirs.addAll(diagDirs);
    }

    //~ Enumerations -----------------------------------------------------------
    /** Status for current move along arc. */
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

    /** Shape detected for arc. */
    private static enum ArcShape
    {
        //~ Enumeration constant initializers ----------------------------------

        /** Not yet known. */
        UNKNOWN,
        /** Short arc. Can be tested as slur or wedge extension. */
        SHORT,
        /** Long portion of slur. Can be part of slur only. */
        SLUR,
        /** Long straight line. Can be part of wedge (and slur). */
        LINE,
        /** Long portion of staff line. Cannot be part of slur or wedge. */
        STAFF_LINE,
        /** Long arc, but no shape detected. Cannot be part of slur/wedge */
        IRRELEVANT;
        //~ Methods ------------------------------------------------------------

        public static ArcShape valueOf (int pix)
        {
            return values()[pix - PROCESSED];
        }

        public boolean isLineRelevant ()
        {
            return (this == SHORT) || (this == LINE);
        }

        public boolean isSlurRelevant ()
        {
            return (this == SHORT) || (this == SLUR) || (this == LINE);
        }
    }

    //~ Instance fields --------------------------------------------------------
    /** For unique slur IDs. (page wise) */
    private int globalSlurId = 0;

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
    private final PixelBuffer buf;

    /** Displayed image raster. (For lack of buf display) */
    private final WritableRaster raster;

    /** For writing to raster. */
    private final int[] pArray = new int[1];

    /** All slurs retrieved. */
    private final Set<Slur> allSlurs = new LinkedHashSet<Slur>();

    /** Map of relevant arcs. */
    private final Map<Point, Arc> arcsMap = new LinkedHashMap<Point, Arc>();

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

        final Picture picture = sheet.getPicture();
        buf = (PixelBuffer) picture.getSource(Picture.SourceKey.SKELETON);

        // Display skeleton
        if (Main.getGui() != null) {
            final BufferedImage img = buf.toBufferedImage();
            raster = img.getRaster();
            sheet.getAssembly()
                    .addViewTab(
                            "Skeleton",
                            new ScrollImageView(sheet, new MyView(img)),
                            new BoardsPane(new PixelBoard(sheet)));
        } else {
            raster = null;
        }
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // buildSlurs //
    //------------//
    public void buildSlurs ()
    {
        logger.debug("{}buildSlurs", sheet.getLogPrefix());

        StopWatch watch = new StopWatch("Slurs");

        // Retrieve junctions.
        watch.start("retrieveJunctions");
        new JunctionRetriever().scan();

        // Scan arcs between junctions
        watch.start("retrieveArcs");
        new ArcRetriever().scan();

        // Retrieve slurs from arcs
        watch.start("retrieveSlurs");
        retrieveSlurs();

        logger.info("Slurs retrieved: {}", allSlurs.size());
        watch.print();
    }

    //-----------//
    // getCircle //
    //-----------//
    /**
     * Check whether the provided collection of points can represent
     * a (portion of a) slur.
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
        double radius = rough.getRadius();

        if ((radius < params.minCircleRadius)
            || (radius > params.maxCircleRadius)) {
            return null;
        }

        // Max arc angle value
        double arcAngle = rough.getStopAngle() - rough.getStartAngle();

        if (arcAngle > params.maxArcAngle) {
            logger.debug("Arc angle too large {} at {}", arcAngle, p0);

            return null;
        }

        // No vertical slur (mid angle close to 0 or PI)
        double midAngle = (rough.getStopAngle() + rough.getStartAngle()) / 2;

        if (midAngle < 0) {
            midAngle += (2 * PI);
        }

        midAngle = midAngle % PI;

        double fromVertical = min(abs(midAngle), abs(PI - midAngle));

        if (fromVertical < params.minAngleFromVertical) {
            logger.debug("Arc too vertical {} at {}", midAngle, p0);

            return null;
        }

        // Now compute precise circle
        Circle circle = new Circle(points);
        double dist = circle.computeDistance(points);

        if (dist > params.maxCircleDistance) {
            return null;
        }

        logger.debug("{} to {} Circle {}", p0, p2, circle);

        return circle;
    }

    private boolean isJunction (int pix)
    {
        return (pix >= JUNCTION) && (pix <= (JUNCTION + 10));
    }

    private boolean isJunctionProcessed (int pix)
    {
        return pix == JUNCTION_PROCESSED;
    }

    private boolean isProcessed (int pix)
    {
        return (pix >= PROCESSED) && (pix < (PROCESSED + 10));
    }

    //---------------//
    // retrieveSlurs //
    //---------------//
    private void retrieveSlurs ()
    {
        // Extend slur arcs as much as possible through junction points
        Set<Arc> relevants = new LinkedHashSet<Arc>(arcsMap.values());

        for (Arc arc : relevants) {
            if (arc.assigned) {
                continue;
            }

            ArcShape shape = arc.shape;

            if (shape == ArcShape.SLUR) {
                Set<Slur> clump = new LinkedHashSet<Slur>();
                Slur slur = new Slur(Arrays.asList(arc), arc.circle);
                logger.debug("\n *** Seed slur {}", slur);
                clump.add(slur);
                slur.extend(Arrays.asList(arc), clump, false);
                weed(clump);
                slur = clump.iterator()
                        .next();
                slur.extend(Arrays.asList(arc), clump, true);
                weed(clump);
                allSlurs.addAll(clump);
            }
        }

        // Extend slurs past gap, with other slurs or compatible arcs
        // Results
        for (Slur slur : allSlurs) {
            logger.info("{}", slur);
        }
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
        buf.setValue(x, y, val);

        // Keep displayed image up to date
        if (raster != null) {
            pArray[0] = val;
            raster.setPixel(x, y, pArray);
        }
    }

    private void weed (Set<Slur> clump)
    {
        // Keep the best one, typically the longest
        int bestNp = 0;
        Slur bestSlur = null;

        for (Slur slur : clump) {
            int np = 0;

            for (Arc a : slur.arcs) {
                np += a.points.size();
            }

            if (bestNp < np) {
                bestNp = np;
                bestSlur = slur;
            }
        }

        clump.retainAll(Collections.singleton(bestSlur));
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----//
    // Arc //
    //-----//
    private class Arc
    {
        //~ Instance fields ----------------------------------------------------

        /**
         * Sequence of arc points so far.
         * This does not presume any orientation of the arc, it's just the
         * order in which the arc points were scanned.
         * An arc has no intrinsic orientation.
         */
        List<Point> points = new ArrayList<Point>();

        /** Current point abscissa. */
        int cx;

        /** Current point ordinate. */
        int cy;

        /** Junction point, if any, at beginning of points. */
        Point startJunction;

        /** Junction point, if any, at end of points. */
        Point stopJunction;

        /** Shape found for this arc. */
        ArcShape shape;

        /** Related circle, if any. */
        Circle circle;

        /** Assigned to a slur?. */
        boolean assigned;

        //~ Constructors -------------------------------------------------------
        public Arc (int x,
                    int y,
                    Point startJunction)
        {
            cx = x;
            cy = y;
            addPoint(false);

            if (startJunction != null) {
                this.startJunction = startJunction;
            }
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Retrieve shape for this arc.
         */
        public ArcShape determineShape ()
        {
            // Too short?
            if (points.size() < params.arcMinQuorum) {
                ///logger.info("Too short: {}", points.size());
                return ArcShape.SHORT;
            }

            // Check arc is not just a long portion of staff line
            if (isStaffLine()) {
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
                circle = fittedCircle;

                return ArcShape.SLUR;
            }

            // Nothing interesting
            return ArcShape.IRRELEVANT;
        }

        public void reverse ()
        {
            // Reverse junctions
            Point temp = startJunction;
            startJunction = stopJunction;
            stopJunction = temp;

            // Reverse points list
            List<Point> rev = new ArrayList<Point>(points.size());

            for (ListIterator<Point> it = points.listIterator(points.size());
                    it.hasPrevious();) {
                rev.add(it.previous());
            }

            points.clear();
            points.addAll(rev);
        }

        public void scan (int x,
                          int y,
                          boolean reverse)
        {
            cx = x;
            cy = y;

            ///logger.info("Arc starting at x:{}, y:{}", cx, cy);
            Status status;

            while (Status.CONTINUE == (status = move(cx, cy, reverse))) {
                addPoint(reverse);
            }
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Arc");

            if (!points.isEmpty()) {
                Point p0 = points.get(0);
                sb.append("[")
                        .append(p0.x)
                        .append(",")
                        .append(p0.y)
                        .append("]");

                if (points.size() > 1) {
                    Point p2 = points.get(points.size() - 1);
                    sb.append("[")
                            .append(p2.x)
                            .append(",")
                            .append(p2.y)
                            .append("]");
                }
            }

            return sb.toString();
        }

        Point getEnd (boolean reverse)
        {
            if (reverse) {
                return points.get(0);
            } else {
                return points.get(points.size() - 1);
            }
        }

        private void addPoint (boolean reverse)
        {
            if (reverse) {
                points.add(0, new Point(cx, cy));
            } else {
                points.add(new Point(cx, cy));
            }

            setPixel(cx, cy, PROCESSED);
        }

        private Point getJunction (boolean reverse)
        {
            if (reverse) {
                return startJunction;
            } else {
                return stopJunction;
            }
        }

        private void hide ()
        {
            for (int i = 1; i < (points.size() - 1); i++) {
                Point p = points.get(i);

                setPixel(p.x, p.y, HIDDEN);
            }
        }

        private boolean isStaffLine ()
        {
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

        private Status move (int x,
                             int y,
                             boolean reverse)
        {
            for (Point dir : allDirs) {
                cx = x + dir.x;
                cy = y + dir.y;

                int pix = buf.getValue(cx, cy);

                if ((pix == BACKGROUND) || (pix == HIDDEN) || isProcessed(pix)) {
                    // Not a possible way
                } else if (pix == ARC) {
                    // Go this way
                    return Status.CONTINUE;
                } else if (isJunction(pix)) {
                    Point junctionPt = new Point(cx, cy);

                    if (!junctionPt.equals(stopJunction)
                        && !junctionPt.equals(startJunction)) {
                        if (reverse) {
                            startJunction = junctionPt;
                        } else {
                            stopJunction = junctionPt;
                        }

                        return Status.SWITCH;
                    }
                }
            }

            // The end (dead end or back to start)
            return Status.END;
        }

        private double sinSq (int x0,
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

        private void storeShape (ArcShape shape)
        {
            this.shape = shape;

            Point first = points.get(0);
            Point last = points.get(points.size() - 1);
            setPixel(first.x, first.y, PROCESSED + shape.ordinal());
            setPixel(last.x, last.y, PROCESSED + shape.ordinal());
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Double maxAlpha = new Constant.Double(
                "degree",
                3,
                "Maximum angle (in degrees) for 3 points colinearity");

        final Scale.Fraction arcMinQuorum = new Scale.Fraction(
                1.5,
                "Minimum arc length for quorum");

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

        Scale.Fraction minStaffLineDistance = new Scale.Fraction(
                0.25,
                "Minimum distance from staff line");

        Constant.Double maxArcAngle = new Constant.Double(
                "degree",
                190.0,
                "Maximum angle (in degrees) of slur arc");

        Constant.Double minAngleFromVertical = new Constant.Double(
                "degree",
                20.0,
                "Minimum angle (in degrees) between slur and vertical");

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

        final double maxLineDistance;

        final double maxCircleDistance;

        final double minCircleRadius;

        final double maxCircleRadius;

        final double maxArcAngle;

        final double minAngleFromVertical;

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
            maxLineDistance = scale.toPixelsDouble(constants.maxLineDistance);
            maxCircleDistance = scale.toPixelsDouble(
                    constants.maxCircleDistance);
            minCircleRadius = scale.toPixelsDouble(constants.minCircleRadius);
            maxCircleRadius = scale.toPixelsDouble(constants.maxCircleRadius);
            maxArcAngle = toRadians(constants.maxArcAngle.getValue());
            minAngleFromVertical = toRadians(
                    constants.minAngleFromVertical.getValue());
            minSlope = constants.minSlope.getValue();
            minStaffLineDistance = scale.toPixelsDouble(
                    constants.minStaffLineDistance);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }

    //------//
    // Slur //
    //------//
    private class Slur
    {
        //~ Instance fields ----------------------------------------------------

        private final int id;

        private Point startJunction;

        private Point stopJunction;

        private final List<Arc> arcs = new ArrayList<Arc>();

        private final Set<Arc> triedArcs = new LinkedHashSet<Arc>();

        private boolean reverse;

        private final Circle circle;

        //~ Constructors -------------------------------------------------------
        public Slur (List<Arc> arcs,
                     Circle circle)
        {
            id = ++globalSlurId;
            this.arcs.addAll(arcs);
            this.circle = circle;

            for (Arc arc : arcs) {
                arc.assigned = true;
            }
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Try to recursively extend a slur in the desired direction.
         *
         * @param pastArcs collection of arcs already browsed
         * @param clump    the clump of slurs this one is part of
         * @param reverse  true for reverse direction, false for normal
         */
        public void extend (Collection<Arc> pastArcs,
                            Set<Slur> clump,
                            boolean reverse)
        {
            this.reverse = reverse;

            Set<Arc> browsedArcs = new LinkedHashSet<Arc>(pastArcs);
            getJunctions();

            Point junction = reverse ? startJunction : stopJunction;

            if (junction != null) {
                List<Slur> newSlurs = grow(junction, browsedArcs);

                if (!newSlurs.isEmpty()) {
                    // Extensions found
                    for (Slur slur : newSlurs) {
                        browsedArcs.add(slur.getEndArc());
                    }

                    for (Slur slur : newSlurs) {
                        slur.extend(browsedArcs, clump, reverse);
                    }
                } else {
                    // No compatible arc beyond the junction
                    clump.add(this);
                }
            } else {
                // It's a free leaf, with no junction
                clump.add(this);
            }
        }

        public void getJunctions ()
        {
            // Retrieve the ending junctions for the slur 
            if (arcs.size() > 1) {
                for (int i = 0; i < (arcs.size() - 1); i++) {
                    Arc a0 = arcs.get(i);
                    Arc a1 = arcs.get(i + 1);
                    Point common = junctionOf(a0, a1);

                    if ((a1.stopJunction != null)
                        && a1.stopJunction.equals(common)) {
                        a1.reverse();
                    }
                }
            }

            startJunction = arcs.get(0).startJunction;
            stopJunction = arcs.get(arcs.size() - 1).stopJunction;
        }

        public List<Slur> grow (Point junction,
                                Set<Arc> browsedArcs)
        {
            Arc endArc = getEndArc();
            Point endPoint = endArc.getEnd(reverse);

            // Use the first and last ends and extend them AMAP
            Point np = new Point();

            // Try to go past this junction
            // Scan all arcs that depart from this junction point
            // And keep only the good ones
            List<Slur> newSlurs = new ArrayList<Slur>();

            for (Point dir : allDirs) {
                np.move(junction.x + dir.x, junction.y + dir.y);

                if (np.equals(endPoint)) {
                    continue;
                }

                int pix = buf.getValue(np.x, np.y);
                Arc arc = null;

                if (pix == BACKGROUND) {
                    continue;
                } else if (pix == ARC) {
                    // Arc never processed
                    logger.error("BINGO");

                    ///arc = scanArc(np.x, np.y, junction);
                } else if (isProcessed(pix)) {
                    // Check arc shape
                    ArcShape shape = ArcShape.valueOf(pix);

                    if (shape.isSlurRelevant()) {
                        // Retrieve arc data
                        arc = arcsMap.get(np);
                    }
                } else if (isJunction(pix)) {
                    if (!np.equals(junction)) {
                        //TODO: 
                    }
                }

                if ((arc != null)
                    && !arc.assigned
                    && !browsedArcs.contains(arc)
                    && !triedArcs.contains(arc)) {
                    triedArcs.add(arc);
                    checkArcDirection(arc);

                    List<Arc> allArcs = getAllArcs(arc);

                    // Check compatibility with slur
                    Circle precise = getSlurCircle(allArcs);

                    if (precise != null) {
                        Slur slur = new Slur(allArcs, precise);
                        logger.debug("slur#{} extended as {}", id, slur);
                        newSlurs.add(slur);
                    }
                }
            }

            return newSlurs;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{Slur#")
                    .append(id);

            sb.append(String.format(" dist:%.3f", circle.getDistance()));

            boolean first = true;

            for (Arc arc : arcs) {
                if (first) {
                    first = false;
                } else {
                    Point j = arc.getJunction(true);
                    sb.append(" <")
                            .append(j.x)
                            .append(",")
                            .append(j.y)
                            .append(">");
                }

                sb.append(" ")
                        .append(arc);
            }

            sb.append("}");

            return sb.toString();
        }

        private void checkArcDirection (Arc arc)
        {
            // Need to reverse arc to conform with arcs list?
            if (reverse) {
                if ((arc.startJunction != null)
                    && arc.startJunction.equals(startJunction)) {
                    arc.reverse();
                }
            } else {
                if ((arc.stopJunction != null)
                    && arc.stopJunction.equals(stopJunction)) {
                    arc.reverse();
                }
            }
        }

        private List<Arc> getAllArcs (Arc arc)
        {
            List<Arc> allArcs = new ArrayList<Arc>(arcs);

            if (reverse) {
                allArcs.add(0, arc);
            } else {
                allArcs.add(arc);
            }

            return allArcs;
        }

        private Arc getEndArc ()
        {
            if (reverse) {
                return arcs.get(0);
            } else {
                return arcs.get(arcs.size() - 1);
            }
        }

        private Circle getSlurCircle (List<Arc> allArcs)
        {
            List<Point> allPoints = new ArrayList<Point>();

            for (Arc a : allArcs) {
                allPoints.addAll(a.points);
            }

            return getCircle(allPoints);
        }

        private Point junctionOf (Arc a1,
                                  Arc a2)
        {
            List<Point> s1 = new ArrayList<Point>();
            s1.add(a1.startJunction);
            s1.add(a1.stopJunction);

            List<Point> s2 = new ArrayList<Point>();
            s2.add(a2.startJunction);
            s2.add(a2.stopJunction);

            s1.retainAll(s2);

            if (!s1.isEmpty()) {
                return s1.get(0);
            } else {
                return null;
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
    private class ArcRetriever
    {
        //~ Methods ------------------------------------------------------------

        private void scan ()
        {
            for (int x = 1, w = buf.getWidth(); x < w; x++) {
                for (int y = 1, h = buf.getHeight(); y < h; y++) {
                    int pix = buf.getValue(x, y);

                    if (pix == ARC) {
                        // Basic arc pixel, not yet processed.
                        // Scan full arc
                        ///logger.info("Initial arc at x:{}, y:{}", x, y);
                        scanArc(x, y, null);
                    } else if (isJunction(pix)) {
                        // Junction pixel, scan arcs linked to this junction point
                        if (!isJunctionProcessed(pix)) {
                            scanJunction(x, y);
                        }
                    }
                }
            }
        }

        private Arc scanArc (int x,
                             int y,
                             Point startJunction)
        {
            // Remember starting point
            Arc arc = new Arc(x, y, startJunction);

            // Scan arc on normal side -> stopJunction
            arc.scan(x, y, false);

            // Scan arc on reverse side -> startJunction
            if (startJunction == null) {
                arc.scan(x, y, true);
            }

            // Check arc shape
            ArcShape shape = arc.determineShape();
            arc.storeShape(shape);

            if (shape.isSlurRelevant()) {
                arcsMap.put(arc.points.get(0), arc);
                arcsMap.put(arc.points.get(arc.points.size() - 1), arc);

                return arc;
            } else {
                arc.hide();

                return null;
            }
        }

        private void scanJunction (int x,
                                   int y)
        {
            Point junctionPt = new Point(x, y);
            setPixel(x, y, JUNCTION_PROCESSED);

            // Scan all arcs that depart from this junction point
            for (Point dir : allDirs) {
                int nx = x + dir.x;
                int ny = y + dir.y;
                int pix = buf.getValue(nx, ny);

                if (pix == ARC) {
                    scanArc(nx, ny, junctionPt);
                }
            }
        }
    }

    //-------------------//
    // JunctionRetriever //
    //-------------------//
    private class JunctionRetriever
    {
        //~ Instance fields ----------------------------------------------------

        /** Vicinity of current pixel. */
        private final Vicinity vicinity = new Vicinity();

        //~ Methods ------------------------------------------------------------
        public void scan ()
        {
            for (int x = 1, w = buf.getWidth(); x < w; x++) {
                for (int y = 1, h = buf.getHeight(); y < h; y++) {
                    int pix = buf.getValue(x, y);

                    if (pix == FOREGROUND) {
                        // Pixel not yet processed
                        checkJunction(x, y);
                    } else if (isJunction(pix)) {
                        // Junction pixel, but perhaps not the best choice
                        checkJunction(x, y);
                    }
                }
            }
        }

        private void checkJunction (int x,
                                    int y)
        {
            // Neighbors 
            int n = vicinityOf(x, y);

            if (n > 2) {
                // We may have a junction candidate
                // Only if there is no other junction side-connected with higher grade.
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
                                  List<Point> dirs)
        {
            int n = 0;

            for (Point dir : dirs) {
                int pix = buf.getValue(x + dir.x, y + dir.y);

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
            // Look for side-connected junction pixels
            // and return their highest junction grade
            int bestGrade = 0;

            for (Point dir : sideDirs) {
                int nx = x + dir.x;
                int ny = y + dir.y;
                int pix = buf.getValue(nx, ny);

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

            // Render slurs
            g.setColor(new Color(255, 0, 0, 50));

            for (Slur slur : allSlurs) {
                for (Arc arc : slur.arcs) {
                    for (Point p : arc.points) {
                        g.fillRect(p.x, p.y, 1, 1);
                    }
                }
            }

            // Render slurs
            g.setColor(new Color(0, 255, 0, 100));

            Stroke lineStroke = new BasicStroke(
                    (float) sheet.getScale().getMainFore(),
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND);
            g.setStroke(lineStroke);

            for (Slur slur : allSlurs) {
                CubicCurve2D curve = slur.circle.getCurve();

                if (curve != null) {
                    g.draw(curve);
                } else {
                    logger.info("No curve for {}", slur);
                }
            }
        }
    }
}
