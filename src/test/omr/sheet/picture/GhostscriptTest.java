//----------------------------------------------------------------------------//
//                                                                            //
//                        G h o s t s c r i p t T e s t                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.picture;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for Ghostscript class.
 * 
 * @author Hervé Bitteur
 */
public class GhostscriptTest
{
    //~ Methods ----------------------------------------------------------------

    //----------//
    // pathTest //
    //----------//
    @Test
    public void pathTest ()
    {
        String result = Ghostscript.getPath();
        System.out.println("Ghostscript path = " + result);
        assertFalse(result.isEmpty());
    }
}
