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
package omr.sheet.ledger;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ledger.LedgersBuilder;
import omr.sheet.ledger.LedgersFilter;
import omr.sheet.ui.SheetTab;

import omr.step.AbstractSystemStep;
import omr.step.Step;
import omr.step.StepException;

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
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // displayUI //
    //-----------//
    @Override
    public void displayUI (Step step,
                           Sheet sheet)
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
