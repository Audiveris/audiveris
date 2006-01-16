//-----------------------------------------------------------------------//
//                                                                       //
//                       U I L o o k A n d F e e l                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.util.Logger;

// import com.jgoodies.looks.windows.*;
// import com.jgoodies.looks.plastic.*;
// import com.jgoodies.looks.plastic.theme.*;

import javax.swing.UIManager;

/**
 * Class <code>UILookAndFeel</code> enables to select the UI Look & Feel to
 * be used in this application.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UILookAndFeel
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(UILookAndFeel.class);
    private static final Constants constants = new Constants();

    //~ Constructors ------------------------------------------------------

    private UILookAndFeel()
    {
    }

    //~ Methods -----------------------------------------------------------
        // Available Themes:
        //
        // AbstractSkyTheme
        // BrownSugar
        // Colors
        // DarkStar
        // DesertBlue
        // DesertBluer
        // DesertGreen
        // DesertRed
        // DesertYellow
        // ExperienceBlue
        // ExperienceGreen
        // Silver
        // SkyBlue
        // SkyBluer
        // SkyBluerTahoma
        // SkyGreen
        // SkyKrupp
        // SkyPink
        // SkyRed
        // SkyYellow

//         try {
//             // Available Look & Feel:
//             //
//             // WindowsLookAndFeel
//             // PlasticLookAndFeel
//             // Plastic3DLookAndFeel
//             // PlasticXPLookAndFeel

//             PlasticLookAndFeel.setMyCurrentTheme(new SkyKrupp());
//             UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
//         } catch (Exception ex) {
//             ex.printStackTrace();
//         }

    //-------//
    // setUI //
    //-------//
    /**
     * Set the UI look & feel. If a non-null class name is provided, it is
     * used (and remembered in the related constant), otherwise the related
     * constant is used.
     *
     * @param className the full path to the desired UI class name
     */
    public static void setUI(String className)
    {
        com.jgoodies.looks.Options.setUseNarrowButtons(true);
        try {
            if (className != null) {
                UIManager.setLookAndFeel(className);
                constants.lookAndFeel.setValue(className);
            } else {
                UIManager.setLookAndFeel(constants.lookAndFeel.getValue());
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
            logger.warning(ex.toString());
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        Constant.String lookAndFeel = new Constant.String
                ("com.jgoodies.looks.plastic.Plastic3DLookAndFeel",
                 "Full class path to the desired UI Look & Feel");

        Constants ()
        {
            initialize();
        }
    }
}
