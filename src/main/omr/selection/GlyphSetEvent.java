//----------------------------------------------------------------------------//
//                                                                            //
//                         G l y p h S e t E v e n t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.glyph.Glyph;
import omr.glyph.Glyphs;

import java.util.Set;

/**
 * Class <code>GlyphSetEvent</code> represents a Glyph Set selection
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>GlyphLag, GlyphLagView
 * <dt><b>Subscribers:</b><dd>GlyphBoard, GlyphLagView, ScoreMenu,
 * SymbolGlyphBoard, SymbolsEditor
 * <dt><b>Readers:</b><dd>GlyphBoard, GlyphLag, GlyphLagView, GlyphMenu,
 * SymbolsEditor
 * </dl>
 * @author Hervé Bitteur
 */
public class GlyphSetEvent
    extends GlyphLagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected glyph set, which may be null */
    public final Set<Glyph> glyphs;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // GlyphEvent //
    //------------//
    /**
     * Creates a new GlyphEvent object.
     *
     * @param source the entity that created this event
     * @param hint hint about event origin (or null)
     * @param movement the user movement
     * @param glyphs the selected collection of glyphs (or null)
     */
    public GlyphSetEvent (Object        source,
                          SelectionHint hint,
                          MouseMovement movement,
                          Set<Glyph>    glyphs)
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
        return Glyphs.toString(glyphs);
    }
}
