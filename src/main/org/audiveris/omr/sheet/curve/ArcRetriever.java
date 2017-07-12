//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     A r c R e t r i e v e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.BasicLine;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import static org.audiveris.omr.sheet.curve.Skeleton.*;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.toRadians;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code ArcRetriever} retrieves all arcs and store the interesting ones in
 * skeleton arcsMap (and voidArcsMap for void arcs).
 * Each non-void arc has its two ending points flagged with a specific gray value to remember the
 * arc shape.
 * <p>
 * This arc retrieval is performed as a standalone task, out of any curve retrieval (which is
 * performed later).
 * Hence it cannot use the model of a current curve to drive arc retrieval, which could be
 * interesting when dealing with near vertical arcs.
 * Only if the vertical run which contains the current point is longer than a threshold, an
 * artificial junction point is set to force arc split at this point (since a switch from horizontal
 * to vertical does not always result in a junction point being detected).
 * If a skeleton sequence of points share the same (long) vertical run, only two junction points are
 * set, one at the beginning and one at the end.
 * <pre>
 * - scanImage()              // Scan the whole image for arc starts
 *   + scanJunction()         // Scan all arcs leaving a junction point
 *   |   + scanArc()
 *   + scanArc()              // Scan one arc
 *       + walkAlong()        // Walk till arc end (forward or backward)
 *       |   + move()         // Move just one pixel
 *       + determineShape()   // Determine the global arc shape
 *       + storeShape()       // Store arc shape in its ending pixels
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class ArcRetriever
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ArcRetriever.class);

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

    /** All vertical runs of the page. */
    private final RunTable verticalRuns;

    /** Curves environment. */
    @Navigable(false)
    private final Curves curves;

    /** Underlying skeleton. */
    private final Skeleton skeleton;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Current point abscissa. */
    int cx;

    /** Current point ordinate. */
    int cy;

    /** Last direction. */
    int lastDir;

    /** Are we in a long run part?. */
    boolean longRunPart;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates an ArcRetriever object
     *
     * @param curves curves environment
     */
    public ArcRetriever (Curves curves)
    {
        this.curves = curves;
        sheet = curves.getSheet();
        verticalRuns = sheet.getPicture().getTable(Picture.TableKey.BINARY);
        skeleton = curves.getSkeleton();

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // scanImage //
    //-----------//
    /**
     * Scan the whole image.
     * Note the skeleton image has background pixels on the image border, hence there is no
     * foreground point to look for there.
     */
    public void scanImage ()
    {
        for (int x = 1, w = sheet.getWidth() - 1; x < w; x++) {
            for (int y = 1, h = sheet.getHeight() - 1; y < h; y++) {
                int pix = skeleton.getPixel(x, y);

                if (pix == ARC) {
                    // Basic arc pixel, not yet processed, so scan full arc
                    scanArc(x, y, null, 0);

                    // Re-read pixel value, since an artificial junction may have been set
                    // (when the starting point is located on a long vertical run)
                    pix = skeleton.getPixel(x, y);
                }

                if (isJunction(pix)) {
                    // Junction pixel, so scan all arcs linked to this junction point
                    if (!isJunctionProcessed(pix)) {
                        scanJunction(x, y);
                    }
                }
            }
        }

        // Sort arcsEnds by abscissa
        Collections.sort(skeleton.arcsEnds, PointUtil.byAbscissa);
    }

    //----------//
    // addPoint //
    //----------//
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
        List<Point> points = arc.getPoints();

        if (reverse) {
            points.add(0, new Point(cx, cy));
        } else {
            points.add(new Point(cx, cy));
        }

        skeleton.setPixel(cx, cy, PROCESSED);
    }

    //----------------//
    // determineShape //
    //----------------//
    /**
     * Determine shape for this arc.
     *
     * @param arc arc to evaluate
     * @return the shape classification
     */
    private ArcShape determineShape (Arc arc)
    {
        ///curves.checkBreak(arc);
        List<Point> points = arc.getPoints();

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

                if (abs(invSlope + sheet.getSkew().getSlope()) <= params.minSlope) {
                    //logger.info("Vertical line");
                    return ArcShape.IRRELEVANT;
                }

                double slope = line.getSlope();

                if (abs(slope - sheet.getSkew().getSlope()) <= params.minSlope) {
                    //logger.info("Horizontal  line");
                }

                ///logger.info("Straight line");
                return ArcShape.LINE;
            }
        }

        // Circle?
        Model fittedModel = curves.getSlursBuilder().computeModel(points, false);

        if (fittedModel instanceof CircleModel) {
            arc.setModel(fittedModel);

            return ArcShape.SLUR;
        }

        // Nothing interesting
        return ArcShape.IRRELEVANT;
    }

    //------//
    // hide //
    //------//
    /**
     * Update display to show the arc points as "discarded".
     */
    private void hide (Arc arc)
    {
        List<Point> points = arc.getPoints();

        for (int i = 1; i < (points.size() - 1); i++) {
            Point p = points.get(i);

            skeleton.setPixel(p.x, p.y, HIDDEN);
        }
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
        List<Point> points = arc.getPoints();

        if (points.size() < params.minStaffArcLength) {
            return false;
        }

        Point p0 = points.get(0);
        Staff staff = sheet.getStaffManager().getClosestStaff(p0);
        LineInfo line = staff.getClosestLine(p0);
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

    //------//
    // move //
    //------//
    /**
     * Try to move to the next point of the arc.
     *
     * @param x       abscissa of current point
     * @param y       ordinate of current point
     * @param reverse current orientation
     * @return code describing the move performed if any. The new position is stored in (cx, cy).
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

            int pix = skeleton.getPixel(cx, cy);

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

            int pix = skeleton.getPixel(cx, cy);

            if (pix == ARC) {
                lastDir = dir;

                return Status.CONTINUE;
            }
        }

        // The end (dead end or back to start)
        return Status.END;
    }

    //---------//
    // scanArc //
    //---------//
    /**
     * Start the scan of an arc both ways, starting from a point of the arc, not
     * necessarily an end point.
     * <p>
     * If the very first point is part of a long run, walk along the arc both ways until either a
     * non-long run is met, or a junction point or a dead end.
     * Make sure the "long-run part" is surrounded by junction points (or arc dead end).
     * <p>
     * If, during the scan, a "long-run" is encountered, a junction must be inserted and the arc
     * scan stopped. The "long-run part" will be scanned later, certainly from the inserted junction
     * point.
     * <p>
     * TODO: Perhaps a similar junction point should be inserted when arc gets vertical (and not
     * necessarily composed of long runs).
     * <p>
     * A long-run cannot be part of a curve, unless the curve is nearly vertical at this point.
     * We should keep track of crossed runs, since they define the curve thickness (unless the run
     * gets too far away from the curve, in this case it is crossed by the curve rather than being
     * part of it).
     *
     * @param x             starting abscissa
     * @param y             starting ordinate
     * @param startJunction start junction point if any, null if none
     * @param lastDir       last direction (0 if none)
     */
    private void scanArc (int x,
                          int y,
                          Point startJunction,
                          int lastDir)
    {
        // Check vertical run at first location
        Run run = verticalRuns.getRunAt(x, y);
        longRunPart = run.getLength() > params.maxRunLength;

        // Remember starting point
        Arc arc = new Arc(startJunction);
        addPoint(arc, x, y, false);

        // Scan arc on normal side -> stopJunction
        walkAlong(arc, x, y, false, lastDir);

        // Scan arc on reverse side -> startJunction if needed
        // If we scanned from a junction, startJunction is already set
        if (startJunction == null) {
            // Set lastDir as the opposite of initial starting dir
            if (arc.getPoints().size() > 1) {
                lastDir = getDir(arc.getPoints().get(1), arc.getPoints().get(0));
            } else if ((arc.getJunction(false) != null) && (arc.getPoints().size() > 0)) {
                lastDir = getDir(arc.getJunction(false), arc.getPoints().get(0));
            } else {
                lastDir = 0;
            }

            longRunPart = run.getLength() > params.maxRunLength;
            walkAlong(arc, x, y, true, lastDir);
        }

        // Whenever possible, orient an arc from left to right
        arc.checkOrientation();

        // Check arc shape
        ArcShape shape = determineShape(arc);

        if (arc.getLength() > 0) {
            storeShape(arc, shape);

            if (shape.isSlurRelevant()) {
                Point first = arc.getEnd(true);
                skeleton.arcsMap.put(first, arc);
                skeleton.arcsEnds.add(first);

                Point last = arc.getEnd(false);
                skeleton.arcsMap.put(last, arc);
                skeleton.arcsEnds.add(last);
            } else {
                hide(arc);
            }
        }
    }

    //--------------//
    // scanJunction //
    //--------------//
    /**
     * Scan all arcs connected to this junction point
     *
     * @param x junction point abscissa
     * @param y junction point ordinate
     */
    private void scanJunction (int x,
                               int y)
    {
        final Point startJunction = new Point(x, y);
        skeleton.setPixel(x, y, JUNCTION_PROCESSED);

        // Scan all arcs that depart from this junction point
        for (int dir : allDirs) {
            int nx = x + dxs[dir];
            int ny = y + dys[dir];
            int pix = skeleton.getPixel(nx, ny);

            if (pix == ARC) {
                scanArc(nx, ny, startJunction, dir);

                // Re-read the pixel value
                pix = skeleton.getPixel(nx, ny);
            }

            if (isJunction(pix)) {
                if (!isJunctionProcessed(pix)) {
                    // We have a junction point, touching this one, hence use a no-point arc
                    Point stopJunction = new Point(nx, ny);
                    Arc arc = new Arc(startJunction, stopJunction);
                    arc.checkOrientation();
                    skeleton.addVoidArc(arc);
                }
            }
        }
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
    // storeShape //
    //------------//
    /**
     * "Store" the arc shape in its ending points, so that scanning from a junction
     * point can immediately tell whether the arc is relevant without having to rescan
     * the full arc.
     *
     * @param shape the arc shape
     */
    private void storeShape (Arc arc,
                             ArcShape shape)
    {
        arc.setShape(shape);

        List<Point> points = arc.getPoints();

        Point first = points.get(0);
        Point last = points.get(points.size() - 1);
        skeleton.setPixel(first.x, first.y, PROCESSED + shape.ordinal());
        skeleton.setPixel(last.x, last.y, PROCESSED + shape.ordinal());
    }

    //-----------//
    // walkAlong //
    //-----------//
    /**
     * Walk along the arc in the desired orientation, starting at (x,y) point, until no
     * more incremental move is possible.
     * Always check the vertical run which contains the current point.
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

        while (move(arc, cx, cy, reverse) == Status.CONTINUE) {
            // Check vertical run length at current point
            Run run = verticalRuns.getRunAt(cx, cy);

            if (run.getLength() > params.maxRunLength) {
                if (!longRunPart) {
                    logger.debug("Start of long run part at {} before x:{} y:{}", arc, cx, cy);
                    // Insert a junction point
                    skeleton.setPixel(cx, cy, JUNCTION);
                    arc.setJunction(new Point(cx, cy), reverse);

                    return; // Stop the arc
                }
            } else if (longRunPart) {
                // We have detected the end of the long run part
                logger.debug("End of long run part {} before x:{} y:{}", arc, cx, cy);

                // Insert a junction point and shorten the points sequence accordingly
                Point vp = arc.getEnd(reverse);

                if (vp != null) {
                    skeleton.setPixel(vp.x, vp.y, JUNCTION);
                    arc.setJunction(vp, reverse);
                    arc.getPoints().remove(reverse ? 0 : (arc.getPoints().size() - 1));
                }

                return; // Stop the arc
            }

            addPoint(arc, cx, cy, reverse);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Double maxAlpha = new Constant.Double(
                "degree",
                4.0,
                "Maximum angle (in degrees) for 3 points colinearity");

        private final Scale.Fraction arcMinQuorum = new Scale.Fraction(
                1.75,
                "Minimum arc length for quorum");

        private final Scale.Fraction maxLineDistance = new Scale.Fraction(
                0.1,
                "Maximum distance from straight line");

        private final Scale.Fraction minStaffArcLength = new Scale.Fraction(
                0.5,
                "Minimum length for a staff arc");

        private final Scale.Fraction maxStaffArcLength = new Scale.Fraction(
                5.0,
                "Maximum length for a staff arc");

        private final Scale.Fraction minStaffLineDistance = new Scale.Fraction(
                0.15,
                "Minimum distance from staff line");

        private final Constant.Double minSlope = new Constant.Double(
                "(co)tangent",
                0.03,
                "Minimum (inverted) slope, to detect vertical and horizontal lines");

        private final Scale.Fraction maxRunLength = new Scale.Fraction(
                0.6,
                "Maximum length for a vertical run");
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

        final int arcMinQuorum;

        final int minStaffArcLength;

        final int maxStaffArcLength;

        final double minStaffLineDistance;

        final double maxSinSq;

        final double maxLineDistance;

        final double minSlope;

        final int maxRunLength;

        //~ Constructors ---------------------------------------------------------------------------
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
            minSlope = constants.minSlope.getValue();
            minStaffArcLength = scale.toPixels(constants.minStaffArcLength);
            maxStaffArcLength = scale.toPixels(constants.maxStaffArcLength);
            minStaffLineDistance = scale.toPixelsDouble(constants.minStaffLineDistance);
            maxRunLength = scale.toPixels(constants.maxRunLength);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
