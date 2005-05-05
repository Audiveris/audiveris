//-----------------------------------------------------------------------//
//                                                                       //
//                            I c o n U t i l                            //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.util.Logger;

import java.net.URL;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Class <code>IconUtil</code> gathers utilities for icon management
 */
public class IconUtil
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(IconUtil.class);

    //~ Constructors ------------------------------------------------------

    // No instantiation
    private IconUtil()
    {
    }

    //~ Methods -----------------------------------------------------------

    //--------//
    // iconOf //
    //--------//
    /**
     * Build an icon, given its name and size.
     *
     * @param path the directoty path where the image is to be found
     * @param fname name of the icon
     *
     * @return the newly built icon
     */
    public static Icon iconOf (String path,
                               String fname)
    {
        final String resName = path + "/" + fname + ".gif";
        final URL iconUrl = Jui.class.getResource(resName);
        if (iconUrl == null) {
            logger.warning("iconOf. Could not load icon from " + resName);

            return null;
        }

        return new ImageIcon(iconUrl);
    }

    //--------------//
    // buttonIconOf //
    //--------------//
    /**
     * Build an icon, searched in the button directory.
     *
     * @param fname name of the icon
     *
     * @return the newly built icon
     */
    public static Icon buttonIconOf (String fname)
    {
        return iconOf("/toolbarButtonGraphics",
                      fname + constants.buttonIconSize.getValue());
    }

    //-------------//
    // musicIconOf //
    //-------------//
    /**
     * Build an icon, searched in the music directory.
     *
     * @param fname name of the icon
     *
     * @return the newly built icon
     */
    public static Icon musicIconOf (String fname)
    {
        return iconOf("/music",
                      fname);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Constant.Integer buttonIconSize = new Constant.Integer
                (16,
                 "Size of toolbar icons (16 or 24)");

        Constant.Integer musicIconSize = new Constant.Integer
                (32,
                 "Size of music icons (16 or 32)");

        Constants ()
        {
            initialize();
        }
    }
}
