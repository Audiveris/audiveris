//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                          S t a f f B a r l i n e I n t e r T e s t                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/**
 * Unitary tests for class <code>StaffBarlineInter</code>.
 *
 * @author Hervé Bitteur
 */
public class StaffBarlineInterTest
{
    //~ Constructors -------------------------------------------------------------------------------

    public StaffBarlineInterTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Test that the counts actually printed on a chart are read.
     */
    @Test
    public void testRepeatCountAccepted ()
    {
        assertEquals(Integer.valueOf(4), StaffBarlineInter.repeatCountOf("x4"));
        assertEquals(Integer.valueOf(4), StaffBarlineInter.repeatCountOf("X4"));
        assertEquals(Integer.valueOf(4), StaffBarlineInter.repeatCountOf("4x"));
        assertEquals(Integer.valueOf(4), StaffBarlineInter.repeatCountOf("4X"));
        assertEquals(Integer.valueOf(4), StaffBarlineInter.repeatCountOf("x 4"));
        assertEquals(Integer.valueOf(4), StaffBarlineInter.repeatCountOf("4 x"));
        assertEquals(Integer.valueOf(19), StaffBarlineInter.repeatCountOf("19x"));
        assertEquals(Integer.valueOf(2), StaffBarlineInter.repeatCountOf(" 2x "));
    }

    /**
     * Test that whatever is not a printed count is refused.
     */
    @Test
    public void testRepeatCountRefused ()
    {
        assertNull(StaffBarlineInter.repeatCountOf(null));
        assertNull(StaffBarlineInter.repeatCountOf(""));

        // A bare number is an ending number or a measure number
        assertNull(StaffBarlineInter.repeatCountOf("4"));
        assertNull(StaffBarlineInter.repeatCountOf("2."));

        // A bare x is a cross note head
        assertNull(StaffBarlineInter.repeatCountOf("x"));
        assertNull(StaffBarlineInter.repeatCountOf("xx x x"));

        // Playing once is not a repeat
        assertNull(StaffBarlineInter.repeatCountOf("x1"));

        // Prose is out of scope
        assertNull(StaffBarlineInter.repeatCountOf("Play 4 times"));
        assertNull(StaffBarlineInter.repeatCountOf("Fill 4"));
        assertNull(StaffBarlineInter.repeatCountOf("4x4"));
    }
}
