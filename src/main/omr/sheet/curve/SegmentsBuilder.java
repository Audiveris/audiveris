//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S e g m e n t s B u i l d e r                                 //
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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.grid.FilamentLine;

import omr.math.LineUtil;

import omr.sheet.Scale;

import omr.sig.Inter;

import omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import omr.sig.GradeImpacts;
import omr.sig.SegmentInter;

/**
 * Class {@code SegmentsBuilder} retrieves straight segments that can be used to build
 * wedges or endings.
 *
 * @author Hervé Bitteur
 */
public class SegmentsBuilder
        extends CurvesBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SegmentsBuilder.class);

    private static final Color SEGMENT = Color.CYAN;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Scale-dependent parameters. */
    private final Parameters params;

    /** All segments retrieved in sheet. */
    private final List<SegmentInter> segments;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SegmentsBuilder object.
     *
     * @param curves curves environment
     */
    public SegmentsBuilder (Curves curves)
    {
        super(curves);
        params = new Parameters(sheet.getScale());
        segments = curves.getSegments();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // buildSegments //
    //---------------//
    /**
     * Build segments (for wedges and endings).
     */
    public void buildSegments ()
    {
        List<Arc> relevants = getSeedArcs();

        for (Arc arc : relevants) {
            if (!arc.isAssigned()) {
                buildCurve(arc);
            }
        }

        // Purge duplicates
        purgeDuplicates();

        logger.info("Segments: {}", segments.size());
    }

    @Override
    public void renderItems (Graphics2D g)
    {
        final Rectangle clip = g.getClipBounds();

        // Render info attachments
        Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

        for (SegmentInter segment : segments) {
            segment.getInfo().renderAttachments(g);
        }

        // Render segments
        g.setColor(SEGMENT);

        for (SegmentInter segment : segments) {
            Shape curve = segment.getInfo().getModel().getCurve();

            if (curve != null) {
                if ((clip == null) || clip.intersects(curve.getBounds())) {
                    g.draw(curve);
                }
            }
        }

        g.setStroke(oldStroke);
    }

    @Override
    protected Curve addArc (Arc arc,
                            Curve curve,
                            Set<Arc> browsed)
    {
        SegmentInfo segment = (SegmentInfo) curve;
        ArcView arcView = segment.getArcView(arc, reverse);

        // Check extension is compatible with line model
        Model model = needGlobalModel(segment);
        double dist = arcDistance(model, arcView);

        if (dist <= params.maxExtDistance) {
            List<Point> pts = curve.getAllPoints(arcView, reverse);
            SegmentInfo s = (SegmentInfo) createCurve(curve, arcView, pts, null);
            s.setModel(computeModel(pts));
            logger.debug("Slur#{} extended as {} dist:{}", segment.getId(), s, dist);

            return s;
        } else {
            logger.debug("Line#{} could not add {} dist:{}", segment.getId(), arc, dist);

            return null;
        }
    }

    //--------------//
    // computeModel //
    //--------------//
    @Override
    protected Model computeModel (List<Point> points)
    {
        return new LineModel(points);
    }

    @Override
    protected Curve createInstance (Point firstJunction,
                                    Point lastJunction,
                                    List<Point> points,
                                    Model model,
                                    Collection<Arc> parts)
    {
        return new SegmentInfo(++globalId, firstJunction, lastJunction, points, model, parts);
    }

    @Override
    protected void createInter (Curve curve,
                                Set<Inter> inters)
    {
        SegmentInfo segment = (SegmentInfo) curve;
        needGlobalModel(segment);
        GradeImpacts impacts = computeImpacts(segment, true);

        if (impacts != null) {
            SegmentInter inter = new SegmentInter(segment, impacts);
            inters.add(inter);
            segments.add(inter);
        }
    }

    @Override
    protected void filterInters (Set<Inter> inters)
    {
    }

    @Override
    protected Integer getArcCheckLength ()
    {
        return null; // No limit
    }

    @Override
    protected Point2D getEndVector (Curve curve)
    {
        return curve.getModel().getEndVector(reverse);
    }

    @Override
    protected FilamentLine getTangentLine (Curve curve)
    {
        return null;
    }

    @Override
    protected void weed (Set<Curve> clump)
    {
        // simply keep the longest one
        List<Curve> list = new ArrayList<Curve>(clump);
        Collections.sort(list, Curve.byReverseLength);
        clump.clear();
        clump.add(list.get(0));
    }

    //----------------//
    // computeImpacts //
    //----------------//
    private GradeImpacts computeImpacts (SegmentInfo segment,
                                         boolean b)
    {
        double dist = segment.getModel().getDistance();
        double distImpact = 1 - (dist / params.maxSegmentDistance);
        return new SegmentInter.Impacts(distImpact);
    }

    //-------------//
    // getSeedArcs //
    //-------------//
    /**
     * Build the arcs that can be used to start line/wedge building.
     * They contain only arcs with relevant shape and of sufficient length.
     *
     * @return the collection sorted by decreasing length
     */
    private List<Arc> getSeedArcs ()
    {
        Set<Arc> set = new HashSet<Arc>();

        for (Arc arc : skeleton.arcsMap.values()) {
            // Member of a slur
            if (arc.isAssigned()) {
                continue;
            }

            // Check min seed length
            if (arc.getLength() < params.arcMinSeedLength) {
                continue;
            }

            // Check shape
            if (!arc.getShape().isWedgeRelevant()) {
                continue;
            }

            // Check slope
            double slope = LineUtil.getSlope(arc.getEnd(true), arc.getEnd(false));

            if (slope > params.maxWedgeSlope) {
                continue;
            }

            set.add(arc);
        }

        List<Arc> list = new ArrayList<Arc>(set);
        Collections.sort(list, Arc.byReverseLength);

        return list;
    }

    private void purgeDuplicates ()
    {
        Collections.sort(segments, Inter.byAbscissa);

        for (int i = 0; i < segments.size(); i++) {
            SegmentInfo seg = segments.get(i).getInfo();
            Point start = seg.getEnd(true);
            Point stop = seg.getEnd(false);

            for (ListIterator<SegmentInter> it = segments.listIterator(i + 1); it.hasNext();) {
                SegmentInfo s = it.next().getInfo();
                Point start2 = s.getEnd(true);

                if (start2.x > start.x) {
                    break;
                }

                if (start.equals(start2) && s.getEnd(false).equals(stop)) {
                    it.remove();
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

        final Scale.Fraction arcMinSeedLength = new Scale.Fraction(
                1.5,
                "Minimum arc length for starting a wedge build");

        final Constant.Double maxWedgeAngle = new Constant.Double(
                "degree",
                20.0,
                "Maximum angle (in degrees) for a wedge branch with x-axis");

        final Scale.Fraction maxExtDistance = new Scale.Fraction(
                0.35,
                "Maximum line distance for extension arc");

        final Scale.Fraction maxSegmentDistance = new Scale.Fraction(
                0.3,
                "Maximum line distance for final segment");

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

        final int arcMinSeedLength;

        final double maxWedgeSlope;

        final double maxExtDistance;

        final double maxSegmentDistance;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            arcMinSeedLength = scale.toPixels(constants.arcMinSeedLength);
            maxWedgeSlope = Math.tan(Math.toRadians(constants.maxWedgeAngle.getValue()));
            maxExtDistance = scale.toPixelsDouble(constants.maxExtDistance);
            maxSegmentDistance = scale.toPixelsDouble(constants.maxSegmentDistance);

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
