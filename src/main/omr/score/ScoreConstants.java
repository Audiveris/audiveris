//-----------------------------------------------------------------------//
//                                                                       //
//                      S c o r e C o n s t a n t s                      //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

/**
 * Class <code>ScoreConstants</code> gathers all constants related to the
 * display of a score.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreConstants
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();

    /** Height in pixels of a stave display */
    public static final int STAVE_AREA_HEIGHT = constants.staveAreaheight.getValue();

    /** Height in pixels above/under a stave display */
    public static final int STAVE_MARGIN_HEIGHT = constants.staveMarginHeight.getValue();

    /** Horizontal offset in pixels of the score origin */
    public static final int SCORE_INIT_X = constants.scoreInitX.getValue();

    /** Vertical offset in pixels of the score origin */
    public static final int SCORE_INIT_Y = constants.scoreInitY.getValue();

    /** Horizontal gutter in pixels between two systems */
    public static final int INTER_SYSTEM = constants.interSystem.getValue();

    /** Vertical distance in pixels between two lines of a staff : {@value} */
    public static final int INTER_LINE = 16;

    /** Horizontal gutter in pixels between two pages */
    public static final int INTER_PAGE = constants.interPage.getValue();

    /** Number of lines in a staff */
    public static final int LINE_NB = constants.lineNb.getValue();

    /** Height in pixels of one staff */
    public static final int STAFF_HEIGHT = (LINE_NB - 1) * INTER_LINE;

    /** Used to code fractions with an integer value, with a resolution of 1/{@value} */
    public static final int BASE = 1024;

    //~ Constructors ------------------------------------------------------

    //----------------//
    // ScoreConstants // Not to be instantiated
    //----------------//
    private ScoreConstants ()
    {
    }

    //~ Classes -----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        Constant.Integer staveAreaheight = new Constant.Integer
                (100,
                 "Height in pixels of a stave display");

        Constant.Integer staveMarginHeight = new Constant.Integer
                (40,
                 "Height in pixels above/under a stave display : ");

        Constant.Integer scoreInitX = new Constant.Integer
                (200,
                 "Horizontal offset in pixels of the score origin");

        Constant.Integer scoreInitY = new Constant.Integer
                (150,
                 "Vertical offset in pixels of the score origin");

        Constant.Integer interSystem = new Constant.Integer
                (100,
                 "Horizontal gutter in pixels between two systems");

        Constant.Integer interPage = new Constant.Integer
                (200,
                 "Horizontal gutter in pixels between two pages");

        Constant.Integer lineNb = new Constant.Integer
                (5,
                 "Number of lines in a staff");

        Constant.Integer base = new Constant.Integer
                (1024,
                 "Used to code fractions with an integer value, with a resolution of 1/value");

        Constants ()
        {
            initialize();
        }
    }
}
