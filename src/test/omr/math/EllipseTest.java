/*
 * EllipseTest.java
 * JUnit based test
 *
 * Created on 5 janvier 2007, 21:39
 */
package omr.math;

import junit.framework.*;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hb115668
 */
public class EllipseTest
    extends TestCase
{
    //~ Instance fields --------------------------------------------------------

    private final double a = 100;
    private final double b = 100;
    private final double dx = 2000;
    private final double dy = 2000;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new EllipseTest object.
     *
     * @param testName DOCUMENT ME!
     */
    public EllipseTest (String testName)
    {
        super(testName);
    }

    //~ Methods ----------------------------------------------------------------

    public static void main (String... notUsed)
    {
        EllipseTest test = new EllipseTest("EllipseTest");
        test.testFit();
    }

    /**
     * Test of fit method, of class omr.math.Ellipse.
     */
    public void testFit ()
    {
        List<Point2D.Double> points = new ArrayList<Point2D.Double>();

        for (double i = -a; i <= a; i += 1) {
            points.add(new Point2D.Double(i + dx, dy + getY(i)));
            points.add(new Point2D.Double(i + dx, dy - getY(i)));
        }

        double[] x = new double[points.size()];
        double[] y = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            Point2D.Double point = points.get(i);
            x[i] = point.x;
            y[i] = point.y;
        }

        Ellipse instance = new Ellipse();
        instance.fit(x, y);
    }

    private double getY (double x)
    {
        return (b / a) * Math.sqrt((a * a) - (x * x));
    }
}
