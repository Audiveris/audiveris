//----------------------------------------------------------------------------//
//                                                                            //
//                              L i n e D e s c                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.log.Logger;

import omr.score.entity.Text;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
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

    // A VERIFIER
    private static FontRenderContext frc = new FontRenderContext(
        null,
        false,
        false);

    //~ Instance fields --------------------------------------------------------

    /** Chars that compose this line */
    private final List<CharDesc> chars = new ArrayList<CharDesc>();

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

    //---------//
    // addChar //
    //---------//
    public void addChar (CharDesc charDesc)
    {
        chars.add(charDesc);

        // Invalidate content
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
            Font          font = Text.getLyricsFont()
                                     .deriveFont(computeFontSize());

            // Retrieve half standard space width with this font
            double        halfSpace = font.getStringBounds(" ", frc)
                                          .getWidth() / 2;

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
                    int pixelTarget = ch.getBox().x - firstBox.x;

                    while (font.getStringBounds(sb.toString(), frc)
                               .getWidth() < (pixelTarget - halfSpace)) {
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
     * Compute the font size using the whole set of chars
     * @return the computed font size
     */
    private float computeFontSize ()
    {
        StringBuilder sb = new StringBuilder();
        int           pixelCount = 0;

        for (CharDesc charDesc : chars) {
            pixelCount += charDesc.getBox().width;
            sb.append(charDesc.toString());
        }

        Font        font = Text.getLyricsFont();
        Rectangle2D rect = font.getStringBounds(sb.toString(), frc);
        float       size = 1.0f * (float) ((pixelCount * font.getSize2D()) / rect.getWidth());

        return size;
    }

    //----------------------//
    // computeMeanCharWidth //
    //----------------------//
    /**
     * Compute the average width (in pixels) of a char among all line chars.
     * @return the mean char width
     */
    private double computeMeanCharWidth ()
    {
        int pixelCount = 0;
        int charCount = 0;

        for (CharDesc charDesc : chars) {
            pixelCount += charDesc.getBox().width;
            charCount += charDesc.toString()
                                 .length();
        }

        return (double) pixelCount / (double) charCount;
    }
}
