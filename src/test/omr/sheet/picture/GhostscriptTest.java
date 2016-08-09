//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G h o s t s c r i p t T e s t                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.picture;

import omr.image.Ghostscript;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for Ghostscript class.
 *
 * @author Hervé Bitteur
 */
public class GhostscriptTest
{
    //~ Methods ------------------------------------------------------------------------------------

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
