//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    T e x t W o r d T e s t                                     //
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
package org.audiveris.omr.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import java.awt.Rectangle;

/**
 * Unitary tests for class <code>TextWord</code>.
 */
public class TextWordTest
{
    @Test
    public void lone_J_is_a_slur ()
    {
        assertEquals("slur-like-word-value", word("J").checkValidity(null));
    }

    @Test
    public void J_before_text_is_a_slur ()
    {
        assertEquals("slur-like-word-value", word("J").checkValidity(word("Allegro")));
    }

    @Test
    public void J_before_equal_sign_is_a_metronome_note ()
    {
        assertNull(word("J").checkValidity(word("=121")));
        assertNull(word("J").checkValidity(word("=")));
    }

    @Test
    public void J_before_colon_is_a_metronome_note ()
    {
        assertNull(word("J").checkValidity(word(":160")));
    }

    private static TextWord word (String value)
    {
        return new TextWord(null, new Rectangle(0, 0, 10, 10), value, null, 1.0, null, null);
    }
}
