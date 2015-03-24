//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C u e B e a m s S t e p                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.beam;

import omr.sheet.SystemInfo;
import omr.sheet.beam.BeamsBuilder;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

/**
 * Class {@code CueBeamsStep} implements <b>CUE_BEAMS</b> step, which attempts to
 * retrieve beams for cue notes.
 *
 * @author Hervé Bitteur
 */
public class CueBeamsStep
        extends AbstractSystemStep<Void>
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new CueBeamsStep object.
     */
    public CueBeamsStep ()
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
        new BeamsBuilder(system, null).buildCueBeams(); // -> Cue beams
    }
}
