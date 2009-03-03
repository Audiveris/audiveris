//----------------------------------------------------------------------------//
//                                                                            //
//                              C h a r D e s c                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import java.awt.Rectangle;

/**
 * Class <code>CharDesc</code> manages information about a OCR-decoded char,
 * such as provided by tesseract dlltest
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class CharDesc
{
    //~ Instance fields --------------------------------------------------------

    /** Character codePoint */
    public final int codePoint;

    /** Character bounding box */
    private final Rectangle box;

    /** Flag for a char that follows one or several spaces */
    public final boolean afterSpace;

    /** Flag for a char that follows a dash */
    public final boolean afterDash;

    /** Character value */
    private final String content;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // CharDesc //
    //----------//
    /**
     * Creates a new CharDesc object.
     *
     * @param codePoint the character codepoint
     * @param box the bounding box of this character wrt the decoded image
     * @param afterSpace if the char occurs right after a space
     * @param afterDash if the char occurs right after a dash
     */
    public CharDesc (int       codePoint,
                     Rectangle box,
                     boolean   afterSpace,
                     boolean   afterDash)
    {
        this.codePoint = codePoint;
        this.box = box;
        this.afterSpace = afterSpace;
        this.afterDash = afterDash;

        content = String.valueOf(Character.toChars(codePoint));
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // isDash //
    //--------//
    public boolean isDash ()
    {
        return content.equals("-");
    }

    //--------//
    // getBox //
    //--------//
    public Rectangle getBox ()
    {
        return box;
    }

    //------------------//
    // toInternalString //
    //------------------//
    public String toInternalString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(" ")
          .append(Character.toChars(codePoint))
          .append("(")
          .append(codePoint)
          .append(")");
        sb.append(" ")
          .append(getBox());

        if (afterSpace) {
            sb.append(" *AS*");
        }

        if (afterDash) {
            sb.append(" *AD*");
        }

        sb.append("}");

        return sb.toString();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return the character value (say ASCII) of the char description
     * @return the recognized character value
     */
    @Override
    public String toString ()
    {
        return content;
    }
}
