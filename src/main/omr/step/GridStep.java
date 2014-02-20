//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G r i d S t e p                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.glyph.ui.SymbolsEditor;

import omr.grid.LagWeaver;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code GridStep} implements <b>GRID</b> step, which retrieves all staves and
 * systems of a sheet.
 *
 * @author Hervé Bitteur
 */
public class GridStep
        extends AbstractStep
{
    //~ Constructors -------------------------------------------------------------------------------

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
                DATA_TAB,
                "Retrieve the grid of all systems");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        SymbolsEditor editor = sheet.getSymbolsEditor();

        if (editor != null) {
            editor.refresh();
        }
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> unused,
                      Sheet sheet)
            throws StepException
    {
        sheet.createNest();
        sheet.getGridBuilder().buildInfo();

        // Purge sections & runs of staff lines from hLag
        // Cross-connect vertical & remaining horizontal sections
        new LagWeaver(sheet).buildInfo();

        // Populate systems
        sheet.createSystemManager();
        sheet.getSystemManager().populateSystems();
    }
}
