//----------------------------------------------------------------------------//
//                                                                            //
//                         B a s i c L i n e T e s t                          //
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

import org.junit.Test;

/**
 * Class <code>BasicLineTest</code> performs unit tests on BasicLine class.
 *
 * @author Hervé Bitteur
 */
public class BasicLineTest
    extends BaseTestCase
{
    //~ Static fields/initializers ---------------------------------------------

    protected static final double[] xx = new double[] { 1d, 2d, 3d, 4d, 5d };
    protected static final double[] yy = new double[] { 4d, 9d, 14d, 19d, 24d };

    //~ Instance fields --------------------------------------------------------

    BasicLine l = new BasicLine();
    BasicLine l2 = new BasicLine();
    BasicLine lxy = new BasicLine(xx, yy);
    BasicLine l3 = new BasicLine(-0.80343527d, -0.5953921d, 385.66354d);

    //~ Methods ----------------------------------------------------------------
    //@Test (expected = IllegalArgumentException.class)
    public void testDifferentPoints ()
    {
        double[] my_xx = new double[]{1d, 2d};
        double[] my_yy = new double[]{4d, 9d, 14d};

        try {
            BasicLine line = new BasicLine(my_xx, my_yy);
            print(line);
            fail(
                    "Exception should be raised"
                    + " when arrays have different lengths");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testDistanceOf ()
    {
        BasicLineCheck.checkDistanceOf(lxy);
    }

    public void testGetMeanDistance ()
    {
        BasicLineCheck.checkGetMeanDistance(lxy);
    }

    public void testGetNoMeanDistance ()
    {
        BasicLineCheck.checkGetNoMeanDistance(new BasicLine(1.2, 3.4, 1));
    }

    public void testHorizontalPoints ()
    {
        BasicLineCheck.checkHorizontalPoints(l);
    }

    public void testHorizontalXAt ()
    {
        BasicLineCheck.checkHorizontalXAt(l);
    }

    public void testHorizontalYAt ()
    {
        BasicLineCheck.checkHorizontalYAt(l);
    }

    public void testInclude ()
    {
        BasicLineCheck.checkInclude(l);
    }

    public void testIncludeOther ()
    {
        BasicLineCheck.checkIncludeOther(l, l2);
    }

    public void testIncludeOther2 ()
    {
        BasicLineCheck.checkIncludeOther2(l, l2);
    }

    public void testNoArgLine ()
    {
        BasicLineCheck.checkNoArgLine(l);
    }

    public void testNoDistanceOf ()
    {
        BasicLineCheck.checkNoDistanceOf(l);
    }

    public void testNullPoints ()
    {
        try {
            BasicLine l = new BasicLine(xx, null);
            print(l);
            fail("Exception should be raised" + " when one array is null");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testObliquePoints ()
    {
        BasicLineCheck.checkObliquePoints(l);
    }

    // --------------------------------------------------------------------
    public void testPointsLineA ()
    {
        BasicLineCheck.checkPointsLineNb(lxy);
    }

    public void testPointsLineB ()
    {
        BasicLineCheck.checkPointsLineB(lxy);
    }

    public void testPointsLineC ()
    {
        BasicLineCheck.checkPointsLineC(lxy);
    }

    public void testPointsLineNb ()
    {
        BasicLineCheck.checkPointsLineNb(lxy);
    }

    public void testReset ()
    {
        BasicLineCheck.checkReset(l);
    }

    public void testShortPoints ()
    {
        double[] xx = new double[] { 1d };
        double[] yy = new double[] { 4d };

        try {
            BasicLine l = new BasicLine(xx, yy);
            print(l);
            fail("Exception should be raised" + " when arrays are too short");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testSingleInclude ()
    {
        BasicLineCheck.checkSingleInclude(l);
    }

    public void testSingularMeanDistance ()
    {
        double[] xx = new double[] { 1d, 1d, 1d };
        double[] yy = new double[] { 2d, 2d, 2d };

        try {
            BasicLine l = new BasicLine(xx, yy);
            print(l);

            double md = l.getMeanDistance();
            fail(
                "Exception should be raised" +
                " when using a line not properly defined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testSingularPoints ()
    {
        double[] xx = new double[] { 1d, 1d, 1d };
        double[] yy = new double[] { 2d, 2d, 2d };

        try {
            BasicLine l = new BasicLine(xx, yy);
            print(l);
            fail(
                "Exception should be raised" +
                " when line is singularly defined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testStandardLine ()
    {
        double a = -2;
        double b = 1;
        double c = -5;
        BasicLineCheck.checkStandardLine(new BasicLine(a, b, c));
    }

    public void testTangent ()
    {
        BasicLineCheck.checkTangent(l3);
    }

    public void testTangent1 ()
    {
        BasicLineCheck.checkTangent1(l);
    }

    public void testTangent10 ()
    {
        BasicLineCheck.checkTangent10(l);
    }

    public void testTangent2 ()
    {
        BasicLineCheck.checkTangent2(l);
    }

    public void testTangent3 ()
    {
        BasicLineCheck.checkTangent3(l);
    }

    public void testVerticalPoints ()
    {
        l.includePoint(1, 0);
        l.includePoint(1, -2);
        print(l);
    }

    public void testVerticalXAt ()
    {
        BasicLineCheck.checkVerticalXAt(l);
    }

    public void testVerticalYAt ()
    {
        try {
            l.includePoint(0, 0);
            l.includePoint(0, 1);

            double y = l.yAtX(0d);
            fail(
                "Exception should be raised" +
                " when Yat is called on a vertical line");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testXAt ()
    {
        BasicLineCheck.checkXAt(l);
    }

    public void testXAtInt ()
    {
        BasicLineCheck.checkXAtInt(l);
    }

    public void testYAt ()
    {
        BasicLineCheck.checkYAt(l);
    }

    public void testYAtInt ()
    {
        BasicLineCheck.checkYAtInt(l);
    }
}
