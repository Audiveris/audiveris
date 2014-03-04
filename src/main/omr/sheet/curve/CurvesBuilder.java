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

import omr.math.GeoPath;
import omr.math.PointUtil;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import static omr.sheet.curve.Skeleton.*;

import omr.sig.Inter;

import omr.ui.util.ItemRenderer;

import omr.util.Navigable;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code CurvesBuilder} is an abstract basis to build curves for slurs, wedges
 * or endings on top of the underlying skeleton.
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

    /** image skeleton. */
    protected final Skeleton skeleton;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** For unique line IDs. (page wise) */
    protected int globalId = 0;

    /** Clump on left side of aggregated candidates. */
    protected final Set<Curve> leftClump = new LinkedHashSet<Curve>();

    /** Clump on right side of aggregated lines candidates. */
    protected final Set<Curve> rightClump = new LinkedHashSet<Curve>();

    /** Current orientation for walking along a curve. */
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

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Try to append one arc to an existing curve and thus create a new curve.
     *
     * @param arc     the candidate for extension
     * @param curve   the curve to be extended by arc
     * @param browsed collections of arcs already visited
     * @return the newly created curve, if successful
     */
    protected abstract Curve addArc (Arc arc,
                                     Curve curve,
                                     Set<Arc> browsed);

    /**
     * Measure the mean distance from additional arc to provided (side) model .
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

    /**
     * Check whether the provided points can represent a curve.
     *
     * @param points the provided points
     * @return the model if OK, null if not
     */
    protected abstract Model computeModel (List<Point> points);

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
        // Build the list of points with no duplicates
        List<Point> points = new ArrayList<Point>(left.getPoints());
        Point leftJunction = left.getJunction(false);

        if ((leftJunction != null) && !points.contains(leftJunction)) {
            points.add(leftJunction);
        }

        Point rightJunction = right.getJunction(true);

        if ((rightJunction != null) && !points.contains(rightJunction)) {
            points.add(rightJunction);
        }

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
        Point se = curve.getEnd(reverse);
        Point2D uv = getEndVector(curve);
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
        curve.setExtArea(area, reverse);
        curve.addAttachment(reverse ? "t" : "f", area);

        return area;
    }

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

    protected abstract void weed (Set<Curve> clump);

    /**
     * Create a curve instance from a seed arc
     *
     * @param arc   the seed arc
     * @param model a model if any
     * @return the created curve
     */
    protected Curve createCurve (Arc arc,
                                 Model model)
    {
        return createInstance(
                arc.getJunction(true),
                arc.getJunction(false),
                arc.getPoints(),
                model,
                Arrays.asList(arc));
    }

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

    /**
     * Build all possible curve extensions, just one arc past the ending gap.
     *
     * @param curve   the curve to extend
     * @param browsed arcs already browsed (kept or not)
     * @param newSeqs (output) to be populated with new sequences found
     * @param tgLine  tangent staff line if any
     */
    private void scanGap (Curve curve,
                          Set<Arc> browsed,
                          List<Curve> newSeqs,
                          FilamentLine tgLine)
    {
        // Look for arcs ends in extension window
        Area area = defineExtArea(curve, tgLine);

        if (area != null) {
            Rectangle box = area.getBounds();
            int xMax = (box.x + box.width) - 1;

            for (Point end : skeleton.arcsEnds) {
                if (area.contains(end)) {
                    Arc arc = skeleton.arcsMap.get(end);

                    if (!arc.isAssigned() && !browsed.contains(arc)) {
                        curves.checkBreak(arc);
                        browsed.add(arc);

                        Curve sl = addArc(arc, curve, browsed);

                        if (sl != null) {
                            newSeqs.add(sl);

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

            Arc arc = null;

            if (isProcessed(pix)) {
                // Check arc shape
                ArcShape shape = ArcShape.values()[pix - PROCESSED];

                if (shape.isSlurRelevant()) {
                    arc = skeleton.arcsMap.get(np); // Retrieve arc data
                    curves.checkBreak(arc);
                }
            } else if (isJunction(pix)) {
                if (!np.equals(pivot)) {
                    arc = skeleton.arcsMap.get(pivot);
                }
            }

            if ((arc != null) && !browsed.contains(arc)) {
                browsed.add(arc);
                Curve sl = addArc(arc, curve, browsed);

                if (sl != null) {
                    newCurves.add(sl);

                    if (isSide(dir) && isJunction(pix)) {
                        sideJunctionMet = true;
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

        final Scale.Fraction gapBoxLength = new Scale.Fraction(1.0, "Length for gap box");

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
