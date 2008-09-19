//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h I d E v e n t                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.selection;


/**
 * Class <code>GlyphIdEvent</code> represents a Glyph Id selection
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>GlyphBoard, ShapeFocusBoard
 * <dt><b>Subscribers:</b><dd>GlyphLag, GlyphLagView
 * <dt><b>Readers:</b><dd>
 * </dl>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GlyphIdEvent
    extends GlyphLagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected glyph id, which may be null */
    public final Integer id;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new GlyphIdEvent object.
     *
     * @param source the entity that created this event
     * @param hint hint about event origin (or null)
     * @param movement the precise mouse movement
     * @param id the glyph id
     */
    public GlyphIdEvent (Object        source,
                         SelectionHint hint,
                         MouseMovement movement,
                         Integer       id)
    {
        super(source, hint, null);
        this.id = id;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getEntity //
    //-----------//
    @Override
    public Integer getData ()
    {
        return id;
    }
}
