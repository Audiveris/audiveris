//----------------------------------------------------------------------------//
//                                                                            //
//                         U I L o o k A n d F e e l                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.UIManager;

/**
 * Class {@code UILookAndFeel} enables to select the UI Look & Feel to
 * be used in this application.
 *
 * @author Hervé Bitteur and Brenton Partridge
 */
public class UILookAndFeel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            UILookAndFeel.class);

    static {
        if (WellKnowns.MAC_OS_X) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            //                  System.setProperty("apple.awt.brushMetalLook", "true");
            //                  System.setProperty("apple.awt.brushMetalRounded", "true");
            constants.lookAndFeel.setValue(
                    UIManager.getSystemLookAndFeelClassName());
        }
    }

    //~ Constructors -----------------------------------------------------------
    private UILookAndFeel ()
    {
    }

    //~ Methods ----------------------------------------------------------------
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
    public static void setUI (String className)
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
            logger.warn(ex.toString());
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String lookAndFeel = new Constant.String(
                "com.jgoodies.looks.plastic.Plastic3DLookAndFeel",
                "Full class path to the desired UI Look & Feel");

    }
}
