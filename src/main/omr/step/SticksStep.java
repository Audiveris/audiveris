//----------------------------------------------------------------------------//
//                                                                            //
//                            S t i c k s S t e p                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code SticksStep} retrieves the vertical sticks such as
 * stems and horizontal sticks such as ledgers and endings.
 *
 * @author Hervé Bitteur
 */
public class SticksStep
        extends AbstractSystemStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SticksStep.class);

    //~ Constructors -----------------------------------------------------------
    //------------//
    // SticksStep //
    //------------//
    /**
     * Creates a new SticksStep object.
     */
    public SticksStep ()
    {
        super(
                Steps.STICKS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Extract vertical & horizontal sticks");
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

        // Add ledger checkboard
        sheet.getSystems()
                .get(0)
                .getHorizontalsBuilder()
                .addCheckBoard();
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        clearSystemErrors(system);
        system.retrieveVerticals();
        system.retrieveHorizontals();
    }

    //----------//
    // doEpilog //
    //----------//
    @Override
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        sheet.createVerticalsController();
    }
}
