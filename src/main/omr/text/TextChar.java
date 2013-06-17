//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t C h a r                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import java.awt.Rectangle;

/**
 * Class {@code TextChar} manages information about a OCR-decoded
 * character.
 *
 * @author Hervé Bitteur
 */
public class TextChar
        extends TextItem
{
    //~ Constructors -----------------------------------------------------------

    //
    //---------//
    // TextChar //
    //---------//
    /**
     * Creates a new TextChar object.
     *
     * @param bounds the bounding box of this character wrt the decoded image
     * @param value  the character string value
     */
    public TextChar (Rectangle bounds,
                     String value)
    {
        super(bounds, value);
    }
}
