//----------------------------------------------------------------------------//
//                                                                            //
//                       V e r s i o n N u m b e r T e s t                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer.unix;

import static org.junit.Assert.*;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unitary tests for VersionNumber.
 *
 * @author Hervé Bitteur
 */
public class VersionNumberTest
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            VersionNumberTest.class);

    //~ Methods ----------------------------------------------------------------
    /**
     * Test of compareTo method, of class VersionNumber.
     */
    @Test
    public void test_compareTo ()
    {
        System.out.println("test_compareTo");

        test("1.2.3", "2.3", -1);
        test("1.2.3", "1.2.3", 0);
        test("1.2.3-57", "1.2.3", 1);
        test("1.2.3-57", "1.2.3-25alpha", 1);
        test("1:4.7.2-2ubuntu1", "4.7.3", 1);
        test("1:4.7.2-2ubuntu1", "2:0", -1);
        test("1-~~", "1-~~a", -1);
        test("2-~~a", "2-~", -1);
        test("3~", "3", -1);
        test("4", "4a", -1);
        test("9.06~dfsg-0ubuntu4", "9.06", -1);
        test("7u9-2.3.4-0ubuntu1.12.10.1", "9.06", -1);
    }

    //------//
    // test //
    //------//
    private void test (String v1,
                       String v2,
                       int exp)
    {
        logger.info("Test {} vs {}, exp:{}", v1, v2, exp);

        VersionNumber vn1 = new VersionNumber(v1);
        VersionNumber vn2 = new VersionNumber(v2);
        int res = vn1.compareTo(vn2);
        logger.info("Result:{}", res);
        assertEquals(exp, res);
    }
}
