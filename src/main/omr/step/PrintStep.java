//----------------------------------------------------------------------------//
//                                                                            //
//                             P r i n t S t e p                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.log.Logger;

import omr.score.ScoresManager;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

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
    private static final Logger logger = Logger.getLogger(PrintStep.class);

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
            Redoable.REDOABLE,
            DATA_TAB,
            "Write the output PDF file");
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // doit //
    //------//
    @Override
    public void doit (Collection<SystemInfo> systems,
                      Sheet                  sheet)
        throws StepException
    {
        ScoresManager.getInstance()
                     .writePhysicalPdf(sheet.getScore(), null);
    }
}
