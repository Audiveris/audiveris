//----------------------------------------------------------------------------//
//                                                                            //
//                            S h e e t E v e n t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.sheet.Sheet;

/**
 * Class <code>SheetEvent</code> represent a Sheet selection event
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>SheetController, SheetManager
 * <dt><b>Subscribers:</b><dd>ActionManager, MainGui, MidiActions, ScoreDependent, SheetDependent
 * <dt><b>Readers:</b><dd>SheetManager
 * </dl>
 *
 * @author Hervé Bitteur
 */
public class SheetEvent
    extends UserEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected sheet, which may be null */
    public final Sheet sheet;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // SheetEvent //
    //------------//
    /**
     * Creates a new SheetEvent object.
     *
     * @param source the entity that created this event
     * @param sheet the selected sheet (or null)
     */
    public SheetEvent (Object source,
                       Sheet  sheet)
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
