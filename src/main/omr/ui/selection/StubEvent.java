//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t u b E v e n t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.selection;

import omr.sheet.SheetStub;

/**
 * Class {@code StubEvent} represent a SheetStub selection event, used to call attention
 * about a selected stub.
 *
 * @author Hervé Bitteur
 */
public class StubEvent
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected sheet stub, which may be null. */
    private final SheetStub stub;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SheetEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin
     * @param movement the mouse movement
     * @param stub     the selected sheet stub (or null)
     */
    public StubEvent (Object source,
                      SelectionHint hint,
                      MouseMovement movement,
                      SheetStub stub)
    {
        super(source, null, null);
        this.stub = stub;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public SheetStub getData ()
    {
        return stub;
    }
}
