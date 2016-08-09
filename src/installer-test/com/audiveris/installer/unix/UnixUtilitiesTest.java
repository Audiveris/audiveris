//----------------------------------------------------------------------------//
//                                                                            //
//                       U n i x U t i l i t i e s T e s t                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer.unix;

import com.audiveris.installer.DescriptorFactory;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author herve
 */
public class UnixUtilitiesTest
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Test of getCommandLine method, of class UnixUtilities.
     */
    @Test
    public void testGetCommandLine ()
    {
        if (DescriptorFactory.LINUX) {
            System.out.println("getCommandLine");

            String result = UnixUtilities.getCommandLine();
            System.out.println("result = " + result);
        }
    }

    /**
     * Test of getPid method, of class UnixUtilities.
     */
    @Test
    public void testGetPid ()
        throws Exception
    {
        if (DescriptorFactory.LINUX) {
            System.out.println("getPid");

            String result = UnixUtilities.getPid();
            System.out.println("result = " + result);
        }
    }
}
