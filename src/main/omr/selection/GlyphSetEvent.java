//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h S e t E v e n t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import java.util.Set;

/**
 * Class {@code GlyphSetEvent} represents a Glyph Set selection.
 *
 * @author Hervé Bitteur
 */
public class GlyphSetEvent
        extends NestEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected glyph set, which may be null */
    private final Set<Glyph> glyphs;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // GlyphEvent //
    //------------//
    /**
     * Creates a new GlyphEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the user movement
     * @param glyphs   the selected collection of glyphs (or null)
     */
    public GlyphSetEvent (Object source,
                          SelectionHint hint,
                          MouseMovement movement,
                          Set<Glyph> glyphs)
    {
        super(source, hint, movement);
        this.glyphs = glyphs;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getEntity //
    //-----------//
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
