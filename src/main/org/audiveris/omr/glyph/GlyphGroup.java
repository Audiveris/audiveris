//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       G l y p h G r o u p                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.glyph;

/**
 * This enumeration is used to group glyph instances by their intended use.
 * <p>
 * A single glyph instance can be assigned several groups.
 *
 * @author Hervé Bitteur
 */
public enum GlyphGroup
{
    /** Only the first 4 ones are needed. */
    BEAM_SPOT("Beam-oriented spot"),
    BLACK_HEAD_SPOT("BlackHead-oriented spot"),
    BLACK_STACK_SPOT("Stack of blackHead-oriented spot"),
    HEAD_SPOT("Head-oriented spot"),
    VERTICAL_SEED("Vertical seed"),
    SYMBOL("Fixed symbol"),
    /**
     * The remaining ones below are not needed. But may still exist in .omr files...
     */
    STAFF_LINE("Staff Line"),
    LEDGER("Ledger"),
    LEDGER_CANDIDATE("Ledger candidate"),
    WEAK_PART("Optional part"),
    TIME_PART("Part of time sig"),
    ALTER_PART("Part of alteration"),
    CLEF_PART("Part of clef"),
    DROP("DnD glyph");

    /** Role of the group. */
    public final String description;

    GlyphGroup (String description)
    {
        this.description = description;
    }
}
