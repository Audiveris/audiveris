//----------------------------------------------------------------------------//
//                                                                            //
//                               O c r C h a r                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.score.common.PixelRectangle;

import java.awt.Rectangle;

/**
 * Class {@code OcrChar} manages information about a OCR-decoded
 * character.
 *
 * @author Hervé Bitteur
 */
public class OcrChar
{
    //~ Instance fields --------------------------------------------------------

    /** Character value. */
    public final String value;

    /** Character bounds. */
    private final PixelRectangle bounds;

    //~ Constructors -----------------------------------------------------------
    //
    //---------//
    // OcrChar //
    //---------//
    /**
     * Creates a new OcrChar object.
     *
     * @param bounds the bounding box of this character wrt the decoded image
     * @param value  the character string value
     */
    public OcrChar (Rectangle bounds,
                    String value)
    {
        this.bounds = new PixelRectangle(bounds);
        this.value = value;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the bounding box of the char.
     *
     * @return (a copy of) the box
     */
    public PixelRectangle getBounds ()
    {
        return new PixelRectangle(bounds);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" \"").append(value).append("\"");

        sb.append(" ").append(getBounds());

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the coordinates of this char descriptor
     *
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    public void translate (int dx,
                           int dy)
    {
        bounds.translate(dx, dy);
    }
}
