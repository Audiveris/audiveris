//----------------------------------------------------------------------------//
//                                                                            //
//                            S h e e t E v e n t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.sheet.Sheet;

/**
 * Class {@code SheetEvent} represent a Sheet selection event, used to
 * call attention about a selected sheet.
 *
 * @author Hervé Bitteur
 */
public class SheetEvent
        extends UserEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected sheet, which may be null */
    private final Sheet sheet;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // SheetEvent //
    //------------//
    /**
     * Creates a new SheetEvent object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin
     * @param movement the mouse movement
     * @param sheet    the selected sheet (or null)
     */
    public SheetEvent (Object source,
                       SelectionHint hint,
                       MouseMovement movement,
                       Sheet sheet)
    {
        super(source, null, null);
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getEntity //
    //-----------//
    @Override
    public Sheet getData ()
    {
        return sheet;
    }
}
