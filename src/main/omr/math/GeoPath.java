//----------------------------------------------------------------------------//
//                                                                            //
//                               G e o P a t h                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code GeoPath} is a Path2D.Double with some additions
 *
 * @author Hervé Bitteur
 */
public class GeoPath
        extends Path2D.Double
{
    //~ Constructors -----------------------------------------------------------

    //---------//
    // GeoPath //
    //---------//
    /**
     * Creates a new GeoPath object.
     */
    public GeoPath ()
    {
    }

    //---------//
    // GeoPath //
    //---------//
    /**
     * Creates a new GeoPath object.
     *
     * @param s the specified {@code Shape} object
     */
    public GeoPath (Shape s)
    {
        this(s, null);
    }

    //---------//
    // GeoPath //
    //---------//
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

    //~ Methods ----------------------------------------------------------------
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

    //------------//
    // intersects //
    //------------//
    /**
     * Check whether the flattened path intersects the provided rectangle.
     *
     * @param rect     the provided rectangle to check for intersection
     * @param flatness maximum distance used for line segment approximation
     * @return true if intersection found
     */
    public boolean intersects (Rectangle2D rect,
                               double flatness)
    {
        final double[] buffer = new double[6];
        double x1 = 0;
        double y1 = 0;

        for (PathIterator it = getPathIterator(null, flatness); !it.isDone();
                it.next()) {
            int segmentKind = it.currentSegment(buffer);
            int count = countOf(segmentKind);
            final double x2 = buffer[count - 2];
            final double y2 = buffer[count - 1];

            switch (segmentKind) {
            case SEG_MOVETO:
                x1 = x2;
                y1 = y2;

                break;

            case SEG_LINETO:

                if (rect.intersectsLine(x1, y1, x2, y2)) {
                    return true;
                }

                break;

            case SEG_CLOSE:
                break;

            default:
                throw new RuntimeException(
                        "Illegal segmentKind " + segmentKind);
            }
        }

        return false;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        double[] buffer = new double[6];

        for (PathIterator it = getPathIterator(null); !it.isDone();
                it.next()) {
            int segmentKind = it.currentSegment(buffer);

            sb.append(" ")
                    .append(labelOf(segmentKind))
                    .append("(");

            int coords = countOf(segmentKind);
            boolean firstCoord = true;

            for (int ic = 0; ic < (coords - 1); ic += 2) {
                if (!firstCoord) {
                    sb.append(",");
                    firstCoord = false;
                }

                sb.append("[")
                        .append((float) buffer[ic])
                        .append(",")
                        .append((float) buffer[ic + 1])
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
     * @param y the provided ordinate
     * @return the abscissa value at this ordinate
     */
    public double xAtY (double y)
    {
        final double[] buffer = new double[6];
        final Point2D.Double p1 = new Point2D.Double();
        final Point2D.Double p2 = new Point2D.Double();
        final int segmentKind = getYSegment(y, buffer, p1, p2);
        final double t = (y - p1.y) / (p2.y - p1.y);
        final double u = 1 - t;

        switch (segmentKind) {
        case SEG_LINETO:
            return p1.x + (t * (p2.x - p1.x));

        case SEG_QUADTO: {
            double cpx = buffer[0];

            return (p1.x * u * u) + (2 * cpx * t * u) + (p2.x * t * t);
        }

        case SEG_CUBICTO: {
            double cpx1 = buffer[0];
            double cpx2 = buffer[2];

            return (p1.x * u * u * u) + (3 * cpx1 * t * u * u)
                   + (3 * cpx2 * t * t * u) + (p2.x * t * t * t);
        }

        default:
            throw new RuntimeException("Illegal segmentKind " + segmentKind);
        }
    }

    //------//
    // yAtX //
    //------//
    /**
     * Report the ordinate value of the spline at provided abscissa
     * (assuming true function)
     *
     * @param x the provided abscissa
     * @return the ordinate value at this abscissa
     */
    public double yAtX (double x)
    {
        final double[] buffer = new double[6];
        final Point2D.Double p1 = new Point2D.Double();
        final Point2D.Double p2 = new Point2D.Double();
        final int segmentKind = getXSegment(x, buffer, p1, p2);
        final double t = (x - p1.x) / (p2.x - p1.x);
        final double u = 1 - t;

        switch (segmentKind) {
        case SEG_LINETO:
            return p1.y + (t * (p2.y - p1.y));

        case SEG_QUADTO: {
            double cpy = buffer[1];

            return (p1.y * u * u) + (2 * cpy * t * u) + (p2.y * t * t);
        }

        case SEG_CUBICTO: {
            double cpy1 = buffer[1];
            double cpy2 = buffer[3];

            return (p1.y * u * u * u) + (3 * cpy1 * t * u * u)
                   + (3 * cpy2 * t * t * u) + (p2.y * t * t * t);
        }

        default:
            throw new RuntimeException("Illegal segmentKind " + segmentKind);
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
     * Retrieve the first segment of the curve that contains the provided
     * abscissa
     *
     * @param x      the provided abscissa
     * @param buffer output
     * @param p1     output: start of segment
     * @param p2     output: end of segment
     * @return the segment kind
     */
    protected int getXSegment (double x,
                               double[] buffer,
                               Point2D.Double p1,
                               Point2D.Double p2)
    {
        PathIterator it = getPathIterator(null);
        double x1 = 0;
        double y1 = 0;

        while (!it.isDone()) {
            final int segmentKind = it.currentSegment(buffer);
            final int count = countOf(segmentKind);
            final double x2 = buffer[count - 2];
            final double y2 = buffer[count - 1];

            if ((segmentKind == SEG_MOVETO)
                || (segmentKind == SEG_CLOSE)
                || (x > x2)) {
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
     * Retrieve the first segment of the curve that contains the provided
     * ordinate
     *
     * @param y      the provided ordinate
     * @param buffer output
     * @param p1     output: start of segment
     * @param p2     output: end of segment
     * @return the segment kind
     */
    protected int getYSegment (double y,
                               double[] buffer,
                               Point2D.Double p1,
                               Point2D.Double p2)
    {
        PathIterator it = getPathIterator(null);
        double x1 = 0;
        double y1 = 0;

        while (!it.isDone()) {
            final int segmentKind = it.currentSegment(buffer);
            final int count = countOf(segmentKind);
            final double x2 = buffer[count - 2];
            final double y2 = buffer[count - 1];

            if ((segmentKind == SEG_MOVETO)
                || (segmentKind == SEG_CLOSE)
                || (y > y2)) {
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
