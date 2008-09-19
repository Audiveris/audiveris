//----------------------------------------------------------------------------//
//                                                                            //
//                              R u n E v e n t                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
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
     * @param run the selected run (or null)
     */
    public RunEvent (Object source,
                     Run    run)
    {
        super(source, null);
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
