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
package omr.sheet.curve;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.math.Circle;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;
import omr.sheet.grid.FilamentLine;

import omr.sig.GradeImpacts;
import omr.sig.SIGraph;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.Inter;
import omr.sig.inter.SlurInter;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;

import omr.ui.util.UIUtil;

import omr.util.Dumping;
import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.toRadians;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code SlursBuilder} builds all slur curves from a sheet skeleton.
 *
 * @author Hervé Bitteur
 */
public class SlursBuilder
        extends CurvesBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlursBuilder.class);

    private static final Color SLUR_POINTS = new Color(255, 0, 0, 50);

    private static final Color SLUR_CURVES = new Color(0, 255, 0, 100);

    private static final Color SLUR_MODELS = new Color(255, 255, 0, 100);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Companion for slur-notes connections. */
    private final SlursLinker slursLinker;

    /** All slur infos created. */
    private final List<SlurInfo> pageInfos = new ArrayList<SlurInfo>();

    /** All slur inters retrieved. */
    private final List<SlurInter> pageSlurs = new ArrayList<SlurInter>();

    /** Current maximum length for arcs to be tried. */
    private Integer maxLength = null;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SlursBuilder object.
     *
     * @param curves curves environment
     */
    public SlursBuilder (Curves curves)
    {
        super(curves);
        slursLinker = new SlursLinker(sheet);

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildSlurs //
    //------------//
    public void buildSlurs ()
    {
        try {
            final List<Arc> relevants = getSeedArcs();
            maxLength = null;

            // Build slurs from initial SLUR seed
            // Extend slur seeds as much as possible through junction points & small gaps
            for (Arc arc : relevants) {
                if (!arc.isAssigned() && (arc.getShape() == ArcShape.SLUR)) {
                    buildCurve(arc);
                }
            }

            // Build slurs from NO initial SLUR seed
            // Since arcs are sorted by decreasing length, extension should
            // never try to include an arc longer than the initial one.
            for (Arc arc : relevants) {
                if (!arc.isAssigned() && (arc.getShape() != ArcShape.SLUR)) {
                    maxLength = arc.getLength();
                    buildCurve(arc);
                }
            }

            // Handle slurs collision on same head (TODO: not yet fully implemented!!!!!!!!!!!!!!!)
            handleCollisions();

            logger.info("{}Slurs: {}", sheet.getLogPrefix(), pageSlurs.size());
            logger.debug("Slur maxClumpSize: {}", maxClumpSize);
        } catch (Throwable ex) {
            logger.warn("Error in SlursBuilder: " + ex, ex);
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    public void renderItems (Graphics2D g)
    {
        final Rectangle clip = g.getClipBounds();

        // Render info attachments
        Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

        for (SlurInfo info : pageInfos) {
            info.renderAttachments(g);
        }

        g.setStroke(oldStroke);

        // Render slurs points
        g.setColor(SLUR_POINTS);

        for (SlurInter slur : pageSlurs) {
            for (Point p : slur.getInfo().getPoints()) {
                g.fillRect(p.x, p.y, 1, 1);
            }
        }

        // Render slurs curves
        g.setColor(SLUR_CURVES);

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
            if (info.getSideModel(true) != info.getSideModel(false)) {
                Color oldColor = g.getColor();
                g.setColor(SLUR_MODELS);

                for (boolean rev : new boolean[]{true, false}) {
                    Model sideModel = info.getSideModel(rev);

                    if (sideModel != null) {
                        g.draw(sideModel.getCurve());
                    }
                }

                g.setColor(oldColor);
            }
        }
    }

    //--------//
    // addArc //
    //--------//
    @Override
    protected Curve addArc (ArcView arcView,
                            Curve curve)
    {
        final Arc arc = arcView.getArc();

        if ((maxLength != null) && (arc.getLength() > maxLength)) {
            return null;
        }

        SlurInfo slur = (SlurInfo) curve;

        // Check extension is compatible with slur (side) circle
        // Use slur side circle to check position of arc WRT circle
        // If OK, allocate a new slur
        Model sideModel = slur.getSideModel(reverse);

        if (sideModel == null) {
            return null;
        }

        // Check arc roughly goes in curve end direction
        double projection = projection(arcView, sideModel);

        if (projection < params.minProjection) {
            logger.debug("{} not extended by {} projection:{}", slur, arc, projection);

            return null;
        }

        double dist = arcDistance(sideModel, arcView);

        if (dist <= params.maxExtDistance) {
            // Check new side model (computed on slur side + arc)
            Model newSideModel = null;
            List<Point> pts = curve.getAllPoints(arcView, reverse);

            if (pts.size() >= params.sideLength) {
                // Side CCW cannot change with respect to slur CCW
                newSideModel = slur.computeSideModel(pts, reverse);

                if (newSideModel == null) {
                    return null;
                }

                if (slur.getModel() != null) {
                    if ((newSideModel.ccw() * slur.getModel().ccw()) < 0) {
                        return null;
                    }
                }
            }

            SlurInfo s = (SlurInfo) createCurve(slur, arcView, pts, newSideModel);
            logger.debug("Slur#{} extended as {} dist:{}", slur.getId(), s, dist);

            if (newSideModel != null) {
                s.setSideModel(newSideModel, reverse);
            }

            if (slur.hasSideModel(!reverse)) {
                s.setSideModel(slur.getSideModel(!reverse), !reverse);
            }

            Model sModel = computeModel(pts);
            s.setModel(sModel);

            return s;
        } else {
            logger.debug("Slur#{} could not add {} dist:{}", slur.getId(), arc, dist);

            return null;
        }
    }

    /**
     * Compute impacts for slur candidate.
     *
     * @param slur      slur information
     * @param bothSides true for both sides, false for current side
     * @return grade impacts
     */
    @Override
    protected SlurInter.Impacts computeImpacts (Curve curve,
                                                boolean bothSides)
    {
        SlurInfo slur = (SlurInfo) curve;
        Model global = needGlobalModel(slur);

        if (!(global instanceof CircleModel)) {
            return null;
        }

        // Distance to model (both side models or just a single side model)
        double dist;

        if (bothSides) {
            double sum = 0;

            for (boolean bool : new boolean[]{true, false}) {
                Model sideModel = slur.getSideModel(bool);

                if (sideModel == null) {
                    return null;
                }

                double d = sideModel.computeDistance(slur.getSidePoints(bool));
                sideModel.setDistance(d);
                sum += d;
            }

            dist = sum / 2;
        } else {
            Model sideModel = slur.getSideModel(reverse);

            if (sideModel == null) {
                return null;
            }

            dist = sideModel.computeDistance(slur.getSidePoints(reverse));
        }

        // Distance to model
        if (dist > params.maxSlurDistance) {
            return null;
        }

        double distImpact = 1 - (dist / params.maxSlurDistance);

        // Max arc angle value
        Circle circle = ((CircleModel) global).getCircle();

        double arcAngle = circle.getArcAngle();

        if (arcAngle > params.maxArcAngleHigh) {
            logger.debug("Slur too curved {} {}", arcAngle, this);

            return null;
        }

        double angleImpact = (params.maxArcAngleHigh - arcAngle) / (params.maxArcAngleHigh
                                                                    - params.maxArcAngleLow);

        // Features below are relevant only for full slur evaluations
        if (!bothSides) {
            return new SlurInter.Impacts(distImpact, angleImpact, 1, 1, 1);
        }

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

        List<Point> points = slur.getPoints();
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

        return new SlurInter.Impacts(
                distImpact,
                angleImpact,
                widthImpact,
                heightImpact,
                vertImpact);
    }

    //--------------//
    // computeModel //
    //--------------//
    @Override
    protected Model computeModel (List<Point> points)
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
        double arcAngle = rough.getArcAngle();

        if (arcAngle > params.maxArcAngleHigh) {
            logger.debug("Arc angle too large {} at {}", arcAngle, p0);

            return null;
        }

        // Now compute "precise" circle
        try {
            Circle fitted = new Circle(points);

            // Check circle radius is rather similar to rough radius. If not, keep the rough one.
            // (because algebraic circle are sometimes fancy on short slurs).
            final Circle circle;
            final double dist;
            final double r1 = rough.getRadius();
            final double r2 = fitted.getRadius();

            if ((abs(r1 - r2) / (max(r1, r2))) <= params.similarRadiusRatio) {
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

                return new CircleModel(circle);
            }
        } catch (Exception ex) {
            logger.debug("Could not compute circle {} at {}", p0);

            return null;
        }
    }

    @Override
    protected Curve createInstance (Point firstJunction,
                                    Point lastJunction,
                                    List<Point> points,
                                    Model model,
                                    Collection<Arc> parts)
    {
        SlurInfo slur = new SlurInfo(
                ++globalId,
                firstJunction,
                lastJunction,
                points,
                model,
                parts,
                params.sideLength);
        pageInfos.add(slur);

        return slur;
    }

    @Override
    protected void createInter (Curve seq,
                                Set<Inter> inters)
    {
        SlurInfo slur = (SlurInfo) seq;
        GradeImpacts impacts = computeImpacts(slur, true);

        if ((impacts != null)
            && (impacts.getGrade() >= SlurInter.getMinGrade())
            && (slur.getCurve() != null)) {
            slur.retrieveGlyph(sheet, params.maxRunDistance);

            if (slur.getGlyph() != null) {
                slur.getGlyph().setShape(Shape.SLUR);
                inters.add(new SlurInter(slur, impacts));
            }
        }
    }

    @Override
    protected void filterInters (Set<Inter> inters)
    {
        // Delegate final selection to SlursLinker
        SlurInter selected = slursLinker.prune(inters);

        if (selected != null) {
            selected.getInfo().assign(); // Assign arcs
            pageSlurs.add(selected);
        }
    }

    @Override
    protected Integer getArcCheckLength ()
    {
        return params.arcCheckLength;
    }

    @Override
    protected Point2D getEndVector (Curve seq)
    {
        SlurInfo info = (SlurInfo) seq;
        Model model = needGlobalModel(info);

        if (model == null) {
            return null;
        } else {
            return model.getEndVector(reverse);
        }
    }

    //------//
    // weed //
    //------//
    @Override
    protected void weed (Set<Curve> clump)
    {
        // Compute grades
        List<SlurInter> inters = new ArrayList<SlurInter>();

        for (Curve seq : clump) {
            SlurInfo slur = (SlurInfo) seq;
            GradeImpacts impacts = computeImpacts(slur, false);

            if (impacts != null) {
                SlurInter inter = new SlurInter(slur, impacts);
                inters.add(inter);
            }
        }

        clump.clear();

        // In the provided clump, all candidates start from the same point.
        // If several candidates stop at the same point, keep the one with best grade.
        // TODO: can this still occur???
        purgeIdenticalEndings(inters);

        // Discard candidates that end as a portion of staff line
        purgeStaffLines(inters);

        // Discard the ones with too short distance from one end to the other
        SlurInter longest = purgeShortests(inters);

        if (longest == null) {
            return;
        }

        // Discard those with grade lower than grade of longest candidate
        double longestGrade = longest.getGrade();

        for (Iterator<SlurInter> it = inters.iterator(); it.hasNext();) {
            if (it.next().getGrade() < longestGrade) {
                it.remove();
            }
        }

        for (SlurInter slur : inters) {
            clump.add(slur.getInfo());
        }
    }

    /**
     * Report the bounding box of a collection of slurs
     *
     * @param slurs the collection
     * @return the bounding box
     */
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
                set.add(arc);
            }
        }

        List<Arc> list = new ArrayList<Arc>(set);
        Collections.sort(list, Arc.byReverseLength);

        return list;
    }

    //------------------//
    // handleCollisions //
    //------------------//
    /**
     * In crowded areas, a head may got linked to more than one slur on the same side.
     * Note however that having both a tie and a slur on same head side is legal.
     */
    private void handleCollisions ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();
            List<Inter> slurs = sig.inters(SlurInter.class);

            for (Inter inter : slurs) {
                SlurInter slur = (SlurInter) inter;

                // Check on both sides of this slur
                for (HorizontalSide side : HorizontalSide.values()) {
                    AbstractHeadInter head = slur.getHead(side);

                    if (head != null) {
                        // Check this head for colliding slur links
                        Set<Relation> rels = sig.getRelations(head, SlurHeadRelation.class);

                        for (Relation rel : rels) {
                            SlurHeadRelation shRel = (SlurHeadRelation) rel;
                            HorizontalSide relSide = shRel.getSide();

                            if (relSide == side) {
                                SlurInter s = (SlurInter) sig.getOppositeInter(head, rel);

                                if ((slur != s) && (slur.isTie() == s.isTie())) {
                                    logger.info("{} collision {} & {} @ {}", side, slur, s, head);

                                    // TODO: handle collision
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    //-----------------------//
    // purgeIdenticalEndings //
    //-----------------------//
    /**
     * Purge the inters with identical ending point (keeping the best grade)
     *
     * @param inters the collection to purge
     */
    private void purgeIdenticalEndings (List<SlurInter> inters)
    {
        Collections.sort(inters, Inter.byReverseGrade);

        for (int i = 0; i < inters.size(); i++) {
            SlurInter slur = inters.get(i);
            Point end = slur.getInfo().getEnd(reverse);

            List<SlurInter> toDelete = new ArrayList<SlurInter>();

            for (SlurInter otherSlur : inters.subList(i + 1, inters.size())) {
                Point otherEnd = otherSlur.getInfo().getEnd(reverse);

                if (end.equals(otherEnd)) {
                    toDelete.add(otherSlur);
                }
            }

            if (!toDelete.isEmpty()) {
                inters.removeAll(toDelete);
            }
        }
    }

    /**
     * Discard the inters with shortest extension.
     *
     * @param inters the collection to purge
     * @return the longest inter
     */
    private SlurInter purgeShortests (List<SlurInter> inters)
    {
        int maxExt2 = 0;
        SlurInter longest = null;

        for (SlurInter slur : inters) {
            int ext2 = slur.getInfo().getSegmentSq();

            if (maxExt2 < ext2) {
                maxExt2 = ext2;
                longest = slur;
            }
        }

        if (longest == null) {
            return null;
        }

        int quorum = (int) Math.ceil(params.quorumRatio * params.quorumRatio * maxExt2);

        for (Iterator<SlurInter> it = inters.iterator(); it.hasNext();) {
            SlurInfo slur = it.next().getInfo();

            if (slur.getSegmentSq() < quorum) {
                it.remove();
            }
        }

        return longest;
    }

    //
    //    //----------------//
    //    // getTangentLine //
    //    //----------------//
    //    private FilamentLine getTangentLine (Curve curve)
    //    {
    //        SlurInfo slur = (SlurInfo) curve;
    //
    //        // Distance from slur end to closest staff line
    //        Point se = slur.getEnd(reverse);
    //        Staff staff = sheet.getStaffManager().getClosestStaff(se);
    //        FilamentLine line = staff.getClosestLine(se);
    //        double dy = line.yAt(se.x) - se.y;
    //
    //        if (abs(dy) > params.maxStaffLineDy) {
    //            logger.debug("End ({},{}] far from staff line", se.x, se.y);
    //
    //            return null;
    //        }
    //
    //        // General direction of slur end WRT staff line
    //        Point2D uv = getEndVector(slur);
    //
    //        if (uv == null) {
    //            return null;
    //        }
    //
    //        try {
    //            // There may be not computable midPoint
    //            Point2D midPoint = slur.getMidPoint();
    //            double backDy = line.yAt(midPoint.getX()) - midPoint.getY();
    //            boolean crossing = (uv.getY() * backDy) > 0;
    //            Model gc = slur.getModel();
    //            double incidence = gc.getAngle(reverse) - ((gc.ccw() * PI) / 2);
    //
    //            if (crossing && (abs(incidence) <= params.maxIncidence)) {
    //                return line;
    //            }
    //        } catch (Exception ex) {
    //        }
    //
    //        return null;
    //    }
    //
    //-----------------//
    // purgeStaffLines //
    //-----------------//
    /**
     * Purge the clump candidates that end as a portion of staff line.
     * <p>
     * First, check that slur end point is very close to a staff line.
     * Second, check the model is a LineModel (or a CircleModel with a huge radius).
     * Third, check incidence angle with staff line.
     *
     * @param inters the candidates to weed
     */
    private void purgeStaffLines (List<SlurInter> inters)
    {
        List<SlurInter> toDelete = new ArrayList<SlurInter>();

        for (SlurInter inter : inters) {
            SlurInfo slur = inter.getInfo();

            if (debugArc) {
                curves.selectPoint(slur.getEnd(reverse));
            }

            // Vertical distance from slur end to closest staff line
            Point end = slur.getEnd(reverse);
            Staff staff = sheet.getStaffManager().getClosestStaff(end);
            FilamentLine line = staff.getClosestLine(end);
            double dy = line.yAt(end.x) - end.y;

            if (abs(dy) > params.maxStaffLineDy) {
                continue;
            }

            // Check straightness
            Model model = slur.getSideModel(reverse);

            if (model instanceof CircleModel) {
                CircleModel circleModel = (CircleModel) model;

                if (circleModel.getCircle().getRadius() <= params.minStraightEndRadius) {
                    continue;
                }
            }

            // Check incidence angle
            double incidence = model.getAngle(reverse) - ((model.ccw() * PI) / 2);

            if (abs(incidence) > params.maxIncidence) {
                continue;
            }

            logger.debug("{} ending as staff line", slur);
            toDelete.add(inter);
        }

        if (!toDelete.isEmpty()) {
            inters.removeAll(toDelete);
        }
    }

    //----------//
    // register //
    //----------//
    /**
     * Register the slurs of clump into their containing systems.
     * NOTA: This method is kept available only for debug manual action
     *
     * @param clump the clump of slurs
     */
    private void register (Set<SlurInter> clump)
    {
        for (SlurInter slur : clump) {
            pageSlurs.add(slur);
        }

        // Dispatch slurs
        Rectangle clumpBounds = getBounds(clump);
        SystemManager mgr = sheet.getSystemManager();

        for (SystemInfo system : mgr.getSystemsOf(clumpBounds, null)) {
            SIGraph sig = system.getSig();

            for (SlurInter slur : clump) {
                sig.addVertex(slur);
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
                5,
                "Maximum incidence angle (in degrees) for staff tangency");

        final Scale.Fraction minStraightEndRadius = new Scale.Fraction(
                50,
                "Minimum circle radius for a straight slur end");

        final Scale.Fraction arcMinSeedLength = new Scale.Fraction(
                0.8,
                "Minimum arc length for starting a slur build");

        final Scale.Fraction maxStaffLineDy = new Scale.Fraction(
                0.2,
                "Vertical distance to closest staff line to detect tangency");

        final Scale.Fraction maxSlurDistance = new Scale.Fraction(
                0.15,
                "Maximum circle distance for final slur");

        final Scale.Fraction maxExtDistance = new Scale.Fraction(
                0.45,
                "Maximum circle distance for extension arc");

        final Scale.Fraction maxArcsDistance = new Scale.Fraction(
                0.15,
                "Maximum circle distance for intermediate arcs");

        final Scale.Fraction arcCheckLength = new Scale.Fraction(
                2,
                "Length checked for extension arc");

        final Scale.Fraction sideModelLength = new Scale.Fraction(
                6,
                "Length for side osculatory model");

        final Scale.Fraction minCircleRadius = new Scale.Fraction(
                0.4,
                "Minimum circle radius for a slur");

        final Scale.Fraction minSlurWidthLow = new Scale.Fraction(
                0.7,
                "Low minimum width for a slur");

        final Scale.Fraction minSlurWidthHigh = new Scale.Fraction(
                1.5,
                "High minimum width for a slur");

        final Scale.Fraction minSlurHeightLow = new Scale.Fraction(
                0.07,
                "Low minimum height for a slur");

        final Scale.Fraction minSlurHeightHigh = new Scale.Fraction(
                1.0,
                "High minimum height for a slur");

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
                10.0,
                "Low minimum angle (in degrees) between slur and vertical");

        final Constant.Double minAngleFromVerticalHigh = new Constant.Double(
                "degree",
                25.0,
                "High minimum angle (in degrees) between slur and vertical");

        final Constant.Ratio quorumRatio = new Constant.Ratio(
                0.75,
                "Minimum length expressed as ratio of longest in clump");

        final Scale.Fraction minProjection = new Scale.Fraction(
                -1.0,
                "Minimum projection on curve for arc extension");

        final Scale.Fraction maxRunDistance = new Scale.Fraction(
                0.25,
                "Maximum distance from any run end to curve points");
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

        final double minStraightEndRadius;

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

        final double quorumRatio;

        final double minProjection;

        final double maxRunDistance;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            similarRadiusRatio = constants.similarRadiusRatio.getValue();
            sideLength = scale.toPixels(constants.sideModelLength);
            arcCheckLength = scale.toPixels(constants.arcCheckLength);
            arcMinSeedLength = scale.toPixels(constants.arcMinSeedLength);
            maxStaffLineDy = scale.toPixelsDouble(constants.maxStaffLineDy);
            maxIncidence = toRadians(constants.maxIncidence.getValue());
            maxSlurDistance = scale.toPixelsDouble(constants.maxSlurDistance);
            maxExtDistance = scale.toPixelsDouble(constants.maxExtDistance);
            maxArcsDistance = scale.toPixelsDouble(constants.maxArcsDistance);
            minCircleRadius = scale.toPixelsDouble(constants.minCircleRadius);
            minStraightEndRadius = scale.toPixelsDouble(constants.minStraightEndRadius);
            minSlurWidthLow = scale.toPixelsDouble(constants.minSlurWidthLow);
            minSlurWidthHigh = scale.toPixelsDouble(constants.minSlurWidthHigh);
            minSlurHeightLow = scale.toPixelsDouble(constants.minSlurHeightLow);
            minSlurHeightHigh = scale.toPixelsDouble(constants.minSlurHeightHigh);
            maxArcAngleHigh = toRadians(constants.maxArcAngleHigh.getValue());
            maxArcAngleLow = toRadians(constants.maxArcAngleLow.getValue());
            minAngleFromVerticalLow = toRadians(constants.minAngleFromVerticalLow.getValue());
            minAngleFromVerticalHigh = toRadians(constants.minAngleFromVerticalHigh.getValue());
            quorumRatio = constants.quorumRatio.getValue();
            minProjection = scale.toPixelsDouble(constants.minProjection);
            maxRunDistance = scale.toPixelsDouble(constants.maxRunDistance);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
