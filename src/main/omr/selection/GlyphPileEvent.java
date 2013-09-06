//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h P i l e E v e n t                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import java.util.Set;

/**
 * Class {@code GlyphPileEvent} represents a collection of overlapping
 * glyphs, one on top of the other.
 *
 * @author Hervé Bitteur
 */
public class GlyphPileEvent
        extends NestEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected pile of glyphs, which may be null. */
    private final Set<Glyph> glyphs;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // GlyphPileEvent //
    //---------------//
    /**
     * Creates a new GlyphPileEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the user movement
     * @param glyphs   the selected pile of glyph instances (or null)
     */
    public GlyphPileEvent (Object source,
                           SelectionHint hint,
                           MouseMovement movement,
                           Set<Glyph> glyphs)
    {
        super(source, hint, movement);
        this.glyphs = glyphs;
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public Set<Glyph> getData ()
    {
        return glyphs;
    }

    //----------------//
    // internalString //
    //----------------//
    @Override
    protected String internalString ()
    {
        if (glyphs != null) {
            return Glyphs.toString(glyphs);
        } else {
            return "";
        }
    }
}
