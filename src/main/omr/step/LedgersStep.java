//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      L e d g e r s S t e p                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.LedgersBuilder;
import omr.sheet.LedgersFilter;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code LedgersStep} implements <b>LEDGERS</b> step, which retrieves all
 * possible ledger interpretations.
 *
 * @author Hervé Bitteur
 */
public class LedgersStep
        extends AbstractSystemStep<Void>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new LedgersStep object.
     */
    public LedgersStep ()
    {
        super(
                Steps.LEDGERS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve ledgers");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Sheet sheet)
    {
        // Add ledger checkboard
        new LedgersBuilder(sheet.getSystems().get(0)).addCheckBoard();
    }

    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        new LedgersBuilder(system).buildLedgers();
    }

    //----------//
    // doProlog //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Retrieve horizontal sticks for ledger candidates.
     * <p>
     * These candidate sticks are dispatched to their relevant system(s)
     */
    @Override
    protected Void doProlog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        new LedgersFilter(sheet).process();

        return null;
    }
}
