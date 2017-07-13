//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C u r v e s B u i l d e r                                   //
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

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import static org.audiveris.omr.image.PixelSource.BACKGROUND;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoPath;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.SystemInfo;
import static org.audiveris.omr.sheet.curve.Skeleton.*;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.InterIndex;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.util.ItemRenderer;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code CurvesBuilder} is an abstract basis to build curves on top of the
 * underlying skeleton for slurs (see {@link SlursBuilder}), as well as wedges and
 * endings (see {@link SegmentsBuilder}).
 *
 * @author Hervé Bitteur
 */
public abstract class CurvesBuilder
        implements ItemRenderer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(CurvesBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related sheet. */
    @Navigable(false)
    protected final Sheet sheet;

    /** Global sheet skew. */
    protected final Skew skew;

    /** Curves environment. */
    protected final Curves curves;

    /** Image skeleton. */
    protected final Skeleton skeleton;

    /** Binary image (with staff lines). */
    private final ByteProcessor binaryBuf;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** For unique curve IDs. (per page and per type of curve: slur or segment) */
    protected int globalId = 0;

    /** (Current) orientation for walking along a curve. */
    protected boolean reverse;

    /** To sort Extension instances by decreasing grade. */
    private Comparator<Extension> byReverseGrade = new Comparator<Extension>()
    {
        @Override
        public int compare (Extension e1,
                            Extension e2)
        {
            return Double.compare(e2.getGrade(), e1.getGrade());
        }
    };

    /** (Debug) tells whether an arc is being debugged. */
    protected boolean debugArc;

    // Debug, to be removed ASAP.
    protected int maxClumpSize = 0;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SequencesBuilder object.
     *
     * @param curves curves environment
     */
    public CurvesBuilder (Curves curves)
    {
        this.curves = curves;
        sheet = curves.getSheet();
        skeleton = curves.getSkeleton();
        skew = sheet.getSkew();

        binaryBuf = sheet.getPicture().getSource(Picture.SourceKey.BINARY);
        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Try to append one arc to an existing curve and thus create a new curve.
     *
     * @param arcView view on the candidate for extension
     * @param curve   the curve to be extended by arc
     * @return the newly created curve, if successful
     */
    protected abstract Curve addArc (ArcView arcView,
                                     Curve curve);

    /**
     * Compute impacts for curve candidate.
     *
     * @param curve     the curve to evaluate
     * @param bothSides true for both sides, false for current side
     * @return grade impacts
     */
    protected abstract GradeImpacts computeImpacts (Curve curve,
                                                    boolean bothSides);

    /**
     * Check whether the provided points can represent a curve.
     *
     * @param points the provided points
     * @param isSeed true for the very first model computed on a curve seed
     * @return the model if OK, null if not
     */
    protected abstract Model computeModel (List<Point> points,
                                           boolean isSeed);

    //-------------//
    // createCurve //
    //-------------//
    /**
     * Create a curve instance from a seed arc
     *
     * @param seedArc the seed arc
     * @param model   a model if any
     * @return the created curve
     */
    protected Curve createCurve (Arc seedArc,
                                 Model model)
    {
        return createInstance(
                seedArc.getJunction(true),
                seedArc.getJunction(false),
                seedArc.getPoints(),
                model,
                Arrays.asList(seedArc));
    }

    //-------------//
    // createCurve //
    //-------------//
    /**
     * Create a new curve by appending an arc to an existing curve.
     *
     * @param curve   the existing curve
     * @param arcView properly oriented view on appended arc
     * @param points  the full sequence of points (curve + arc)
     * @param model   new underlying model, if any
     * @return the created curve
     */
    protected Curve createCurve (Curve curve,
                                 ArcView arcView,
                                 List<Point> points,
                                 Model model)
    {
        final Point firstJunction;
        final Point lastJunction;

        if (reverse) {
            firstJunction = arcView.getJunction(reverse);
            lastJunction = curve.getJunction(!reverse);
        } else {
            firstJunction = curve.getJunction(!reverse);
            lastJunction = arcView.getJunction(reverse);
        }

        if (points == null) {
            points = curve.getAllPoints(arcView, reverse);
        }

        Set<Arc> parts = new LinkedHashSet<Arc>(curve.getParts());
        parts.add(arcView.getArc());

        return createInstance(firstJunction, lastJunction, points, model, parts);
    }

    /**
     * Create a curve instance of proper type.
     *
     * @param firstJunction first junction point, if any
     * @param lastJunction  second junction point, if any
     * @param points        provided list of points
     * @param model         an already computed model if any
     * @param parts         all arcs used for this curve
     * @return the created curve instance
     */
    protected abstract Curve createInstance (Point firstJunction,
                                             Point lastJunction,
                                             List<Point> points,
                                             Model model,
                                             Collection<Arc> parts);

    /**
     * (Try to) create an Inter instance from a curve candidate
     *
     * @param curve  the candidate
     * @param inters (output) to be appended with created Inter instances
     */
    protected abstract void createInter (Curve curve,
                                         Set<Inter> inters);

    /**
     * Additional filtering if any on the provided collection of inters.
     *
     * @param inters the collection of inters to further filter
     */
    protected abstract void filterInters (Set<Inter> inters);

    /**
     * Report the number of points at beginning of arc tested for connection.
     *
     * @return the number of points for which distance will be checked, or null for no limit
     */
    protected abstract Integer getArcCheckLength ();

    /**
     * Report the tangent unit vector at curve end.
     *
     * @param curve the curve
     * @return the unit vector which extends the curve end
     */
    protected abstract Point2D getEndVector (Curve curve);

    /**
     * Among the clump of curves built from a common trunk on 'reverse' side, weed out
     * some of them.
     *
     * @param clump the competing curves on the same side of a given seed
     */
    protected abstract void weed (Set<Curve> clump);

    //-------------//
    // arcDistance //
    //-------------//
    /**
     * Measure the mean distance from additional arc to provided (side) model.
     * <p>
     * Not all arc points are checked, only the ones close to curve end.
     *
     * @param model   the reference model
     * @param arcView properly oriented view on the extension arc to be checked for compatibility
     * @return the average distance of arc points to the curve model
     */
    protected double arcDistance (Model model,
                                  ArcView arcView)
    {
        // First, determine the collection of points to measure (junction + first arc points)
        List<Point> points = new ArrayList<Point>();
        Point junction = arcView.getJunction(!reverse);

        if (junction != null) {
            points.add(junction);
        }

        points.addAll(arcView.getSidePoints(getArcCheckLength(), !reverse));

        // Second, compute their distance to the model
        return model.computeDistance(points);
    }

    //------------//
    // buildCurve //
    //------------//
    /**
     * Build one curve starting from a seed arc.
     *
     * @param arc the line seed
     */
    protected void buildCurve (Arc arc)
    {
        debugArc = curves.checkBreak(arc); // Debug

        Curve trunk = createCurve(arc, arc.getModel());

        if (debugArc) {
            logger.info("Trunk: {}", trunk);
        }

        if (arc.getModel() != null) {
            trunk.setModel(arc.getModel());
        } else {
            trunk.setModel(computeModel(arc.getPoints(), true));
        }

        // Try to extend the trunk as much as possible, on right end then on left end.
        final Set<Curve> leftClump = new LinkedHashSet<Curve>();
        final Set<Curve> rightClump = new LinkedHashSet<Curve>();

        for (boolean rev : new boolean[]{false, true}) {
            Set<Curve> clump = rev ? leftClump : rightClump;
            reverse = rev;
            extend(trunk, clump);

            weed(clump); // Filter out the least interesting candidates
            maxClumpSize = Math.max(maxClumpSize, clump.size());
        }

        // Combine candidates from both sides
        Set<Inter> inters = new LinkedHashSet<Inter>();

        // Connect lefts & rights
        // Both endings must be OK, hence none of the side clumps is allowed to be empty
        for (Curve sl : leftClump) {
            for (Curve sr : rightClump) {
                Curve curve = (sl == sr) ? sl : createCurve(sl, sr);
                createInter(curve, inters);
            }
        }

        // Finally, filter candidates using their potential links to embraced head-chords
        if (!inters.isEmpty()) {
            ///register(inters); // For DEBUG only
            filterInters(inters);
        }
    }

    //-------------//
    // createCurve //
    //-------------//
    /**
     * Create a curve by concatenating two curves (which may have some points in common).
     *
     * @param left  first curve
     * @param right second curve
     * @return the created curve.
     */
    protected Curve createCurve (Curve left,
                                 Curve right)
    {
        if (left.getEnd(false).equals(right.getEnd(false))) {
            return left;
        }

        if (right.getEnd(true).equals(left.getEnd(true))) {
            return right;
        }

        // Build the list of points with no duplicates
        List<Point> points = new ArrayList<Point>(left.getPoints());

        for (Point p : right.getPoints()) {
            if (!points.contains(p)) {
                points.add(p);
            }
        }

        // Build the set of parts
        Set<Arc> parts = new LinkedHashSet<Arc>(left.getParts());
        parts.addAll(right.getParts());

        return createInstance(
                left.getJunction(true),
                right.getJunction(false),
                points,
                null,
                parts);
    }

    //---------------//
    // defineExtArea //
    //---------------//
    /**
     * Define extension area on current side of the curve.
     * (side is defined by 'reverse' current value)
     *
     * @param curve the curve to extend
     * @return the extension lookup area
     */
    protected Area defineExtArea (Curve curve)
    {
        Point ce = curve.getEnd(reverse); // Curve End
        Point2D uv = getEndVector(curve); // Unit Vector
        GeoPath path;

        //        if (tgLine != null) {
        //            int xDir = (uv.getX() > 0) ? 1 : (-1);
        //            double lg = xDir * params.lineBoxLength;
        //            double dx = xDir * params.lineBoxIn;
        //            int yDir = (uv.getY() > 0) ? 1 : (-1);
        //            double dl1 = yDir * params.lineBoxDeltaIn;
        //            double dl2 = yDir * params.lineBoxDeltaOut;
        //            double yLine = tgLine.yAt(ce.x);
        //            path = new GeoPath(
        //                    new Line2D.Double(
        //                            new Point2D.Double(ce.x - dx, yLine),
        //                            new Point2D.Double(ce.x - dx, yLine + dl1)));
        //            yLine = tgLine.yAt(ce.x + lg);
        //            path.append(
        //                    new Line2D.Double(
        //                            new Point2D.Double(ce.x + lg, yLine + dl2),
        //                            new Point2D.Double(ce.x + lg, yLine)),
        //                    true);
        //        } else {
        if (uv == null) {
            return null;
        }

        double lg = params.gapBoxLength;
        Point2D lgVect = new Point2D.Double(lg * uv.getX(), lg * uv.getY());
        Point2D ce2 = PointUtil.addition(ce, lgVect);

        double dl1 = params.gapBoxDeltaIn;
        Point2D dlVect = new Point2D.Double(-dl1 * uv.getY(), dl1 * uv.getX());
        path = new GeoPath(
                new Line2D.Double(PointUtil.addition(ce, dlVect), PointUtil.subtraction(ce, dlVect)));

        double dl2 = params.gapBoxDeltaOut;
        dlVect = new Point2D.Double(-dl2 * uv.getY(), dl2 * uv.getX());
        path.append(
                new Line2D.Double(PointUtil.subtraction(ce2, dlVect), PointUtil.addition(ce2, dlVect)),
                true);
        //        }
        path.closePath();

        Area area = new Area(path);
        curve.setExtArea(area, reverse);
        curve.addAttachment(reverse ? "t" : "f", area);

        return area;
    }

    //-----------------//
    // needGlobalModel //
    //-----------------//
    /**
     * Make sure the curve has a global model and report it.
     *
     * @param curve the curve at hand
     * @return the curve global model
     */
    protected Model needGlobalModel (Curve curve)
    {
        Model model = curve.getModel();

        if (model == null) {
            model = computeModel(curve.getPoints(), false);
            curve.setModel(model);
        }

        return model;
    }

    //------------//
    // projection //
    //------------//
    /**
     * Report the projection of extension arc on curve end direction
     *
     * @param arcView proper view of extension arc
     * @param model   curve model
     * @return projection of arc on curve end unit vector
     */
    protected double projection (ArcView arcView,
                                 Model model)
    {
        Point a1 = arcView.getEnd(!reverse);

        if (a1 == null) {
            a1 = arcView.getJunction(!reverse);
        }

        Point a2 = arcView.getEnd(reverse);

        if (a2 == null) {
            a2 = arcView.getJunction(reverse);
        }

        Point arcVector = new Point(a2.x - a1.x, a2.y - a1.y);
        Point2D unit = model.getEndVector(reverse);

        return PointUtil.dotProduct(arcVector, unit);
    }

    //--------------//
    // erasedInters //
    //--------------//
    /**
     * Report all erased inters in provided system that intersect the provided box.
     *
     * @param system    the system being processed
     * @param box       the box to be intersected
     * @param crossable true for crossable, false for non-crossable
     * @return the collection of erased inter instances with proper crossable characteristic
     */
    private Set<Inter> erasedInters (SystemInfo system,
                                     Rectangle box,
                                     boolean crossable)
    {
        Set<Inter> found = new LinkedHashSet<Inter>();
        List<Inter> inters = skeleton.getErasedInters(crossable).get(system);

        for (Inter inter : inters) {
            if (inter.getBounds().intersects(box)) {
                Area area = inter.getArea();

                if ((area == null) || area.intersects(box)) {
                    found.add(inter);
                }
            }
        }

        return found;
    }

    //--------//
    // extend //
    //--------//
    /**
     * Try to extend a trunk, using the current 'reverse' orientation.
     *
     * @param trunk the trunk to extend
     * @param clump (output) the clump to populate with candidates
     */
    private void extend (Curve trunk,
                         Set<Curve> clump)
    {
        // Debug: display the trunk end point
        if (debugArc) {
            curves.selectPoint(trunk.getEnd(reverse));
        }

        clump.add(trunk);

        // Extensions kept for clump
        final Set<Extension> candidates = new LinkedHashSet<Extension>();

        // Map of pivot points reached so far
        final Map<Point, Extension> pivots = new LinkedHashMap<Point, Extension>();

        // Extensions created in last pass
        List<Extension> rookies = new ArrayList<Extension>();
        Extension trunkExt = new Extension(trunk, trunk.getParts());
        Point trunkPivot = trunk.getJunction(reverse);

        if (trunkPivot != null) {
            pivots.put(trunkPivot, trunkExt);
        }

        rookies.add(trunkExt);

        // Gather all interesting trunk extensions (in current orientation)
        // We use a breadth-first strategy, to detect converging extensions ASAP
        do {
            final List<Extension> actives = rookies;
            rookies = new ArrayList<Extension>();

            for (Extension ext : actives) {
                Point pivot = ext.curve.getJunction(reverse);

                if (pivot != null) {
                    if (debugArc) {
                        curves.selectPoint(pivot);
                    }

                    // Check beyond pivot: scan arcs starting at pivot
                    scanPivot(ext, pivot, rookies);
                } else {
                    // Check beyond gap: scan arcs starting in extension window
                    scanGap(ext, rookies);
                }
            }

            // To avoid endless extensions...
            if (rookies.size() >= params.maxExtensionRookies) {
                logger.info("Curve: {} Too many rookies: {}, giving up!", trunk, rookies.size());

                break;
            }

            if (rookies.size() > 1) {
                Collections.sort(rookies, byReverseGrade);
            }

            // Check junction point at end of each rookie
            for (int i = 0; i < rookies.size(); i++) {
                Extension rookie = rookies.get(i);
                Point pivot = rookie.curve.getJunction(reverse);

                if (pivot != null) {
                    if (debugArc) {
                        curves.selectPoint(pivot);
                    }

                    // Have we already met this pivot?
                    // If so, make a decision between such competing paths
                    Extension other = pivots.get(pivot);

                    if (other != null) {
                        // Compete with other extension
                        if (rookie.getGrade() <= other.getGrade()) {
                            ///logger.info("Weaker {} than {} at {}", rookie, other, pivot);
                            // Rookie is not better, so delete rookie
                            rookies.remove(rookie);
                        } else {
                            // Rookie is better, so delete other
                            ///logger.info("Better {} than {} at {}", rookie, other, pivot);
                            if (rookies.contains(other)) {
                                rookies.remove(other);
                            } else if (actives.contains(other)) {
                                actives.remove(other);
                            } else {
                                candidates.remove(other);
                            } //TODO: We could update the extensions of 'other', if any

                            pivots.put(pivot, rookie);
                        }
                    } else {
                        pivots.put(pivot, rookie);
                    }
                }
            }

            candidates.addAll(actives);
        } while (!rookies.isEmpty());

        for (Extension ext : candidates) {
            clump.add(ext.curve);
        }
    }

    //---------------------//
    // filterReachableArcs //
    //---------------------//
    /**
     * Check extensions to the arcs reachable from curve end and remove the ones that
     * would cross non-crossable items.
     *
     * @param curve         the curve being extended (on 'reverse' side)
     * @param reachableArcs (input/output) the set of arcs within reach
     */
    private void filterReachableArcs (Curve curve,
                                      Set<ArcView> reachableArcs)
    {
        final Point ce = curve.getEnd(reverse);
        final Rectangle extBox = getExtensionBox(curve, reachableArcs);
        final List<SystemInfo> systems = sheet.getSystemManager().getSystemsOf(ce);

        for (SystemInfo system : systems) {
            final Collection<Inter> ncs = erasedInters(system, extBox, false);

            if (ncs.isEmpty()) {
                continue;
            }

            for (Iterator<ArcView> it = reachableArcs.iterator(); it.hasNext();) {
                final ArcView arcView = it.next();
                final Point ae = arcView.getEnd(!reverse); // Arc end
                Area lineArea = null; // Lazily computed

                for (Inter nc : ncs) {
                    // Check extension line would cross item bounds
                    if (nc.getBounds().intersectsLine(ce.x, ce.y, ae.x, ae.y)) {
                        final boolean crossing;

                        // Have a closer look (for beams mainly)
                        Area ncArea = nc.getArea();

                        if (ncArea != null) {
                            if (lineArea == null) {
                                lineArea = getLineArea(ce, ae);
                            }

                            crossing = AreaUtil.intersection(lineArea, ncArea);
                        } else {
                            crossing = true;
                        }

                        if (crossing) {
                            logger.debug("Non-crossable {} for {} rev:{}", nc, curve, reverse);
                            it.remove();

                            break;
                        }
                    }
                }
            }
        }
    }

    //-------------------//
    // findReachableArcs //
    //-------------------//
    /**
     * Retrieve all arcs within reach from curve end (over some white gap)
     *
     * @param ext extension (on 'reverse' side)
     * @return the set of (new) reachable arcs
     */
    private Set<ArcView> findReachableArcs (Extension ext)
    {
        final Set<ArcView> reachableArcs = new LinkedHashSet<ArcView>();
        final Area area = defineExtArea(ext.curve);

        if (area != null) {
            // Check for reachable arcs in the extension area
            final Rectangle box = area.getBounds();
            final int xMax = (box.x + box.width) - 1;

            // Look for free-standing end points (with no junction point)
            for (Point end : skeleton.arcsEnds) {
                if (area.contains(end)) {
                    final Arc arc = skeleton.arcsMap.get(end);

                    if (!arc.isAssigned() && !ext.browsed.contains(arc)) {
                        // Check for lack of junction point
                        ArcView arcView = ext.curve.getArcView(arc, reverse);
                        Point pivot = arcView.getJunction(!reverse);

                        if (pivot == null) {
                            reachableArcs.add(arcView);
                            ext.browsed.add(arc);
                        }
                    }
                } else if (end.x > xMax) {
                    break; // Since list arcsEnds is sorted
                }
            }
        }

        return reachableArcs;
    }

    //-----------------//
    // getExtensionBox //
    //-----------------//
    /**
     * Compute the bounding box of all extension lines from curve end to reachable arcs.
     * (making sure that interior of rectangle is not empty)
     *
     * @param curve         the curve being extended (on 'reverse' side)
     * @param reachableArcs the arcs nearby
     * @return the global bounding box
     */
    private Rectangle getExtensionBox (Curve curve,
                                       Set<ArcView> reachableArcs)
    {
        Point ce = curve.getEnd(reverse);
        Rectangle extBox = new Rectangle(ce.x, ce.y, 1, 1);

        for (ArcView arcView : reachableArcs) {
            Point arcEnd = arcView.getEnd(!reverse);

            if (arcEnd != null) {
                extBox.add(arcEnd);
            } else {
                Point arcPt = arcView.getJunction(!reverse);

                if (arcPt != null) {
                    extBox.add(arcPt);
                }
            }
        }

        return extBox;
    }

    //-------------//
    // getLineArea //
    //-------------//
    /**
     * Define a line "area" from one point to the other with non-zero thickness
     *
     * @param p1 one point
     * @param p2 another point
     * @return a line area with a thickness of approximately 2 pixels
     */
    private Area getLineArea (Point p1,
                              Point p2)
    {
        // Determine if line is rather horizontal or vertical to infer margin values to apply
        final int dx = Math.abs(p2.x - p1.x);
        final int dy = Math.abs(p2.y - p1.y);

        final int mx = (dx >= dy) ? 0 : 1; // Margin on X
        final int my = (dx >= dy) ? 1 : 0; // Margin on Y

        GeoPath path = new GeoPath(new Line2D.Double(p1.x - mx, p1.y - my, p2.x - mx, p2.y - my));
        path.append(new Line2D.Double(p2.x + mx, p2.y + my, p1.x + mx, p1.y + my), true);
        path.closePath();

        return new Area(path);
    }

    //-----------------//
    // isGapAcceptable //
    //-----------------//
    /**
     * Check whether the gap between curve and candidate arc is acceptable, taking any
     * hidden inter into account.
     * To do so, we use a "line area" between curve and arc and fill it with hidden items.
     * We then check the widest true gap against maximum acceptable value.
     *
     * @param curve        curve to extend
     * @param arcView      candidate arc
     * @param hiddenInters relevant hidden inters
     * @return true for acceptable, false otherwise
     */
    private boolean isGapAcceptable (Curve curve,
                                     ArcView arcView)
    {
        Point ce = curve.getEnd(reverse);

        if (ce == null) {
            ce = curve.getJunction(reverse);
        }

        Point ae = arcView.getEnd(!reverse);

        if (ae == null) {
            ae = arcView.getJunction(!reverse);
        }

        // Check for adjacent points
        if ((Math.abs(ae.x - ce.x) <= 1) && (Math.abs(ae.y - ce.y) <= 1)) {
            logger.debug("Adjacent points {} {}", ce, ae);

            return true;
        }

        CurveGap gap = CurveGap.create(ce, ae);
        Area lArea = gap.getArea();
        curve.addAttachment("G", lArea);

        int[] vector = gap.computeVector(binaryBuf);
        int hole = gap.getLargestGap();

        logger.debug("{} {} rev:{} {} hole:{}", curve, arcView.getArc(), reverse, vector, hole);

        return hole <= params.gapMaxLength;
    }

    /**
     * Kept for debugging
     *
     * @param inters
     */
    private void register (Set<Inter> inters)
    {
        final InterIndex index = sheet.getInterIndex();

        for (Inter inter : inters) {
            index.register(inter);
        }
    }

    //---------//
    // scanGap //
    //---------//
    /**
     * Build all possible curve extensions, just one arc past the ending gap.
     * <p>
     * The gap may be due to actual lack of black pixels but also to hidden items, some of which
     * being crossable (like bar line or stem) and some not (like note or beam). So the improved
     * strategy for handling gaps is the following:<ol>
     * <li>Using large gap window, check whether there is at least one reachable arc. If not, scan
     * is stopped.</li>
     * <li>If line between curve end and reachable arc would hit a non-crossable item, this
     * extension is not allowed.</li>
     * <li>For allowed extensions, check the actual white gap, taking crossable items if any into
     * account.</li>
     * </ol>
     *
     * @param ext     the curve being extended
     * @param rookies (output) to be populated with new extensions found
     */
    private void scanGap (Extension ext,
                          List<Extension> rookies)
    {
        // Find arcs within reach
        final Set<ArcView> reachables = findReachableArcs(ext);

        if (reachables.isEmpty()) {
            return;
        }

        // Remove extensions that hit non-crossable inters
        filterReachableArcs(ext.curve, reachables);

        // Closer look at actual white gap for each allowed extension
        for (ArcView arcView : reachables) {
            curves.checkBreak(arcView.getArc());

            // Check true gaps in the extension
            if (!isGapAcceptable(ext.curve, arcView)) {
                continue;
            }

            // OK, let's try to append arc to curve and check resulting model
            Curve sl = addArc(arcView, ext.curve);

            if (sl != null) {
                rookies.add(new Extension(sl, ext.browsed));
            }
        }
    }

    //-----------//
    // scanPivot //
    //-----------//
    /**
     * Build all possible curve extensions, just one arc past the ending junction
     *
     * @param ext     the curve being extended
     * @param pivot   the ending junction
     * @param rookies (output) to be populated with new extensions
     */
    private void scanPivot (Extension ext,
                            Point pivot,
                            List<Extension> rookies)
    {
        // What was the last direction?
        final Point prevPoint = ext.curve.getEnd(reverse);
        final int lastDir = getDir(prevPoint, pivot);

        // Try to go past this pivot, keeping only the acceptable possibilities
        List<Curve> newCurves = new ArrayList<Curve>();
        final Point np = new Point();
        boolean sideJunctionMet = false;
        List<Arc> arcs = new ArrayList<Arc>();

        for (int dir : scans[lastDir]) {
            // If junction has already been met on side dir, stop here
            if (!isSide(dir) && sideJunctionMet) {
                break;
            }

            np.move(pivot.x + dxs[dir], pivot.y + dys[dir]);

            int pix = skeleton.buf.get(np.x, np.y);

            if (pix == BACKGROUND) {
                continue;
            }

            arcs.clear();

            if (isProcessed(pix)) {
                // Check arc shape
                ArcShape shape = ArcShape.values()[pix - PROCESSED];

                if (shape.isSlurRelevant()) {
                    Arc arc = skeleton.arcsMap.get(np); // Retrieve arc data
                    curves.checkBreak(arc);
                    arcs.add(arc);
                }
            } else if (isJunction(pix)) {
                if (!np.equals(pivot)) {
                    List<Arc> pivotArcs = skeleton.voidArcsMap.get(pivot);

                    if (pivotArcs != null) {
                        arcs.addAll(pivotArcs);
                    }
                }
            }

            for (Arc arc : arcs) {
                if (!arc.isAssigned() && !ext.browsed.contains(arc)) {
                    ext.browsed.add(arc);

                    Curve sl = addArc(ext.curve.getArcView(arc, reverse), ext.curve);

                    if (sl != null) {
                        newCurves.add(sl);

                        if (isSide(dir) && isJunction(pix)) {
                            sideJunctionMet = true;
                        }
                    }
                }
            }
        }

        for (Curve sl : newCurves) {
            rookies.add(new Extension(sl, ext.browsed));
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

        private final Scale.Fraction gapMaxLength = new Scale.Fraction(
                0.25,
                "Maximum acceptable length for true gap");

        private final Scale.Fraction gapBoxLength = new Scale.Fraction(
                1.0,
                "Length used for gap lookup box");

        private final Scale.Fraction gapBoxDeltaIn = new Scale.Fraction(
                0.2,
                "Delta for gap box on slur side");

        private final Scale.Fraction gapBoxDeltaOut = new Scale.Fraction(
                0.3,
                "Delta for gap box on extension side");

        private final Scale.Fraction lineBoxLength = new Scale.Fraction(
                1.8,
                "Length for box across staff line");

        private final Scale.Fraction lineBoxIn = new Scale.Fraction(
                0.2,
                "Overlap for line box on slur side");

        private final Scale.Fraction lineBoxDeltaIn = new Scale.Fraction(
                0.2,
                "Delta for line box on slur side");

        private final Scale.Fraction lineBoxDeltaOut = new Scale.Fraction(
                0.3,
                "Delta for line box on extension side");

        private final Constant.Integer maxExtensionRookies = new Constant.Integer(
                "extensions",
                50,
                "Maximum rookies when extending a curve");
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

        final double gapMaxLength;

        final double gapBoxLength;

        final double gapBoxDeltaIn;

        final double gapBoxDeltaOut;

        final double lineBoxLength;

        final double lineBoxIn;

        final double lineBoxDeltaIn;

        final double lineBoxDeltaOut;

        final int maxExtensionRookies;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            gapMaxLength = scale.toPixels(constants.gapMaxLength);
            gapBoxLength = scale.toPixels(constants.gapBoxLength);
            gapBoxDeltaIn = scale.toPixels(constants.gapBoxDeltaIn);
            gapBoxDeltaOut = scale.toPixels(constants.gapBoxDeltaOut);
            lineBoxLength = scale.toPixels(constants.lineBoxLength);
            lineBoxIn = scale.toPixels(constants.lineBoxIn);
            lineBoxDeltaIn = scale.toPixels(constants.lineBoxDeltaIn);
            lineBoxDeltaOut = scale.toPixels(constants.lineBoxDeltaOut);
            maxExtensionRookies = constants.maxExtensionRookies.getValue();

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }

    //-----------//
    // Extension //
    //-----------//
    /**
     * Meant to handle the process of extending a curve, by keeping track of arcs
     * already browsed (regardless whether these arcs were actually kept or not).
     */
    private class Extension
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Curve as defined so far. */
        Curve curve;

        /** Arcs considered for curve immediate extension. */
        Set<Arc> browsed;

        /** Curve quality. */
        private Double grade;

        //~ Constructors ---------------------------------------------------------------------------
        public Extension (Curve curve,
                          Set<Arc> browsed)
        {
            this.curve = curve;
            this.browsed = new LinkedHashSet<Arc>(browsed); // Copy is needed
        }

        //~ Methods --------------------------------------------------------------------------------
        public double getGrade ()
        {
            if (grade == null) {
                GradeImpacts impacts = computeImpacts(curve, false);

                if (impacts != null) {
                    grade = impacts.getGrade();
                } else {
                    grade = 0.0;
                }
            }

            return grade;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("{Ext ");
            sb.append(String.format("%.3f", getGrade()));
            sb.append(curve);
            ///sb.append(" browsed:").append(browsed);
            sb.append('}');

            return sb.toString();
        }
    }
}
