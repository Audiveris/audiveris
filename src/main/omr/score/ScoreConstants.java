//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e C o n s t a n t s                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
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
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    /** Height in pixels of a staff display */
    public static final int STAFF_AREA_HEIGHT = constants.staffAreaheight.getValue();

    /** Height in pixels above/under a staff display */
    public static final int STAFF_MARGIN_HEIGHT = constants.staffMarginHeight.getValue();

    /** Horizontal offset in pixels of the score origin */
    public static final int SCORE_INIT_X = constants.scoreInitX.getValue();

    /** Vertical offset in pixels of the score origin */
    public static final int SCORE_INIT_Y = constants.scoreInitY.getValue();

    /** Horizontal gutter in pixels between two systems */
    public static final int INTER_SYSTEM = constants.interSystem.getValue();

    /** Vertical distance in pixels between two lines of a standard staff :
       {@value} */
    public static final int INTER_LINE = 16;

    /** Horizontal gutter in pixels between two pages */
    public static final int INTER_PAGE = constants.interPage.getValue();

    /** Number of lines in a staff */
    public static final int LINE_NB = constants.lineNb.getValue();

    /** Height in pixels of one staff */
    public static final int STAFF_HEIGHT = (LINE_NB - 1) * INTER_LINE;

    /** Used to code fractions with an integer value, with a resolution of
       1/{@value} */
    public static final int BASE = 1024;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // ScoreConstants // Not meant to be instantiated
    //----------------//
    private ScoreConstants ()
    {
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.Integer interPage = new Constant.Integer(
            200,
            "Horizontal gutter in pixels between two pages");
        Constant.Integer interSystem = new Constant.Integer(
            100,
            "Horizontal gutter in pixels between two systems");
        Constant.Integer lineNb = new Constant.Integer(
            5,
            "Number of lines in a staff");
        Constant.Integer scoreInitX = new Constant.Integer(
            100,
            "Horizontal offset in pixels of the score origin");
        Constant.Integer scoreInitY = new Constant.Integer(
            150,
            "Vertical offset in pixels of the score origin");
        Constant.Integer staffAreaheight = new Constant.Integer(
            100,
            "Height in pixels of a staff display");
        Constant.Integer staffMarginHeight = new Constant.Integer(
            40,
            "Height in pixels above/under a staff display : ");

        Constants ()
        {
            initialize();
        }
    }
}
