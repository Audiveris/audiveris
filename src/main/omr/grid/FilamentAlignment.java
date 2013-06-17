//----------------------------------------------------------------------------//
//                                                                            //
//                     F i l a m e n t A l i g n m e n t                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.facets.BasicAlignment;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition.Linking;

import omr.lag.Section;

import omr.math.LineUtil;
import omr.math.NaturalSpline;
import omr.math.Population;

import omr.run.Orientation;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code FilamentAlignment} is a GlyphAlignment meant for a
 * Filament instance, where the underlying Line is actually not a
 * straight line, but a NaturalSpline.
 *
 * @author Hervé Bitteur
 */
public class FilamentAlignment
        extends BasicAlignment
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            FilamentAlignment.class);

    //~ Instance fields --------------------------------------------------------
    /** Absolute defining points */
    protected List<Point2D> points;

    /** Mean distance from a straight line */
    protected Double meanDistance;

    //~ Constructors -----------------------------------------------------------
    //-------------------//
    // FilamentAlignment //
    //-------------------//
    /**
     * Creates a new FilamentAlignment object.
     */
    public FilamentAlignment (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.dumpOf());
        sb.append(String.format("   meanRadius:%.3f%n", getMeanCurvature()));

        return sb.toString();
    }

    //---------//
    // getLine //
    //---------//
    @Override
    public NaturalSpline getLine ()
    {
        return (NaturalSpline) super.getLine();
    }

    //------------------//
    // getMeanCurvature //
    //------------------//
    /**
     * Report the average radius of curvature along all segments of
     * the curve.
     * This is not a global radius, but rather a way to mesure how straight
     * the curve is.
     *
     * @return the average of radius measurements along all curve segments
     */
    public double getMeanCurvature ()
    {
        Point2D prevPoint = null;
        Line2D prevBisector = null;
        Line2D bisector = null;
        Population curvatures = new Population();

        for (Point2D point : points) {
            if (prevPoint != null) {
                bisector = LineUtil.bisector(
                        new Line2D.Double(prevPoint, point));
            }

            if (prevBisector != null) {
                Point2D inter = LineUtil.intersection(
                        prevBisector.getP1(),
                        prevBisector.getP2(),
                        bisector.getP1(),
                        bisector.getP2());
                double radius = Math.hypot(
                        inter.getX() - point.getX(),
                        inter.getY() - point.getY());

                curvatures.includeValue(1 / radius);
            }

            prevBisector = bisector;
            prevPoint = point;
        }

        if (curvatures.getCardinality() > 0) {
            return 1 / curvatures.getMeanValue();
        } else {
            return 0;
        }
    }

    //-----------------//
    // getMeanDistance //
    //-----------------//
    @Override
    public double getMeanDistance ()
    {
        if (line == null) {
            computeLine();
        }

        if (meanDistance == null) {
            Line2D straight = new Line2D.Double(startPoint, stopPoint);

            double totalDistSq = 0;
            int pointCount = points.size() - 2; // Only intermediate points!

            for (int i = 1, iMax = pointCount; i <= iMax; i++) {
                totalDistSq += straight.ptLineDistSq(points.get(i));
            }

            if (pointCount > 0) {
                meanDistance = Math.sqrt(totalDistSq / pointCount);
            }
        }

        return (meanDistance != null) ? meanDistance : 0;
    }

    //---------------//
    // getPositionAt //
    //---------------//
    @Override
    public double getPositionAt (double coord,
                                 Orientation orientation)
    {
        if (line == null) {
            computeLine();
        }

        if (orientation == Orientation.HORIZONTAL) {
            if ((coord < startPoint.getX()) || (coord > stopPoint.getX())) {
                double sl = (stopPoint.getY() - startPoint.getY()) / (stopPoint.
                        getX()
                                                                      - startPoint.
                        getX());

                return startPoint.getY() + (sl * (coord - startPoint.getX()));
            } else {
                return line.yAtX(coord);
            }
        } else {
            if ((coord < startPoint.getY()) || (coord > stopPoint.getY())) {
                double sl = (stopPoint.getX() - startPoint.getX()) / (stopPoint.
                        getY()
                                                                      - startPoint.
                        getY());

                return startPoint.getX() + (sl * (coord - startPoint.getY()));
            } else {
                return line.xAtY(coord);
            }
        }
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        super.invalidateCache();
        points = null;
        meanDistance = null;
    }

    //-----------------//
    // polishCurvature //
    //-----------------//
    /**
     * Polish the filament by looking at local curvatures and removing
     * sections when necessary.
     * <p>If the local point is next to the first or last point of the curve,
     * then the point to modify is likely to be this first or last point.
     * In the other cases, the local point itself is modified.
     */
    public void polishCurvature ()
    {
        boolean modified = false;

        do {
            modified = false;

            final List<Line2D> bisectors = getBisectors();

            // Compute radius values (using same index as points)
            final List<Double> radii = new ArrayList<>();
            radii.add(null); // To skip index 0 for which we have no value (???)

            for (int i = 1, iBreak = points.size() - 1; i < iBreak; i++) {
                radii.add(getRadius(i, bisectors));
            }

            // Check smallest radius
            Integer idx = null;
            double minRadius = Integer.MAX_VALUE;

            for (int i = 1, iBreak = points.size() - 1; i < iBreak; i++) {
                double radius = radii.get(i);

                if (minRadius > radius) {
                    minRadius = radius;
                    idx = i;
                }
            }

            double rad = minRadius / glyph.getInterline();

            if (rad < constants.minRadius.getValue()) {
                if (logger.isDebugEnabled() || glyph.isVip()) {
                    logger.info("Polishing F#{} minRad: {} seq:{} {}",
                            glyph.getId(), (float) rad, idx, points.get(idx));
                }

                // Adjust the removable point for first & last points
                if (idx == 1) {
                    idx--;
                } else if (idx == (points.size() - 2)) {
                    idx++;
                }

                // Lookup corresponding section(s)
                Scale scale = new Scale(glyph.getInterline());
                int probeWidth = scale.toPixels(
                        BasicAlignment.getProbeWidth());
                Orientation orientation = getRoughOrientation();
                final Point2D point = points.get(idx);
                Point2D orientedPt = orientation.oriented(
                        points.get(idx));
                Rectangle2D rect = new Rectangle2D.Double(
                        orientedPt.getX() - (probeWidth / 2),
                        orientedPt.getY() - (probeWidth / 2),
                        probeWidth,
                        probeWidth);
                List<Section> found = new ArrayList<>();

                for (Section section : glyph.getMembers()) {
                    if (rect.intersects(section.getOrientedBounds())) {
                        found.add(section);
                    }
                }

                if (found.size() > 1) {
                    // Pick up the section closest to the point
                    Collections.sort(
                            found,
                            new Comparator<Section>()
                    {
                        @Override
                        public int compare (Section s1,
                                            Section s2)
                        {
                            return Double.compare(
                                    point.distance(s1.getCentroid()),
                                    point.distance(s2.getCentroid()));
                        }
                    });
                }

                Section section = found.isEmpty() ? null : found.get(0);

                if (section != null) {
                    logger.debug("Removed section#{} from {} F{}",
                            section.getId(), orientation, glyph.getId());
                    glyph.removeSection(section, Linking.LINK_BACK);
                    modified = true;
                }
            }
        } while (modified);
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g)
    {
        if (!glyph.getBounds().intersects(g.getClipBounds())) {
            return;
        }

        // The curved line itself
        if (line != null) {
            g.draw((NaturalSpline) line);
        }

        // Then the absolute defining points?
        if (constants.showFilamentPoints.isSet() && (points != null)) {
            // Point radius
            double r = glyph.getInterline() * constants.filamentPointSize.
                    getValue();
            Ellipse2D ellipse = new Ellipse2D.Double();

            for (Point2D p : points) {
                ellipse.setFrame(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
                g.fill(ellipse);
            }
        }
    }

    //---------//
    // slopeAt //
    //---------//
    public double slopeAt (double coord,
                           Orientation orientation)
    {
        if (line == null) {
            computeLine();
        }

        if (orientation == Orientation.HORIZONTAL) {
            return getLine().yDerivativeAtX(coord);
        } else {
            return getLine().xDerivativeAtY(coord);
        }
    }

    //-------------//
    // computeLine //
    //-------------//
    /**
     * Compute cached data: curve, startPoint, stopPoint, slope.
     * Curve goes from startPoint to stopPoint through intermediate points
     * regularly spaced
     */
    @Override
    protected void computeLine ()
    {
        try {
            Scale scale = new Scale(glyph.getInterline());

            /** Width of window to retrieve pixels */
            int probeWidth = scale.toPixels(BasicAlignment.getProbeWidth());

            /** Typical length of curve segments */
            double typicalLength = scale.toPixels(constants.segmentLength);

            // We need a rough orientation right now
            Orientation orientation = getRoughOrientation();
            Point2D orientedStart = (startPoint == null) ? null
                    : orientation.oriented(startPoint);
            Point2D orientedStop = (stopPoint == null) ? null
                    : orientation.oriented(stopPoint);
            Rectangle oBounds = orientation.oriented(glyph.getBounds());
            double oStart = (orientedStart != null) ? orientedStart.getX()
                    : oBounds.x;
            double oStop = (orientedStop != null) ? orientedStop.getX()
                    : (oBounds.x + (oBounds.width - 1));
            double length = oStop - oStart + 1;

            Rectangle oProbe = new Rectangle(oBounds);
            oProbe.x = (int) Math.ceil(oStart);
            oProbe.width = probeWidth;

            // Determine the number of segments and their precise length
            int segCount = (int) Math.rint(length / typicalLength);
            double segLength = length / segCount;
            List<Point2D> newPoints = new ArrayList<>(segCount + 1);

            // First point
            if (startPoint == null) {
                Point2D p = orientation.oriented(
                        getRectangleCentroid(orientation.absolute(oProbe)));
                startPoint = orientation.absolute(
                        new Point2D.Double(oStart, p.getY()));
            }

            newPoints.add(startPoint);

            // Intermediate points (perhaps none)
            for (int i = 1; i < segCount; i++) {
                oProbe.x = (int) Math.rint(oStart + (i * segLength));

                Point2D pt = getRectangleCentroid(orientation.absolute(oProbe));

                // If, unfortunately, we are in a filament hole, just skip it
                if (pt != null) {
                    newPoints.add(pt);
                }
            }

            // Last point
            if (stopPoint == null) {
                oProbe.x = (int) Math.floor(oStop - oProbe.width + 1);

                Point2D p = orientation.oriented(
                        getRectangleCentroid(orientation.absolute(oProbe)));
                stopPoint = orientation.absolute(
                        new Point2D.Double(oStop, p.getY()));
            }

            newPoints.add(stopPoint);

            // Interpolate the best spline through the provided points
            line = NaturalSpline.interpolate(
                    newPoints.toArray(new Point2D[newPoints.size()]));

            // Remember points (atomically)
            this.points = newPoints;

            // Cache global slope
            getSlope();
        } catch (Exception ex) {
            logger.warn("Filament cannot computeData", ex);
        }
    }

    //-----------//
    // findPoint //
    //-----------//
    protected Point2D findPoint (int coord,
                                 Orientation orientation,
                                 int margin)
    {
        Point2D best = null;
        double bestDeltacoord = Integer.MAX_VALUE;

        for (Point2D p : points) {
            double dc = Math.abs(
                    coord
                    - ((orientation == Orientation.HORIZONTAL) ? p.getX() : p.
                    getY()));

            if ((dc <= margin) && (dc < bestDeltacoord)) {
                bestDeltacoord = dc;
                best = p;
            }
        }

        return best;
    }

    //--------------//
    // getBisectors //
    //--------------//
    /**
     * Report bisectors of inter-points segments.
     *
     * @return sequence of bisectors, such that bisectors[i] is bisector of
     *         segment (i -> i+1)
     */
    private List<Line2D> getBisectors ()
    {
        if (line == null) {
            computeLine();
        }

        List<Line2D> bisectors = new ArrayList<>();

        for (int i = 0; i < (points.size() - 1); i++) {
            bisectors.add(
                    LineUtil.bisector(
                    new Line2D.Double(points.get(i), points.get(i + 1))));
        }

        return bisectors;
    }

    //-----------//
    // getRadius //
    //-----------//
    /**
     * Report radius computed at point with index 'i'.
     * <p>TODO: This a simplistic way for computing radius, based on insection
     * of the two adjacent bisectors.
     * There may be other ways, such as using the following property:
     * sin angle a / length of segment a = 1 / (2 * radius)
     *
     * @param i         the index of desired point
     * @param bisectors the sequence of bisectors
     * @return the value of radius of curvature
     */
    private double getRadius (int i,
                              List<Line2D> bisectors)
    {
        Line2D prevBisector = bisectors.get(i - 1);
        Point2D point = points.get(i);
        Line2D nextBisector = bisectors.get(i);

        Point2D inter = LineUtil.intersection(
                prevBisector.getP1(),
                prevBisector.getP2(),
                nextBisector.getP1(),
                nextBisector.getP2());

        return Math.hypot(
                inter.getX() - point.getX(),
                inter.getY() - point.getY());
    }

    //---------------------//
    // getRoughOrientation //
    //---------------------//
    private Orientation getRoughOrientation ()
    {
        Rectangle box = glyph.getBounds();

        return (box.height > box.width) ? Orientation.VERTICAL
                : Orientation.HORIZONTAL;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Scale.Fraction segmentLength = new Scale.Fraction(
                2,
                "Typical length between filament curve intermediate points");

        Constant.Boolean showFilamentPoints = new Constant.Boolean(
                false,
                "Should we display filament points?");

        Scale.Fraction filamentPointSize = new Scale.Fraction(
                0.05,
                "Size of displayed filament points");

        Scale.Fraction minRadius = new Scale.Fraction(
                12,
                "Minimum acceptable radius of curvature");

    }
}
