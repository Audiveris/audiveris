//----------------------------------------------------------------------------//
//                                                                            //
//                            S h e e t E v e n t                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
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
 * @author Herv&eacute Bitteur
 * @version $Id$
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
