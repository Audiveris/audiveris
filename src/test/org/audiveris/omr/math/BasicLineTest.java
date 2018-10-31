//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B a s i c L i n e T e s t                                    //
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

import static junit.framework.Assert.*;

import org.audiveris.omr.util.BaseTestCase;

/**
 * Class <code>BasicLineTest</code> performs unit tests on BasicLine class.
 *
 * @author Hervé Bitteur
 */
public class BasicLineTest
        extends BaseTestCase
{
    //~ Static fields/initializers -----------------------------------------------------------------

    protected static final double[] xx = new double[]{1d, 2d, 3d, 4d, 5d};

    protected static final double[] yy = new double[]{4d, 9d, 14d, 19d, 24d};

    //~ Instance fields ----------------------------------------------------------------------------
    BasicLine l;

    BasicLine l2;

    BasicLine lxy;

    //~ Methods ------------------------------------------------------------------------------------
    //@Test (expected = IllegalArgumentException.class)
    public void testDifferentPoints ()
    {
        double[] my_xx = new double[]{1d, 2d};
        double[] my_yy = new double[]{4d, 9d, 14d};

        try {
            BasicLine line = new BasicLine(my_xx, my_yy);
            print(line);
            fail("Exception should be raised" + " when arrays have different lengths");
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
        double[] xx = new double[]{1d};
        double[] yy = new double[]{4d};

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
        double[] xx = new double[]{1d, 1d, 1d};
        double[] yy = new double[]{2d, 2d, 2d};

        try {
            BasicLine l = new BasicLine(xx, yy);
            print(l);

            l.getMeanDistance();
            fail("Exception should be raised when using a line not properly defined");
        } catch (Exception expected) {
            checkException(expected);
        }
    }

    public void testSingularPoints ()
    {
        double[] xx = new double[]{1d, 1d, 1d};
        double[] yy = new double[]{2d, 2d, 2d};

        try {
            BasicLine l = new BasicLine(xx, yy);
            print(l);
            fail("Exception should be raised when line is singularly defined");
        } catch (Exception expected) {
            checkException(expected);
        }
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

            l.yAtX(0d);
            fail("Exception should be raised" + " when Yat is called on a vertical line");
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

    @Override
    protected void setUp ()
            throws Exception
    {
        System.out.println("BasicLineTest setUp() called.");

        l = new BasicLine();
        l2 = new BasicLine();
        lxy = new BasicLine(xx, yy);
    }
}
