//-----------------------------------------------------------------------//
//                                                                       //
//                           L i n e C h e c k                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.math;

import omr.util.BaseTestCase;

import static java.lang.Math.*;

import static junit.framework.Assert.*;
import junit.framework.*;

/**
 * Class <code>LineCheck</code> gathers test bodies for various unit tests
 * on Line class.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class LineCheck
    extends BaseTestCase
{
    //~ Static variables/initializers -------------------------------------

    protected static final double[] xx = new double[] {1d,  2d,  3d,  4d,  5d};
    protected static final double[] yy = new double[] {4d,  9d, 14d, 19d, 24d};

    //~ Instance variables ------------------------------------------------

    //~ Constructors ------------------------------------------------------

    //~ Methods -----------------------------------------------------------

    //-----------------------//
    // assertParamsUndefined //
    //-----------------------//
    protected static void assertParamsUndefined(Line l)
    {
        try {
            double a = l.getA();
            fail("Exception should be raised"+
                 " when parameter A is undefined");
        } catch (Exception expected) {
            checkException(expected);
        }
        try {
            double a = l.getB();
            fail("Exception should be raised"+
                 " when parameter B is undefined");
        } catch (Exception expected) {
            checkException(expected);
        }
        try {
            double a = l.getC();
            fail("Exception should be raised"+
                 " when parameter C is undefined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkNoArgLine (Line l)
    {
        assertEquals("No defining points",
                     0, l.getNumberOfPoints());
        assertParamsUndefined(l);
    }

    public static void checkStandardLine (Line l)
    {
        double a = -2;
        double b = 1;
        double c = -5;

        print(l);
        double norm = hypot(a, b);
        assertEquals("No defining points",
                     0, l.getNumberOfPoints());
        assertEquals("Check a", a/norm, l.getA());
        assertEquals("Check b", b/norm, l.getB());
        assertEquals("Check c", c/norm, l.getC());
    }

    public static void checkPointsLineNb (Line l)
    {
        print(l);
        assertEquals("Five defining points",
                     5, l.getNumberOfPoints());
    }

    public static void checkReset(Line l)
    {
        l.includePoint(12, 34);
        l.includePoint(56, 78);
        l.reset();
        assertEquals("Number of points", 0, l.getNumberOfPoints());
        assertParamsUndefined(l);
    }

    public static void checkDistanceOf(Line l)
    {
        print(l);
        double d = l.distanceOf(0,0);
        System.out.println("Distance of origin: " + d);
        assertNears("Distance of origin",
                    0.19611613513818404d, abs(d));
    }

    public static void checkNoDistanceOf(Line l)
    {
        try {
            double d = l.distanceOf(0,0);
            fail("Exception should be raised"+
                 " when line parameters are not set");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkSingleInclude(Line l)
    {
        l.includePoint( 1d, 3d);
        assertEquals("Number of points", 1, l.getNumberOfPoints());
        try {
            print(l);
            double d = l.distanceOf(0,0);
            fail("Exception should be raised"+
                 " when line parameters are not set");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    // --------------------------------------------------------------------

    public static void checkPointsLineA (Line l)
    {
        System.out.println("Points Line: " + l);
        assertNears("Check A", -0.9805806756909202d, l.getA());
    }

    public static void checkPointsLineB (Line l)
    {
        print(l);
        assertNears("Check B", 0.19611613513818377d, l.getB());
    }

    public static void checkPointsLineC (Line l)
    {
        print(l);
        assertNears("Check C",  0.1961161351381877d, l.getC());
    }

    public static void checkObliquePoints(Line l)
    {
        l.includePoint(0,0);
        l.includePoint(1,1);
        print(l);
    }

    public static void checkVerticalPoints(Line l)
    {
        l.includePoint(1, 0);
        l.includePoint(1,-2);
        print(l);
    }

    public static void checkHorizontalPoints(Line l)
    {
        l.includePoint( 0, 1);
        l.includePoint(-2, 1);
        print(l);
    }

    public static void checkGetMeanDistance (Line l)
    {
        print(l);
        double md = l.getMeanDistance();
        System.out.println(" md=" + md);
        assertNears("Check zero mean distance", 0d, md);
    }

    public static void checkGetNoMeanDistance(Line l)
    {
        try {
            double md = l.getMeanDistance();
            print(l);
            fail("Exception should be raised"+
                 " when less than 2 points are known");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkInclude(Line l)
    {
        l.includePoint( 1d, 3d);
        l.includePoint(-1d, 1d);
        print(l);
        assertEquals("Number of points", 2, l.getNumberOfPoints());
        assertNears("A",   sqrt(0.5), l.getA());
        assertNears("B", - sqrt(0.5), l.getB());
        assertNears("C",   sqrt(2.0), l.getC());
    }

    public static void checkIncludeOther(Line l, Line o)
    {
        double base = -100d;
        int half = 1;
        for (int i = -half; i <= half; i++) {
            l.includePoint(base + i, -1d);
        }
        print(l);

        base = -base;
        for (int i = -half; i <= half; i++) {
            o.includePoint(base + i, 1d);
        }
        print(o);

        l.includeLine(o);
        print(l);
        assertNears("A coeff", 0.01d, l.getA(), 1E-3);
        assertNears("B coeff",   -1d, l.getB(), 1E-3);
        assertNears("C coeff",    0d, l.getC(), 1E-3);
    }

    public static void checkIncludeOther2(Line l, Line o)
    {
        double base = -100d;
        int half = 10;
        for (int i = -half; i <= half; i++) {
            l.includePoint(base + i, -1d);
        }
        print(l);

        for (int i = -half; i <= half; i++) {
            o.includePoint(base + i, 1d);
        }
        print(o);

        l.includeLine(o);
        print(l);
        assertNears("A coeff", 0d, l.getA(), 1E-3);
        assertNears("B coeff",-1d, l.getB(), 1E-3);
        assertNears("C coeff", 0d, l.getC(), 1E-3);
    }

    public static void checkXAt(Line l)
    {
        l.includePoint( 1d, 3d);
        l.includePoint(-1d, 1d);
        print(l);
        assertNears("Crossing x axis", -2d, l.xAt(0d));
        assertNears("Crossing y axis",  0d, l.xAt(2d));
    }

    public static void checkYAt(Line l)
    {
        l.includePoint( 1d, 3d);
        l.includePoint(-1d, 1d);
        print(l);
        assertNears("Crossing x axis", 0d, l.yAt(-2d));
        assertNears("Crossing y axis", 2d, l.yAt( 0d));
    }

    public static void checkVerticalYAt(Line l)
    {
        l.includePoint(0, 0);
        l.includePoint(0, 1);
        print(l);
        try {
            double y = l.yAt(0d);
            System.out.println("at x=0, y=" + y);
            fail("Exception should be raised"+
                 " when ordinate cannot be determined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkVerticalXAt(Line l)
    {
        l.includePoint(0, 0);
        l.includePoint(0, 1);
        assertNears("Ordinate axis", 0d, l.xAt(0d));
    }

    public static void checkHorizontalXAt(Line l)
    {
        l.includePoint(0, 0);
        l.includePoint(1, 0);
        print(l);
        try {
            double x = l.xAt(0d);
            System.out.println("at y=0, x=" + x);
            fail("Exception should be raised"+
                 " when abscissa cannot be determined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkHorizontalYAt(Line l)
    {
        l.includePoint(0, 0);
        l.includePoint(1, 0);
        print(l);
        double y = l.yAt(0d);
        System.out.println("at x=0, y=" + y);
    }

    public static void checkXAtInt(Line l)
    {
        l.includePoint(0, 0);
        l.includePoint(2, 5);
        print(l);
        assertEquals("Rounding test.", 1, l.xAt(3));
    }

    public static void checkYAtInt(Line l)
    {
        l.includePoint(0, 0);
        l.includePoint(5, 2);
        print(l);
        assertEquals("Rounding test.", 2, l.yAt(4));
    }

    public static void checkTangent(Line l)
    {
        print(l);
        int y;
        y = 322; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 241, l.xAt(y));
        y = 323; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 241, l.xAt(y));
        y = 324; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 240, l.xAt(y));
    }

    public static void checkTangent1(Line l)
    {
        l.includePoint(214, 624);
        l.includePoint(215, 625);
        l.includePoint(215, 626);
        l.includePoint(215, 627);
        print(l);
        double y;
        y = 624; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 214.3, l.xAt(y));
        y = 625; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 214.6, l.xAt(y));
        y = 626; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 214.9, l.xAt(y));
        y = 627; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 215.2, l.xAt(y));
    }

    public static void checkTangent10(Line l)
    {
        l.includePoint(-1, 4);
        l.includePoint(0, 5);
        l.includePoint(0, 6);
        l.includePoint(0, 7);
        print(l);
        double y;
        y = 4; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, -0.7, l.xAt(y));
        y = 5; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, -0.4, l.xAt(y));
        y = 6; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, -0.1, l.xAt(y));
        y = 7; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 0.2, l.xAt(y));
    }

    public static void checkTangent2(Line l)
    {
        l.includePoint(215, 627);
        l.includePoint(215, 626);
        l.includePoint(215, 625);
        l.includePoint(214, 624);
        print(l);
        double y;
        y = 624; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 214.3, l.xAt(y));
        y = 625; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 214.6, l.xAt(y));
        y = 626; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 214.9, l.xAt(y));
        y = 627; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 215.2, l.xAt(y));
        y = 628; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 215.5, l.xAt(y));
        System.out.println();
        y = 629; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 215.8, l.xAt(y));
        y = 630; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 216.1, l.xAt(y));
        y = 631; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 216.4, l.xAt(y));
    }

    public static void checkTangent3(Line l)
    {
        l.includePoint(222, 627);
        l.includePoint(222, 626);
        l.includePoint(222, 625);
        l.includePoint(221, 624);
        print(l);
        double y;
        y = 624; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 221.3, l.xAt(y));
        y = 625; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 221.6, l.xAt(y));
        y = 626; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 221.9, l.xAt(y));
        y = 627; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 222.2, l.xAt(y));
        y = 628; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 222.5, l.xAt(y));
        System.out.println();
        y = 629; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 222.8, l.xAt(y));
        y = 630; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 223.1, l.xAt(y));
        y = 631; System.out.println("x=" + l.xAt(y) + ", y=" + y);
        assertNears("xAt " + y, 223.4, l.xAt(y));
    }
}
