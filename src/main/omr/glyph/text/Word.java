//----------------------------------------------------------------------------//
//                                                                            //
//                                  W o r d                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.glyph.facets.Glyph;

import java.util.List;

/**
 * Class {@code Word}
 *
 * @author Hervé Bitteur
 */
public class Word
    implements Comparable<Word>
{
    //~ Instance fields --------------------------------------------------------

    /** String content */
    private final String text;

    /** OCR chars descriptors */
    private final List<OcrChar> chars;

    /** Underlying glyph */
    Glyph glyph;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Word object.     *
     * @param text DOCUMENT ME!
     * @param chars DOCUMENT ME!
     */
    public Word (String        text,
                 List<OcrChar> chars)
    {
        this.text = text;
        this.chars = chars;
    }

    //~ Methods ----------------------------------------------------------------

    // Order by ascending text length
    @Override
    public int compareTo (Word other)
    {
        if (this == other) {
            return 0;
        }

        if (this.text.length() <= other.text.length()) {
            return -1;
        }

        return +1;
    }

    //----------//
    // getChars //
    //----------//
    /**
     * @return the chars
     */
    public List<OcrChar> getChars ()
    {
        return chars;
    }

    //---------//
    // getText //
    //---------//
    /**
     * @return the text
     */
    public String getText ()
    {
        return text;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Word");
        sb.append(" text:")
          .append(text);

        if (glyph != null) {
            sb.append(" ")
              .append(glyph.idString());
        }

        for (OcrChar ch : chars) {
            sb.append("\n")
              .append(ch);
        }

        sb.append("\n}");

        return sb.toString();
    }
}
