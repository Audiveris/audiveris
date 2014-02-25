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
package omr.skeleton;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.grid.FilamentLine;
import omr.grid.StaffInfo;
import static omr.image.PixelSource.BACKGROUND;

import omr.math.Circle;
import omr.math.GeoPath;
import omr.math.PointUtil;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;
import omr.sheet.ui.ImageView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScrollImageView;

import omr.sig.Exclusion.Cause;
import omr.sig.GradeImpacts;
import omr.sig.SIGraph;
import omr.sig.SlurInter;
import omr.sig.SlurInter.Impacts;
import static omr.skeleton.Arc.ArcShape;
import static omr.skeleton.Skeleton.*;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code SlursBuilder} retrieves slurs in a sheet by walking along the arcs of
 * image skeleton.
 * <p>
 * We have to visit each pixel of the buffer, detect junction points and arcs departing or arriving
 * at junction points.
 * A point if a junction point if it has more than 2 immediate neighbors in the 8 peripheral cells
 * of the 3x3 square centered on the point.
 * We may need a way to detect if a given point has already been visited.
 *
 * @author Hervé Bitteur
 */
public class SlursBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Point[] breakPoints = new Point[]{ /* new
     * Point(1495, 619) */}; // BINGO

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlursBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Companion for slur-notes connections. */
    private final SlursLinker slursLinker;

    /** All slur infos created. */
    private final List<SlurInfo> pageInfos = new ArrayList<SlurInfo>();

    /** All slurs retrieved. */
    private final List<SlurInter> pageSlurs = new ArrayList<SlurInter>();

    /** View on skeleton, if any. */
    private MyView view;

    /** Underlying skeleton. */
    private final Skeleton skeleton;

    /** The skeleton buffer. */
    public final ByteProcessor buf;

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
        params = new Parameters(sheet.getScale());

        skeleton = new Skeleton(sheet);

        BufferedImage img = skeleton.buildSkeleton();
        buf = skeleton.buf;
        slursLinker = new SlursLinker(sheet);

        // Display skeleton
        if (Main.getGui() != null) {
            view = new SlursBuilder.MyView(img);
            sheet.getAssembly().addViewTab(
                    "Skeleton",
                    new ScrollImageView(sheet, view),
                    new BoardsPane(new PixelBoard(sheet)));
        }
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
        new JunctionRetriever(skeleton).scanImage();

        // Scan arcs between junctions
        watch.start("retrieveArcs");
        new ArcRetriever(sheet, this, skeleton).scanImage();

        // Retrieve slurs from arcs
        watch.start("retrieveSlurs");
        new SlurRetriever().retrieveSlurs();

        watch.print();
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
    Circle getCircle (List<Point> points)
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

            // Check circle radius is rather similar to rough radius. If not, keep the rough one.
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
            } else {
                logger.debug("{} to {} Circle {}", p0, p2, circle);

                return circle;
            }
        } catch (Exception ex) {
            logger.debug("Could not compute circle {} at {}", p0);

            return null;
        }
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio similarRadiusRatio = new Constant.Ratio(
                0.25,
                "Maximum difference ratio between radius of similar circles");

        final Constant.Double maxIncidence = new Constant.Double(
                "degree",
                45,
                "Maximum incidence angle (in degrees) for staff tangency");

        final Scale.Fraction arcMinSeedLength = new Scale.Fraction(
                1.0,
                "Minimum arc length for starting a slur build");

        final Scale.Fraction maxStaffLineDy = new Scale.Fraction(
                0.2,
                "Vertical distance to closest staff line to detect tangency");

        final Scale.Fraction maxSlurDistance = new Scale.Fraction(
                0.1,
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

        final Constant.Ratio quorumRatio = new Constant.Ratio(
                0.75,
                "Minimum length expressed as ratio of longest in clump");
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

        final int sideLength;

        final int arcCheckLength;

        final int arcMinSeedLength;

        final double maxStaffLineDy;

        final double maxIncidence;

        final double maxSlurDistance;

        final double maxExtDistance;

        final double maxArcsDistance;

        final double minCircleRadius;

        final double minSlurWidthLow;

        final double minSlurWidthHigh;

        final double minSlurHeightLow;

        final double minSlurHeightHigh;

        final double maxArcAngleLow;

        final double maxArcAngleHigh;

        final double minAngleFromVerticalLow;

        final double minAngleFromVerticalHigh;

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
            similarRadiusRatio = constants.similarRadiusRatio.getValue();
            sideLength = scale.toPixels(constants.sideCircleLength);
            arcCheckLength = scale.toPixels(constants.arcCheckLength);
            arcMinSeedLength = scale.toPixels(constants.arcMinSeedLength);
            maxStaffLineDy = scale.toPixelsDouble(constants.maxStaffLineDy);
            maxIncidence = toRadians(constants.maxIncidence.getValue());
            maxSlurDistance = scale.toPixelsDouble(constants.maxSlurDistance);
            maxExtDistance = scale.toPixelsDouble(constants.maxExtDistance);
            maxArcsDistance = scale.toPixelsDouble(constants.maxArcsDistance);
            minCircleRadius = scale.toPixelsDouble(constants.minCircleRadius);
            minSlurWidthLow = scale.toPixelsDouble(constants.minSlurWidthLow);
            minSlurWidthHigh = scale.toPixelsDouble(constants.minSlurWidthHigh);
            minSlurHeightLow = scale.toPixelsDouble(constants.minSlurHeightLow);
            minSlurHeightHigh = scale.toPixelsDouble(constants.minSlurHeightHigh);
            maxArcAngleHigh = toRadians(constants.maxArcAngleHigh.getValue());
            maxArcAngleLow = toRadians(constants.maxArcAngleLow.getValue());
            minAngleFromVerticalLow = toRadians(constants.minAngleFromVerticalLow.getValue());
            minAngleFromVerticalHigh = toRadians(constants.minAngleFromVerticalHigh.getValue());
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
            for (Arc arc : skeleton.arcsMap.values()) {
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

    //---------------//
    // SlurRetriever //
    //---------------//
    /**
     * Scan all arcs to retrieve slurs.
     */
    private class SlurRetriever
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Clump on left side of aggregated slurs candidates. */
        private final Set<SlurInfo> leftClump = new LinkedHashSet<SlurInfo>();

        /** Clump on right side of aggregated slurs candidates. */
        private final Set<SlurInfo> rightClump = new LinkedHashSet<SlurInfo>();

        /** Current orientation for walking along a slur. */
        private boolean reverse;

        /** Current maximum length for arcs to be tried. */
        private Integer maxLength = null;

        /** For unique slur IDs. (page wise) */
        private int globalSlurId = 0;

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

                if (pts.size() >= params.sideLength) {
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
                SlurInfo s = new SlurInfo(id, arcs, null, params.sideLength);
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
            for (Point p : arc.getSidePoints(params.arcCheckLength, !reverse)) {
                double dx = p.x - center.x;
                double dy = p.y - center.y;
                sum += ((dx * dx) + (dy * dy));
                np++;
            }

            double meanRadius = sqrt(sum / np);

            return Math.abs(meanRadius - radius);
        }

        private List<Arc> arcsOf (SlurInfo left,
                                  SlurInfo right)
        {
            List<Arc> list = new ArrayList<Arc>(left.getArcs());
            list.removeAll(right.getArcs());
            list.addAll(right.getArcs());

            return list;
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

            int tid = ++globalSlurId;
            SlurInfo trunk = new SlurInfo(tid, Arrays.asList(arc), arc.circle, params.sideLength);
            pageInfos.add(trunk);

            if (arc.circle == null) {
                trunk.setCircle(getCircle(trunk.pointsOf(Arrays.asList(arc))));
            }

            //            logger.info("----------------------------\nbuildSlur {}", trunk);
            // Extensions in "normal" & "reverse" parts of the trunk
            for (boolean rev : new boolean[]{false, true}) {
                Set<SlurInfo> clump = rev ? leftClump : rightClump;
                reverse = rev;
                clump.clear();
                trunk.setCrossedLine(null);
                extend(trunk, trunk.getArcs(), clump);

                if (clump.size() > 1) {
                    weed(clump); // Filter out the least interesting candidates
                }
            }

            // Combine candidates from both sides
            Set<SlurInter> inters = new HashSet<SlurInter>();

            for (SlurInfo sl : leftClump) {
                for (SlurInfo sr : rightClump) {
                    SlurInfo slur;

                    if (sl == sr) {
                        slur = sl;
                    } else {
                        int id = ++globalSlurId;
                        slur = new SlurInfo(id, arcsOf(sl, sr), null, params.sideLength);
                    }

                    GradeImpacts impacts = computeImpacts(slur, true);

                    if (impacts != null) {
                        SlurInter inter = new SlurInter(slur, impacts);
                        inters.add(inter);
                    }
                }
            }

            if (inters.isEmpty()) {
                return;
            }

            // Delegate final selection to SlursLinker
            SlurInter selected = slursLinker.prune(inters);

            if (selected != null) {
                selected.getInfo().assign(); // Assign arcs
                pageSlurs.add(selected);
            }
        }

        /**
         * Compute impacts for slur candidate.
         *
         * @param slur slur information
         * @param both true for both sides, false for single side (the one designated by reverse)
         * @return grade impacts
         */
        private Impacts computeImpacts (SlurInfo slur,
                                        boolean both)
        {
            Circle global = needCircle(slur);
            List<Arc> arcs = slur.getArcs();

            // Distance to circle (both side circles or just a single side circle)
            double dist;

            if (both) {
                double sum = 0;

                for (boolean bool : new boolean[]{true, false}) {
                    Circle sideCircle = slur.getSideCircle(bool);

                    if (sideCircle == null) {
                        return null;
                    }

                    double d = sideCircle.computeDistance(slur.getSidePoints(arcs, bool));
                    sideCircle.setDistance(d);
                    sum += d;
                }

                dist = sum / 2;
            } else {
                Circle sideCircle = slur.getSideCircle(reverse);

                if (sideCircle == null) {
                    return null;
                }

                dist = sideCircle.computeDistance(slur.getSidePoints(arcs, reverse));
            }

            // Distance to circle
            if (dist > params.maxSlurDistance) {
                return null;
            }

            double distImpact = 1 - (dist / params.maxSlurDistance);

            // Max arc angle value
            if (global == null) {
                return null;
            }

            double arcAngle = global.getArcAngle();

            if (arcAngle > params.maxArcAngleHigh) {
                logger.debug("Slur too curved {} {}", arcAngle, this);

                return null;
            }

            double angleImpact = (params.maxArcAngleHigh - arcAngle) / (params.maxArcAngleHigh
                                                                        - params.maxArcAngleLow);

            // No vertical slur (mid angle close to 0 or PI)
            double midAngle = global.getMidAngle();

            if (midAngle < 0) {
                midAngle += (2 * PI);
            }

            midAngle = midAngle % PI;

            double fromVertical = min(abs(midAngle), abs(PI - midAngle));

            if (fromVertical < params.minAngleFromVerticalLow) {
                logger.debug("Slur too vertical {} {}", midAngle, global);

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
                             Collection<Arc> pastArcs,
                             Set<SlurInfo> clump)
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
                    extend(s, browsed, clump); // Increment further
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

            for (Arc arc : skeleton.arcsMap.values()) {
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
            StaffInfo staff = sheet.getStaffManager().getStaffAt(se);
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

                for (Point end : skeleton.arcsEnds) {
                    if (area.contains(end)) {
                        Arc arc = skeleton.arcsMap.get(end);

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
                        arc = skeleton.arcsMap.get(np); // Retrieve arc data
                        checkBreak(arc);
                    }
                } else if (isJunction(pix)) {
                    if (!np.equals(pivot)) {
                        arc = skeleton.arcsMap.get(pivot);
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
        private void weed (Set<SlurInfo> clump)
        {
            // Compute grades
            List<SlurInter> inters = new ArrayList<SlurInter>();

            for (SlurInfo slur : clump) {
                GradeImpacts impacts = computeImpacts(slur, false);

                if (impacts != null) {
                    SlurInter inter = new SlurInter(slur, impacts);
                    inters.add(inter);
                }
            }

            // Purge clump
            clump.clear();

            // Discard too short ones
            int upperLength = 0;
            SlurInter longest = null;

            for (SlurInter slur : inters) {
                int length = slur.getInfo().getLength();

                if (upperLength < length) {
                    upperLength = length;
                    longest = slur;
                }
            }

            if (longest == null) {
                return;
            }

            int quorum = (int) Math.ceil(params.quorumRatio * upperLength);

            for (Iterator<SlurInter> it = inters.iterator(); it.hasNext();) {
                if (quorum > it.next().getInfo().getLength()) {
                    it.remove();
                }
            }

            // Discard those with grade lower than grade of longest
            double longestGrade = longest.getGrade();

            for (Iterator<SlurInter> it = inters.iterator(); it.hasNext();) {
                if (it.next().getGrade() < longestGrade) {
                    it.remove();
                }
            }

            for (SlurInter slur : inters) {
                clump.add(slur.getInfo());
            }

            //            // Make sure all slurs in clump are left-to-right oriented
            //            if (longest.getEnd(true).x > longest.getEnd(false).x) {
            //                // Reverse ALL arcs & slurs of the clump
            //                Set<Arc> allArcs = new HashSet<Arc>();
            //
            //                for (SlurInter inter : clump) {
            //                    inter.getInfo().reverse();
            //                    allArcs.addAll(inter.getInfo().getArcs());
            //                }
            //
            //                for (Arc arc : allArcs) {
            //                    arc.reverse();
            //                }
            //            }
        }
    }
}
