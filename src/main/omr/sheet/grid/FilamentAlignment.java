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
package omr.sheet.grid;

import omr.constant.ConstantSet;

import omr.glyph.GlyphSection;
import omr.glyph.facets.BasicAlignment;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.Barycenter;
import omr.math.NaturalSpline;

import omr.run.Orientation;

import omr.sheet.Scale;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
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
                double slope = (double) (pStop.getX() - pStart.getX()) / (double) (pStop.getY() -
                               pStart.getY());

                return pStart.getX() + (slope * (coord - pStart.getY()));
            } else {
                return line.xAtY(coord);
            }
        } else {
            if ((coord < pStart.getX()) || (coord > pStop.getX())) {
                double slope = (double) (pStop.getY() - pStart.getY()) / (double) (pStop.getX() -
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

    //------------//
    // renderLine //
    //------------//
    @Override
    public void renderLine (Graphics2D g)
    {
        // Draw the absolute defining points
        if (points != null) {
            // Point radius
            double    r = glyph.getInterline() * constants.filamentPointSize.getValue();
            Color     oldColor = g.getColor();
            Ellipse2D ellipse = new Ellipse2D.Double();

            for (Point2D p : points) {
                ellipse.setFrame(p.getX() - r, p.getY() - r, 2 * r, 2 * r);
                g.setColor(getPointColor(p));
                g.fill(ellipse);
            }

            g.setColor(oldColor);
        }

        // Then the curved line itself
        if (line != null) {
            g.draw((NaturalSpline) line);
        }
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

    //---------------//
    // getPointColor //
    //---------------//
    protected Color getPointColor (Point2D p)
    {
        return Color.YELLOW;
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
            double        segLength = (double) length / segCount;
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
        Scale.Fraction filamentPointSize = new Scale.Fraction(
            0.1,
            "Size of displayed filament points");
    }
}
