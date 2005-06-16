//-----------------------------------------------------------------------//
//                                                                       //
//                             B a r l i n e                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import java.awt.*;

/**
 * Enum <code>Barline</code> handles the enumeration of various kinds of
 * bar lines.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum Barline
{
    //~ Static variables/initializers -------------------------------------

    /**
     * Single bar line
     */
    SINGLE ("SINGLE_BARLINE"),

    /**
     * Double bar line
     */
    DOUBLE ("DOUBLE_BARLINE"),

    /**
     * Bar line with repeat indication on the left
     */
    LEFT_REPEAT ("LEFT_REPEAT_SIGN"),

    /**
     * Bar line with repeat indication on the right
     */
    RIGHT_REPEAT ("RIGHT_REPEAT_SIGN"),

    /**
     * Bar line with repeat indications on both sides
     */
    BACKTOBACK_REPEAT ("BACKTOBACK_REPEAT"),

    /**
     * Ending bar line made of a thin bar and a thick bar
     */
    THIN_THICK ("FINAL_BARLINE");

    //~ Instance variables ---------------------------------------------------

    // Displayed icon
    private final Image image;

    //~ Constructors ------------------------------------------------------

    //---------//
    // Barline //
    //---------//
    private Barline (String imageName)
    {
        image = null;                   // Place Holder

//         // Load barline image
//         if (imageName != null) {
//             image = Symbols.get(imageName);
//         }
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getImage //
    //----------//

    /**
     * Report the image to be used to display a specific bar line in the
     * score representations
     *
     * @return the image ready for display
     */
    public Image getImage ()
    {
        return image;
    }
}
