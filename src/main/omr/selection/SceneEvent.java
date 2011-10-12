//----------------------------------------------------------------------------//
//                                                                            //
//                            S c e n e E v e n t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;


/**
 * Class <code>SceneEvent</code> is an abstract class to represent any event
 * specific to a glyph service (glyph, glyph id, glyph set)
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>
 * <dt><b>Subscribers:</b><dd>
 * <dt><b>Readers:</b><dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
public abstract class SceneEvent
    extends UserEvent
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SceneEvent object.
     * @param source the actual entity that created this event
     * @param hint how the event originated
     * @param movement the precise mouse movement
     */
    public SceneEvent (Object        source,
                       SelectionHint hint,
                       MouseMovement movement)
    {
        super(source, hint, movement);
    }
}
