//----------------------------------------------------------------------------//
//                                                                            //
//                       S c o r e C o n t r o l l e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ScoreController} is only a convenient way to retrieve
 * the current score (which contains the sheet currently selected by
 * the user).
 *
 * @author Hervé Bitteur
 */
public class ScoreController
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ScoreController.class);

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // ScoreController //
    //-----------------//
    /**
     * No meant to be instantiated
     */
    private ScoreController ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // getCurrentScore //
    //-----------------//
    /**
     * Convenient method to get the current score instance
     *
     * @return the current score instance, or null
     */
    public static Score getCurrentScore ()
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet != null) {
            return sheet.getScore();
        }

        return null;
    }
}
