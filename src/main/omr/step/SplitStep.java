//----------------------------------------------------------------------------//
//                                                                            //
//                             S p l i t S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.grid.LagWeaver;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code SplitStep} splits entities among retrieved systems according
 * to precise systemss boundaries
 *
 * @author Hervé Bitteur
 */
public class SplitStep
    extends AbstractStep
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // SplitStep //
    //-----------//
    /**
     * Creates a new SplitStep object.
     */
    public SplitStep ()
    {
        super(
            Steps.SPLIT,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.REDOABLE,
            SYSTEMS_TAB,
            "Retrieve Systems from Bar sticks");
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        //        Main.getGui().scoreController.setScoreEditor(sheet.getScore());
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> systems,
                      Sheet                  sheet)
        throws StepException
    {
        // Purge sections & runs of staff lines from hLag
        // Cross-connect vertical & remaining horizontal sections
        new LagWeaver(sheet).buildInfo();

        // Create systems & parts
        sheet.createSystemsBuilder();
        sheet.getSystemsBuilder()
             .buildSystems();
    }
}
