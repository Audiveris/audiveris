//----------------------------------------------------------------------------//
//                                                                            //
//                       S c o r e C o n t r o l l e r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;

import omr.log.Logger;

import omr.score.Score;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;

/**
 * Class <code>ScoreController</code> encapsulates a set of user interface means
 * on top of ScoreManager, related to score handling actions, typically
 * triggered through menus and buttons.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
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
        Sheet sheet = SheetManager.getSelectedSheet();

        if (sheet != null) {
            return sheet.getScore();
        }

        return null;
    }

    //--------------//
    // setScoreView //
    //--------------//
    /**
     * Set the various display parameter of a view for the given score, if such
     * score is provided, or a blank tab otherwise.
     *
     * @param score the desired score if any, null otherwise
     */
    public void setScoreView (Score score)
    {
        Sheet sheet = null;

        if (score != null) {
            // Make sure we have a proper score view
            ScoreView view = score.getView();

            if (view == null) {
                // Build a brand new display on this score
                view = new ScoreView(score);
            } else {
                // So that scroll bars be OK
                view.computeModelSize();
            }

            // Make sure the view is part of the related sheet assembly
            sheet = score.getSheet();
            sheet.getAssembly()
                 .setScoreView(view);
        }

        SheetManager.setSelectedSheet(sheet);
    }
}
