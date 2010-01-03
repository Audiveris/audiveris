//----------------------------------------------------------------------------//
//                                                                            //
//                               O c r C h a r                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.score.common.PixelRectangle;

/**
 * Class <code>OcrChar</code> manages information about a OCR-decoded character
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class OcrChar
{
    //~ Instance fields --------------------------------------------------------

    /** Character value */
    public final String content;

    /** Character bounding box, relative to top-left image origin */
    private final PixelRectangle box;

    /** Font size of char */
    public final int fontSize;

    /* Number of spaces before this char */
    public final int blanks;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // OcrChar //
    //---------//
    /**
     * Creates a new OcrChar object.
     *
     * @param content the character string value
     * @param box the bounding box of this character wrt the decoded image
     * @param fontSize the font size for this char
     * @param blanks the number of spaces before this char
     */
    public OcrChar (String         content,
                    PixelRectangle box,
                    int            fontSize,
                    int            blanks)
    {
        this.content = content;
        this.box = box;
        this.fontSize = fontSize;
        this.blanks = blanks;
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // getBox //
    //--------//
    /**
     * Return the bounding box of the char
     * @return (a copy of) the box
     */
    public PixelRectangle getBox ()
    {
        return new PixelRectangle(box);
    }

    //--------//
    // isDash //
    //--------//
    public boolean isDash ()
    {
        return content.equals("-");
    }

    //-----------------//
    // hasSpacesBefore //
    //-----------------//
    /**
     * Report whether there must be one or several spaces before this character
     * Note: the precise number of spaces, as reported by Tesseract, is not at
     * all reliable
     * @return true if at least one space is present
     */
    public boolean hasSpacesBefore ()
    {
        return (blanks > 0) || isDash();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        sb.append(" \"")
          .append(content)
          .append("\"");

        sb.append(" ")
          .append(getBox());

        sb.append(" font:")
          .append(fontSize);

        if (blanks > 0) {
            sb.append(" blanks:")
              .append(blanks);
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the coordinates of this char descriptor
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    public void translate (int dx,
                           int dy)
    {
        box.translate(dx, dy);
    }
}
