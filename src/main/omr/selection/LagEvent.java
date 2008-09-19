//----------------------------------------------------------------------------//
//                                                                            //
//                              L a g E v e n t                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.selection;


/**
 * Class <code>LagEvent</code> is an abstract class to represent any event
 * related to a lag (run, section, section id)
 *
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>
 * <dt><b>Subscribers:</b><dd>
 * <dt><b>Readers:</b><dd>
 * </dl>
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class LagEvent
    extends UserEvent
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new LagEvent object.
     * @param source the actual entity that created this event
     * @param hint how the event originated
     */
    public LagEvent (Object        source,
                     SelectionHint hint)
    {
        super(source, hint, null);
    }
}
