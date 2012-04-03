//----------------------------------------------------------------------------//
//                                                                            //
//                             S p l i t S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
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
            DATA_TAB,
            "Split all data per system");
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public void displayUI (Sheet sheet)
    {
        sheet.getAssembly()
             .addBoard(Step.DATA_TAB, sheet.getBarsChecker().getCheckBoard());
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
        // Build glyphs out of connected sections
        new LagWeaver(sheet).buildInfo();

        // Create systems & parts
        sheet.createSystemsBuilder();
        sheet.getSystemsBuilder()
             .buildSystems();
    }
}
