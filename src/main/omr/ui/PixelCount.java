//----------------------------------------------------------------------------//
//                                                                            //
//                            P i x e l C o u n t                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
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
     * Specific constructor, where 'unit' and 'name' are assigned later.
     *
     * @param defaultValue the (int) default value
     * @param description  the semantic of the constant
     */
    public PixelCount (int defaultValue,
                       java.lang.String description)
    {
        super("Pixels", defaultValue, description);
    }
}
