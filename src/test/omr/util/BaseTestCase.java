//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B a s e T e s t C a s e                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import junit.framework.*;
import static junit.framework.Assert.*;

/**
 * Class <code>BaseTestCase</code> is a customized version of TestCase, in
 * order to factor additional test features.
 *
 * @author Hervé Bitteur
 */
public class BaseTestCase
        extends TestCase
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BaseTestCase object.
     */
    public BaseTestCase ()
    {
    }

    /**
     * Creates a new BaseTestCase object.
     *
     * @param name DOCUMENT ME!
     */
    public BaseTestCase (String name)
    {
        super(name);
    }

    //~ Methods ------------------------------------------------------------------------------------
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
        assertTrue(msg, Math.abs(a - b) < maxDiff);
    }

    //----------------//
    // checkException //
    //----------------//
    public static void checkException (Exception ex)
    {
        System.out.println("Got " + ex);
        assertNotNull(ex.getMessage());
    }

    //-------//
    // print //
    //-------//
    public static void print (Object o)
    {
        System.out.println(o);
    }

    public void testDummy ()
    {
        // Nothing
    }

    //---------//
    // runTest //
    //---------//
    @Override
    protected void runTest ()
            throws Throwable
    {
        System.out.println("\n---\n" + getName() + ":");
        super.runTest();
        System.out.println("+++ End " + toString());
    }
}
