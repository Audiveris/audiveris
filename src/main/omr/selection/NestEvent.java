//----------------------------------------------------------------------------//
//                                                                            //
//                             N e s t E v e n t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

/**
 * Class {@code NestEvent} is an abstract class to represent any event
 * specific to a glyph service (glyph, glyph id, glyph set).
 *
 * @author Hervé Bitteur
 */
public abstract class NestEvent
        extends UserEvent
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NestEvent object.
     *
     * @param source   the actual entity that created this event
     * @param hint     how the event originated
     * @param movement the precise mouse movement
     */
    public NestEvent (Object source,
                      SelectionHint hint,
                      MouseMovement movement)
    {
        super(source, hint, movement);
    }
}
