//----------------------------------------------------------------------------//
//                                                                            //
//                              L i n e D e s c                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.log.Logger;

import java.awt.Font;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>LineDesc</code> handles information about a line (composed of
 * CharDesc instances) as decoded by an OCR such as Tesseract dlltest
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
    private Float fontSize;

    /** The string value of this line, with chars precisely positioned */
    private String content;

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
        this.addChar(charDesc);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getFontSize //
    //-------------//
    /**
     * Report the font size measured for this line
     * @return the measured font size
     */
    public Float getFontSize ()
    {
        if (fontSize == null) {
            fontSize = computeFontSize();
        }

        return fontSize;
    }

    //------------//
    // getOcrLine //
    //------------//
    public OCR.OcrLine getOcrLine ()
    {
        Rectangle firstBox = chars.get(0)
                                  .getBox();
        Rectangle lastBox = chars.get(chars.size() - 1)
                                 .getBox();
        int       realWidth = (lastBox.x + lastBox.width) - firstBox.x - 1;
        float     size = TextInfo.computeFontSize(toString(), realWidth);

        return new OCR.OcrLine(size, toString());
    }

    //---------//
    // addChar //
    //---------//
    public void addChar (CharDesc charDesc)
    {
        chars.add(charDesc);

        // Invalidate content
        fontSize = null;
        content = null;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report the string value of the line, using a smart positioning of the
     * various chars. This would work best with fixed-size font, of course...
     * @return the string value of this line
     */
    @Override
    public String toString ()
    {
        if (content == null) {
            // Allocate suitable empty output buffer
            Font          font = TextInfo.basicFont.deriveFont(getFontSize());

            // Retrieve half standard space width with this font
            double        halfSpace = TextInfo.computeWidth(" ", font) / 2;
            Rectangle     firstBox = chars.get(0)
                                          .getBox();
            StringBuilder sb = new StringBuilder();

            // Loop on char descriptions
            for (CharDesc ch : chars) {
                // afterSpace indicates that at least one space is to be
                // inserted before this char
                // '-' or '_' chars may be preceded and followed by spaces
                if (ch.afterSpace) {
                    sb.append(" "); // At least one is needed
                }

                if (ch.afterSpace || ch.isDash() || ch.afterDash) {
                    // Add the spaces needed to insert char at target location
                    double pixelTarget = ch.getBox().x - firstBox.x -
                                         halfSpace;

                    while (TextInfo.computeWidth(sb.toString(), font) < pixelTarget) {
                        sb.append(" ");
                    }
                }

                sb.append(ch.toString().charAt(0));
            }

            content = sb.toString()
                        .trim();
        }

        return content;
    }

    //-----------------//
    // computeFontSize //
    //-----------------//
    /**
     * Compute the font size using the whole set of chars (w/o spaces)
     * @return the computed font size
     */
    private float computeFontSize ()
    {
        StringBuilder sb = new StringBuilder();
        int           width = 0;

        // Here we assume that the boxes can be put side by side
        // and correspond to the way characters are measured by getStringBounds
        for (CharDesc charDesc : chars) {
            width += charDesc.getBox().width;
            sb.append(charDesc.toString());
        }

        return TextInfo.computeFontSize(sb.toString(), width);
    }
}
