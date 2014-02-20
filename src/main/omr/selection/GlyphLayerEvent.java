//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  G l y p h L a y e r E v e n t                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.glyph.GlyphLayer;

/**
 * Class {@code GlyphLayerEvent} represents the selection of a specific glyph layer.
 *
 * @author Hervé Bitteur
 */
public class GlyphLayerEvent
        extends NestEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected glyph layer. (Cannot be null) */
    private final GlyphLayer layer;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new GlyphLayerEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the mouse movement
     * @param layer    the specific layer chosen (not null)
     */
    public GlyphLayerEvent (Object source,
                            SelectionHint hint,
                            MouseMovement movement,
                            GlyphLayer layer)
    {
        super(source, hint, movement);
        this.layer = layer;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public GlyphLayer getData ()
    {
        return layer;
    }
}
