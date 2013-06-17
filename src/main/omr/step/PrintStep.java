//----------------------------------------------------------------------------//
//                                                                            //
//                             P r i n t S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.score.ScoresManager;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Class {@code PrintStep} prints the whole score (using physical layout)
 *
 * @author Hervé Bitteur
 */
public class PrintStep
        extends AbstractStep
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            PrintStep.class);

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // PrintStep //
    //-----------//
    /**
     * Creates a new PrintStep object.
     */
    public PrintStep ()
    {
        super(
                Steps.PRINT,
                Level.SCORE_LEVEL,
                Mandatory.OPTIONAL,
                DATA_TAB,
                "Write the output PDF file");
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
        ScoresManager.getInstance()
                .writePhysicalPdf(sheet.getScore(), null);
    }
}
