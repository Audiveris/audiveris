//----------------------------------------------------------------------------//
//                                                                            //
//                         R e s o l u t i o n S t e p                        //
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
 * Class {@code ResolutionStep}
 *
 * @author Hervé Bitteur
 */
public class ResolutionStep
        extends AbstractSystemStep
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ResolutionStep object.
     */
    public ResolutionStep ()
    {
        super(
                Steps.RESOLUTION,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Resolve interpretations");
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.sigResolver.solve();
    }
}
