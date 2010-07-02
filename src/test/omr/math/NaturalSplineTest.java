/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.math;

import omr.util.BaseTestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Etiolles
 */
public class NaturalSplineTest
    extends BaseTestCase
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NaturalSplineTest object.
     */
    public NaturalSplineTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    @BeforeClass
    public static void setUpClass ()
        throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass ()
        throws Exception
    {
    }

    @Before
    public void setUp ()
    {
    }

    @After
    public void tearDown ()
    {
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolate0 ()
    {
        int[] xx = new int[] {  };
        int[] yy = new int[] {  };

        try {
            NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
            fail("Exception should be raised when no points are defined");
            System.out.println(spline.toString());
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolate1 ()
    {
        int[] xx = new int[] { 1 };
        int[] yy = new int[] { 1 };

        try {
            NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
            fail("Exception should be raised when only one point is defined");
            System.out.println(spline.toString());
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolate4 ()
    {
        int[]         xx = new int[] { 0, 12, 19, 30 };
        int[]         yy = new int[] { 0, 1, 2, 3 };
        NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
        System.out.println(spline.toString());

        for (int x = xx[0]; x <= xx[xx.length - 1]; x++) {
            double y = spline.yAt(x);
            System.out.println("x=" + x + " y=" + (float) y);
        }
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolate5 ()
    {
        int[]         xx = new int[] { 1, 11, 20, 30, 40 };
        int[]         yy = new int[] { 1, 2, 3, 4, 3 };
        NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
        System.out.println(spline.toString());

        for (int x = xx[0]; x <= xx[xx.length - 1]; x++) {
            double y = spline.yAt(x);
            System.out.println("x=" + x + " y=" + (float) y);
        }
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolateDiff ()
    {
        int[] xx = new int[] {  };
        int[] yy = new int[] { 1 };

        try {
            NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
            fail("Exception should be raised when no points are defined");
            System.out.println(spline.toString());
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolateFour ()
    {
        int[]         xx = new int[] { 0, 10, 10, 20 };
        int[]         yy = new int[] { 0, 2, 2, 2 };
        NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
        System.out.println(spline.toString());

        for (int x = xx[0]; x <= xx[xx.length - 1]; x++) {
            double y = spline.yAt(x);
            System.out.println("x=" + x + " y=" + (float) y);
        }
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolateLine ()
    {
        int[]         xx = new int[] { 0, 10 };
        int[]         yy = new int[] { 0, 1 };
        NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
        System.out.println(spline.toString());

        for (int x = xx[0]; x <= xx[xx.length - 1]; x++) {
            double y = spline.yAt(x);
            System.out.println("x=" + x + " y=" + (float) y);
        }
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolateNull ()
    {
        int[] xx = new int[] {  };
        int[] yy = null;

        try {
            NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
            fail("Exception should be raised when no points are defined");
            System.out.println(spline.toString());
        } catch (IllegalArgumentException expected) {
            checkException(expected);
        }
    }

    /**
     * Test of interpolate method, of class NaturalSpline.
     */
    @Test
    public void testInterpolateQuad ()
    {
        int[]         xx = new int[] { 0, 20, 30 };
        int[]         yy = new int[] { 0, 10, 10};
        NaturalSpline spline = NaturalSpline.interpolate(xx, yy);
        System.out.println(spline.toString());

        for (int x = xx[0]; x <= xx[xx.length - 1]; x++) {
            double y = spline.yAt(x);
            System.out.println("x=" + x + " y=" + (float) y);
        }
    }
}
