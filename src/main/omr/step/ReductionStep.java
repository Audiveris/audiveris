//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    R e d u c t i o n S t e p                                   //
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

import omr.sig.SigReducer;

/**
 * Class {@code ReductionStep} implements <b>REDUCTION</b> step, which tries to reduce
 * the SIG incrementally after structures (notes + stems + beams) have been retrieved.
 *
 * @author Hervé Bitteur
 */
public class ReductionStep
        extends AbstractSystemStep<Void>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ReductionStep object.
     */
    public ReductionStep ()
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
        new SigReducer(system).reduce(true);
    }
}
