//----------------------------------------------------------------------------//
//                                                                            //
//                          B a s e T e s t C a s e                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import static junit.framework.Assert.*;

import junit.framework.*;

/**
 * Class <code>BaseTestCase</code> is a customized version of TestCase, in
 * order to factor additional test features.
 *
 * @author Hervé Bitteur
 * @version $Id$
 */
public class BaseTestCase
    extends TestCase
{
    public BaseTestCase()
    {
    }

    public BaseTestCase (String name)
    {
        super(name);
    }

    //---------//
    // runTest //
    //---------//
    @Override
    protected void runTest() throws Throwable {
        System.out.println("\n---\n" + getName() +":");
        super.runTest();
        System.out.println("+++ End " + toString());
    }

    //-------//
    // print //
    //-------//
    public static void print (Object o)
    {
        System.out.println(o);
    }

    //----------------//
    // checkException //
    //----------------//
    public static void checkException (Exception ex)
    {
        System.out.println("Got " + ex);
        assertNotNull(ex.getMessage());
    }

    //-------------//
    // assertNears //
    //-------------//
    public static void assertNears (String msg,
                                    double a,
                                    double b)
    {
        assertNears(msg, a, b, 1E-5);
    }

    //-------------//
    // assertNears //
    //-------------//
    public static void assertNears (String msg,
                                    double a,
                                    double b,
                                    double maxDiff)
    {
        System.out.println("Comparing " + a + " and " + b);
        assertTrue(msg,
                   Math.abs(a - b) < maxDiff);
    }
}
