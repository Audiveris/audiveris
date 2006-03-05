//-----------------------------------------------------------------------//
//                                                                       //
//                          S y m b o l I c o n                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui.icon;

import java.awt.Image;
import javax.swing.*;
import omr.ui.*;

/**
 * Class <code>SymbolIcon</code> is an icon, built from a provided image,
 * with consistent width among all defined symbol icons to ease their
 * presentation in menus.
 */
public class SymbolIcon
    extends ImageIcon
{
    //~ Static variables/initializers -------------------------------------

    // the same width for all such icons
    private static int standardWidth = 0;

    //~ Constructors ------------------------------------------------------

    //------------//
    // SymbolIcon //
    //------------//
    public SymbolIcon (Image image)
    {
        super(image);

        // Gradually update the common standard width
        if (getActualWidth() > getIconWidth()) {
            setStandardWidth(getActualWidth());
        }
    }

    //~ Methods -----------------------------------------------------------


    //----------------//
    // getActualWidth //
    //----------------//
    /**
     * Report the ACTUAL width of the icon (used when storing the icon)
     *
     * @return the real icon width in pixels
     */
    public int getActualWidth()
    {
        return getImage().getWidth(null);
    }

    //--------------//
    // getIconWidth //
    //--------------//
    /**
     * Report the STANDARD width of the icon (used by swing when painting)
     *
     * @return the standard width in pixels
     */
    @Override
    public int getIconWidth()
    {
        return standardWidth;
    }

    //------------------//
    // setStandardWidth //
    //------------------//
    /**
     * Define the STANDARD width for all icons
     *
     * @param standardWidth the standard width in pixels for all such icons
     */
    public static void setStandardWidth (int standardWidth)
    {
        SymbolIcon.standardWidth = standardWidth;
    }
}

