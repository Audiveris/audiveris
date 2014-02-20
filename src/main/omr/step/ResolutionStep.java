//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e s o l u t i o n S t e p                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Picture;
import static omr.sheet.Picture.SourceKey.*;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import java.util.Collection;

/**
 * Class {@code ResolutionStep} implements <b>RESOLUTION</b> step, which tries to reduce
 * the SIG incrementally.
 *
 * @author Hervé Bitteur
 */
public class ResolutionStep
        extends AbstractSystemStep
{
    //~ Constructors -------------------------------------------------------------------------------

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

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.sigSolver.solve();
    }

    @Override
    protected void doEpilog (Collection<SystemInfo> systems,
                             Sheet sheet)
            throws StepException
    {
        final Picture picture = sheet.getPicture();

        //        picture.disposeSource(INITIAL);
        // picture.disposeSource(BINARY);
        picture.disposeSource(GAUSSIAN);
        picture.disposeSource(MEDIAN);

        ///picture.disposeSource(STAFF_LINE_FREE);
    }
}
