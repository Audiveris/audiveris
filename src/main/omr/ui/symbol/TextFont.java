//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t F o n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.text.FontInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TextFont} is meant to simplify the use of text font for
 * pieces of text such as titles, directions, lyrics, etc.
 *
 * @author Hervé Bitteur
 */
public class TextFont
        extends OmrFont
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            TextFont.class);

    /** Name of the chosen underlying text font */
    private static final String fontName = constants.defaultTextFontName.getValue();

    /** The base font used for text entities */
    public static final TextFont baseTextFont = new TextFont(
            constants.defaultTextFontSize.getValue());

    /** Ratio from a 300 DPI scan to font point-size (72 pt/inch) */
    public static final float TO_POINT = 72f / 300f;

    //~ Constructors -----------------------------------------------------------
    //
    //----------//
    // TextFont //
    //----------//
    /**
     * Creates a new TextFont object.
     *
     * @param fontName the font name. This can be a font face name or a font
     *                 family name, and may represent either a logical font or
     *                 a physical font found in this {@code GraphicsEnvironment}.
     * @param style    bit-mask style constant for the {@code Font}
     * @param size     the point size of the {@code Font}
     */
    public TextFont (String fontName,
                     int style,
                     int size)
    {
        super(fontName, style, size);
    }

    //----------//
    // TextFont //
    //----------//
    /**
     * Creates a font based on OCR font information.
     *
     * @param info OCR-based font information
     */
    public TextFont (FontInfo info)
    {
        this(
                info.isSerif ? Font.SERIF
                : (info.isMonospace ? Font.MONOSPACED : Font.SANS_SERIF),
                (info.isBold ? Font.BOLD : 0) | (info.isItalic ? Font.ITALIC : 0),
                info.pointsize);
    }

    //----------//
    // TextFont //
    //----------//
    /**
     * Creates a new TextFont object.
     *
     * @param size the point size of the {@code Font}
     */
    public TextFont (int size)
    {
        super(fontName, Font.PLAIN, size);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-----------------//
    // computeFontSize //
    //-----------------//
    /**
     * Convenient method to compute a font size using a string content
     * and dimension.
     *
     * @param content  the string value
     * @param fontInfo OCR-based font information
     * @param dim      string dimension in pixels
     * @return the computed font size
     */
    public static Float computeFontSize (String content,
                                         FontInfo fontInfo,
                                         Dimension dim)
    {
        if (content == null) {
            return null;
        }

        Font font = new TextFont(fontInfo);
        float fontSize = font.getSize2D();
        GlyphVector glyphVector = font.createGlyphVector(frc, content);
        Rectangle2D basicRect = glyphVector.getVisualBounds();

        if (dim.width >= dim.height) {
            return fontSize * (dim.width / (float) basicRect.getWidth());
        } else {
            return fontSize * (dim.height / (float) basicRect.getHeight());
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String defaultTextFontName = new Constant.String(
                "Serif",
                "Default font name for texts");

        Constant.Integer defaultTextFontSize = new Constant.Integer(
                "points",
                10,
                "Default font point size for texts");

    }
}
