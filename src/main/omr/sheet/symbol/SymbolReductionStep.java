//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              S y m b o l R e d u c t i o n S t e p                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.symbol;

import omr.sheet.SystemInfo;

import omr.sig.SIGraph.ReductionMode;
import omr.sig.SigReducer;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

/**
 * Class {@code SymbolReductionStep}implements <b>SYMBOL_REDUCTION</b> step, which tries
 * to reduce the SIG incrementally after symbols have been retrieved.
 *
 * @author Hervé Bitteur
 */
public class SymbolReductionStep
        extends AbstractSystemStep<Void>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SymbolReductionStep object.
     */
    public SymbolReductionStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        new SigReducer(system).reduce(ReductionMode.STRICT, false);
    }
}
