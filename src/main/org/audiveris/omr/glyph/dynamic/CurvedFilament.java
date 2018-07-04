//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   C u r v e d F i l a m e n t                                  //
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
package org.audiveris.omr.glyph.dynamic;

import org.audiveris.omr.lag.Section;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.NaturalSpline;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Scale.InterlineScale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Class {@code CurvedFilament} is a (perhaps wavy) filament of sections, represented
 * as a natural spline.
 *
 * @author Hervé Bitteur
 */
public class CurvedFilament
        extends Filament
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CurvedFilament.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Typical length between points. */
    protected final int segmentLength;

    /** Absolute defining points (including start &amp; stop points). */
    protected List<Point2D> points;

    /** Curved line across all defining points. */
    protected NaturalSpline spline;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code CurvedFilament} object.
     *
     * @param interline     scaling information
     * @param segmentLength typical length between points
     */
    public CurvedFilament (int interline,
                           int segmentLength)
    {
        super(interline);
        this.segmentLength = segmentLength;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // computeLine //
    //-------------//
    /**
     * Compute cached data: curve, startPoint, stopPoint, slope.
     * Curve goes from startPoint to stopPoint through intermediate points rather regularly spaced.
     */
    @Override
    public void computeLine ()
    {
        try {
            /** Width of window to retrieve pixels */
            int probeWidth = InterlineScale.toPixels(interline, Filament.getProbeWidth());

            // We need a rough orientation right now
            Orientation orientation = getRoughOrientation();
            Point2D orientedStart = (startPoint == null) ? null : orientation.oriented(
                    startPoint);
            Point2D orientedStop = (stopPoint == null) ? null : orientation.oriented(stopPoint);
            Rectangle oBounds = orientation.oriented(getBounds());
            double oStart = (orientedStart != null) ? orientedStart.getX() : oBounds.x;
            double oStop = (orientedStop != null) ? orientedStop.getX()
                    : (oBounds.x + (oBounds.width - 1));
            double length = oStop - oStart + 1;

            Rectangle oProbe = new Rectangle(oBounds);
            oProbe.x = (int) Math.ceil(oStart);
            oProbe.width = probeWidth;

            // Determine the number of segments and their precise length
            int segCount = (int) Math.rint(length / segmentLength);
            double segLength = length / segCount;
            List<Point2D> newPoints = new ArrayList<Point2D>(segCount + 1);

            // First point
            if (startPoint == null) {
                Point2D p = orientation.oriented(getCentroid(orientation.absolute(oProbe)));
                startPoint = orientation.absolute(new Point2D.Double(oStart, p.getY()));
            }

            newPoints.add(startPoint);

            // Intermediate points (perhaps none)
            for (int i = 1; i < segCount; i++) {
                oProbe.x = (int) Math.rint(oStart + (i * segLength));

                Point2D pt = getCentroid(orientation.absolute(oProbe));

                // If, unfortunately, we are in a filament hole, just skip it
                if (pt != null) {
                    newPoints.add(pt);
                }
            }

            // Last point
            if (stopPoint == null) {
                oProbe.x = (int) Math.floor(oStop - oProbe.width + 1);

                Point2D p = orientation.oriented(getCentroid(orientation.absolute(oProbe)));
                stopPoint = orientation.absolute(new Point2D.Double(oStop, p.getY()));
            }

            newPoints.add(stopPoint);

            // Interpolate the best spline through the provided points
            spline = NaturalSpline.interpolate(newPoints);

            // Remember points (atomically)
            this.points = newPoints;
        } catch (Exception ex) {
            logger.warn("Filament cannot computeData", ex);
        }
    }

    //------------------//
    // getMeanCurvature //
    //------------------//
    @Override
    public double getMeanCurvature ()
    {
        Point2D prevPoint = null;
        Line2D prevBisector = null;
        Line2D bisector = null;
        Population curvatures = new Population();

        if (spline == null) {
            computeLine();
        }

        for (Point2D point : points) {
            if (prevPoint != null) {
                bisector = LineUtil.bisector(new Line2D.Double(prevPoint, point));
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
            return Double.POSITIVE_INFINITY;
        }
    }

    //---------------//
    // getPositionAt //
    //---------------//
    @Override
    public double getPositionAt (double coord,
                                 Orientation orientation)
    {
        if (spline == null) {
            computeLine();
        }

        if (orientation == Orientation.HORIZONTAL) {
            if ((coord < startPoint.getX()) || (coord > stopPoint.getX())) {
                double sl = (stopPoint.getY() - startPoint.getY()) / (stopPoint.getX()
                                                                      - startPoint.getX());

                return startPoint.getY() + (sl * (coord - startPoint.getX()));
            } else {
                return spline.yAtX(coord);
            }
        } else if ((coord < startPoint.getY()) || (coord > stopPoint.getY())) {
            double sl = (stopPoint.getX() - startPoint.getX()) / (stopPoint.getY()
                                                                  - startPoint.getY());

            return startPoint.getX() + (sl * (coord - startPoint.getY()));
        } else {
            return spline.xAtY(coord);
        }
    }

    //------------//
    // getSlopeAt //
    //------------//
    @Override
    public double getSlopeAt (double coord,
                              Orientation orientation)
    {
        if (spline == null) {
            computeLine();
        }

        if (orientation == Orientation.HORIZONTAL) {
            return spline.yDerivativeAtX(coord);
        } else {
            return spline.xDerivativeAtY(coord);
        }
    }

    //-----------//
    // getSpline //
    //-----------//
    public NaturalSpline getSpline ()
    {
        if (spline == null) {
            computeLine();
        }

        return spline;
    }

    //-----------------//
    // polishCurvature //
    //-----------------//
    /**
     * Polish the filament by looking at local curvatures and removing
     * sections when necessary.
     * <p>
     * If the local point is next to the first or last point of the curve,
     * then the point to modify is likely to be this first or last point.
     * In the other cases, the local point itself is modified.
     * <p>
     * TODO: Instead of computing radius, it would be easier to use the second
     * derivative, which is linear for each cubic segment. Thus, maximum
     * absolute values of 2nd derivatives occur at spline knots. Simply remove
     * the knots that exhibit a too high absolute value.
     *
     * @param minimumRadius minimum radius value
     */
    public void polishCurvature (int minimumRadius)
    {
        // Perserve ending points
        final Point2D oldStartPoint = getStartPoint();
        final Point2D oldStopPoint = getStopPoint();

        boolean progressed;

        do {
            progressed = false;

            if (spline == null) {
                setEndingPoints(oldStartPoint, oldStopPoint);
            }

            final List<Line2D> bisectors = getBisectors();

            // Compute radius values (using same index as points)
            final List<Double> radii = new ArrayList<Double>();
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

            if (minRadius < minimumRadius) {
                if (logger.isDebugEnabled() || isVip()) {
                    logger.info(
                            "Polishing F#{} minRad: {} seq:{} {}",
                            getId(),
                            (float) minRadius / interline,
                            idx,
                            points.get(idx));
                }

                // Adjust the removable point for first & last points
                if (idx == 1) {
                    idx--;
                } else if (idx == (points.size() - 2)) {
                    idx++;
                }

                // Lookup corresponding section(s)
                int probeWidth = InterlineScale.toPixels(interline, Filament.getProbeWidth());
                Orientation orientation = getRoughOrientation();
                final Point2D point = points.get(idx);
                Point2D orientedPt = orientation.oriented(points.get(idx));
                Rectangle2D rect = new Rectangle2D.Double(
                        orientedPt.getX() - (probeWidth / 2),
                        orientedPt.getY() - (probeWidth / 2),
                        probeWidth,
                        probeWidth);
                List<Section> found = new ArrayList<Section>();

                for (Section section : getMembers()) {
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
                    logger.debug(
                            "*polishCurvature*. Removed section#{} from {} Filament {}",
                            section.getId(),
                            orientation,
                            getId());
                    removeSection(section, true);
                    progressed = true;
                }
            }
        } while (progressed);
    }

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g,
                            boolean showPoints,
                            double pointWidth)
    {
        Rectangle clip = g.getClipBounds();

        if ((clip != null) && !clip.intersects(getBounds())) {
            return;
        }

        // The curved line itself
        if (spline != null) {
            g.draw(spline);
        }

        // Then the absolute defining points?
        if (showPoints && (points != null)) {
            // Point radius
            double r = pointWidth / 2;
            Ellipse2D ellipse = new Ellipse2D.Double();

            for (Point2D p : points) {
                ellipse.setFrame(p.getX() - r, p.getY() - r, 2 * r, 2 * r);

                Color oldColor = null;

                if (p.getClass() != Point2D.Double.class) {
                    oldColor = g.getColor();
                    g.setColor(Color.red);
                }

                g.fill(ellipse);

                if (oldColor != null) {
                    g.setColor(oldColor);
                }
            }
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
                    coord - ((orientation == Orientation.HORIZONTAL) ? p.getX() : p.getY()));

            if ((dc <= margin) && (dc < bestDeltacoord)) {
                bestDeltacoord = dc;
                best = p;
            }
        }

        return best;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    protected void invalidateCache ()
    {
        super.invalidateCache();
        points = null;
        spline = null;
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
        List<Line2D> bisectors = new ArrayList<Line2D>();

        for (int i = 0; i < (points.size() - 1); i++) {
            bisectors.add(LineUtil.bisector(new Line2D.Double(points.get(i), points.get(i + 1))));
        }

        return bisectors;
    }

    //-----------//
    // getRadius //
    //-----------//
    /**
     * Report radius computed at point with index 'i'.
     * <p>
     * TODO: This is a simplistic way for computing radius, based on intersection
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

        return Math.hypot(inter.getX() - point.getX(), inter.getY() - point.getY());
    }
}
