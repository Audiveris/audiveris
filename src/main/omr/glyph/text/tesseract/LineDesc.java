//----------------------------------------------------------------------------//
//                                                                            //
//                              L i n e D e s c                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text.tesseract;

import omr.glyph.text.*;

import omr.log.Logger;

import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>LineDesc</code> handles information about a line (composed of
 * CharDesc instances) as decoded by an OCR such as Tesseract tessdll.dll
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class LineDesc
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LineDesc.class);

    //~ Instance fields --------------------------------------------------------

    /** Chars that compose this line */
    private final List<CharDesc> chars = new ArrayList<CharDesc>();

    /** The measured font size */
    protected Integer fontSize;

    /** The string value of this line, with chars precisely positioned */
    protected String content;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // LineDesc //
    //----------//
    /**
     * Creates a new LineDesc object.
     *
     * @param charDesc the first char of the line
     */
    public LineDesc (CharDesc charDesc)
    {
        addChar(charDesc);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of char descriptors
     * @return the chars
     */
    public List<CharDesc> getChars ()
    {
        return chars;
    }

    //------------//
    // getContent //
    //------------//
    /**
     * Report the string value of the line, using a smart positioning of the
     * various chars, since the count of blanks as provided by Tesseract is
     * often underestimated.
     * @return the string value of this line
     */
    public String getContent ()
    {
        if (content == null) {
            // Font used for space computation only
            Font          font = TextInfo.basicFont.deriveFont(
                (float) getFontSize());

            // Retrieve half standard space width with this font
            double        halfSpace = TextInfo.computeWidth(" ", font); // / 2;

            // Abscissa of right side of previous char
            int           lastRight = 0;

            // Line content so far
            StringBuilder sb = new StringBuilder();

            // Loop on char descriptions
            for (CharDesc ch : chars) {
                Rectangle box = ch.getBox();

                // Do we need to insert spaces?
                if (ch.hasSpacesBefore()) {
                    StringBuilder spaces = new StringBuilder();
                    spaces.append(" "); // At least one!

                    // Add all spaces needed to insert char at target location
                    double gap = ch.getBox().x - lastRight - halfSpace;

                    while (TextInfo.computeWidth(spaces.toString(), font) < gap) {
                        spaces.append(" ");
                    }

                    sb.append(spaces);
                }

                sb.append(ch.content);
                lastRight = box.x + box.width;
            }

            content = sb.toString();
        }

        return content;
    }

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the font size measured for this line
     * @return the measured font size
     */
    public Integer getFontSize ()
    {
        if (fontSize == null) {
            fontSize = computeFontSize();
        }

        return fontSize;
    }

    //------------//
    // getOcrLine //
    //------------//
    public OcrLine getOcrLine ()
    {
        return new OcrLine(getOcrFontSize(), getContent(), this);
    }

    //---------//
    // addChar //
    //---------//
    public void addChar (CharDesc charDesc)
    {
        chars.add(charDesc);

        // Invalidate content
        content = null;
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        for (CharDesc ch : chars) {
            System.out.println(ch.toString());
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "{LineDesc " + "font:" + getFontSize() + " \"" + getContent() +
               "\"}";
    }

    //----------------//
    // getOcrFontSize //
    //----------------//
    private int getOcrFontSize ()
    {
        return chars.get(0).fontSize;
    }

    //-----------------//
    // computeFontSize //
    //-----------------//
    /**
     * Compute the font size using the whole set of chars (w/o spaces)
     * @return the computed font size
     */
    private int computeFontSize ()
    {
        StringBuilder sb = new StringBuilder();
        int           width = 0;

        // Here we assume that the boxes can be put side by side
        // and correspond to the way characters are measured by getStringBounds
        for (CharDesc ch : chars) {
            width += ch.getBox().width;
            sb.append(ch.content);
        }

        return (int) Math.rint(TextInfo.computeFontSize(sb.toString(), width));
    }
}
