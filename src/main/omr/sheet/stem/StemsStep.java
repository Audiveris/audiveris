//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S t e m s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.stem;

import omr.sheet.SystemInfo;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

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
        // -> Stems links to heads & beams
        new StemsBuilder(system).linkStems();

        // Compute all contextual grades (for better UI)
        system.getSig().contextualize();
    }
}
