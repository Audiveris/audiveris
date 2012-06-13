//----------------------------------------------------------------------------//
//                                                                            //
//                               O c r L i n e                                //
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
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code OcrLine} defines a non-mutable structure to report
 * all information on one OCR-decoded line.
 *
 * @author Hervé Bitteur
 */
public class OcrLine
{
    //~ Instance fields --------------------------------------------------------

    /** Line bounds */
    private final PixelRectangle bounds;

    /** Detected line content */
    private final String value;

    /** Words that compose this line */
    private final List<OcrWord> words = new ArrayList<>();

    //~ Constructors -----------------------------------------------------------

    //
    //---------//
    // OcrLine //
    //---------//
    /**
     * Creates a new OcrLine object.
     *
     * @param bounds the line bounding box
     * @param value  the string ascii value
     * @param words  the seqeunce of words
     */
    public OcrLine (Rectangle     bounds,
                    String        value,
                    List<OcrWord> words)
    {
        this(bounds, value);

        for (OcrWord word : words) {
            this.words.add(word);
        }
    }

    //---------//
    // OcrLine //
    //---------//
    /**
     * Creates a new OcrLine object.
     *
     * @param bounds the line bounding box
     * @param value  the string ascii value
     */
    public OcrLine (Rectangle bounds,
                    String    value)
    {
        this.bounds = new PixelRectangle(bounds);
        this.value = value;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // addWord //
    //---------//
    public void addWord (OcrWord word)
    {
        words.add(word);
    }

    //
    //------//
    // dump //
    //------//
    public void dump ()
    {
        for (OcrWord word : words) {
            System.out.println(word.toString());
        }
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Report the contour box of the line
     *
     * @return a COPY of the contour box
     */
    public PixelRectangle getBounds ()
    {
        return new PixelRectangle(bounds);
    }

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of chars descriptors (of words)
     *
     * @return the chars
     */
    public List<OcrChar> getChars ()
    {
        List<OcrChar> chars = new ArrayList<>();

        for (OcrWord word : words) {
            chars.addAll(word.getChars());
        }

        return chars;
    }

    //--------------//
    // getFirstWord //
    //--------------//
    public OcrWord getFirstWord ()
    {
        if (!words.isEmpty()) {
            return words.get(0);
        } else {
            return null;
        }
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the string value
     *
     * @return the value
     */
    public String getValue ()
    {
        return value;
    }

    //----------//
    // getWords //
    //----------//
    /**
     * Report the sequence of words descriptors
     *
     * @return the words
     */
    public List<OcrWord> getWords ()
    {
        return words;
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
          .append(getValue())
          .append("\"");

        if (!words.isEmpty()) {
            // TODO: Choose the most representative word, rather than first one?
            sb.append(" ")
              .append(words.get(0).getFontInfo());
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the coordinates of words descriptors
     *
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    public void translate (int dx,
                           int dy)
    {
        for (OcrWord word : words) {
            word.translate(dx, dy);
        }
    }
}
