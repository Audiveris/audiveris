//----------------------------------------------------------------------------//
//                                                                            //
//                              G r i d S t e p                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.grid.GridBuilder;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code GridStep} determine the systems frames of the picture.
 * This is a temporary attempt, to be evaluated and refined is successful.
 *
 * @author Hervé Bitteur
 */
public class GridStep
    extends AbstractStep
{
    //~ Constructors -----------------------------------------------------------

    //----------//
    // GridStep //
    //----------//
    /**
     * Creates a new GridStep object.
     */
    public GridStep ()
    {
        super(
            Steps.GRID,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.NON_REDOABLE,
            "Dewarped",
            "Retrieve the grid of all systems");
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet                  sheet)
        throws StepException
    {
        new GridBuilder(sheet).buildInfo();
    }
}
