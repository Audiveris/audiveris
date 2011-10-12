//----------------------------------------------------------------------------//
//                                                                            //
//                        B a s i c L i n e C h e c k                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import omr.util.BaseTestCase;
import static junit.framework.Assert.*;
import static java.lang.Math.*;

/**
 * Class <code>BasicLineCheck</code> gathers test bodies for various unit tests
 * on BasicLine class.
 *
 * @author Hervé Bitteur
 */
public class BasicLineCheck
    extends BaseTestCase
{
    //~ Methods ----------------------------------------------------------------

    public static void checkDistanceOf (BasicLine l)
    {
        print(l);

        double d = l.distanceOf(0, 0);
        System.out.println("Distance of origin: " + d);
        assertNears("Distance of origin", 0.19611613513818404d, abs(d));
    }

    public static void checkGetMeanDistance (BasicLine l)
    {
        print(l);

        double md = l.getMeanDistance();
        System.out.println(" md=" + md);
        assertNears("Check zero mean distance", 0d, md);
    }

    public static void checkGetNoMeanDistance (BasicLine l)
    {
        try {
            double md = l.getMeanDistance();
            print(l);
            fail(
                "Exception should be raised" +
                " when less than 2 points are known");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkHorizontalPoints (BasicLine l)
    {
        l.includePoint(0, 1);
        l.includePoint(-2, 1);
        print(l);
    }

    public static void checkHorizontalXAt (BasicLine l)
    {
        l.includePoint(0, 0);
        l.includePoint(1, 0);
        print(l);

        try {
            double x = l.xAtY(0d);
            System.out.println("at y=0, x=" + x);
            fail(
                "Exception should be raised" +
                " when abscissa cannot be determined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkHorizontalYAt (BasicLine l)
    {
        l.includePoint(0, 0);
        l.includePoint(1, 0);
        print(l);

        double y = l.yAtX(0d);
        System.out.println("at x=0, y=" + y);
    }

    public static void checkInclude (BasicLine l)
    {
        l.includePoint(1d, 3d);
        l.includePoint(-1d, 1d);
        print(l);
        assertEquals("Number of points", 2, l.getNumberOfPoints());
        assertNears("A", sqrt(0.5), l.getA());
        assertNears("B", -sqrt(0.5), l.getB());
        assertNears("C", sqrt(2.0), l.getC());
    }

    public static void checkIncludeOther (BasicLine l,
                                          BasicLine o)
    {
        double base = -100d;
        int    half = 1;

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
        assertNears("B coeff", -1d, l.getB(), 1E-3);
        assertNears("C coeff", 0d, l.getC(), 1E-3);
    }

    public static void checkIncludeOther2 (BasicLine l,
                                           BasicLine o)
    {
        double base = -100d;
        int    half = 10;

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
        assertNears("B coeff", -1d, l.getB(), 1E-3);
        assertNears("C coeff", 0d, l.getC(), 1E-3);
    }

    public static void checkNoArgLine (BasicLine l)
    {
        assertEquals("No defining points", 0, l.getNumberOfPoints());
        assertParamsUndefined(l);
    }

    public static void checkNoDistanceOf (BasicLine l)
    {
        try {
            double d = l.distanceOf(0, 0);
            fail(
                "Exception should be raised" +
                " when line parameters are not set");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkObliquePoints (BasicLine l)
    {
        l.includePoint(0, 0);
        l.includePoint(1, 1);
        print(l);
    }

    // --------------------------------------------------------------------
    public static void checkPointsLineA (BasicLine l)
    {
        System.out.println("Points BasicLine: " + l);
        assertNears("Check A", -0.9805806756909202d, l.getA());
    }

    public static void checkPointsLineB (BasicLine l)
    {
        print(l);
        assertNears("Check B", 0.19611613513818377d, l.getB());
    }

    public static void checkPointsLineC (BasicLine l)
    {
        print(l);
        assertNears("Check C", 0.1961161351381877d, l.getC());
    }

    public static void checkPointsLineNb (BasicLine l)
    {
        print(l);
        assertEquals("Five defining points", 5, l.getNumberOfPoints());
    }

    public static void checkReset (BasicLine l)
    {
        l.includePoint(12, 34);
        l.includePoint(56, 78);
        l.reset();
        assertEquals("Number of points", 0, l.getNumberOfPoints());
        assertParamsUndefined(l);
    }

    public static void checkSingleInclude (BasicLine l)
    {
        l.includePoint(1d, 3d);
        assertEquals("Number of points", 1, l.getNumberOfPoints());

        try {
            print(l);

            double d = l.distanceOf(0, 0);
            fail(
                "Exception should be raised" +
                " when line parameters are not set");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkStandardLine (BasicLine l)
    {
        double a = -2;
        double b = 1;
        double c = -5;

        print(l);

        double norm = hypot(a, b);
        assertEquals("No defining points", 0, l.getNumberOfPoints());
        assertEquals("Check a", a / norm, l.getA());
        assertEquals("Check b", b / norm, l.getB());
        assertEquals("Check c", c / norm, l.getC());
    }

    public static void checkTangent (BasicLine l)
    {
        print(l);

        int y;
        y = 322;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 241, l.xAtY(y));
        y = 323;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 241, l.xAtY(y));
        y = 324;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 240, l.xAtY(y));
    }

    public static void checkTangent1 (BasicLine l)
    {
        l.includePoint(214, 624);
        l.includePoint(215, 625);
        l.includePoint(215, 626);
        l.includePoint(215, 627);
        print(l);

        double y;
        y = 624;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 214.3, l.xAtY(y));
        y = 625;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 214.6, l.xAtY(y));
        y = 626;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 214.9, l.xAtY(y));
        y = 627;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 215.2, l.xAtY(y));
    }

    public static void checkTangent10 (BasicLine l)
    {
        l.includePoint(-1, 4);
        l.includePoint(0, 5);
        l.includePoint(0, 6);
        l.includePoint(0, 7);
        print(l);

        double y;
        y = 4;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, -0.7, l.xAtY(y));
        y = 5;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, -0.4, l.xAtY(y));
        y = 6;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, -0.1, l.xAtY(y));
        y = 7;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 0.2, l.xAtY(y));
    }

    public static void checkTangent2 (BasicLine l)
    {
        l.includePoint(215, 627);
        l.includePoint(215, 626);
        l.includePoint(215, 625);
        l.includePoint(214, 624);
        print(l);

        double y;
        y = 624;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 214.3, l.xAtY(y));
        y = 625;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 214.6, l.xAtY(y));
        y = 626;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 214.9, l.xAtY(y));
        y = 627;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 215.2, l.xAtY(y));
        y = 628;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 215.5, l.xAtY(y));
        System.out.println();
        y = 629;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 215.8, l.xAtY(y));
        y = 630;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 216.1, l.xAtY(y));
        y = 631;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 216.4, l.xAtY(y));
    }

    public static void checkTangent3 (BasicLine l)
    {
        l.includePoint(222, 627);
        l.includePoint(222, 626);
        l.includePoint(222, 625);
        l.includePoint(221, 624);
        print(l);

        double y;
        y = 624;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 221.3, l.xAtY(y));
        y = 625;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 221.6, l.xAtY(y));
        y = 626;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 221.9, l.xAtY(y));
        y = 627;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 222.2, l.xAtY(y));
        y = 628;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 222.5, l.xAtY(y));
        System.out.println();
        y = 629;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 222.8, l.xAtY(y));
        y = 630;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 223.1, l.xAtY(y));
        y = 631;
        System.out.println("x=" + l.xAtY(y) + ", y=" + y);
        assertNears("xAt " + y, 223.4, l.xAtY(y));
    }

    public static void checkVerticalPoints (BasicLine l)
    {
        l.includePoint(1, 0);
        l.includePoint(1, -2);
        print(l);
    }

    public static void checkVerticalXAt (BasicLine l)
    {
        l.includePoint(0, 0);
        l.includePoint(0, 1);
        assertNears("Ordinate axis", 0d, l.xAtY(0d));
    }

    public static void checkVerticalYAt (BasicLine l)
    {
        l.includePoint(0, 0);
        l.includePoint(0, 1);
        print(l);

        try {
            double y = l.yAtX(0d);
            System.out.println("at x=0, y=" + y);
            fail(
                "Exception should be raised" +
                " when ordinate cannot be determined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public static void checkXAt (BasicLine l)
    {
        l.includePoint(1d, 3d);
        l.includePoint(-1d, 1d);
        print(l);
        assertNears("Crossing x axis", -2d, l.xAtY(0d));
        assertNears("Crossing y axis", 0d, l.xAtY(2d));
    }

    public static void checkXAtInt (BasicLine l)
    {
        l.includePoint(0, 0);
        l.includePoint(2, 5);
        print(l);
        assertEquals("Rounding test.", 1, l.xAtY(3));
    }

    public static void checkYAt (BasicLine l)
    {
        l.includePoint(1d, 3d);
        l.includePoint(-1d, 1d);
        print(l);
        assertNears("Crossing x axis", 0d, l.yAtX(-2d));
        assertNears("Crossing y axis", 2d, l.yAtX(0d));
    }

    public static void checkYAtInt (BasicLine l)
    {
        l.includePoint(0, 0);
        l.includePoint(5, 2);
        print(l);
        assertEquals("Rounding test.", 2, l.yAtX(4));
    }

    //-----------------------//
    // assertParamsUndefined //
    //-----------------------//
    protected static void assertParamsUndefined (BasicLine l)
    {
        try {
            double a = l.getA();
            fail(
                "Exception should be raised" +
                " when parameter A is undefined");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            double a = l.getB();
            fail(
                "Exception should be raised" +
                " when parameter B is undefined");
        } catch (Exception expected) {
            checkException(expected);
        }

        try {
            double a = l.getC();
            fail(
                "Exception should be raised" +
                " when parameter C is undefined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }
}
