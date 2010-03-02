//----------------------------------------------------------------------------//
//                                                                            //
//                        G h o s t D r o p E v e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.dnd;


/**
 * Class {@code GhostDropEvent} is the type of event that is handed to any
 * {@link GhostDropListener} instance.
 *
 * @param <A> the precise type of the action carried by the drop
 * @author Hervé Bitteur (from Romain Guy's demo)
 */
public class GhostDropEvent<A>
{
    //~ Instance fields --------------------------------------------------------

    /** The drop location with respect to screen */
    private ScreenPoint point;

    /** The action carried by the drop event */
    private A action;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // GhostDropEvent //
    //----------------//
    /**
     * Create a new GhostDropEvent object
     *
     * @param action the action carried by the drop
     * @param point the screen-based location of the drop
     */
    public GhostDropEvent (A           action,
                           ScreenPoint point)
    {
        this.action = action;
        this.point = point;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getAction //
    //-----------//
    /**
     * Report the action carried by the drop
     * @return the carried action
     */
    public A getAction ()
    {
        return action;
    }

    //-----------------//
    // getDropLocation //
    //-----------------//
    /**
     * Report the drop location
     * @return the screen-based location of the drop
     */
    public ScreenPoint getDropLocation ()
    {
        return point;
    }
}
