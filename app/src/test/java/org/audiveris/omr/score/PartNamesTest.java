//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   P a r t N a m e s T e s t                                    //
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
package org.audiveris.omr.score;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link PartNames}.
 */
public class PartNamesTest
{

    @Test
    public void testAbbreviations ()
    {
        assertTrue(PartNames.match("Violin I", "Vln. I"));
        assertTrue(PartNames.match("Viola", "Vla."));
        assertTrue(PartNames.match("Violoncello", "Vc."));
        assertTrue(PartNames.match("Flute", "Fl."));
        assertTrue(PartNames.match("Soprano", "S."));
        assertTrue(PartNames.match("Piano", "Pno"));
        assertTrue(PartNames.match("English Horn", "E. H."));
        assertTrue(PartNames.match("Violin 1", "Violin I"));
        assertTrue(PartNames.match("Choir", "CHOIR"));
        assertTrue(PartNames.match("VIOLIN I", "Vln. I"));
    }

    @Test
    public void testAliases ()
    {
        assertTrue(PartNames.match("Cello", "Vc."));
        assertTrue(PartNames.match("Bassoon", "Fag."));
        assertTrue(PartNames.match("Double Bass", "Cb."));
    }

    @Test
    public void testDifferentParts ()
    {
        assertFalse(PartNames.match("Violin I", "Vln. II"));
        assertFalse(PartNames.match("Violin I", "Vla."));
        assertFalse(PartNames.match("Viola", "Vc."));
        assertFalse(PartNames.match("Flute", "Ob."));
        assertFalse(PartNames.match("Soprano", "A."));
    }

    @Test
    public void testNumberReadOnOneSideOnly ()
    {
        assertTrue(PartNames.match("Violin II", "Vln."));
        assertTrue(PartNames.match("Violin", "Vln. I"));
    }

    @Test
    public void testOcrMisreadings ()
    {
        assertTrue(PartNames.match("Flute", "F1.")); // 1 for l
        assertTrue(PartNames.match("Flute", "FI.")); // I for l
        assertTrue(PartNames.match("Voice", "V0.")); // 0 for o
        assertTrue(PartNames.match("Violoncello", "V0."));
        assertTrue(PartNames.match("Violin I", "Vln. l")); // l for I
        assertTrue(PartNames.match("Violin I", "' . V1n.I"));
        assertTrue(PartNames.match("Violin I", "Violin ,I"));
        assertTrue(PartNames.match("E. H.", "E. H,"));
    }

    @Test
    public void testFit ()
    {
        // An abbreviation fits the name it covers most, and the same number best
        assertTrue(PartNames.fit("Vln.", "Violin") > PartNames.fit("Vln.", "Violoncello"));
        assertTrue(PartNames.fit("Vln. I", "Violin I") > PartNames.fit("Vln. I", "Violin"));
        // Two letters are too few to tell instruments apart by cover
        assertTrue(PartNames.fit("V0.", "Viola") == PartNames.fit("V0.", "Violoncello"));
        assertTrue(PartNames.fit("Viola", "Vc.") == 0);
    }

    @Test
    public void testUnreadable ()
    {
        assertFalse(PartNames.isReadable(null));
        assertFalse(PartNames.isReadable("13."));
        assertFalse(PartNames.isReadable("02-"));
        assertTrue(PartNames.isReadable("B."));
        assertFalse(PartNames.match("Bass", "13."));
        assertFalse(PartNames.match(null, null));
    }
}
