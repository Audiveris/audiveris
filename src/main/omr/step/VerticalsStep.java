//----------------------------------------------------------------------------//
//                                                                            //
//                         V e r t i c a l s S t e p                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.Main;

import omr.log.Logger;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code VerticalsStep} retrieves the vertical items such as stems
 *
 * @author Hervé Bitteur
 */
public class VerticalsStep
    extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(VerticalsStep.class);

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // VerticalsStep //
    //---------------//
    /**
     * Creates a new VerticalsStep object.
     */
    public VerticalsStep ()
    {
        super(
            Steps.VERTICALS,
            Level.SHEET_LEVEL,
            Mandatory.MANDATORY,
            Redoable.REDOABLE,
            VERTICALS_TAB,
            "Extract verticals");
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        // Create verticals display
        sheet.getVerticalsController()
             .refresh();
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

        int stemCount = system.retrieveVerticals();
        ///logger.info("S#" + system.getId() + " Stems found: " + stemCount);
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet                  sheet)
        throws StepException
    {
        sheet.createVerticalsController();
    }
}
