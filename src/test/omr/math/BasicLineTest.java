//-----------------------------------------------------------------------//
//                                                                       //
//                       B a s i c L i n e T e s t                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.math;

import omr.util.BaseTestCase;

import static junit.framework.Assert.*;
import junit.framework.*;

/**
 * Class <code>BasicLineTest</code> performs unit tests on BasicLine class.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BasicLineTest
    extends BaseTestCase
{
    //~ Static variables/initializers -------------------------------------

    protected static final double[] xx = new double[] {1d,  2d,  3d,  4d,  5d};
    protected static final double[] yy = new double[] {4d,  9d, 14d, 19d, 24d};

    //~ Instance variables ------------------------------------------------

    Line l   = new BasicLine();
    Line l2  = new BasicLine();
    Line lxy = new BasicLine(xx, yy);
    Line l3 = new BasicLine(-0.80343527d,
                            -0.5953921d,
                            385.66354d);

    //~ Constructors ------------------------------------------------------

    //~ Methods -----------------------------------------------------------

    public void testNoArgLine ()
    {
        LineCheck.checkNoArgLine(l);
    }

    public void testStandardLine ()
    {
        double a = -2;
        double b = 1;
        double c = -5;
        LineCheck.checkStandardLine(new BasicLine(a, b, c));
    }

    public void testNullPoints()
    {
        try {
            Line l = new BasicLine(xx, null);
            print(l);
            fail("Exception should be raised"+
                 " when one array is null");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testDifferentPoints()
    {
        double[] xx = new double[] {1d,  2d};
        double[] yy = new double[] {4d,  9d, 14d};
        try {
            Line l = new BasicLine(xx, yy);
            print(l);
            fail("Exception should be raised"+
                 " when arrays have different lengths");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testShortPoints()
    {
        double[] xx = new double[] {1d};
        double[] yy = new double[] {4d};
        try {
            Line l = new BasicLine(xx, yy);
            print(l);
            fail("Exception should be raised"+
                 " when arrays are too short");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testPointsLineNb ()
    {
        LineCheck.checkPointsLineNb(lxy);
    }

    public void testReset()
    {
        LineCheck.checkReset(l);
    }

    public void testSingularPoints()
    {
        double[] xx = new double[] {1d, 1d, 1d};
        double[] yy = new double[] {2d, 2d, 2d};
        try {
            Line l = new BasicLine(xx, yy);
            print(l);
            fail("Exception should be raised"+
                 " when line is singularly defined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testSingularMeanDistance ()
    {
        double[] xx = new double[] {1d, 1d, 1d};
        double[] yy = new double[] {2d, 2d, 2d};
        try {
            Line l = new BasicLine(xx, yy);
            print(l);
            double md = l.getMeanDistance();
            fail("Exception should be raised"+
                 " when using a line not properly defined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testDistanceOf()
    {
        LineCheck.checkDistanceOf(lxy);
    }

    public void testNoDistanceOf()
    {
        LineCheck.checkNoDistanceOf(l);
    }

    public void testSingleInclude()
    {
        LineCheck.checkSingleInclude(l);
    }

    // --------------------------------------------------------------------

    public void testPointsLineA ()
    {
        LineCheck.checkPointsLineNb(lxy);
    }

    public void testPointsLineB ()
    {
        LineCheck.checkPointsLineB(lxy);
    }

    public void testPointsLineC()
    {
        LineCheck.checkPointsLineC(lxy);
    }

    public void testObliquePoints()
    {
        LineCheck.checkObliquePoints(l);
    }

    public void testVerticalPoints()
    {
        l.includePoint(1, 0);
        l.includePoint(1,-2);
        print(l);
    }

    public void testHorizontalPoints()
    {
        LineCheck.checkHorizontalPoints(l);
    }

    public void testGetMeanDistance()
    {
        LineCheck.checkGetMeanDistance(lxy);
    }

    public void testGetNoMeanDistance ()
    {
        LineCheck.checkGetNoMeanDistance(new BasicLine(1.2, 3.4, 1));
    }

    public void testInclude()
    {
        LineCheck.checkInclude(l);
    }

    public void testIncludeOther()
    {
        LineCheck.checkIncludeOther(l, l2);
    }

    public void testIncludeOther2()
    {
        LineCheck.checkIncludeOther2(l, l2);
    }

    public void testXAt()
    {
        LineCheck.checkXAt(l);
    }

    public void testYAt()
    {
        LineCheck.checkYAt(l);
    }

    public void testVerticalYAt()
    {
        try {
            l.includePoint(0, 0);
            l.includePoint(0, 1);
            double y = l.yAt(0d);
            fail("Exception should be raised"+
                 " when Yat is called on a vertical line");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testVerticalXAt()
    {
        LineCheck.checkVerticalXAt(l);
    }

    public void testHorizontalXAt()
    {
        LineCheck.checkHorizontalXAt(l);
    }

    public void testHorizontalYAt()
    {
        LineCheck.checkHorizontalYAt(l);
    }

    public void testXAtInt()
    {
        LineCheck.checkXAtInt(l);
    }

    public void testYAtInt()
    {
        LineCheck.checkYAtInt(l);
    }

    public void testTangent()
    {
        LineCheck.checkTangent(l3);
    }

    public void testTangent1()
    {
        LineCheck.checkTangent1(l);
    }

    public void testTangent10()
    {
        LineCheck.checkTangent10(l);
    }

    public void testTangent2()
    {
        LineCheck.checkTangent2(l);
    }

    public void testTangent3()
    {
        LineCheck.checkTangent3(l);
    }
}
