//-----------------------------------------------------------------------//
//                                                                       //
//                          G l y p h F o c u s                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

/**
 * Interface <code>GlyphFocus</code> define the features related to setting
 * a focus determined by a glyph, it is thus an input entity.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public interface GlyphFocus
{
    /**
     * Focus on a glyph
     *
     * @param glyph the glyph to focus upon
     */
    void setFocusGlyph (Glyph glyph);

    /**
     * Focus on a glyph, knowing its id
     *
     * @param id the glyph is
     */
    void setFocusGlyph (int id);

    /**
     * Can retrieve a glyph knowing its id
     *
     * @param id id of the desired glyph
     * @return the glyph found, or null otherwise
     */
    Glyph getGlyphById (int id);

    /**
     * Cancel the definition of the given glyph
     *
     * @param glyph the glyph to de-assign
     */
    void deassignGlyph (Glyph glyph);
}
