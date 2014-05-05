//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    C u r v e s B u i l d e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.Main;

import omr.constant.ConstantSet;

import omr.grid.FilamentLine;
import static omr.image.PixelSource.BACKGROUND;

import omr.math.AreaUtil;
import omr.math.GeoPath;
import omr.math.PointUtil;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.SystemInfo;
import static omr.sheet.curve.Skeleton.*;

import omr.sig.Inter;

import omr.ui.util.ItemRenderer;

import omr.util.Navigable;

import ij.process.ByteProcessor;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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

    /** (Current) clump on left side of aggregated candidates. */
    protected final Set<Curve> leftClump = new LinkedHashSet<Curve>();

    /** (Current) clump on right side of aggregated lines candidates. */
    protected final Set<Curve> rightClump = new LinkedHashSet<Curve>();

    /** (Current) orientation for walking along a curve. */
    protected boolean reverse;

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
     * Check whether the provided points can represent a curve.
     *
     * @param points the provided points
     * @return the model if OK, null if not
     */
    protected abstract Model computeModel (List<Point> points);

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
        Point firstJunction;
        Point lastJunction;

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

        Set<Arc> parts = new HashSet<Arc>(curve.getParts());
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
     * Check whether the curve end is getting tangent to staff line.
     *
     * @param curve the curve to check (on current side)
     * @return the tangent staff line or null
     */
    protected abstract FilamentLine getTangentLine (Curve curve);

    /**
     * Among the clump of curves built from a common trunk, weed out some of them.
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
        curves.checkBreak(arc); // Debug

        Curve trunk = createCurve(arc, arc.getModel());

        if (arc.getModel() != null) {
            trunk.setModel(arc.getModel());
        } else {
            trunk.setModel(computeModel(arc.getPoints()));
        }

        logger.debug("Trunk: {}", trunk);
        leftClump.clear();
        rightClump.clear();

        for (boolean rev : new boolean[]{false, true}) {
            Set<Curve> clump = rev ? leftClump : rightClump;
            reverse = rev;
            trunk.setCrossedLine(null);
            extend(trunk, trunk.getParts(), clump);

            if (clump.size() > 1) {
                weed(clump); // Filter out the least interesting candidates
            }
        }

        // Combine candidates from both sides
        Set<Inter> inters = new HashSet<Inter>();

        if (leftClump.isEmpty()) {
            // Use rights
            for (Curve curve : rightClump) {
                createInter(curve, inters);
            }
        } else if (rightClump.isEmpty()) {
            // Use lefts
            for (Curve curve : leftClump) {
                createInter(curve, inters);
            }
        } else {
            // Connect lefts & rights
            for (Curve sl : leftClump) {
                for (Curve sr : rightClump) {
                    Curve curve = (sl == sr) ? sl : createCurve(sl, sr);
                    createInter(curve, inters);
                }
            }
        }

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
        Set<Arc> parts = new HashSet<Arc>(left.getParts());
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
     * <p>
     * Shape and size of lookup area depend highly on the potential crossing of a staff line.
     *
     * @param curve  the curve to extend
     * @param tgLine slur tangent staff line, if any
     * @return the extension lookup area
     */
    protected Area defineExtArea (Curve curve,
                                  FilamentLine tgLine)
    {
        Point ce = curve.getEnd(reverse); // Curve End
        Point2D uv = getEndVector(curve); // Unit Vector
        GeoPath path;

        if (tgLine != null) {
            int xDir = (uv.getX() > 0) ? 1 : (-1);
            double lg = xDir * params.lineBoxLength;
            double dx = xDir * params.lineBoxIn;
            int yDir = (uv.getY() > 0) ? 1 : (-1);
            double dl1 = yDir * params.lineBoxDeltaIn;
            double dl2 = yDir * params.lineBoxDeltaOut;
            double yLine = tgLine.yAt(ce.x);
            path = new GeoPath(
                    new Line2D.Double(
                            new Point2D.Double(ce.x - dx, yLine),
                            new Point2D.Double(ce.x - dx, yLine + dl1)));
            yLine = tgLine.yAt(ce.x + lg);
            path.append(
                    new Line2D.Double(
                            new Point2D.Double(ce.x + lg, yLine + dl2),
                            new Point2D.Double(ce.x + lg, yLine)),
                    true);
        } else {
            if (uv == null) {
                return null;
            }

            double lg = params.gapBoxLength;
            Point2D lgVect = new Point2D.Double(lg * uv.getX(), lg * uv.getY());
            Point2D ce2 = PointUtil.addition(ce, lgVect);

            double dl1 = params.gapBoxDeltaIn;
            Point2D dlVect = new Point2D.Double(-dl1 * uv.getY(), dl1 * uv.getX());
            path = new GeoPath(
                    new Line2D.Double(
                            PointUtil.addition(ce, dlVect),
                            PointUtil.subtraction(ce, dlVect)));

            double dl2 = params.gapBoxDeltaOut;
            dlVect = new Point2D.Double(-dl2 * uv.getY(), dl2 * uv.getX());
            path.append(
                    new Line2D.Double(
                            PointUtil.subtraction(ce2, dlVect),
                            PointUtil.addition(ce2, dlVect)),
                    true);
        }

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
            model = computeModel(curve.getPoints());
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
        Set<Inter> found = new HashSet<Inter>();
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
     * Try to recursively extend a curve in the current orientation.
     *
     * @param curve    the curve to extend
     * @param pastArcs collection of arcs already browsed
     */
    private void extend (Curve curve,
                         Collection<Arc> pastArcs,
                         Set<Curve> clump)
    {
        Set<Arc> browsed = new LinkedHashSet<Arc>(pastArcs);
        List<Curve> newCurves = new ArrayList<Curve>();
        clump.add(curve);

        // Check whether this curve end is getting tangent to a staff line
        FilamentLine tgLine = getTangentLine(curve);

        if (tgLine != null) {
            // Check beyond staff line: scan arcs ending in extension window
            scanGap(curve, browsed, newCurves, tgLine);
        } else {
            Point pivot = curve.getJunction(reverse);

            if (pivot != null) {
                // Check beyond pivot: scan arcs ending at pivot
                scanPivot(curve, pivot, browsed, newCurves);
            } else {
                // Check beyond gap: scan arcs ending in extension window
                scanGap(curve, browsed, newCurves, null);
            }
        }

        if (!newCurves.isEmpty()) {
            for (Curve s : newCurves) {
                ////browsed.addAll(s.getParts());
                if ((s.getCrossedLine() == null) && (curve.getCrossedLine() != null)) {
                    s.setCrossedLine(curve.getCrossedLine());
                }
            }

            for (Curve s : newCurves) {
                extend(s, browsed, clump); // Increment further
            }
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

    //
    //    //------------------//
    //    // findHiddenInters //
    //    //------------------//
    //    /**
    //     * Retrieve all the hidden inters that are relevant for the considered extensions.
    //     *
    //     * @param curve         curve being extended (on 'reverse' side)
    //     * @param reachableArcs the arcs nearby
    //     * @return the collection of relevant erased inters
    //     */
    //    private Set<Inter> findHiddenInters (Curve curve,
    //                                         Set<ArcView> reachableArcs)
    //    {
    //        final Set<Inter> hiddens = new HashSet<Inter>();
    //        final Point ce = curve.getEnd(reverse);
    //        final Rectangle extBox = getExtensionBox(curve, reachableArcs);
    //        final List<SystemInfo> systems = sheet.getSystemManager().getSystemsOf(ce);
    //
    //        for (SystemInfo system : systems) {
    //            hiddens.addAll(erasedInters(system, extBox, true));
    //        }
    //
    //        return hiddens;
    //    }
    //
    //-------------------//
    // findReachableArcs //
    //-------------------//
    /**
     * Retrieve all arcs within reach from curve end (over some white gap)
     *
     * @param curve        current curve (on 'reverse' side)
     * @param browsed arcs already considered
     * @param tgLine       tangent line nearby if any
     * @return the set of (new) reachable arcs
     */
    private Set<ArcView> findReachableArcs (Curve curve,
                                            Set<Arc> browsed,
                                            FilamentLine tgLine)
    {
        final Set<ArcView> reachableArcs = new HashSet<ArcView>();
        final Area area = defineExtArea(curve, tgLine);

        if (area != null) {
            // Check for reachable arcs in the extension area
            final Rectangle box = area.getBounds();
            final int xMax = (box.x + box.width) - 1;

            // Look for end points
            for (Point end : skeleton.arcsEnds) {
                if (area.contains(end)) {
                    final Arc arc = skeleton.arcsMap.get(end);

                    if (!arc.isAssigned() && !browsed.contains(arc)) {
                        reachableArcs.add(curve.getArcView(arc, reverse));
                        browsed.add(arc);
                    }
                } else if (end.x > xMax) {
                    break; // Since list arcsEnds is sorted
                }
            }

//            // Look for pivots of void arcs
//            for (Point pt : skeleton.arcsPivots) {
//                if (area.contains(pt)) {
//                    List<Arc> arcs = skeleton.voidArcsMap.get(pt);
//
//                    if (arcs != null) {
//                        for (Arc arc : arcs) {
//                            if (!arc.isAssigned() && !browsed.contains(arc)) {
//                                reachableArcs.add(curve.getArcView(arc, reverse));
//                                browsed.add(arc);
//                            }
//                        }
//                    }
//                } else if (pt.x > xMax) {
//                    break; // Since list arcsPivots is sorted
//                }
//            }
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
     * @param curve     the curve to extend
     * @param browsed   arcs already browsed (kept or not)
     * @param newCurves (output) to be populated with new curves found
     * @param tgLine    tangent staff line if any
     */
    private void scanGap (Curve curve,
                          Set<Arc> browsed,
                          List<Curve> newCurves,
                          FilamentLine tgLine)
    {
        // Find arcs within reach
        final Set<ArcView> reachables = findReachableArcs(curve, browsed, tgLine);

        if (reachables.isEmpty()) {
            return;
        }

        // Remove extensions that hit non-crossable inters
        filterReachableArcs(curve, reachables);

        // Closer look at actual white gap for each allowed extension
        for (ArcView arcView : reachables) {
            curves.checkBreak(arcView.getArc());

            // Check true gaps in the extension
            if (!isGapAcceptable(curve, arcView)) {
                continue;
            }

            // OK, let's try to append arc to curve and check resulting model
            Curve sl = addArc(arcView, curve);

            if (sl != null) {
                newCurves.add(sl);

                if (tgLine != null) {
                    sl.setCrossedLine(tgLine);
                }
            }
        }
    }

    //-----------//
    // scanPivot //
    //-----------//
    /**
     * Build all possible curve extensions, just one arc past the ending junction
     *
     * @param curve     the curve to extend
     * @param pivot     the ending junction
     * @param browsed   arcs already browsed (kept or not)
     * @param newCurves (output) to be populated with new curves found
     */
    private void scanPivot (Curve curve,
                            Point pivot,
                            Set<Arc> browsed,
                            List<Curve> newCurves)
    {
        // What was the last direction?
        final Point prevPoint = curve.getEnd(reverse);
        final int lastDir = getDir(prevPoint, pivot);

        // Try to go past this pivot, keeping only the acceptable possibilities
        final Point np = new Point();
        boolean sideJunctionMet = false;
        List<Arc> arcs = new ArrayList<Arc>();

        for (int dir : scans[lastDir]) {
            // If junction has already been met on side dir, stop here
            if (!isSide(dir) && sideJunctionMet) {
                return;
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
                if (!arc.isAssigned() && !browsed.contains(arc)) {
                    browsed.add(arc);

                    Curve sl = addArc(curve.getArcView(arc, reverse), curve);

                    if (sl != null) {
                        newCurves.add(sl);

                        if (isSide(dir) && isJunction(pix)) {
                            sideJunctionMet = true;
                        }
                    }
                }
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

        final Scale.Fraction gapMaxLength = new Scale.Fraction(
                0.25,
                "Maximum acceptable length for true gap");

        final Scale.Fraction gapBoxLength = new Scale.Fraction(
                1.0,
                "Length used for gap lookup box");

        final Scale.Fraction gapBoxDeltaIn = new Scale.Fraction(
                0.2,
                "Delta for gap box on slur side");

        final Scale.Fraction gapBoxDeltaOut = new Scale.Fraction(
                0.3,
                "Delta for gap box on extension side");

        final Scale.Fraction lineBoxLength = new Scale.Fraction(
                1.8,
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

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
