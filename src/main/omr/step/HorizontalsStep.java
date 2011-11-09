//----------------------------------------------------------------------------//
//                                                                            //
//                       H o r i z o n t a l s S t e p                        //
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

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

/**
 * Class {@code HorizontalsStep} retrieves the horizontal dashes (ledgers,
 * endings)
 *
 * @author Hervé Bitteur
 */
public class HorizontalsStep
    extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        HorizontalsStep.class);

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // HorizontalsStep //
    //-----------------//
    /**
     * Creates a new HorizontalsStep object.
     */
    public HorizontalsStep ()
    {
        super(
            Steps.HORIZONTALS,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.REDOABLE,
            HORIZONTALS_TAB,
            "Extract ledgers, tenutos & endings");
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        // Add ledger checkboard
        sheet.getSystems()
             .get(0)
             .getHorizontalsBuilder()
             .displayFrame();
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
        throws StepException
    {
        if (Main.getGui() != null) {
            system.getSheet()
                  .getErrorsEditor()
                  .clearSystem(this, system.getId());
        }

        try {
            system.retrieveHorizontals();
        } catch (Throwable ex) {
            logger.warning("Error in HorizontalsStep", ex);
        }
    }
}
