//-----------------------------------------------------------------------//
//                                                                       //
//                         M o m e n t s T e s t                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.math;

//import org.testng.annotations.*;
import omr.util.BaseTestCase;

import static junit.framework.Assert.*;
import junit.framework.*;

/**
 * Class <code>MomentsTest</code> performs unit tests on Moments class.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class MomentsTest
    extends BaseTestCase
{
    //~ Static variables/initializers -------------------------------------

    private static final int[] xx = new int[] {1,  2,  3,  4,  5};
    private static final int[] yy = new int[] {4,  5, 24,  9,  0};

    //~ Instance variables ------------------------------------------------

    //~ Constructors ------------------------------------------------------

    //~ Methods -----------------------------------------------------------

    //@Test
        public void testDefaultConstructor ()
    {

        Moments m = new Moments();
        print(m);
    }

    //@Test
        public void testConstructor()
    {

        Moments m = new Moments(xx, yy, xx.length, 1);
        print(m);
        Double[] k = m.getValues();

        assertNears("weight", k[0],  5.00000);
        assertNears("width",  k[1],  5.00000);
        assertNears("height", k[2],  25.0000);
        assertNears("n20",    k[3],  0.400000);
        assertNears("n11",    k[4],  -0.160000);
        assertNears("n02",    k[5],  13.8080);
        assertNears("n30",    k[6],  0.00000);
        assertNears("n21",    k[7],  -0.965981);
        assertNears("n12",    k[8],  1.63144);
        assertNears("n03",    k[9],  55.0867);
        assertNears("h1",     k[10], 14.2080);
        assertNears("h2",     k[11], 179.876864);
        assertNears("h3",     k[12], 3386.172875);
        assertNears("h4",     k[13], 2931.71348);
        assertNears("h5",     k[14], 9236948.170205);
        assertNears("h6",     k[15], 39180.53298);
        assertNears("h7",     k[16], 57255.22444);
        assertNears("xBar",   k[17], 3.00000);
        assertNears("yBar",   k[18], 8.40000);
    }

    //~ Methods private----------------------------------------------------
}
