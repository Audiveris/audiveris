//----------------------------------------------------------------------------//
//                                                                            //
//                               O c r L i n e                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.text;

import omr.ui.symbol.TextFont;
import omr.score.common.PixelRectangle;

import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>OcrLine</code> defines an non-mutable structure to report useful
 * info on one OCR-decoded line
 *
 * @author Hervé Bitteur
 */
public class OcrLine
{
    //~ Instance fields --------------------------------------------------------

    /** Detected font size, defined in points. Null if invalid */
    public final Float fontSize;

    /** Detected line content */
    public final String value;

    /** Chars that compose this line */
    private final List<OcrChar> chars = new ArrayList<OcrChar>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new OcrLine object.
     *
     * @param fontSize the detected font size, or -1 if not known
     * @param chars the sequence of character descriptors
     * @param value the string ascii value
     */
    public OcrLine (float         fontSize,
                    List<OcrChar> chars,
                    String        value)
    {
        this.fontSize = fontSize;
        this.chars.addAll(chars);

        if (value != null) {
            this.value = value;
        } else {
            this.value = computeValue();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of char descriptors
     * @return the chars
     */
    public List<OcrChar> getChars ()
    {
        return chars;
    }

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Report the contour box of the line
     * @return the contour box
     */
    public PixelRectangle getContourBox ()
    {
        PixelRectangle contour = null;

        for (OcrChar ch : chars) {
            PixelRectangle box = ch.getBox();

            if (contour == null) {
                contour = box;
            } else {
                contour = contour.union(box);
            }
        }

        return contour;
    }

    //-----------------//
    // isFontSizeValid //
    //-----------------//
    /**
     * Report whether the font size field contains a valid value
     * @return true if font size is valid
     */
    public boolean isFontSizeValid ()
    {
        return fontSize != null;
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        for (OcrChar ch : chars) {
            System.out.println(ch.toString());
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());
        sb.append(" font:")
          .append(fontSize);
        sb.append(" \"")
          .append(value)
          .append("\"");
        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply a translation to the coordinates of char descriptors
     * @param dx abscissa translation
     * @param dy ordinate translation
     */
    public void translate (int dx,
                           int dy)
    {
        for (OcrChar ch : chars) {
            ch.translate(dx, dy);
        }
    }

    //--------------//
    // computeValue //
    //--------------//
    /**
     * Compute the string value of the line, using a smart positioning of the
     * various chars, since the count of blanks as provided by Tesseract is
     * often underestimated.
     * @return the string value of this line
     */
    private String computeValue ()
    {
        // Font used for space computation only
        Font          font = TextFont.baseTextFont.deriveFont((float) fontSize);

        // Retrieve half standard space width with this font
        double        halfSpace = TextFont.computeWidth(" ", font); // / 2;

        // Abscissa of right side of previous char
        int           lastRight = 0;

        // Line content so far
        StringBuilder sb = new StringBuilder();

        // Loop on char descriptions
        int index = 0;

        for (OcrChar ch : chars) {
            Rectangle box = ch.getBox();

            // Do we need to insert spaces?
            if ((index != 0) && ch.hasSpacesBefore()) {
                StringBuilder spaces = new StringBuilder();
                spaces.append(" "); // At least one!

                // Add all spaces needed to insert char at target location
                double gap = ch.getBox().x - lastRight - halfSpace;

                while (TextFont.computeWidth(spaces.toString(), font) < gap) {
                    spaces.append(" ");
                }

                sb.append(spaces);
            }

            sb.append(ch.content);
            lastRight = box.x + box.width;
            index++;
        }

        return sb.toString();
    }
}
