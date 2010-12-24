//----------------------------------------------------------------------------//
//                                                                            //
//                           S y s t e m s S t e p                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code SystemsStep} retrieves the vertical bar lines, and thus the
 * systems
 *
 * @author Hervé Bitteur
 */
public class SystemsStep
    extends AbstractStep
{
    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SystemsStep //
    //-------------//
    /**
     * Creates a new SystemsStep object.
     */
    public SystemsStep ()
    {
        super(
            Steps.SYSTEMS,
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
        sheet.createSystemsBuilder();
        sheet.getSystemsBuilder()
             .buildSystems();
    }
}
