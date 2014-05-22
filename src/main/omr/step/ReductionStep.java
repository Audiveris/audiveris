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

import omr.sheet.Picture;
import static omr.sheet.Picture.SourceKey.*;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph.ReductionMode;

import java.util.Collection;

/**
 * Class {@code ReductionStep} implements <b>REDUCTION</b> step, which tries to reduce
 * the SIG incrementally after structures (notes + stems + beams) have been retrieved.
 *
 * @author Hervé Bitteur
 */
public class ReductionStep
        extends AbstractSystemStep
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new ReductionStep object.
     */
    public ReductionStep ()
    {
        super(
                Steps.REDUCTION,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Reduce structures");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system)
            throws StepException
    {
        system.sigReducer.reduce(ReductionMode.STRICT);
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
