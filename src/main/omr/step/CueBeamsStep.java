//----------------------------------------------------------------------------//
//                                                                            //
//                           C u e B e a m s S t e p                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.SystemInfo;

/**
 * Class {@code CueBeamsStep} implements <b>CUE_BEAMS</b> step, which
 * attempts to retrieve beams for cue notes.
 *
 * @author Hervé Bitteur
 */
public class CueBeamsStep
        extends AbstractSystemStep
{
    //~ Constructors -----------------------------------------------------------

    //--------------//
    // CueBeamsStep //
    //--------------//
    /**
     * Creates a new CueBeamsStep object.
     */
    public CueBeamsStep ()
    {
        super(
                Steps.CUE_BEAMS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve cue beams");
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.beamsBuilder.buildCueBeams(); // -> Cue beams
    }
}
