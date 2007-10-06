//-----------------------------------------------------------------------//
//                                                                       //
//                              U I T e s t                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui;

import omr.score.Score;
import omr.score.ScoreActions;
import omr.score.ScoreController;

import omr.util.Logger;

import javax.swing.*;

/**
 * A utility class, just used for small test action triggered from UI
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UITest
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UITest.class);

    //~ Constructors -----------------------------------------------------------

    private UITest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    public static void test ()
    {
        Score score = ScoreController.getCurrentScore();

        if (score != null) {
            ScoreActions.defineParameters(score);
        }
    }
}
