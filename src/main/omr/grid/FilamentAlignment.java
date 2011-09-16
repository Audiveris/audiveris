//----------------------------------------------------------------------------//
//                                                                            //
//                     F i l a m e n t A l i g n m e n t                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphSection;
import omr.glyph.facets.BasicAlignment;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.Barycenter;
import omr.math.LineUtilities;
import omr.math.NaturalSpline;
import omr.math.Population;

import omr.run.Orientation;

import omr.sheet.Scale;

import omr.ui.Colors;

import java.awt.Color;
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
 * Class {@code FilamentAlignment} is a GlyphAlignment meant for a Filament
 * instance, where the underlying Line is actually not a straight line, but a
 * NaturalSpline.
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
    private static final Logger logger = Logger.getLogger(
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

    //-----------------//
    // getAbsoluteLine //
    //-----------------//
    @Override
    public NaturalSpline getAbsoluteLine ()
    {
        return getLine();
    }

    //-----------------//
    // setEndingPoints //
    //-----------------//
    @Override
    public void setEndingPoints (Point2D pStart,
                                 Point2D pStop)
    {
        super.setEndingPoints(pStart, pStop);
        computeLine();
    }

    //------------------//
    // getMeanCurvature //
    //------------------//
    /**
     * Report the average radius of curvature along all segments of the curve.
     * This is not a global radius, but rather a way to mesure how straight
     * the curve is
     * @return the average of radius measurements along all curve segments
     */
    public double getMeanCurvature ()
    {
        Point2D    prevPoint = null;
        Line2D     prevBisector = null;
        Line2D     bisector = null;
        Population curvatures = new Population();

        for (Point2D point : points) {
            if (prevPoint != null) {
                bisector = LineUtilities.bisector(
                    new Line2D.Double(prevPoint, point));
            }

            if (prevBisector != null) {
                Point2D inter = LineUtilities.intersection(
                    prevBisector.getP1(),
                    prevBisector.getP2(),
                    bisector.getP1(),
                    bisector.getP2());
                double  radius = Math.hypot(
                    inter.getX() - point.getX(),
                    inter.getY() - point.getY());

                curvatures.includeValue(1 / radius);
            }

            prevBisector = bisector;
            prevPoint = point;
        }

        return 1 / curvatures.getMeanValue();
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
            Line2D straight = new Line2D.Double(pStart, pStop);

            double totalDistSq = 0;
            int    pointCount = points.size() - 2; // Only intermediate points!

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
    public double getPositionAt (double coord)
    {
        if (line == null) {
            computeLine();
        }

        Orientation orientation = glyph.getLag()
                                       .getOrientation();

        if (orientation.isVertical()) {
            if ((coord < pStart.getY()) || (coord > pStop.getY())) {
                double slope = (pStop.getX() - pStart.getX()) / (pStop.getY() -
                                                                pStart.getY());

                return pStart.getX() + (slope * (coord - pStart.getY()));
            } else {
                return line.xAtY(coord);
            }
        } else {
            if ((coord < pStart.getX()) || (coord > pStop.getX())) {
                double slope = (pStop.getY() - pStart.getY()) / (pStop.getX() -
                                                                pStart.getX());

                return pStart.getY() + (slope * (coord - pStart.getX()));
            } else {
                return line.yAtX(coord);
            }
        }
    }

    //---------------//
    // getStartPoint //
    //---------------//
    @Override
    public Point2D getStartPoint ()
    {
        if (pStart == null) {
            computeLine();
        }

        return pStart;
    }

    //--------------//
    // getStopPoint //
    //--------------//
    @Override
    public Point2D getStopPoint ()
    {
        if (pStop == null) {
            computeLine();
        }

        return pStop;
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        super.dump();
        System.out.println("   meanRadius:" + (float) getMeanCurvature());
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
     * Polish the filament by looking at local curvatures and removing sections
     * when necessary.
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
            final List<Double> radii = new ArrayList<Double>();
            radii.add(null); // To skip index 0 for which we have no value (???)

            for (int i = 1, iBreak = points.size() - 1; i < iBreak; i++) {
                radii.add(getRadius(i, bisectors));
            }

            // Check smallest radius
            Integer idx = null;
            double  minRadius = Integer.MAX_VALUE;

            for (int i = 1, iBreak = points.size() - 1; i < iBreak; i++) {
                double radius = radii.get(i);

                if (minRadius > radius) {
                    minRadius = radius;
                    idx = i;
                }
            }

            double rad = minRadius / glyph.getInterline();

            if (rad < constants.minRadius.getValue()) {
                if (logger.isFineEnabled() || glyph.isVip()) {
                    logger.info(
                        "Polishing F#" + glyph.getId() + " minRad: " +
                        (float) rad + " seq:" + idx + " " + points.get(idx));
                }

                // Adjust the removable point for first & last points
                if (idx == 1) {
                    idx--;
                } else if (idx == (points.size() - 2)) {
                    idx++;
                }

                // Lookup corresponding section(s)
                Scale              scale = new Scale(glyph.getInterline());
                int                probeWidth = scale.toPixels(
                    super.getProbeWidth());
                Orientation        orientation = glyph.getLag()
                                                      .getOrientation();
                final Point2D      point = points.get(idx);
                Point2D            orientedPt = orientation.oriented(
                    points.get(idx));
                Rectangle2D        rect = new Rectangle2D.Double(
                    orientedPt.getX() - (probeWidth / 2),
                    orientedPt.getY() - (probeWidth / 2),
                    probeWidth,
                    probeWidth);
                List<GlyphSection> found = new ArrayList<GlyphSection>();

                for (GlyphSection section : glyph.getMembers()) {
                    if (rect.intersects(section.getOrientedBounds())) {
                        found.add(section);
                    }
                }

                if (found.size() > 1) {
                    // Pick up the section closest to the point
                    Collections.sort(
                        found,
                        new Comparator<GlyphSection>() {
                                public int compare (GlyphSection s1,
                                                    GlyphSection s2)
                                {
                                    return Double.compare(
                                        point.distance(s1.getCentroid()),
                                        point.distance(s2.getCentroid()));
                                }
                            });
                }

                GlyphSection section = found.isEmpty() ? null : found.get(0);

                if (section != null) {
                    logger.info(
                        "Removed section#" + section.getId() + " from " +
                        orientation + " F" + glyph.getId());
                    glyph.getMembers()
                         .remove(section);
                    section.setGlyph(null);
                    glyph.invalidateCache();
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
        // We render filaments differently, according to their orientation
        Color oldColor = g.getColor();
        g.setColor(
            (glyph.getLag()
                  .getOrientation() == Orientation.HORIZONTAL)
                        ? Colors.LINE_HORIZONTAL : Colors.LINE_VERTICAL);

        // The curved line itself
        if (line != null) {
            g.draw((NaturalSpline) line);
        }

        // Then the absolute defining points?
        if (constants.showFilamentPoints.getValue() && (points != null)) {
            // Point radius
            double    r = glyph.getInterline() * constants.filamentPointSize.getValue();
            Ellipse2D ellipse = new Ellipse2D.Double();

            for (Point2D p : points) {
                ellipse.setFrame(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
                g.fill(ellipse);
            }
        }

        g.setColor(oldColor);
    }

    //---------//
    // slopeAt //
    //---------//
    public double slopeAt (double coord)
    {
        if (line == null) {
            computeLine();
        }

        Orientation orientation = glyph.getLag()
                                       .getOrientation();

        if (orientation.isVertical()) {
            return getLine()
                       .xDerivativeAtY(coord);
        } else {
            return getLine()
                       .yDerivativeAtX(coord);
        }
    }

    //---------//
    // getLine //
    //---------//
    @Override
    protected NaturalSpline getLine ()
    {
        return (NaturalSpline) line;
    }

    //-------------//
    // computeLine //
    //-------------//
    /**
     * Compute cached data: curve, pStart, pStop
     * Curve goes from pStart to pStop through intermediate points regularly
     * spaced
     */
    @Override
    protected void computeLine ()
    {
        try {
            Scale  scale = new Scale(glyph.getInterline());

            /** Width of window to retrieve pixels */
            int probeWidth = scale.toPixels(super.getProbeWidth());

            /** Typical length of curve segments */
            double typicalLength = scale.toPixels(constants.segmentLength);

            // We need lag orientation
            if (glyph.getLag() == null) {
                glyph.setLag(glyph.getMembers().first().getGraph());
            }

            Orientation orientation = glyph.getLag()
                                           .getOrientation();

            Point2D     orientedStart = (pStart == null) ? null
                                        : orientation.oriented(pStart);
            Point2D     orientedStop = (pStop == null) ? null
                                       : orientation.oriented(pStop);
            Rectangle   bounds = glyph.getOrientedBounds();
            double      start = (orientedStart != null) ? orientedStart.getX()
                                : bounds.x;
            double      stop = (orientedStop != null) ? orientedStop.getX()
                               : (bounds.x + (bounds.width - 1));
            double      length = stop - start + 1;

            Rectangle   probe = new Rectangle(bounds);
            probe.x = (int) Math.ceil(start);
            probe.width = probeWidth;

            // Determine the number of segments and their precise length
            int           segCount = (int) Math.rint(length / typicalLength);
            double        segLength = length / segCount;
            List<Point2D> newPoints = new ArrayList<Point2D>(segCount + 1);

            // First point
            if (pStart == null) {
                Point2D p = getRectangleCentroid(probe);
                pStart = orientation.absolute(
                    new Point2D.Double(start, p.getY()));
            }

            newPoints.add(pStart);

            // Intermediate points (perhaps none)
            for (int i = 1; i < segCount; i++) {
                probe.x = (int) Math.rint(start + (i * segLength));

                Point2D pt = getRectangleCentroid(probe);

                // If, unfortunately, we are in a filament hole, just skip it
                if (pt != null) {
                    newPoints.add(orientation.absolute(pt));
                }
            }

            // Last point
            if (pStop == null) {
                probe.x = (int) Math.floor(stop - probe.width + 1);

                Point2D p = getRectangleCentroid(probe);
                pStop = orientation.absolute(
                    new Point2D.Double(stop, p.getY()));
            }

            newPoints.add(pStop);

            // Interpolate the best spline through the provided points
            line = NaturalSpline.interpolate(
                newPoints.toArray(new Point2D[newPoints.size()]));

            // Remember points (atomically)
            this.points = newPoints;
        } catch (Exception ex) {
            logger.warning("Filament cannot computeData", ex);
        }
    }

    //-----------//
    // findPoint //
    //-----------//
    protected Point2D findPoint (int x,
                                 int margin)
    {
        Point2D best = null;
        int     bestDx = Integer.MAX_VALUE;

        for (Point2D p : points) {
            int dx = Math.abs((int) Math.rint(p.getX() - x));

            if ((dx <= margin) && (dx < bestDx)) {
                bestDx = dx;
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
     * @return sequence of bisectors, such that bisectors[i] is bisector of
     * segment (i -> i+1)
     */
    private List<Line2D> getBisectors ()
    {
        if (line == null) {
            computeLine();
        }

        List<Line2D> bisectors = new ArrayList<Line2D>();

        for (int i = 0; i < (points.size() - 1); i++) {
            bisectors.add(
                LineUtilities.bisector(
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
     *    sin angle a / length of segment a = 1 / (2 * radius)
     * @param i the index of desired point
     * @param bisectors the sequence of bisectors
     * @return the value of radius of curvature
     */
    private double getRadius (int          i,
                              List<Line2D> bisectors)
    {
        Line2D  prevBisector = bisectors.get(i - 1);
        Point2D point = points.get(i);
        Line2D  nextBisector = bisectors.get(i);

        Point2D inter = LineUtilities.intersection(
            prevBisector.getP1(),
            prevBisector.getP2(),
            nextBisector.getP1(),
            nextBisector.getP2());

        return Math.hypot(
            inter.getX() - point.getX(),
            inter.getY() - point.getY());
    }

    //----------------------//
    // getRectangleCentroid //
    //----------------------//
    /**
     * Report the oriented centroid of all the filament pixels found in the
     * provided oriented ROI
     * @param roi the desired oriented region of interest
     * @return the oriented barycenter of the pixels found
     */
    private Point2D getRectangleCentroid (Rectangle roi)
    {
        Barycenter barycenter = new Barycenter();

        for (GlyphSection section : glyph.getMembers()) {
            section.cumulate(barycenter, roi);
        }

        if (barycenter.getWeight() != 0) {
            return new Point2D.Double(barycenter.getX(), barycenter.getY());
        } else {
            return null;
        }
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

        //
        Constant.Boolean showFilamentPoints = new Constant.Boolean(
            false,
            "Should we display filament points?");

        //
        Scale.Fraction filamentPointSize = new Scale.Fraction(
            0.05,
            "Size of displayed filament points");

        //
        Scale.Fraction minRadius = new Scale.Fraction(
            12,
            "Minimum acceptable radius of curvature");
    }
}
