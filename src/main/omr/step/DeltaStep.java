//----------------------------------------------------------------------------//
//                                                                            //
//                              D e l t a S t e p                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.Sheet;
import omr.sheet.SheetDiff;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code DeltaStep} computes the delta value as a kind of
 * recognition level on a whole sheet.
 *
 * @author Hervé Bitteur
 */
public class DeltaStep
        extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            DeltaStep.class);

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // DeltaStep //
    //-----------//
    /**
     * Creates a new DeltaStep object.
     */
    public DeltaStep ()
    {
        super(
                Steps.DELTA,
                Level.SCORE_LEVEL,
                Mandatory.OPTIONAL,
                DATA_TAB,
                "Compute page delta");
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> systems,
                      Sheet sheet)
            throws StepException
    {
        SheetDiff sheetDelta = new SheetDiff(sheet);
        sheetDelta.computeDiff();
    }
}
