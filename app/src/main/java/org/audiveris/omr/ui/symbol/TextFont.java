//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t F o n t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;

/**
 * Class <code>TextFont</code> is meant to simplify the use of text font for pieces of text
 * such as titles, directions, lyrics, as well as symbols for fingering, plucking, frets.
 *
 * @author Hervé Bitteur
 */
public class TextFont
        extends OmrFont
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TextFont.class);

    /** Ratio from a 300 DPI scan to font point-size (72 pt/inch). */
    public static final float TO_POINT = 72f / 300f;

    /** Name of the chosen underlying text font. */
    private static final String TEXT_FONT_NAME = constants.defaultTextFontName.getValue();

    /** The base font used for text entities. */
    public static final TextFont TEXT_FONT_BASE = new TextFont(
            constants.defaultTextFontSize.getValue());

    /** Default text font family. */
    public static final Param<TextFamily> defaultTextParam = new ConstantBasedParam<>(
            constants.defaultTextFamily,
            Param.GLOBAL_SCOPE);

    //~ Constructors -------------------------------------------------------------------------------

    public TextFont (Font font)
    {
        super(font);
    }

    /**
     * Creates a new <code>TextFont</code> based on OCR-based font information.
     *
     * @param info OCR-based font information
     */
    public TextFont (FontInfo info)
    {
        this(
                info.isSerif ? Font.SERIF : (info.isMonospace ? Font.MONOSPACED : Font.SANS_SERIF),
                null,
                (info.isBold ? Font.BOLD : 0) | (info.isItalic ? Font.ITALIC : 0),
                info.pointsize);
    }

    /**
     * Creates a new <code>TextFont</code> object of provided point size, with default font name
     * and plain style.
     *
     * @param size the point size of the <code>Font</code>
     */
    public TextFont (int size)
    {
        this(TEXT_FONT_NAME, null, Font.PLAIN, size);
    }

    /**
     * Creates a new <code>TextFont</code> object.
     *
     * @param fontName the font name. This can be a font face name or a font
     *                 family name, and may represent either a logical font or
     *                 a physical font found in this <code>GraphicsEnvironment</code>.
     * @param fileName the file name if any
     * @param style    bit-mask style constant for the <code>Font</code>
     * @param size     the point size of the <code>Font</code>
     */
    public TextFont (String fontName,
                     String fileName,
                     int style,
                     int size)
    {
        super(fontName, fileName, style, size);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------//
    // deriveFont //
    //------------//
    @Override
    public TextFont deriveFont (float pointSize)
    {
        return new TextFont(super.deriveFont(pointSize));
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // create //
    //--------//
    /**
     * Creates a TextFont based on OCR font information.
     *
     * @param baseFont base text font
     * @param info     OCR-based font information
     * @return the derived TextFont
     */
    public static TextFont create (TextFont baseFont,
                                   FontInfo info)
    {
        final int style = (info.isBold ? Font.BOLD : 0) | (info.isItalic ? Font.ITALIC : 0);
        final float size = info.pointsize;
        final Font font = baseFont.deriveFont(style, size);
        return new TextFont(font);
    }

    //-------------//
    // getBaseFont //
    //-------------//
    /**
     * Report the (perhaps cached) text font based on chosen family and staff interline.
     *
     * @param family         chosen family font
     * @param staffInterline real interline value for related staves
     * @return proper scaled music font
     */
    public static TextFont getBaseFont (TextFamily family,
                                        int staffInterline)
    {
        return getTextFont(family, MusicFont.getPointSize(staffInterline));
    }

    //-------------------//
    // getBaseFontBySize //
    //-------------------//
    /**
     * Report the (perhaps cached) text font based on chosen family and point size.
     *
     * @param family    chosen family font
     * @param pointSize desired font point size
     * @return proper scaled music font
     */
    public static TextFont getBaseFontBySize (TextFamily family,
                                              int pointSize)
    {
        return getTextFont(family, pointSize);
    }

    //------------------//
    // getCurrentFamily //
    //------------------//
    /**
     * Report the text family used in the sheet currently displayed.
     *
     * @return the current sheet text family, null if no sheet is displayed
     */
    public static TextFamily getCurrentFamily ()
    {
        final SheetStub stub = StubsController.getInstance().getSelectedStub();
        return (stub != null) ? stub.getTextFamily() : null;
    }

    //-------------//
    // getTextFont //
    //-------------//
    public static TextFont getTextFont (TextFamily family,
                                        int size)
    {
        Font font = getFont(family.getFontName(), family.getFileName(), Font.PLAIN, size);

        if (!(font instanceof TextFont)) {
            font = new TextFont(font);
            cacheFont(font);
        }

        return (TextFont) font;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Enum<TextFamily> defaultTextFamily = new Constant.Enum<>(
                TextFamily.class,
                TextFamily.SansSerif,
                "Default font family for text");

        private final Constant.String defaultTextFontName = new Constant.String(
                "Sans Serif",
                "Default font name for texts");

        private final Constant.Integer defaultTextFontSize = new Constant.Integer(
                "points",
                10,
                "Default font point size for texts");
    }
}
