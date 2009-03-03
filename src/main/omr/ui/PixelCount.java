//-----------------------------------------------------------------------//
//                                                                       //
//                          P i x e l C o u n t                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//
package omr.ui;

import omr.constant.Constant;

/**
 * A subclass of Constant.Integer, meant to store a number of pixels.
 */
public class PixelCount
    extends Constant.Integer
{
    //~ Constructors -----------------------------------------------------------

    //------------//
    // PixelCount //
    //------------//
    /**
     * Specific constructor, where 'unit' and 'name' are assigned later
     *
     * @param defaultValue the (int) default value
     * @param description  the semantic of the constant
     */
    public PixelCount (int              defaultValue,
                       java.lang.String description)
    {
        super("Pixels", defaultValue, description);
    }
}
