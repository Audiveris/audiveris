//----------------------------------------------------------------------------//
//                                                                            //
//                              T e x t F o n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.log.Logger;

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
    private static final Logger logger = Logger.getLogger(TextFont.class);

    /** Name of the chosen underlying text font */
    private static final String fontName = constants.textFontName.getValue();

    /** The base font used for text entities */
    public static final TextFont baseTextFont = new TextFont(
        constants.textFontSize.getValue());

    /** (So far empirical) ratio between width and point values */
    public static final float FONT_WIDTH_POINT_RATIO = 4.4f;

    /** Ratio from a 300 DPI scan to font point-size (72 pt/inch) */
    public static final float TO_POINT = 72f / 300f;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // TextFont //
    //----------//
    /**
     * Creates a new TextFont object.
     *
     * @param sizePts the point size of the {@code Font}
     */
    public TextFont (int sizePts)
    {
        super(fontName, Font.PLAIN, sizePts);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // computeFontSize //
    //-----------------//
    /**
     * Convenient method to compute a font size using a string content
     * and width.
     * @param content the string value
     * @param width the string width in pixels
     * @return the computed font size
     */
    public static Float computeFontSize (String content,
                                         int    width)
    {
        if (content == null) {
            return null;
        }

        GlyphVector glyphVector = baseTextFont.createGlyphVector(frc, content);
        Rectangle2D basicRect = glyphVector.getVisualBounds();

        return baseTextFont.getSize2D() * (width / (float) basicRect.getWidth()); // * TO_POINT;

        //        Font        font = baseTextFont.deriveFont(
        //            baseTextFont.getSize2D() * (width / (float) basicRect.getWidth()));
        //
        //        if (logger.isFineEnabled()) {
        //            GlyphVector newVector = font.createGlyphVector(frc, content);
        //            Rectangle2D rect = newVector.getVisualBounds();
        //            logger.warning(
        //                "TextInfo " + content + " width:" + width +
        //                " basicRect.width:" + basicRect.getWidth() + " rect.width:" +
        //                rect.getWidth() + " fontSize:" + (font.getSize2D() * TO_POINT));
        //        }
        //
        //        return font.getSize2D() * TO_POINT;
    }

    //--------------//
    // computeWidth //
    //--------------//
    /**
     * Convenient method to report the width of a string in a given font
     * @param content the string value
     * @param font the provided font
     * @return the computed width
     */
    public static double computeWidth (String content,
                                       Font   font)
    {
        return font.getStringBounds(content, frc)
                   .getWidth() * FONT_WIDTH_POINT_RATIO;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.String  textFontName = new Constant.String(
            "Times New Roman", //"Serif" or "Sans Serif",
            "Standard font name for texts");
        Constant.Integer textFontSize = new Constant.Integer(
            "points",
            10,
            "Standard font point size for texts");
    }
}
