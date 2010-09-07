//----------------------------------------------------------------------------//
//                                                                            //
//                       S c o r e C o n t r o l l e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.log.Logger;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.ui.SheetsController;

/**
 * Class <code>ScoreController</code> encapsulates a set of user interface means
 * on top of ScoreManager, related to score handling actions, typically
 * triggered through menus and buttons.
 *
 * @author Hervé Bitteur
 */
public class ScoreController
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ScoreController.class);

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // ScoreController //
    //-----------------//
    /**
     * Create the Controller instance
     */
    public ScoreController ()
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
        Sheet sheet = SheetsController.selectedSheet();

        if (sheet != null) {
            return sheet.getScore();
        }

        return null;
    }

    //----------------//
    // setScoreEditor //
    //----------------//
    /**
     * Set the various display parameter of an editor view for the given score,
     * if such score is provided
     *
     * @param score the desired score if any, null otherwise
     */
    public void setScoreEditor (Score score)
    {
        Sheet sheet = null;

        if (score != null) {
            // Make sure we have a proper score view
            ScoreEditor editor = score.getEditor();

            if (editor == null) {
                // Build a brand new display on this score
                editor = new ScoreEditor(score);
                score.setEditor(editor);
            } else {
                // So that scroll bars be OK
                editor.update();
            }

            // Make sure the editor view is part of the related sheet assembly
            sheet = score.getSheet();
            sheet.getAssembly()
                 .setScoreEditor(editor);
        }

        SheetsController.getInstance()
                        .setSelectedSheet(sheet);
    }
}
