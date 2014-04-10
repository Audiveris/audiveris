//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       G l y p h L a y e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

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
