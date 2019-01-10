//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   U I L o o k A n d F e e l                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.util;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Class {@code UILookAndFeel} enables to select the UI Look and Feel to be used in this
 * application.
 *
 * @author Hervé Bitteur and Brenton Partridge
 */
public class UILookAndFeel
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(UILookAndFeel.class);

    static {
        if (WellKnowns.MAC_OS_X) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            //                  System.setProperty("apple.awt.brushMetalLook", "true");
            //                  System.setProperty("apple.awt.brushMetalRounded", "true");
            constants.lookAndFeel.setStringValue(UIManager.getSystemLookAndFeelClassName());
        }
    }

    private UILookAndFeel ()
    {
    }

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
     * Set the UI look and feel.
     * If a non-null class name is provided, it is used (and remembered in the related constant),
     * otherwise the related constant is used.
     *
     * @param className the full path to the desired UI class name
     */
    public static void setUI (String className)
    {
        com.jgoodies.looks.Options.setUseNarrowButtons(true);

        try {
            if (className != null) {
                UIManager.setLookAndFeel(className);
                constants.lookAndFeel.setStringValue(className);
            } else {
                UIManager.setLookAndFeel(constants.lookAndFeel.getValue());
            }
        } catch (ClassNotFoundException |
                 IllegalAccessException |
                 InstantiationException |
                 UnsupportedLookAndFeelException ex) {
            //ex.printStackTrace();
            logger.warn(ex.toString());
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.String lookAndFeel = new Constant.String(
                "com.jgoodies.looks.plastic.Plastic3DLookAndFeel",
                "Full class path to the desired UI Look & Feel");
    }
}
