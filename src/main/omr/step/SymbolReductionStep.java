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
package omr.step;

import omr.sheet.SystemInfo;

import omr.sig.SIGraph.ReductionMode;
import static omr.step.Step.DATA_TAB;

/**
 * Class {@code SymbolReductionStep}implements <b>SYMBOL_REDUCTION</b> step, which tries
 * to reduce the SIG incrementally after symbols have been retrieved.
 *
 * @author Hervé Bitteur
 */
public class SymbolReductionStep
        extends AbstractSystemStep
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SymbolReductionStep object.
     */
    public SymbolReductionStep ()
    {
        super(
                Steps.SYMBOL_REDUCTION,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Reduce symbols");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.sigReducer.reduce(ReductionMode.RELAXED);
    }
}
