//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            G e o m e t r i c M o m e n t s T e s t                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

//import org.testng.annotations.*;
import org.audiveris.omr.moments.GeometricMoments;
import org.audiveris.omr.util.BaseTestCase;

import org.junit.Test;

/**
 * Class <code>GeometricMomentsTest</code> performs unit tests on
 * GeometricMoments class.
 *
 * @author Hervé Bitteur
 */
public class GeometricMomentsTest
        extends BaseTestCase
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final int[] xx = new int[]{1, 2, 3, 4, 5};

    private static final int[] yy = new int[]{4, 5, 24, 9, 0};

    //~ Methods ------------------------------------------------------------------------------------
    @Test
    public void testConstructor ()
    {
        GeometricMoments m = new GeometricMoments(xx, yy, xx.length, 1);
        print(m);

        double[] k = m.getValues();

        assertNears("weight", k[0], 5.00000);
        assertNears("width", k[1], 5.00000);
        assertNears("height", k[2], 25.0000);

        assertNears("n20", k[3], 0.400000);
        assertNears("n11", k[4], -0.160000);
        assertNears("n02", k[5], 13.8080);
        assertNears("n30", k[6], 0.00000);
        assertNears("n21", k[7], -0.965981);
        assertNears("n12", k[8], 1.63144);
        assertNears("n03", k[9], 55.0867);

        assertNears("xBar", k[10], 3.00000);
        assertNears("yBar", k[11], 8.40000);

        if (GeometricMoments.useHuCoefficients) {
            assertNears("h1", k[12], 14.2080);
            assertNears("h2", k[13], 179.876864);
            assertNears("h3", k[14], 3386.172875);
            assertNears("h4", k[15], 2931.71348);
            assertNears("h5", k[16], 9236948.170205);
            assertNears("h6", k[17], 39180.53298);
            assertNears("h7", k[18], 57255.22444);
        }
    }

    @Test
    public void testDefaultConstructor ()
    {
        GeometricMoments m = new GeometricMoments();
        print(m);
    }
}
