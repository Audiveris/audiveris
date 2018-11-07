//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         G e o P a t h                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.math;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;

/**
 * Class {@code GeoPath} is a Path2D.Double with some additions
 *
 * @author Hervé Bitteur
 */
public class GeoPath
        extends Path2D.Double
{

    /**
     * Creates a new GeoPath object.
     */
    public GeoPath ()
    {
    }

    /**
     * Creates a new GeoPath object.
     *
     * @param s the specified {@code Shape} object
     */
    public GeoPath (Shape s)
    {
        this(s, null);
    }

    /**
     * Creates a new GeoPath object.
     *
     * @param s  the specified {@code Shape} object
     * @param at the specified {@code AffineTransform} object
     */
    public GeoPath (Shape s,
                    AffineTransform at)
    {
        super(s, at);
    }

    //---------------//
    // getFirstPoint //
    //---------------//
    /**
     * Report the first defining point of the path.
     *
     * @return the first path point
     */
    public Point2D getFirstPoint ()
    {
        final double[] buffer = new double[6];
        final PathIterator it = getPathIterator(null);

        if (!it.isDone()) {
            final int segmentKind = it.currentSegment(buffer);
            final int count = countOf(segmentKind);
            final double x = buffer[count - 2];
            final double y = buffer[count - 1];

            if ((segmentKind == SEG_MOVETO) || (segmentKind == SEG_CLOSE)) {
                return new Point2D.Double(x, y);
            } else {
                return new Point2D.Double(0, 0);
            }
        }

        return null;
    }

    //--------------//
    // getLastPoint //
    //--------------//
    /**
     * Report the last defining point of the path.
     *
     * @return the last path point
     */
    public Point2D getLastPoint ()
    {
        final double[] buffer = new double[6];
        final PathIterator it = getPathIterator(null);

        if (it.isDone()) {
            return null;
        }

        double x = 0;
        double y = 0;

        while (!it.isDone()) {
            final int segmentKind = it.currentSegment(buffer);
            final int count = countOf(segmentKind);
            x = buffer[count - 2];
            y = buffer[count - 1];
            it.next();
        }

        return new Point2D.Double(x, y);
    }

    //---------//
    // labelOf //
    //---------//
    /**
     * Report the kind label of a segment.
     *
     * @param segmentKind the int-based segment kind
     * @return the label for the curve
     */
    public static String labelOf (int segmentKind)
    {
        switch (segmentKind) {
        case SEG_MOVETO:
            return "SEG_MOVETO";

        case SEG_LINETO:
            return "SEG_LINETO";

        case SEG_QUADTO:
            return "SEG_QUADTO";

        case SEG_CUBICTO:
            return "SEG_CUBICTO";

        case SEG_CLOSE:
            return "SEG_CLOSE";

        default:
            throw new RuntimeException("Illegal segmentKind " + segmentKind);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        double[] buffer = new double[6];

        for (PathIterator it = getPathIterator(null); !it.isDone(); it.next()) {
            int segmentKind = it.currentSegment(buffer);

            sb.append(" ").append(labelOf(segmentKind)).append("(");

            int coords = countOf(segmentKind);
            boolean firstCoord = true;

            for (int ic = 0; ic < (coords - 1); ic += 2) {
                if (!firstCoord) {
                    sb.append(",");
                    firstCoord = false;
                }

                sb.append("[").append((float) buffer[ic]).append(",").append((float) buffer[ic + 1])
                        .append("]");
            }

            sb.append(")");
        }

        sb.append("}");

        return sb.toString();
    }

    //------//
    // xAtY //
    //------//
    /**
     * Report the abscissa value of the spline at provided ordinate
     * (assuming true function)
     *
     * @param y the provided ordinate (must be in y range of the spline)
     * @return the abscissa value at this ordinate
     */
    public double xAtY (double y)
    {
        final double[] coords = new double[6];
        final Point2D.Double p1 = new Point2D.Double();
        final Point2D.Double p2 = new Point2D.Double();
        final int segmentKind = getYSegment(y, coords, p1, p2);
        final double t = (y - p1.y) / (p2.y - p1.y);
        final double u = 1 - t;

        switch (segmentKind) {
        case SEG_LINETO:
            return p1.x + (t * (p2.x - p1.x));

        case SEG_QUADTO: {
            double cpx = coords[0];

            return (p1.x * u * u) + (2 * cpx * t * u) + (p2.x * t * t);
        }

        case SEG_CUBICTO: {
            double cpx1 = coords[0];
            double cpx2 = coords[2];

            return (p1.x * u * u * u) + (3 * cpx1 * t * u * u) + (3 * cpx2 * t * t * u) + (p2.x * t
                                                                                                   * t
                                                                                           * t);
        }

        default:
            throw new RuntimeException("Illegal segmentKind " + segmentKind);
        }
    }

    //---------//
    // xAtYExt //
    //---------//
    /**
     * Similar functionality as xAtY, but also accepts ordinates outside the line
     * ordinate range by extrapolating the line based on start and stop points.
     *
     * @param y the provided ordinate
     * @return the abscissa value at this ordinate
     */
    public double xAtYExt (double y)
    {
        Point2D startPoint = getFirstPoint();
        Point2D stopPoint = getLastPoint();

        if ((y < startPoint.getY()) || (y > stopPoint.getY())) {
            double sl = (stopPoint.getX() - startPoint.getX()) / (stopPoint.getY() - startPoint
                    .getY());

            return startPoint.getX() + (sl * (y - startPoint.getY()));
        } else {
            return xAtY(y);
        }
    }

    //------//
    // yAtX //
    //------//
    /**
     * Report the ordinate value of the spline at provided abscissa
     * (assuming true function)
     *
     * @param x the provided abscissa (must be in x range of the spline)
     * @return the ordinate value at this abscissa
     */
    public double yAtX (double x)
    {
        final double[] coords = new double[6];
        final Point2D.Double p1 = new Point2D.Double();
        final Point2D.Double p2 = new Point2D.Double();
        final int segmentKind = getXSegment(x, coords, p1, p2);
        final double t = (x - p1.x) / (p2.x - p1.x);
        final double u = 1 - t;

        switch (segmentKind) {
        case SEG_LINETO:
            return p1.y + (t * (p2.y - p1.y));

        case SEG_QUADTO: {
            double cpy = coords[1];

            return (p1.y * u * u) + (2 * cpy * t * u) + (p2.y * t * t);
        }

        case SEG_CUBICTO: {
            double cpy1 = coords[1];
            double cpy2 = coords[3];

            return (p1.y * u * u * u) + (3 * cpy1 * t * u * u) + (3 * cpy2 * t * t * u) + (p2.y * t
                                                                                                   * t
                                                                                           * t);
        }

        default:
            throw new RuntimeException("Illegal segmentKind " + segmentKind);
        }
    }

    //---------//
    // yAtXExt //
    //---------//
    /**
     * Similar functionality as yAtX, but also accepts abscissae outside the spline
     * abscissa range by extrapolating the line based on start and stop points.
     *
     * @param x the provided abscissa
     * @return the ordinate value at this abscissa
     */
    public double yAtXExt (double x)
    {
        Point2D startPoint = getFirstPoint();
        Point2D stopPoint = getLastPoint();

        if ((x < startPoint.getX()) || (x > stopPoint.getX())) {
            double sl = (stopPoint.getY() - startPoint.getY()) / (stopPoint.getX() - startPoint
                    .getX());

            return startPoint.getY() + (sl * (x - startPoint.getX()));
        } else {
            return yAtX(x);
        }
    }

    //---------//
    // countOf //
    //---------//
    /**
     * Report how many coordinate values a path segment contains.
     *
     * @param segmentKind the int-based segment kind
     * @return the number of coordinates values
     */
    protected static int countOf (int segmentKind)
    {
        switch (segmentKind) {
        case SEG_MOVETO:
        case SEG_LINETO:
            return 2;

        case SEG_QUADTO:
            return 4;

        case SEG_CUBICTO:
            return 6;

        case SEG_CLOSE:
            return 0;

        default:
            throw new RuntimeException("Illegal segmentKind " + segmentKind);
        }
    }

    //-------------//
    // getXSegment //
    //-------------//
    /**
     * Retrieve the first segment of the curve that contains the provided abscissa.
     *
     * @param x      the provided abscissa
     * @param coords output: coordinates
     * @param p1     output: start of segment
     * @param p2     output: end of segment
     * @return the segment kind
     */
    protected int getXSegment (double x,
                               double[] coords,
                               Point2D.Double p1,
                               Point2D.Double p2)
    {
        PathIterator it = getPathIterator(null);
        double x1 = 0;
        double y1 = 0;

        while (!it.isDone()) {
            final int segmentKind = it.currentSegment(coords);
            final int count = countOf(segmentKind);
            final double x2 = coords[count - 2];
            final double y2 = coords[count - 1];

            if ((segmentKind == SEG_MOVETO) || (segmentKind == SEG_CLOSE) || (x > x2)) {
                // Move to next segment
                x1 = x2;
                y1 = y2;
                it.next();
            } else {
                p1.x = x1;
                p1.y = y1;
                p2.x = x2;
                p2.y = y2;

                return segmentKind;
            }
        }

        // Not found
        throw new RuntimeException("Abscissa not in range: " + x);
    }

    //-------------//
    // getYSegment //
    //-------------//
    /**
     * Retrieve the first segment of the curve that contains the provided ordinate.
     *
     * @param y      the provided ordinate
     * @param coords output: coordinates
     * @param p1     output: start of segment
     * @param p2     output: end of segment
     * @return the segment kind
     */
    protected int getYSegment (double y,
                               double[] coords,
                               Point2D.Double p1,
                               Point2D.Double p2)
    {
        PathIterator it = getPathIterator(null);
        double x1 = 0;
        double y1 = 0;

        while (!it.isDone()) {
            final int segmentKind = it.currentSegment(coords);
            final int count = countOf(segmentKind);
            final double x2 = coords[count - 2];
            final double y2 = coords[count - 1];

            if ((segmentKind == SEG_MOVETO) || (segmentKind == SEG_CLOSE) || (y > y2)) {
                // Move to next segment
                x1 = x2;
                y1 = y2;
                it.next();
            } else {
                p1.x = x1;
                p1.y = y1;
                p2.x = x2;
                p2.y = y2;

                return segmentKind;
            }
        }

        // Not found
        throw new RuntimeException("Ordinate not in range: " + y);
    }
}
