//----------------------------------------------------------------------------//
//                                                                            //
//                              R u n E v e n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.run.Run;

/**
 * Class {@code RunEvent} represents a Run selection.
 *
 * @author Hervé Bitteur
 */
public class RunEvent
        extends UserEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected run, which may be null */
    private final Run run;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // RunEvent //
    //----------//
    /**
     * Creates a new RunEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     how the event originated
     * @param movement the mouse movement
     * @param run      the selected run (or null)
     */
    public RunEvent (Object source,
                     SelectionHint hint,
                     MouseMovement movement,
                     Run run)
    {
        super(source, hint, movement);
        this.run = run;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getEntity //
    //-----------//
    @Override
    public Run getData ()
    {
        return run;
    }
}
