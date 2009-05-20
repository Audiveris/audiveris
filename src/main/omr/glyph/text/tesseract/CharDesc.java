//----------------------------------------------------------------------------//
//                                                                            //
//                              C h a r D e s c                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text.tesseract;

import java.awt.Rectangle;

/**
 * Class <code>CharDesc</code> manages information about a OCR-decoded char,
 * such as provided by tesseract tessdll.dll
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class CharDesc
{
    //~ Instance fields --------------------------------------------------------

    /** Character value */
    public final String content;

    /** Character bounding box, relative to top-left origin (often the glyph) */
    private final Rectangle box;

    /** Font size of char */
    public final int fontSize;

    /* Number of spaces before this char */
    public final int blanks;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // CharDesc //
    //----------//
    /**
     * Creates a new CharDesc object.
     *
     * @param content the character string value
     * @param box the bounding box of this character wrt the decoded image
     * @param fontSize the font size for this char
     * @param blanks the number of spaces before this char
     */
    public CharDesc (String    content,
                     Rectangle box,
                     int       fontSize,
                     int       blanks)
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
    public Rectangle getBox ()
    {
        return new Rectangle(box);
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

        sb.append(" ")
          .append(content);

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
}
