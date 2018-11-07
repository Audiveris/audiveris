//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       G l y p h L a y e r                                      //
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
 * Enum {@code GlyphLayer} defines populations of glyph instances.
 */
public enum GlyphLayer
{
    /** Glyph instances from initial binary image. */
    DEFAULT("B", "Glyphs from initial binary image"),
    /** Horizontal glyph instances from ledgers and endings. */
    LEDGER("L", "Horizontal glyphs from ledgers and endings"),
    /** Glyph instances from foreground spots. */
    SPOT("S", "Glyphs from foreground spots"),
    /** Glyph instances from symbols. */
    SYMBOL("Y", "Glyphs from symbols"),
    /** Virtual glyph instances from Drag and Drop. */
    DROP("D", "Virtual glyphs from Drag n' Drop"),
    /** Glyph instances unmarshalled from XML file. */
    XML("X", "Sample glyphs unmarshalled from XML file");

    /** Simple key to refer to the layer. */
    public final String key;

    /** Description for end user. */
    public final String desc;

    private GlyphLayer (String key,
                        String desc)
    {
        this.key = key;
        this.desc = desc;
    }

    //----------------//
    // concreteValues //
    //----------------//
    public static GlyphLayer[] concreteValues ()
    {
        return concreteValues;
    }

    private static final GlyphLayer[] concreteValues = new GlyphLayer[]{DEFAULT, LEDGER, SPOT};
}
