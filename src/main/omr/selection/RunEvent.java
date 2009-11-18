//----------------------------------------------------------------------------//
//                                                                            //
//                              R u n E v e n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.lag.Run;

/**
 * Class <code>RunEvent</code> represents a Run selection
 *
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>GlyphLag, Lag, LagView
 * <dt><b>Subscribers:</b><dd>RunBoard
 * <dt><b>Readers:</b><dd>
 * </dl>
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class RunEvent
    extends LagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected run, which may be null */
    public final Run run;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // RunEvent //
    //----------//
    /**
     * Creates a new RunEvent object.
     *
     * @param source the entity that created this event
     * @param hint how the event originated
     * @param movement the mouse movement
     * @param run the selected run (or null)
     */
    public RunEvent (Object        source,
                     SelectionHint hint,
                     MouseMovement movement,
                     Run           run)
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
