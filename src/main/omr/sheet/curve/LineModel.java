//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L i n e M o d e l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import omr.math.BasicLine;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code LineModel} implements a curve model as a straight line.
 *
 * @author Hervé Bitteur
 */
public class LineModel
        implements Model
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying line. */
    private final BasicLine line;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new LineModel object.
     *
     * @param points the sequence of defining points
     */
    public LineModel (List<? extends Point2D> points)
    {
        line = new BasicLine(points);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int above ()
    {
        return 0;
    }

    @Override
    public int ccw ()
    {
        return 0;
    }

    @Override
    public double computeDistance (Collection<? extends Point2D> points)
    {
        if (points.isEmpty()) {
            return 0;
        }

        double sum = 0;

        for (Point2D p : points) {
            double d = line.distanceOf(p);
            sum += (d * d);
        }

        sum /= points.size();

        return Math.sqrt(sum);
    }

    @Override
    public Double getAngle (boolean reverse)
    {
        return Math.atan(line.getSlope());
    }

    @Override
    public Rectangle getBounds ()
    {
        return line.getBounds();
    }

    @Override
    public Shape getCurve ()
    {
        return line.toDouble();
    }

    @Override
    public double getDistance ()
    {
        return line.getMeanDistance();
    }

    @Override
    public Point2D getEndVector (boolean reverse)
    {
        int dir = reverse ? (-1) : 1;
        Line2D l = line.toDouble();
        double length = l.getP1().distance(l.getP2());

        return new Point2D.Double(
                (dir * (l.getX2() - l.getX1())) / length,
                (dir * (l.getY2() - l.getY1())) / length);
    }

    @Override
    public Point2D getMidPoint ()
    {
        Line2D l = line.toDouble();

        return new Point2D.Double((l.getX1() + l.getX2()) / 2, (l.getY1() + l.getY2()) / 2);
    }

    @Override
    public void reverse ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDistance (double dist)
    {
        // void?
    }
}
