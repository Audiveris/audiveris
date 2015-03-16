//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t e m s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.StemsBuilder;
import omr.sheet.SystemInfo;

import omr.sig.SigReducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code StemsStep} implements <b>STEMS</b> step, which establishes all
 * possible relations between stems and note heads or beams.
 *
 * @author Hervé Bitteur
 */
public class StemsStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StemsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new StemsStep object.
     */
    public StemsStep ()
    {
        super(
                Steps.STEMS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Build stems for heads & beams");
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
        new StemsBuilder(system).linkStems(); // -> Stems links to heads & beams
        new SigReducer(system).contextualize();
    }
}
